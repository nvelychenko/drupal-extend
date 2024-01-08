package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.extensions.findVariablesByName
import com.github.nvelychenko.drupalextend.index.types.DrupalField
import com.github.nvelychenko.drupalextend.value.ExtendableContentEntityRelatedClasses
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
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
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import java.io.DataInput
import java.io.DataOutput

class ConfigSchemaIndex : FileBasedIndexExtension<String, DrupalField>() {
    private val myKeyDescriptor: KeyDescriptor<String> = EnumeratorStringDescriptor()

    private val myDataExternalizer: DataExternalizer<DrupalField> =
        object : DataExternalizer<DrupalField> {
            override fun save(out: DataOutput, value: DrupalField) {
                out.writeUTF(value.entityType)
                out.writeUTF(value.fieldType)
                out.writeUTF(value.fieldName)
            }

            override fun read(input: DataInput): DrupalField {
                return DrupalField(input.readUTF(), input.readUTF(), input.readUTF())
            }
        }

    override fun getName(): ID<String, DrupalField> {
        return KEY
    }

    override fun getIndexer(): DataIndexer<String, DrupalField, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, DrupalField>()
            val psiFile = inputData.psiFile

            when (psiFile) {
                is YAMLFile -> processYml(map, psiFile)
                is PhpFile -> processPhp(map, psiFile)
                else -> map
            }
        }
    }

    private fun processYml(map: HashMap<String, DrupalField>, psiFile: PsiFile): HashMap<String, DrupalField> {
        if (!psiFile.name.startsWith("field.storage.")) {
            return map
        }

        PsiTreeUtil.getChildOfType(psiFile, YAMLDocument::class.java)?.topLevelValue?.children?.forEach { node ->
            if (node is YAMLKeyValue && node.keyText == "type") {
                val identifier = psiFile.name.substringAfter("field.storage.").replace(".yml", "")
                val entityType = identifier.substringBeforeLast(".")
                val fieldName = identifier.substringAfterLast(".")
                map["${entityType}|${fieldName}"] = DrupalField(entityType, node.valueText, fieldName)
            }
        }

        return map
    }

    private fun processPhp(map: HashMap<String, DrupalField>, phpFile: PhpFile): HashMap<String, DrupalField> {
        val phpClass = PsiTreeUtil.findChildOfType(phpFile, PhpClass::class.java) ?: return map

        if (!isValidPhpClass(phpClass)) {
            return map
        }

        val entityTypeId = getEntityTypeName(phpClass) ?: return map

        val methodName =
            ExtendableContentEntityRelatedClasses.getClass(phpClass.fqn)?.methodName ?: "baseFieldDefinitions"

        val fields = phpClass.findOwnMethodByName(methodName)?.let { processMethod(it) } ?: HashMap()

        for (field in fields) {
            map["${entityTypeId}|${field.key}"] = DrupalField(entityTypeId, field.key, field.value)
            map["${entityTypeId}|${field.key}"] = DrupalField(entityTypeId, field.key, field.value)
            field.key
        }

        return map
    }

    private fun isValidPhpClass(phpClass: PhpClass): Boolean {
        if (phpClass.isInterface) {
            return false
        }

        val docComment = phpClass.docComment
        if (docComment !is PhpDocComment) {
            return ExtendableContentEntityRelatedClasses.hasClass(phpClass.fqn)
        }

        return if (docComment.getTagElementsByName("@ContentEntityType").isEmpty()) {
            return ExtendableContentEntityRelatedClasses.hasClass(phpClass.fqn)
        } else {
            true
        }
    }

    private fun getEntityTypeName(phpClass: PhpClass): String? {
        val docComment = phpClass.docComment
        if (docComment !is PhpDocComment) {
            return phpClass.fqn
        }

        val contentType = docComment.getTagElementsByName("@ContentEntityType")
        if (contentType.isEmpty()) {
            return phpClass.fqn
        }

        val contentTypeDoc = contentType[0].text

        return getPhpDocParameter(contentTypeDoc, "id")
    }

    private fun processMethod(method: Method): HashMap<String, String> {
        val fieldsDefinitions = HashMap<String, String>()

        val returnType = PsiTreeUtil.findChildOfType(method, PhpReturn::class.java)?.firstPsiChild
        if (returnType !is Variable && returnType !is ArrayCreationExpression) {
            return fieldsDefinitions
        }

        if (returnType is Variable) {
            for (variable in method.findVariablesByName(returnType.name)) {
                val assignment = PhpPsiUtil.getParentOfClass(variable, AssignmentExpression::class.java) ?: continue

                val assignedMethod = PsiTreeUtil.findChildOfType(assignment, ClassReference::class.java)?.parent

                if (assignedMethod is MethodReferenceImpl && assignedMethod.parameters.isNotEmpty() && assignedMethod.parameters[0]?.text != null) {
                    val fieldType = assignedMethod.parameters[0].text
                    val index = PsiTreeUtil.findChildOfType(assignment, ArrayIndex::class.java)

                    val fieldName = when (val indexValue = index?.value) {
                        is StringLiteralExpression -> indexValue.contents
                        is MethodReference -> {
                            // Jesus?
                            if ((index.name == "getKey" || index.name == "getRevisionKey" || index.name == "getRevisionMetadataKey") && indexValue.parameters[0] is StringLiteralExpression) {
                                "KEY|${(indexValue.parameters[0] as StringLiteralExpression).contents}"
                            } else {
                                null
                            }
                        }

                        else -> null
                    } ?: continue

                    fieldsDefinitions[fieldName] = fieldType
                    continue
                }

                val assignmentType = assignment.variable
                if (assignmentType is MethodReferenceImpl && assignmentType.isStatic) {
                    assignmentType.resolve()
                }

            }
        }

        return fieldsDefinitions
    }

    private fun getPhpDocParameter(phpDocText: String, id: String): String? {
        val entityTypeMatch = Regex("${id}\\s*=\\s*\"([^\"]+)\"").find(phpDocText)

        return entityTypeMatch?.groups?.get(1)?.value
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = myKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<DrupalField> = myDataExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file: VirtualFile -> file.fileType == YAMLFileType.YML || file.fileType == PhpFileType.INSTANCE }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 0

    companion object {
        val KEY = ID.create<String, DrupalField>("com.github.nvelychenko.drupalextend.index.base_field")
    }

}