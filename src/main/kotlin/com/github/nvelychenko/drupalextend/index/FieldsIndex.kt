package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.data.ExtendableContentEntityRelatedClasses
import com.github.nvelychenko.drupalextend.extensions.findVariablesByName
import com.github.nvelychenko.drupalextend.extensions.isInConfigurationDirectory
import com.github.nvelychenko.drupalextend.extensions.isValidForIndex
import com.github.nvelychenko.drupalextend.index.dataExternalizer.SerializedObjectDataExternalizer
import com.github.nvelychenko.drupalextend.index.types.DrupalField
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.util.getPhpDocParameter
import com.github.nvelychenko.drupalextend.util.yml.StringFinderInContext
import com.github.nvelychenko.drupalextend.util.yml.YAMLKeyValueFinder
import com.intellij.openapi.util.text.StringUtil.unquoteString
import com.intellij.patterns.PlatformPatterns
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
import kotlinx.serialization.serializer
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLFile

class FieldsIndex : FileBasedIndexExtension<String, DrupalField>() {
    private val myKeyDescriptor: KeyDescriptor<String> = EnumeratorStringDescriptor()

    private val myDataExternalizer: DataExternalizer<DrupalField> =
            SerializedObjectDataExternalizer(serializer<DrupalField>())

    override fun getName(): ID<String, DrupalField> {
        return KEY
    }

    override fun getIndexer(): DataIndexer<String, DrupalField, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, DrupalField>()

            if (!inputData.project.drupalExtendSettings.isEnabled) return@DataIndexer map

            val psiFile = inputData.psiFile

            if (!inputData.isValidForIndex()) {
                return@DataIndexer map
            }

            when (psiFile) {
                is YAMLFile -> {
                    if (inputData.isInConfigurationDirectory()) {
                        processYml(map, psiFile)
                    }
                }

                is PhpFile -> processPhp(map, psiFile)
            }

            map
        }
    }

    private fun processYml(map: HashMap<String, DrupalField>, psiFile: PsiFile) {
        if (!psiFile.name.startsWith("field.storage.")) {
            return
        }

        val entityType = YAMLKeyValueFinder("entity_type").findIn(psiFile)?.value?.text ?: return
        val fieldName = YAMLKeyValueFinder("field_name").findIn(psiFile)?.value?.text ?: return
        val type = YAMLKeyValueFinder("type").findIn(psiFile)?.value?.text?: return
        val targetType = YAMLKeyValueFinder("settings.target_type").findIn(psiFile)?.value?.text

        map["${entityType}|${fieldName}"] = DrupalField(entityType, type, fieldName, "field_name", targetType)
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
                map["${entityTypeId}|${it.key}"] = DrupalField(entityTypeId, it.value, it.key, it.path ?: methName, it.targetType)
            }
    }

    /**
     * @sample
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
            var targetType: String? = null
            if (fieldType == "entity_reference" && hashElement.value != null) {
                targetType = getBaseFieldReferenceTargetType(hashElement.value!!)
            }

            fieldsDefinitions.add(
                FieldRepresentation(
                    fieldId,
                    fieldType,
                    null,
                    targetType
                ),
            )
        }

        return fieldsDefinitions
    }

    /**
     * @sample
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
            var targetType: String? = null
            if (fieldType == "entity_reference" && assignment.value != null) {
                targetType = getBaseFieldReferenceTargetType(assignment.value!!)
            }

            fieldsDefinitions.add(
                FieldRepresentation(
                    fieldId,
                    fieldType,
                    method.name,
                    targetType
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
     * @sample
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
                        "$GENERAL_BASE_FIELD_KEY_PREFIX$contents"
                    }
            }

            else -> null
        }
    }

    /**
     * @sample
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

    private fun getBaseFieldReferenceTargetType(assignment: PhpPsiElement): String? {
        val stringFinder = StringFinderInContext("target_type", PlatformPatterns.psiElement(StringLiteralExpression::class.java))
        val stringLiteral = stringFinder.findIn(assignment) ?: return null
        val targetType = when (val parent = stringLiteral.parent.parent) {
            is MethodReference -> stringLiteral.nextPsiSibling
            is ArrayHashElement -> parent.value
            else -> return null
        }
        return (targetType as? StringLiteralExpression)?.contents
    }

    private fun isValidPhpClass(phpClass: PhpClass): Boolean {
        if (phpClass.isInterface) {
            return false
        }

        val attribute = phpClass.attributes.find { it.fqn == "\\Drupal\\Core\\Entity\\Attribute\\ContentEntityType" }

        if (attribute != null) {
            return true
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
        val attributeId = phpClass.attributes.find { it.fqn == "\\Drupal\\Core\\Entity\\Attribute\\ContentEntityType" }
            ?.arguments?.find { it.name == "id" }?.argument?.value?.let { unquoteString(it) }

        if (attributeId != null) {
            return attributeId
        }

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

    override fun getVersion(): Int = 12

    companion object {
        val KEY = ID.create<String, DrupalField>("com.github.nvelychenko.drupalextend.index.fields")

        const val GENERAL_BASE_FIELD_KEY_PREFIX = "KEY|"
    }

    private data class FieldRepresentation(val key: String, val value: String, val path: String?, val targetType: String? = null)

}