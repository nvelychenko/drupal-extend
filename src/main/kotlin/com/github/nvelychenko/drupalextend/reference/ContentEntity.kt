package com.github.nvelychenko.drupalextend.reference

import com.github.nvelychenko.drupalextend.reference.referenceType.ContentEntityReference
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.parser.PhpElementTypes
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class ContentEntity: PsiReferenceContributor() {
    private val ENTITY_STORAGE_SIGNATURES = arrayOf(
        Pair("\\Drupal\\Core\\Entity\\EntityStorageInterface", "load"),
        Pair("\\Drupal\\Core\\Entity\\EntityBase", "load"),
    )

    private val ENTITY_LOADERS_SIGNATURES = arrayOf(
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
                    ENTITY_LOADERS_SIGNATURES.find { it.second == methodReference.name } ?: return psiReferences
                    val method = methodReference.resolve() ?: return psiReferences

                    if (method !is Method) return psiReferences

                    val methodClass = method.containingClass as PhpClass
                    val project = method.project

                    val entityReferences = PhpIndex.getInstance(project).getAnyByFQN("\\Drupal\\Core\\Entity\\EntityTypeManagerInterface")
                    if (entityReferences.isEmpty()) return psiReferences
                    val entityTypeManagerInterface: PhpClass = entityReferences.first()

                    var isSuperInterface = false
                    PhpClassHierarchyUtils.processSuperInterfaces(methodClass, true, true) {
                        return@processSuperInterfaces if (PhpClassHierarchyUtils.classesEqual(entityTypeManagerInterface, it)) {
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

//        // \Drupal::entityTypeManager()->getStorage('node')->load(id)
//        // Node::load(id)
//        registrar.registerReferenceProvider(
//            PlatformPatterns
//                .or(
//                    PlatformPatterns.psiElement(StringLiteralExpression::class.java)
//                    .withParent(
//                        PlatformPatterns.psiElement(PhpElementTypes.PARAMETER_LIST)
//                            .withParent(PlatformPatterns.psiElement(PhpElementTypes.METHOD_REFERENCE))
//                    )
//                    .withLanguage(PhpLanguage.INSTANCE),
//                    PlatformPatterns.psiElement(PhpExpression::class.java)
//                        .withParent(
//                            PlatformPatterns.psiElement(PhpElementTypes.PARAMETER_LIST)
//                                .withParent(PlatformPatterns.psiElement(PhpElementTypes.METHOD_REFERENCE))
//                        )
//                        .withLanguage(PhpLanguage.INSTANCE),
//                ),
//            object : PsiReferenceProvider() {
//                override fun getReferencesByElement(
//                    element: PsiElement,
//                    context: ProcessingContext
//                ): Array<PsiReference> {
//                    val psiReferences = PsiReference.EMPTY_ARRAY
//
//                    if (element is StringLiteralExpression && element.contents.isEmpty()) return psiReferences
////                    if (element is PhpExpressionImpl && element.value)
//
//                    val parameterList = (element.parent as ParameterList)
//                    val methodReference = (parameterList.parent as MethodReference)
//                    ENTITY_STORAGE_SIGNATURES.find { it.second == methodReference.name } ?: return psiReferences
//                    val method = methodReference.resolve() ?: return psiReferences
//
//                    if (method !is Method) return psiReferences
//
//                    val methodClass = method.containingClass as PhpClass
//                    val project = method.project
//
//                    val entityReferences = PhpIndex.getInstance(project).getAnyByFQN("\\Drupal\\Core\\Entity\\EntityTypeManagerInterface")
//                    if (entityReferences.isEmpty()) return psiReferences
//                    val entityTypeManagerInterface: PhpClass = entityReferences.first()
//
//                    var isSuperInterface = false
//                    PhpClassHierarchyUtils.processSuperInterfaces(methodClass, true, true) {
//                        return@processSuperInterfaces if (PhpClassHierarchyUtils.classesEqual(entityTypeManagerInterface, it)) {
//                            isSuperInterface = true
//                            false
//                        } else {
//                            true
//                        }
//                    }
//
//                    if (isSuperInterface) {
////                        psiReferences[0] = ContentEntityStorageReference(element, element.contents)
//                    }
//
//
//
//                    return psiReferences
//                }
//            }
//        )
//
//        registrar.registerReferenceProvider(
//            PlatformPatterns
//                .or(
//                    PlatformPatterns.psiElement(FieldReference::class.java)
//                        .withLanguage(PhpLanguage.INSTANCE),
//                    PlatformPatterns.psiElement(StringLiteralExpression::class.java)
//                        .withParent(
//                            PlatformPatterns.psiElement(PhpElementTypes.PARAMETER_LIST)
//                                .withParent(PlatformPatterns.psiElement(PhpElementTypes.METHOD_REFERENCE))
//                        )
//                        .withLanguage(PhpLanguage.INSTANCE)
//                ),
//            object : PsiReferenceProvider() {
//                override fun getReferencesByElement(
//                    element: PsiElement,
//                    context: ProcessingContext
//                ): Array<PsiReference> {
//                    val psiReferences = PsiReference.EMPTY_ARRAY
//
//                    when (element) {
//                        is StringLiteralExpression -> {
//                            val parameterList = (element.parent as ParameterList)
//                            val methodReference = (parameterList.parent as MethodReference)
//                        }
//                        is FieldReference -> {
//
//                        }
//                        else -> {}
//                    }
//
////                    if (MethodMatcher.getMatchedSignatureWithDepth(element, ENTITY_LOADERS_SIGNATURES) != null) {
////                        psiReferences[0] = ServiceReference((element as StringLiteralExpression), false)
////                    }
//
//
//                    return psiReferences
//                }
//            }
//        )
    }
}