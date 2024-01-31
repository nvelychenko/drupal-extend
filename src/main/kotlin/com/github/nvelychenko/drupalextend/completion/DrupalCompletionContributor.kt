package com.github.nvelychenko.drupalextend.completion

import com.github.nvelychenko.drupalextend.completion.providers.EntityStorageProvider
import com.github.nvelychenko.drupalextend.completion.providers.FieldCompletionProvider
import com.github.nvelychenko.drupalextend.extensions.getAllProjectKeys
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.ContentEntityFqnIndex
import com.github.nvelychenko.drupalextend.index.FieldTypeIndex
import com.github.nvelychenko.drupalextend.index.FieldsIndex
import com.github.nvelychenko.drupalextend.index.RenderElementIndex
import com.github.nvelychenko.drupalextend.patterns.Patterns.LEAF_INSIDE_METHOD_PARAMETER
import com.github.nvelychenko.drupalextend.type.EntityStorageTypeProvider
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.or
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.jetbrains.php.lang.parser.PhpElementTypes
import com.jetbrains.php.lang.psi.elements.*


class DrupalCompletionContributor : CompletionContributor() {

    private class RenderElementTypeInsertionHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val document = context.document
            val editor = context.editor


            val fileText: String = editor.document.text
            if (fileText.isEmpty()) {
                return
            }

            if (document.charsSequence[context.startOffset - 1] != '#') {
                context.document.insertString(context.startOffset, "#")
            }
        }

    }

    init {
        // \Drupal::entityTypeManager->getStorage('no|
        extend(CompletionType.BASIC, LEAF_INSIDE_METHOD_PARAMETER, EntityStorageProvider())

        // $node->set('fi
        // $node->get('fi|
        extend(CompletionType.BASIC, LEAF_INSIDE_METHOD_PARAMETER, FieldCompletionProvider())

        // $render = [
        //    '#type' => '|
        // ];
        // @todo Move to separate provider.
        extend(CompletionType.BASIC,
            psiElement(LeafPsiElement::class.java)
                .withParent(
                    psiElement(StringLiteralExpression::class.java)
                        .withParent(
                            psiElement(PhpElementTypes.ARRAY_VALUE)
                                .withParent(
                                    psiElement(PhpElementTypes.HASH_ARRAY_ELEMENT)
                                        .withChild(
                                            psiElement(PhpElementTypes.ARRAY_KEY)
                                                .withChild(
                                                    psiElement(StringLiteralExpression::class.java)
                                                )
                                        )
                                )
                        )
                ),
            object : CompletionProvider<CompletionParameters>() {
                public override fun addCompletions(
                    completionParameters: CompletionParameters,
                    processingContext: ProcessingContext,
                    completionResultSet: CompletionResultSet
                ) {
                    val leaf = completionParameters.originalPosition ?: return

                    val hash = PsiTreeUtil.getParentOfType(leaf, ArrayHashElement::class.java)!!

                    val key = hash.key as StringLiteralExpression

                    if (key.contents != "#type") {
                        return
                    }
                    FileBasedIndex.getInstance().getAllProjectKeys(RenderElementIndex.KEY, hash.project)
                        .filter { !it.startsWith("\\") }
                        .forEach {
                            completionResultSet.addElement(
                                LookupElementBuilder.create(it)
                            )
                        }

                }
            })

        // $render = [
        //    '#type' => 'checkbox',
        //    '#|
        // ];
        // @todo Move to separate provider.
        extend(
            CompletionType.BASIC,
            psiElement(LeafPsiElement::class.java)
                .withParent(
                    psiElement(StringLiteralExpression::class.java)
                        .withParent(
                            or(
                                psiElement(PhpElementTypes.ARRAY_KEY)
                                    .withParent(
                                        or(
                                            psiElement(PhpElementTypes.HASH_ARRAY_ELEMENT)
                                                .withParent(psiElement(PhpElementTypes.ARRAY_CREATION_EXPRESSION)),
                                            psiElement(PhpElementTypes.ARRAY_CREATION_EXPRESSION)
                                        )
                                    ),
                                psiElement(PhpElementTypes.ARRAY_VALUE)
                                    .withParent(psiElement(PhpElementTypes.ARRAY_CREATION_EXPRESSION))
                            )
                        )
                ),
            object : CompletionProvider<CompletionParameters>() {
                public override fun addCompletions(
                    completionParameters: CompletionParameters,
                    processingContext: ProcessingContext,
                    completionResultSet: CompletionResultSet
                ) {
                    val leaf = completionParameters.originalPosition ?: return

                    val array = PsiTreeUtil.getParentOfType(leaf, ArrayCreationExpression::class.java)!!

                    val typeElement = array.hashElements.find {
                        val key = it.key
                        key is StringLiteralExpression && key.contents == "#type" && it.value is StringLiteralExpression
                    } ?: return

                    val type =
                        (typeElement.value as StringLiteralExpression).contents.takeIf { it.isNotEmpty() } ?: return

                    val project = array.project

                    val fileBasedIndex = FileBasedIndex.getInstance()

                    val renderElementType = fileBasedIndex.getValue(RenderElementIndex.KEY, type, project) ?: return

                    val elementClass =
                        PhpIndex.getInstance(project).getClassesByFQN(renderElementType.typeClass).firstOrNull()
                            ?: return

                    var properties = renderElementType.properties

                    PhpClassHierarchyUtils.processSuperClasses(elementClass, false, true) { phpClass ->
                        fileBasedIndex.getValue(RenderElementIndex.KEY, phpClass.fqn, project)
                            ?.let { properties = arrayOf(*properties, *it.properties) }
                        if (phpClass.fqn == "\\Drupal\\Core\\Render\\Element\\RenderElement") {
                            return@processSuperClasses false
                        }
                        true
                    }

                    properties.forEach {
                        completionResultSet.addElement(
                            PrioritizedLookupElement.withPriority(
                                LookupElementBuilder.create(it.id.replace("#", ""))
                                    .withTypeText(it.type)
                                    .withBoldness(it.priority > 0.0)
                                    .withInsertHandler(RenderElementTypeInsertionHandler()),
                                it.priority
                            )
                        )
                    }
                }
            })

        // $node->get('field_entity')->entity
        // $node->get('field_entity')[0]->entity
        // $node->get('field_entity')->first()->entity
        // @todo Fix completion for base fields that are not inside main entity.
        // e.g. 'status' -> \Drupal\Core\Entity\EntityPublishedTrait::publishedBaseFieldDefinitions
        extend(
            CompletionType.BASIC,
            psiElement().withLanguage(PhpLanguage.INSTANCE),
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