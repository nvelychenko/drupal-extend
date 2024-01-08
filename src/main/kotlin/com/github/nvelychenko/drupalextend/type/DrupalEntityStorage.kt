package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.github.nvelychenko.drupalextend.type.DrupalEntityStorage.Util.SPLITER_KEY
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.search.ProjectScope
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.parser.PhpElementTypes
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4


class DrupalEntityStorage : PhpTypeProvider4 {

    private val ENTITY_LOADERS_SIGNATURES = arrayOf(
        Pair("\\Drupal\\Core\\Entity\\EntityTypeManagerInterface", "getStorage"),
    )

    override fun getKey(): Char {
        return Util.KEY
    }

    override fun getType(psiElement: PsiElement): PhpType? {
        // container calls are only on "get" methods
        if (psiElement !is MethodReference) {
            return null
        }

        if (psiElement.name != "getStorage") {
            return null
        }

        if (!PlatformPatterns
                .psiElement(PhpElementTypes.METHOD_REFERENCE)
                .withChild(
                    PlatformPatterns
                        .psiElement(PhpElementTypes.PARAMETER_LIST)
                        .withFirstChild(
                            PlatformPatterns.psiElement(StringLiteralExpression::class.java),
                        )
                ).accepts(psiElement)) {
            return null
        }

        val parameterList = PhpPsiUtil.getChildOfClass(psiElement, ParameterList::class.java)
        if (parameterList !is ParameterList) return null

        val firstParameter = parameterList.getParameter(0) as StringLiteralExpression
        if (firstParameter.contents.isEmpty()) return null

        val signature = psiElement.signature.replace('|', SPLITER_KEY)
        return PhpType().add("#" + key + signature + Util.SPLIT_KEY + firstParameter.contents)
    }

    override fun complete(expression: String?, project: Project?): PhpType? {
        if (project == null || expression == null) return null

        val parts = expression.split(Util.SPLIT_KEY)

        if (parts.size != 2) {
            return null
        }

        val (originalSignature, entityTypeId) = parts

        val entityTypeIndex = FileBasedIndex.getInstance().getValues(ContentEntityIndex.KEY, entityTypeId, ProjectScope.getAllScope(project))

        if (entityTypeIndex.isEmpty()) return null

        val phpIndex = PhpIndex.getInstance(project)
        val namedCollection = mutableListOf<PhpNamedElement>()
        for (partialSignature in originalSignature.split(SPLITER_KEY)) {
            namedCollection.addAll(phpIndex.getBySignature(partialSignature, null, 0))
        }

        if (namedCollection.isEmpty()) return null

        var isEntityStorage = false;

        val loadedStorageClasses = mutableMapOf<String, PhpClass>()
        val localEntityStorageSignatures = ENTITY_LOADERS_SIGNATURES.clone().toMutableList()

        for (method in namedCollection) {
            if (method !is Method) {
                continue;
            }

            val methodClass = method.containingClass ?: continue
            val isImmediateEntityStorage = localEntityStorageSignatures.find { it.first == methodClass.fqn }

            if (isImmediateEntityStorage != null) {
                isEntityStorage = true
                break;
            }

            if (loadedStorageClasses.size != localEntityStorageSignatures.size) {
                localEntityStorageSignatures.forEach {
                    val entityReferences = phpIndex.getAnyByFQN(it.first)
                    if (entityReferences.isEmpty()) {
                        localEntityStorageSignatures.remove(it)
                    }
                    loadedStorageClasses[it.first] = entityReferences.first()
                }
            }

            for (storageClass in loadedStorageClasses) {
                PhpClassHierarchyUtils.processSuperInterfaces(methodClass, true, true) {
                    return@processSuperInterfaces if (PhpClassHierarchyUtils.classesEqual(storageClass.value, it)) {
                        isEntityStorage = true
                        false
                    } else {
                        true
                    }
                }
            }

            if (isEntityStorage) break
        }

        if (!isEntityStorage) return null

//        val classesByFQN = phpIndex.getClassesByFQN("\\Drupal\\Core\\Entity\\Sql\\SqlContentEntityStorage").toMutableList()
//        val classesByFQN1 = phpIndex.getClassesByFQN("\\Drupal\\Core\\Entity\\EntityStorageBase").toMutableList()
//
//        classesByFQN.addAll(classesByFQN1)

        return PhpType()
            .add("\\Drupal\\Core\\Entity\\Sql\\SqlContentEntityStorage")
            .add("\\Drupal\\Core\\Entity\\EntityStorageBase")
    }

    override fun getBySignature(
        expression: String,
        visited: Set<String?>?,
        depth: Int,
        project: Project?
    ): Collection<PhpClass> {
        return emptyList()
//        if (project == null) return emptyList()
//
//        val parts = expression.split(Util.SPLIT_KEY)
//
//        if (parts.size != 2) {
//            return emptyList()
//        }
//
//        val (originalSignature, entityTypeId) = parts
//
//        val entityTypeIndex = FileBasedIndex.getInstance().getValues(ContentEntityIndex.KEY, entityTypeId, ProjectScope.getAllScope(project))
//
//        if (entityTypeIndex.isEmpty()) return emptyList()
//
//        val phpIndex = PhpIndex.getInstance(project)
//        val namedCollection = mutableListOf<PhpNamedElement>()
//        for (partialSignature in originalSignature.split(SPLITER_KEY)) {
//            namedCollection.addAll(phpIndex.getBySignature(partialSignature, null, 0))
//        }
//
//        if (namedCollection.isEmpty()) return emptyList()
//
//        var isEntityStorage = false;
//
//        val loadedStorageClasses = mutableMapOf<String, PhpClass>()
//        val localEntityStorageSignatures = ENTITY_LOADERS_SIGNATURES.clone().toMutableList()
//
//        for (method in namedCollection) {
//            if (method !is Method) {
//                continue;
//            }
//
//            val methodClass = method.containingClass ?: continue
//            val isImmediateEntityStorage = localEntityStorageSignatures.find { it.first == methodClass.fqn }
//
//            if (isImmediateEntityStorage != null) {
//                isEntityStorage = true
//                break;
//            }
//
//            if (loadedStorageClasses.size != localEntityStorageSignatures.size) {
//                localEntityStorageSignatures.forEach {
//                    val entityReferences = phpIndex.getAnyByFQN(it.first)
//                    if (entityReferences.isEmpty()) {
//                        localEntityStorageSignatures.remove(it)
//                    }
//                    loadedStorageClasses[it.first] = entityReferences.first()
//                }
//            }
//
//            for (storageClass in loadedStorageClasses) {
//                PhpClassHierarchyUtils.processSuperInterfaces(methodClass, true, true) {
//                    return@processSuperInterfaces if (PhpClassHierarchyUtils.classesEqual(storageClass.value, it)) {
//                        isEntityStorage = true
//                        false
//                    } else {
//                        true
//                    }
//                }
//            }
//
//            if (isEntityStorage) break
//        }
//
//        if (!isEntityStorage) return emptyList()
//
//        val classesByFQN = phpIndex.getClassesByFQN("\\Drupal\\Core\\Entity\\Sql\\SqlContentEntityStorage").toMutableList()
//        val classesByFQN1 = phpIndex.getClassesByFQN("\\Drupal\\Core\\Entity\\EntityStorageBase").toMutableList()
//
//        classesByFQN.addAll(classesByFQN1)
//
//        return classesByFQN
    }

    object Util {
        val SPLIT_KEY = '\u3333'
        val KEY = '\u3334'
        val SPLITER_KEY = '\u3336'
    }


}