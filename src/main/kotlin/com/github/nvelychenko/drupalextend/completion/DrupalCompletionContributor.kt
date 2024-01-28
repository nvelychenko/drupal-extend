package com.github.nvelychenko.drupalextend.completion

import com.github.nvelychenko.drupalextend.extensions.isSuperInterfaceOf
import com.github.nvelychenko.drupalextend.index.ConfigEntityIndex
import com.github.nvelychenko.drupalextend.index.ContentEntityFqnIndex
import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.github.nvelychenko.drupalextend.index.FieldTypeIndex
import com.github.nvelychenko.drupalextend.index.FieldsIndex
import com.github.nvelychenko.drupalextend.type.EntityStorageTypeProvider
import com.github.nvelychenko.drupalextend.extensions.getAllProjectKeys
import com.github.nvelychenko.drupalextend.extensions.getAllValuesWithKeyPrefix
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.jetbrains.php.lang.parser.PhpElementTypes
import com.jetbrains.php.lang.psi.elements.*

class DrupalCompletionContributor : CompletionContributor() {

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
                        // $xex->
                        prevSibling.parent
                    } else {
                        // $xex->ad
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
                            entityType = methodReference.signature.substringAfter(EntityStorageTypeProvider.Util.SPLIT_KEY).substringBefore('.')
                        } else {
                            val globalTypes = methodReference.classReference?.globalType?.types?.map { it.replace("[]", "") }

                            if (globalTypes.isNullOrEmpty()) return

                            val entityTypeFqn = index.getAllProjectKeys(
                                    ContentEntityFqnIndex.KEY, project
                            ).find { globalTypes.contains(it) } ?: return

                            val contentEntity = index.getValue(ContentEntityFqnIndex.KEY, entityTypeFqn, project) ?: return
                            entityType = contentEntity.entityTypeId
                        }

                        val fieldName = (methodReference.parameters[0] as StringLiteralExpression).contents
                        if (entityType.isEmpty() || fieldName.isEmpty()) return

                        val fieldInstance = FileBasedIndex.getInstance().getValue(FieldsIndex.KEY, "${entityType}|${fieldName}", project) ?: return
                        val fieldType = FileBasedIndex.getInstance().getValue(FieldTypeIndex.KEY, fieldInstance.fieldType, project) ?: return

                        val properties = HashMap<String, String>()
                        properties.putAll(fieldType.properties)

                        val fieldClassInstance = PhpIndex.getInstance(project).getClassesByFQN(fieldType.fqn).firstOrNull() ?: return
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
                                        Double.MAX_VALUE,
                                ),
                            )
                        }

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

                    if (element !is StringLiteralExpression) return

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

                    val parameterList = element.parent as ParameterList
                    if (!parameterList.parameters.first().isEquivalentTo(element)) {
                        return
                    }
                    val methodReference = parameterList.parent as MethodReference

                    val allowedMethods = arrayOf(
                        "getAccessControlHandler",
                        "getStorage",
                        "getViewBuilder",
                        "getListBuilder",
                        "getFormObject",
                        "getRouteProviders",
                        "hasHandler",
                        "getDefinition",
                    )

                    if (!allowedMethods.contains(methodReference.name)) return
                    val method = methodReference.resolve() ?: return

                    if (method !is Method) return

                    val methodClass = method.containingClass as PhpClass
                    val project = method.project

                    val entityReferences =
                        PhpIndex.getInstance(project)
                            .getInterfacesByFQN("\\Drupal\\Core\\Entity\\EntityTypeManagerInterface").firstOrNull()
                            ?: return

                    if (!methodClass.isSuperInterfaceOf(entityReferences)) return

                    val instance = FileBasedIndex.getInstance()

                    instance
                        .getAllProjectKeys(ContentEntityIndex.KEY, project)
                        .filter { !it.contains("\\") }
                        .forEach {
                            val contentEntity = instance.getValue(ContentEntityIndex.KEY, it, project) ?: return@forEach

                            completionResultSet.addElement(
                                LookupElementBuilder.create(it)
                                    .withTypeText(contentEntity.fqn, true)
                            )
                        }

                    instance
                        .getAllProjectKeys(ConfigEntityIndex.KEY, project)
                        .forEach {
                            val configEntityFqn = instance.getValue(ConfigEntityIndex.KEY, it, project) ?: return@forEach

                            completionResultSet.addElement(
                                LookupElementBuilder.create(it)
                                    .withTypeText(configEntityFqn, true)
                            )
                        }
                }
            })
    }

    fun getFieldCompletionForClassReference(classReference: PhpReference, completionResultSet: CompletionResultSet) {
        val project = classReference.project
        val index = FileBasedIndex.getInstance()

        val entityTypeId: String?

        if (classReference.signature.contains(EntityStorageTypeProvider.Util.SPLIT_KEY)) {
            entityTypeId =
                classReference.signature.substringAfter(EntityStorageTypeProvider.Util.SPLIT_KEY).substringBefore('.')
        } else {
            val globalTypes = classReference.globalType.types.map { it.replace("[]", "") }

            if (globalTypes.isEmpty()) return

            val entityTypeFqn = index.getAllProjectKeys(
                ContentEntityFqnIndex.KEY, project
            ).find { globalTypes.contains(it) } ?: return

            val contentEntity = index.getValue(ContentEntityFqnIndex.KEY, entityTypeFqn, project) ?: return
            entityTypeId = contentEntity.entityTypeId
        }

        val keys = index.getValue(ContentEntityIndex.KEY, entityTypeId, project)?.keys ?: return

        index.getAllValuesWithKeyPrefix(FieldsIndex.KEY, "$entityTypeId|", project)
            .forEach {
                var fieldName = it.fieldName
                if (fieldName.contains("KEY|")) {
                    val key = fieldName.split("|")[1]
                    if (keys.contains(key)) {
                        return@forEach
                    }
                    fieldName = keys[key] ?: return@forEach
                }

                completionResultSet.addElement(
                    LookupElementBuilder.create(fieldName)
                        .withTypeText(it.fieldType, true)
                )
            }
    }
}