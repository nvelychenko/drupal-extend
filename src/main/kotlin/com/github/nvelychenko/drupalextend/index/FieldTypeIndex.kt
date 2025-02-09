package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.extensions.findVariablesByName
import com.github.nvelychenko.drupalextend.extensions.getModificationTrackerForIndexId
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.extensions.isValidForIndex
import com.github.nvelychenko.drupalextend.index.types.DrupalFieldType
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.util.getPhpDocParameter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil.unquoteString
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.impl.MethodReferenceImpl
import java.io.DataInput
import java.io.DataOutput

class FieldTypeIndex : FileBasedIndexExtension<String, DrupalFieldType>() {
    private val myKeyDescriptor: KeyDescriptor<String> = EnumeratorStringDescriptor()

    private val myDataExternalizer: DataExternalizer<DrupalFieldType> =
        object : DataExternalizer<DrupalFieldType> {
            override fun save(out: DataOutput, value: DrupalFieldType) {
                out.writeUTF(value.fieldTypeId)
                out.writeUTF(value.fqn)
                out.writeUTF(value.listClassFqn)
                out.writeInt(value.properties.size)
                for (key in value.properties) {
                    out.writeUTF(key.key)
                    out.writeUTF(key.value)
                }
            }

            override fun read(input: DataInput): DrupalFieldType {
                val entityType = input.readUTF()
                val fqn = input.readUTF()
                val listClassFqn = input.readUTF()

                val properties = hashMapOf<String, String>()
                for (i in 1..input.readInt()) {
                    properties[input.readUTF()] = input.readUTF()
                }

                return DrupalFieldType(entityType, fqn, listClassFqn, properties)
            }
        }

    override fun getName(): ID<String, DrupalFieldType> {
        return KEY
    }

    override fun getIndexer(): DataIndexer<String, DrupalFieldType, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, DrupalFieldType>()

            val phpFile = inputData.psiFile as PhpFile
            if (!inputData.project.drupalExtendSettings.isEnabled) return@DataIndexer map
            if (!inputData.isValidForIndex()) return@DataIndexer map

            val phpClass = PsiTreeUtil.findChildOfType(phpFile, PhpClass::class.java) ?: return@DataIndexer map

            val docComment = phpClass.docComment

            var listClass = DUMMY_LIST_CLASS
            var hasEntityTypeAnnotation = false

            var id = phpClass.attributes.find { it.fqn == "\\Drupal\\Core\\Field\\Attribute\\FieldType" }
                ?.arguments?.find { it.name == "id" }?.argument?.value?.let {
                    hasEntityTypeAnnotation = true
                    unquoteString(it)
                }

            // I beg you to refactor me
            if (id == null) {
                id = if (docComment is PhpDocComment) {
                    val fieldTypeDocs = docComment.getTagElementsByName("@FieldType")
                    if (fieldTypeDocs.isEmpty()) {
                        listClass = DUMMY_LIST_CLASS
                        phpClass.fqn
                    } else {
                        hasEntityTypeAnnotation = true

                        val fieldTypeDoc = fieldTypeDocs[0].text
                        listClass = getPhpDocParameter(fieldTypeDoc, "list_class") ?: DUMMY_LIST_CLASS
                        getPhpDocParameter(fieldTypeDoc, "id") ?: phpClass.fqn
                    }
                } else {
                    phpClass.fqn
                }
            }

            val propertyDefinitionsMethod = phpClass.findOwnMethodByName("propertyDefinitions")

            if (propertyDefinitionsMethod == null && hasEntityTypeAnnotation) {
                map[id] = DrupalFieldType(id, phpClass.fqn, listClass, HashMap())
                return@DataIndexer map
            }

            propertyDefinitionsMethod ?: return@DataIndexer map

            if (!propertyDefinitionsMethod.isStatic) return@DataIndexer map
            val parameters = propertyDefinitionsMethod.parameters
            if (parameters.isEmpty()) return@DataIndexer map

            parameters.first().type.filterUnknown().types.forEach {
                if (it.toString() != "\\Drupal\\Core\\Field\\FieldStorageDefinitionInterface")
                    return@DataIndexer map
            }

            map[id] = DrupalFieldType(id, phpClass.fqn, listClass, processMethod(propertyDefinitionsMethod))

            map
        }
    }

    private fun processMethod(method: Method): HashMap<String, String> {
        val fieldProperties = HashMap<String, String>()

        val returnType = PsiTreeUtil.findChildOfType(method, PhpReturn::class.java)?.firstPsiChild
        // @todo Implement ability to process ArrayCreationExpression
        if (returnType !is Variable) {
            return fieldProperties
        }
        for (variable in method.findVariablesByName(returnType.name)) {
            val assignment = PhpPsiUtil.getParentOfClass(variable, AssignmentExpression::class.java) ?: continue

            val assignedMethod =
                PsiTreeUtil.findChildOfType(assignment, ClassReference::class.java)?.parent ?: continue

            if (assignedMethod !is MethodReferenceImpl) continue
            val parameters = assignedMethod.parameters
            if (parameters.isEmpty()) continue
            val firstParameter = parameters.first()
            if (firstParameter !is StringLiteralExpression) continue

            val propertyType = firstParameter.contents

            val index = PsiTreeUtil.findChildOfType(assignment, ArrayIndex::class.java) ?: continue

            val fieldName = when (val indexValue = index.value) {
                is StringLiteralExpression -> indexValue.contents
                else -> null
            } ?: continue

            fieldProperties[fieldName] = propertyType
        }

        return fieldProperties
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = myKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<DrupalFieldType> = myDataExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file: VirtualFile -> file.fileType == PhpFileType.INSTANCE }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 3

    companion object {
        val index: FileBasedIndex by lazy {
            FileBasedIndex.getInstance()
        }

        val KEY = ID.create<String, DrupalFieldType>("com.github.nvelychenko.drupalextend.index.field_type")
        const val DUMMY_LIST_CLASS = "Dummy"

        @Synchronized
        fun getAllFqns(project: Project): HashMap<String, DrupalFieldType> {
            return CachedValuesManager.getManager(project).getCachedValue(project) {
                val results = hashMapOf<String, DrupalFieldType>()

                index.getAllKeys(KEY, project).forEach {
                    index.getValue(KEY, it, project)?.let { contentEntity ->
                        results[contentEntity.fqn] = contentEntity
                    }
                }

                CachedValueProvider.Result.create(results, getModificationTrackerForIndexId(project, KEY, index))
            }
        }
    }


}