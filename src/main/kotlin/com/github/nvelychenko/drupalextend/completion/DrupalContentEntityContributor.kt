package com.github.nvelychenko.drupalextend.completion

import com.github.nvelychenko.drupalextend.index.ContentEntityFqnIndex
import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.github.nvelychenko.drupalextend.index.FieldsIndex
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.jetbrains.php.lang.parser.PhpElementTypes
import com.jetbrains.php.lang.psi.elements.*

class DrupalContentEntityContributor : CompletionContributor() {
    init {
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
                        // $foo->
                        prevSibling.parent
                    } else {
                        // $foo->ad
                        psiElement.parent
                    }

                    if (parent !is FieldReference) return
                    val classReference = parent.classReference
                    if (classReference !is PhpReference) return

                    getFieldCompletionForClassReference(classReference, completionResultSet)
                }
            })

        extend(
            CompletionType.BASIC,
            PlatformPatterns
                .psiElement(LeafPsiElement::class.java)
                .withParent(
                    PlatformPatterns.psiElement(StringLiteralExpression::class.java)
                        .withParent(
                            PlatformPatterns.psiElement(PhpElementTypes.PARAMETER_LIST)
                                .withParent(PlatformPatterns.psiElement(PhpElementTypes.METHOD_REFERENCE))
                        )
                )
                .withLanguage(PhpLanguage.INSTANCE),
            object : CompletionProvider<CompletionParameters>() {
                public override fun addCompletions(
                    completionParameters: CompletionParameters,
                    processingContext: ProcessingContext,
                    completionResultSet: CompletionResultSet
                ) {
                    val leaf = completionParameters.originalPosition ?: return
                    val element = leaf.parent

                    if (element !is StringLiteralExpression || element.contents.isEmpty()) return

                    val parameterList = (element.parent as ParameterList)
                    val methodReference = (parameterList.parent as MethodReference)

                    if ("get" != methodReference.name) return

                    var classReference = methodReference.classReference

                    if (classReference !is PhpReference) {
                        if (classReference !is ArrayAccessExpression || classReference.value !is PhpReference) {
                            return
                        }

                        classReference = classReference.value as PhpReference
                    }
                    getFieldCompletionForClassReference(classReference, completionResultSet)
                }
            })

        extend(
            CompletionType.BASIC,
            PlatformPatterns
                .psiElement(LeafPsiElement::class.java)
                .withParent(
                    PlatformPatterns.psiElement(StringLiteralExpression::class.java)
                        .withParent(
                            PlatformPatterns.psiElement(PhpElementTypes.PARAMETER_LIST)
                                .withParent(PlatformPatterns.psiElement(PhpElementTypes.METHOD_REFERENCE))
                        )
                )
                .withLanguage(PhpLanguage.INSTANCE),
            object : CompletionProvider<CompletionParameters>() {
                public override fun addCompletions(
                    completionParameters: CompletionParameters,
                    processingContext: ProcessingContext,
                    completionResultSet: CompletionResultSet
                ) {
                    val leaf = completionParameters.originalPosition ?: return

                    val element = leaf.parent

                    if (element !is StringLiteralExpression || element.contents.isEmpty()) return

                    val parameterList = (element.parent as ParameterList)
                    val methodReference = (parameterList.parent as MethodReference)

                    if ("getStorage" != methodReference.name) return
                    val method = methodReference.resolve() ?: return

                    if (method !is Method) return

                    val methodClass = method.containingClass as PhpClass
                    val project = method.project

                    val entityReferences =
                        PhpIndex.getInstance(project).getAnyByFQN("\\Drupal\\Core\\Entity\\EntityTypeManagerInterface")
                    if (entityReferences.isEmpty()) return
                    val entityTypeManagerInterface: PhpClass = entityReferences.first()

                    var isSuperInterface = false
                    PhpClassHierarchyUtils.processSuperInterfaces(methodClass, true, true) {
                        return@processSuperInterfaces if (PhpClassHierarchyUtils.classesEqual(
                                entityTypeManagerInterface,
                                it
                            )
                        ) {
                            isSuperInterface = true
                            false
                        } else {
                            true
                        }
                    }

                    if (!isSuperInterface) return

                    // @todo Implement caching
                    val instance = FileBasedIndex.getInstance()
                    instance
                        .getAllKeys(ContentEntityIndex.KEY, project)
                        .filter { !it.contains("\\") }
                        .forEach {
                            val values = instance.getValues(ContentEntityIndex.KEY, it, GlobalSearchScope.allScope(project))
                            if (values.isEmpty()) return@forEach

                            completionResultSet.addElement(
                                LookupElementBuilder.create(it)
                                    .withTypeText(values.first().fqn, true)
                            )
                        }
                }
            })
    }

    fun getFieldCompletionForClassReference(classReference: PhpReference, completionResultSet: CompletionResultSet) {
        val project = classReference.project

        val globalTypes = classReference.type.global(project).types

        if (globalTypes.isEmpty()) return

        var entityTypeFqn: String? = null
        val instance = FileBasedIndex.getInstance()

        // @todo Implement caching
        instance
            .processAllKeys(ContentEntityFqnIndex.KEY, { fqn ->
                val foundEntityTypeFqn = globalTypes.find { it.replace("[]", "") == fqn }
                if (foundEntityTypeFqn != null) {
                    entityTypeFqn = foundEntityTypeFqn.replace("[]", "")
                    return@processAllKeys false
                }
                true
            }, project)

        if (entityTypeFqn == null) return

        val allScope = GlobalSearchScope.allScope(project)
        val contentEntity = instance.getValues(ContentEntityFqnIndex.KEY, entityTypeFqn!!, allScope).first()
        PhpIndex.getInstance(project).getAnyByFQN(contentEntity.fqn).first() ?: return

        val entityTypeId = contentEntity.entityTypeId
        val keys = instance.getValues(ContentEntityIndex.KEY, entityTypeId, allScope).first().keys

        // @todo Implement caching
        instance
            .getAllKeys(FieldsIndex.KEY, project)
            .filter { it.contains("$entityTypeId|") }
            .forEach {
                val fieldIndexes =  instance.getValues(FieldsIndex.KEY, it, allScope)
                if (fieldIndexes.isEmpty()) return@forEach

                val fieldIndex = fieldIndexes.first()
                var fieldName = fieldIndex.fieldName
                if (fieldName.contains("KEY|")) {
                    val key = fieldName.split("|")[1]
                    if (keys.contains(key)) {
                        return@forEach
                    }
                    fieldName = keys[key] ?: return@forEach
                }

                completionResultSet.addElement(
                    LookupElementBuilder.create(fieldName)
                        .withTypeText(fieldIndex.fieldType, true)
                )
            }

    }
}