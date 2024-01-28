package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.data.ExtendableContentEntityRelatedClasses
import com.github.nvelychenko.drupalextend.extensions.findVariablesByName
import com.github.nvelychenko.drupalextend.extensions.isValidForIndex
import com.github.nvelychenko.drupalextend.extensions.keyPath
import com.github.nvelychenko.drupalextend.index.types.DrupalField
import com.github.nvelychenko.drupalextend.util.getPhpDocParameter
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
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
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import java.io.DataInput
import java.io.DataOutput

class FieldsIndex : FileBasedIndexExtension<String, DrupalField>() {
    private val myKeyDescriptor: KeyDescriptor<String> = EnumeratorStringDescriptor()

    private val myDataExternalizer: DataExternalizer<DrupalField> =
        object : DataExternalizer<DrupalField> {
            override fun save(out: DataOutput, value: DrupalField) {
                out.writeUTF(value.entityType)
                out.writeUTF(value.fieldType)
                out.writeUTF(value.fieldName)
                out.writeUTF(value.path)
            }

            override fun read(input: DataInput): DrupalField {
                return DrupalField(input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF())
            }
        }

    override fun getName(): ID<String, DrupalField> {
        return KEY
    }

    override fun getIndexer(): DataIndexer<String, DrupalField, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, DrupalField>()
            val psiFile = inputData.psiFile

            if (!isValidForIndex(inputData)) {
                return@DataIndexer map
            }

            // @todo Improve directory handing
            if (psiFile is YAMLFile) {
                val baseDir = inputData.project.guessProjectDir()

                if (baseDir != null) {
                    val relativePath =
                        VfsUtil.getRelativePath(inputData.file, baseDir, '/') ?: return@DataIndexer map
                    if (!relativePath.contains("config/sync")) {
                        return@DataIndexer map
                    }
                }
            }

            when (psiFile) {
                is YAMLFile -> processYml(map, psiFile)
                is PhpFile -> processPhp(map, psiFile)
            }

            map
        }
    }

    private fun processYml(map: HashMap<String, DrupalField>, psiFile: PsiFile) {
        if (!psiFile.name.startsWith("field.storage.")) {
            return
        }

        PsiTreeUtil.getChildOfType(psiFile, YAMLDocument::class.java)?.topLevelValue?.children?.forEach { node ->
            if (node is YAMLKeyValue && node.keyText == "type") {
                val identifier = psiFile.name.substringAfter("field.storage.").replace(".yml", "")
                val entityType = identifier.substringBeforeLast(".")
                val fieldName = identifier.substringAfterLast(".")
                map["${entityType}|${fieldName}"] = DrupalField(entityType, node.valueText, fieldName, node.keyPath)
            }
        }
    }

    private fun processPhp(map: HashMap<String, DrupalField>, phpFile: PhpFile) {
        val phpClass = PsiTreeUtil.findChildOfType(phpFile, PhpClass::class.java) ?: return

        if (!isValidPhpClass(phpClass)) {
            return
        }

        val entityTypeId = getEntityTypeName(phpClass) ?: return

        val methodName =
            ExtendableContentEntityRelatedClasses.getClass(phpClass.fqn)?.methodName ?: "baseFieldDefinitions"

        val baseFieldDefinitionMethod = phpClass.findOwnMethodByName(methodName) ?: return
        val methName = baseFieldDefinitionMethod.name

        baseFieldDefinitionMethod
            .let { PsiTreeUtil.findChildOfType(it, PhpReturn::class.java)?.firstPsiChild }
            // We are only interested in
            // 1. return $fields
            // 2. return ['field' => BaseFieldDefinition::create('entity_reference')
            .takeIf { it is Variable || it is ArrayCreationExpression }
            ?.let {
                when (it) {
                    is Variable -> processVariables(baseFieldDefinitionMethod, it)
                    is ArrayCreationExpression -> processArrayCreationExpression(it)
                    else -> null
                }
            }?.forEach {
                map["${entityTypeId}|${it.key}"] = DrupalField(entityTypeId, it.value, it.key, it.path ?: methName)
            }
    }

    /**
     * @code
     *
     * return [
     *   'field' => ...
     *   'field2' => ...
     * ];
     */
    private fun processArrayCreationExpression(expression: ArrayCreationExpression): List<FieldRepresentation> {
        val fieldsDefinitions = mutableListOf<FieldRepresentation>()

        expression.hashElements.forEach { hashElement ->

            val fieldId = hashElement.key?.let { getBaseFieldId(it) } ?: return@forEach
            val fieldType = hashElement.value?.let { getBaseFieldType(it) } ?: return@forEach

            fieldsDefinitions.add(
                FieldRepresentation(
                    fieldId,
                    fieldType,
                    null
                ),
            )
        }

        return fieldsDefinitions
    }

    /**
     * @code
     *
     * $variable['field'] = ..
     * $variable['field2'] = ..
     *
     * return $variable;
     */
    private fun processVariables(method: Method, returnType: Variable): ArrayList<FieldRepresentation> {
        val fieldsDefinitions = arrayListOf<FieldRepresentation>()
        method.findVariablesByName(returnType.name).forEach { variable ->
            val assignment = PhpPsiUtil.getParentOfClass(variable, AssignmentExpression::class.java) ?: return@forEach

            val fieldType = getBaseFieldType(assignment) ?: return@forEach
            val index = (assignment.variable as? ArrayAccessExpression)?.index ?: return@forEach
            val fieldId = index.value?.let { getBaseFieldId(it) } ?: return@forEach

            fieldsDefinitions.add(
                FieldRepresentation(
                    fieldId,
                    fieldType,
                    method.name
                ),
            )
            return@forEach

            // @todo Implement handing of static methods.
            //            val assignmentType = assignment.variable
            //            if (assignmentType is MethodReferenceImpl && assignmentType.isStatic) {
            //            }
        }

        return fieldsDefinitions
    }

    /**
     * Gets base field type.
     *
     * @code
     * $entity_type->getKey('owner') => BaseFieldDefinition::create('entity_reference')
     *   ->setLabel(new TranslatableMarkup('User ID'))
     *   ...
     *
     * Here we need to fetch 'owner' from array key
     */
    private fun getBaseFieldId(element: PhpPsiElement): String? {
        return when (element) {
            is StringLiteralExpression -> element.contents
            is MethodReference -> {
                element
                    .takeIf {
                        arrayOf(
                            "getKey",
                            "getRevisionKey",
                            "getRevisionMetadataKey"
                        ).contains(element.name)
                    }
                    ?.parameters
                    ?.filterIsInstance<StringLiteralExpression>()
                    ?.firstOrNull()
                    ?.takeIf { it.contents.isNotEmpty() }
                    ?.let {
                        val contents = it.contents.replace("'", "")
                        "KEY|$contents"
                    }
            }

            else -> null
        }
    }

    /**
     * Gets base field type.
     *
     * @code
     * $entity_type->getKey('owner') => BaseFieldDefinition::create('entity_reference')
     *   ->setLabel(new TranslatableMarkup('User ID'))
     *   ...
     *
     * Here we need to fetch entity_reference from ::create() method.
     */
    private fun getBaseFieldType(parent: PsiElement): String? {
        // First find class reference that should be BaseFieldDefinition::create()
        val assignedMethod = PsiTreeUtil.findChildOfType(parent, ClassReference::class.java)
            ?.parent
            .takeIf { it is MethodReference } as? MethodReference ?: return null

        // Then try to find 'entity_reference' from first parameter.
        return assignedMethod.parameters
            .firstOrNull()
            ?.let { (it as? StringLiteralExpression)?.contents }
            ?.takeIf { it.isNotEmpty() }
            ?.replace("'", "")
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

    override fun getKeyDescriptor(): KeyDescriptor<String> = myKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<DrupalField> = myDataExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file -> file.fileType == YAMLFileType.YML || file.fileType == PhpFileType.INSTANCE }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 11

    companion object {
        val KEY = ID.create<String, DrupalField>("com.github.nvelychenko.drupalextend.index.fields")
    }

    private data class FieldRepresentation(val key: String, val value: String, val path: String?)

}