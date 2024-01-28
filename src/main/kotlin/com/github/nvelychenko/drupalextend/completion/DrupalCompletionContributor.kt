package com.github.nvelychenko.drupalextend.completion

import com.github.nvelychenko.drupalextend.completion.providers.EntityStorageProvider
import com.github.nvelychenko.drupalextend.completion.providers.FieldCompletionProvider
import com.github.nvelychenko.drupalextend.completion.providers.MagicPropertyFieldCompletionProvider
import com.github.nvelychenko.drupalextend.extensions.getAllProjectKeys
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.ContentEntityFqnIndex
import com.github.nvelychenko.drupalextend.index.FieldTypeIndex
import com.github.nvelychenko.drupalextend.index.FieldsIndex
import com.github.nvelychenko.drupalextend.patterns.Patterns
import com.github.nvelychenko.drupalextend.type.EntityStorageTypeProvider
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.FieldReference
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class DrupalCompletionContributor : CompletionContributor() {

    init {
        // \Drupal::entityTypeManager->getStorage('no|
        extend(CompletionType.BASIC, Patterns.LEAF_INSIDE_METHOD_PARAMETER, EntityStorageProvider())

        // $node->set('fi
        // $node->get('fi|
        extend(CompletionType.BASIC, Patterns.LEAF_INSIDE_METHOD_PARAMETER, FieldCompletionProvider())

        // $node->get('field_entity')->entity
        // $node->get('field_entity')[0]->entity
        // $node->get('field_entity')->first()->entity
        // @todo Fix completion for base fields that are not inside main entity.
        // e.g. 'status' -> \Drupal\Core\Entity\EntityPublishedTrait::publishedBaseFieldDefinitions
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(PhpLanguage.INSTANCE),
            object : CompletionProvider<CompletionParameters>() {
                public override fun addCompletions(
                    completionParameters: CompletionParameters,
                    processingContext: ProcessingContext,
                    completionResultSet: CompletionResultSet
                ) {
                    val psiElement = completionParameters.originalPosition ?: return
                    val parent: PsiElement
                    val prevSibling = completionParameters.position.prevSibling
                    parent = if (prevSibling != null && prevSibling.node.elementType === PhpTokenTypes.ARROW) {
                        when (prevSibling.parent) {
                            is FieldReference -> prevSibling.parent
                            else -> return
                        }
                    } else {
                        psiElement.parent
                    }

                    if (parent !is FieldReference) return
                    val methodReference = when (parent.classReference) {
                        is MethodReference -> when ((parent.classReference as MethodReference).name) {
                            "get" -> parent.classReference
                            "first" -> (parent.classReference as MethodReference).classReference
                            else -> return
                        }

                        is ArrayAccessExpression -> parent.classReference?.children?.firstOrNull()
                        else -> return
                    }
                    if (methodReference !is MethodReference) return

                    val entityType: String?
                    val index = FileBasedIndex.getInstance()
                    val project = psiElement.project
                    if (methodReference.signature.contains(EntityStorageTypeProvider.Util.SPLIT_KEY)) {
                        entityType = methodReference.signature.substringAfter(EntityStorageTypeProvider.Util.SPLIT_KEY)
                            .substringBefore('.')
                    } else {
                        val globalTypes =
                            methodReference.classReference?.globalType?.types?.map { it.replace("[]", "") }

                        if (globalTypes.isNullOrEmpty()) return

                        val entityTypeFqn = index.getAllProjectKeys(
                            ContentEntityFqnIndex.KEY, project
                        ).find { globalTypes.contains(it) } ?: return

                        val contentEntity = index.getValue(ContentEntityFqnIndex.KEY, entityTypeFqn, project) ?: return
                        entityType = contentEntity.entityTypeId
                    }

                    val fieldName = (methodReference.parameters[0] as StringLiteralExpression).contents
                    if (entityType.isEmpty() || fieldName.isEmpty()) return

                    val fieldInstance =
                        FileBasedIndex.getInstance().getValue(FieldsIndex.KEY, "${entityType}|${fieldName}", project)
                            ?: return
                    val fieldType =
                        FileBasedIndex.getInstance().getValue(FieldTypeIndex.KEY, fieldInstance.fieldType, project)
                            ?: return

                    val properties = HashMap<String, String>()
                    properties.putAll(fieldType.properties)

                    val fieldClassInstance =
                        PhpIndex.getInstance(project).getClassesByFQN(fieldType.fqn).firstOrNull() ?: return
                    PhpClassHierarchyUtils.processSuperClasses(fieldClassInstance, false, true) {
                        it.findOwnMethodByName("propertyDefinitions") ?: return@processSuperClasses true
                        val superProperties = index.getValue(FieldTypeIndex.KEY, it.fqn, project)
                        superProperties?.let { _ -> properties.putAll(superProperties.properties) }
                        return@processSuperClasses true
                    }

                    properties.forEach {
                        completionResultSet.addElement(
                            PrioritizedLookupElement.withPriority(
                                LookupElementBuilder.create(it.key).withTypeText(it.value).withBoldness(true),
                                100.0,
                            ),
                        )
                    }
                }
            })
    }

}