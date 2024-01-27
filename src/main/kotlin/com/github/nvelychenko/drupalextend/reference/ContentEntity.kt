package com.github.nvelychenko.drupalextend.reference

import com.github.nvelychenko.drupalextend.index.ContentEntityFqnIndex
import com.github.nvelychenko.drupalextend.reference.referenceType.ContentEntityReference
import com.github.nvelychenko.drupalextend.reference.referenceType.FieldPropertyReference
import com.github.nvelychenko.drupalextend.type.EntityStorageTypeProvider
import com.github.nvelychenko.drupalextend.extensions.getAllProjectKeys
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.parser.PhpElementTypes
import com.jetbrains.php.lang.psi.elements.*

class ContentEntity : PsiReferenceContributor() {
    private val entityStorageSignature = arrayOf(
        Pair("\\Drupal\\Core\\Entity\\EntityTypeManagerInterface", "getStorage"),
    )

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // \Drupal::entityTypeManager()->getStorage('ENTITY_TYPE')
        registrar.registerReferenceProvider(
            PlatformPatterns
                .psiElement(StringLiteralExpression::class.java)
                .withParent(
                    PlatformPatterns.psiElement(PhpElementTypes.PARAMETER_LIST)
                        .withParent(PlatformPatterns.psiElement(PhpElementTypes.METHOD_REFERENCE))
                )
                .withLanguage(PhpLanguage.INSTANCE),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    val psiReferences = PsiReference.EMPTY_ARRAY

                    if (element !is StringLiteralExpression || element.contents.isEmpty()) return psiReferences

                    val parameterList = (element.parent as ParameterList)
                    val methodReference = (parameterList.parent as MethodReference)
                    entityStorageSignature.find { it.second == methodReference.name } ?: return psiReferences
                    val method = methodReference.resolve() ?: return psiReferences

                    if (method !is Method) return psiReferences

                    val methodClass = method.containingClass as PhpClass
                    val project = method.project

                    val entityReferences =
                        PhpIndex.getInstance(project).getAnyByFQN("\\Drupal\\Core\\Entity\\EntityTypeManagerInterface")
                    if (entityReferences.isEmpty()) return psiReferences
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

                    return if (isSuperInterface) {
                        arrayOf(ContentEntityReference(element, element.contents))
                    } else psiReferences
                }
            }
        )


        // $node->get('field_body')
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression::class.java)
                .withParent(
                    PlatformPatterns.psiElement(PhpElementTypes.PARAMETER_LIST)
                        .withParent(PlatformPatterns.psiElement(PhpElementTypes.METHOD_REFERENCE))
                )
                .withLanguage(PhpLanguage.INSTANCE),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    val psiReferences = PsiReference.EMPTY_ARRAY

                    element as StringLiteralExpression
                    val parameterList = (element.parent as ParameterList)
                    val methodReference = (parameterList.parent as MethodReference)

                    if (methodReference.name != "get") return psiReferences

                    val reference = when (val classReference = methodReference.classReference) {
                        is PhpReference -> classReference
                        is ArrayAccessExpression -> if (classReference.value is PhpReference) {
                            (classReference.value as PhpReference)
                        }  else { null }

                        else -> null
                    } ?: return psiReferences

                    val entityTypeId: String?

                    if (reference.signature.contains(EntityStorageTypeProvider.Util.SPLIT_KEY)) {
                            entityTypeId = reference.signature.substringAfter(EntityStorageTypeProvider.Util.SPLIT_KEY).substringBefore('.')
                    } else {
                        val project = element.project
                        val types = reference.type.global(project).filterPrimitives().types

                        val index = FileBasedIndex.getInstance()

                        val contentEntity = index.getAllProjectKeys(ContentEntityFqnIndex.KEY, project)
                            .find { types.contains(it) } ?: return psiReferences

                        entityTypeId = index.getValue(ContentEntityFqnIndex.KEY, contentEntity, project)?.entityTypeId
                    }

                    if (entityTypeId == null) {
                        return psiReferences
                    }



                    return arrayOf(FieldPropertyReference(element, entityTypeId, element.contents))
                }
            }
        )
    }
}