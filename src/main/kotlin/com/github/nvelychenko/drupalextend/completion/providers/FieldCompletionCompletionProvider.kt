package com.github.nvelychenko.drupalextend.completion.providers

import com.github.nvelychenko.drupalextend.data.ExtendableContentEntityRelatedClasses
import com.github.nvelychenko.drupalextend.extensions.getAllProjectKeys
import com.github.nvelychenko.drupalextend.extensions.getAllValuesWithKeyPrefix
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.ContentEntityFqnIndex
import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.github.nvelychenko.drupalextend.index.FieldsIndex
import com.github.nvelychenko.drupalextend.index.FieldsIndex.Companion.GENERAL_BASE_FIELD_KEY_PREFIX
import com.github.nvelychenko.drupalextend.index.types.DrupalContentEntity
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.type.EntityStorageTypeProvider.Util.SPLIT_KEY
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*

/**
 * Provides autocompletion for fields.
 *
 * $node->get('field_|
 * $node->set('field_|
 * $node->hasField('field_|
 */
open class FieldCompletionCompletionProvider : CompletionProvider<CompletionParameters>() {
    val fileBasedIndex: FileBasedIndex by lazy {
        FileBasedIndex.getInstance()
    }

    protected open val methodsToAutocomplete = arrayOf("get", "set", "hasField")

    open val priority = 10.0

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = (parameters.originalPosition ?: return).parent as StringLiteralExpression

        if (!element.project.drupalExtendSettings.isEnabled) return

        val parameterList = element.parent as ParameterList

        if (!parameterList.parameters.first().isEquivalentTo(element)) return

        val methodReference = parameterList.parent as MethodReference

        if (!methodsToAutocomplete.contains(methodReference.name)) return

        processMemberReference(methodReference, result)
    }

    open fun processMemberReference(memberReference: MemberReference, result: CompletionResultSet) {
        when (val classReference = memberReference.classReference) {
            is PhpReference -> getContentEntityFromReferenceAndBuildAutocomplete(classReference, result)
            is ArrayAccessExpression -> (classReference.value as? PhpReference)?.let {
                getContentEntityFromReferenceAndBuildAutocomplete(it, result, true)
            }
        }
    }

    fun getContentEntityFromReferenceAndBuildAutocomplete(
        classReference: PhpReference,
        result: CompletionResultSet,
        isArrayAccessExpression: Boolean = false
    ) {
        val project = classReference.project

        // ECK Require special handing.
        val signature = classReference.signature
        if (signature.contains(SPLIT_KEY)) {
            val entityTypeId = signature.substringAfter(SPLIT_KEY).substringBefore('.')
            val contentEntity = fileBasedIndex.getValue(ContentEntityIndex.KEY, entityTypeId, project) ?: return

            if (contentEntity.isEck) {
                buildResultForEntity(contentEntity, project, result)
                return
            }
        }


        val globalTypes = classReference.globalType.types.map {
            // Situation is following, there is array of nodes. Node[]
            // Only in case if classReference is taken from ArrayAccessExpression
            // e.g. $nodes[0]->field we want to remove "[]" from type. Otherwise,
            // autocomplete will work for $nodes->field too.
            it.takeIf { isArrayAccessExpression }?.replace("[]", "") ?: it
        }.takeIf { it.isNotEmpty() } ?: return

        val contentEntityFqn = fileBasedIndex.getAllProjectKeys(ContentEntityFqnIndex.KEY, project)
            .find { globalTypes.contains(it) }
            ?.let { fileBasedIndex.getValue(ContentEntityFqnIndex.KEY, it, project) } ?: return

        val contentEntity =
            fileBasedIndex.getValue(ContentEntityIndex.KEY, contentEntityFqn.entityTypeId, project) ?: return

        buildResultForEntity(contentEntity, project, result)
    }

    protected fun buildResultForEntity(
        contentEntity: DrupalContentEntity,
        project: Project,
        result: CompletionResultSet
    ) {
        val entityTypeId = contentEntity.entityTypeId
        val keys =
            fileBasedIndex.getValue(ContentEntityIndex.KEY, entityTypeId, project)?.keys?.toMutableMap() ?: return

        // General fields definitions are usually preset in subclasses.
        val additionalClasses = mutableListOf<String>()
        PhpIndex.getInstance(project).getClassesByFQN(contentEntity.fqn)
            .firstOrNull()
            ?.let { contentEntityClass ->
                PhpClassHierarchyUtils.processSupers(contentEntityClass, false, true) { supper ->
                    if (ExtendableContentEntityRelatedClasses.hasClass(supper.fqn)) {
                        additionalClasses.add(supper.fqn)
                    }
                    true
                }
            }

        fileBasedIndex.getAllValuesWithKeyPrefix(FieldsIndex.KEY, "$entityTypeId|", project)
            .toMutableList()
            // Merge additional fields.
            .let { main ->
                additionalClasses.forEach {
                    main.addAll(fileBasedIndex.getAllValuesWithKeyPrefix(FieldsIndex.KEY, "$it|", project))
                }

                main
            }
            .forEach { drupalField ->
                val name = drupalField.fieldName
                when {
                    // For cases when field key is
                    // $entity_type->getKey('owner') => BaseFieldDefinition::create('entity_reference')
                    // we use special prefix that later is replaced by actual key from annotations.
                    name.contains(GENERAL_BASE_FIELD_KEY_PREFIX) -> keys[name.substringAfter("|")]
                    else -> name
                }?.let { fieldName ->
                    result.addElement(
                        PrioritizedLookupElement.withPriority(
                            LookupElementBuilder.create(fieldName).withTypeText(drupalField.fieldType, true),
                            priority
                        )

                    )
                }

            }
    }

}