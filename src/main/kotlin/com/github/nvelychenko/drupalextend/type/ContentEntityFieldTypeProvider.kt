package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpReference
import com.jetbrains.php.lang.psi.elements.Variable
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4


class ContentEntityFieldTypeProvider : PhpTypeProvider4 {

    private val possibleMethods = mutableMapOf (
        Pair("load", ""),
        Pair("loadByProperties", "[]"),
        Pair("loadMultiple", "[]"),
    )

    private val splitKey = '\u3337'

    override fun getKey(): Char {
        return '\u3335'
    }

    override fun getType(psiElement: PsiElement): PhpType? {
        if (DumbService.getInstance(psiElement.project).isDumb) {
            return null
        }
        val signature = getSignature(psiElement) ?: return null

        if (!signature.contains(EntityStorageTypeProvider.Util.SPLIT_KEY)) {
            return null
        }

        val entityTypeId = signature.substringAfter(EntityStorageTypeProvider.Util.SPLIT_KEY).substringBefore(".load")

        if (entityTypeId.isEmpty()) return null

        val methodName = if (psiElement is MethodReference) {
            psiElement.name
        } else {
            ""
        }

        return PhpType().add("#$key$entityTypeId$splitKey$methodName")
    }

    private fun getSignature(psiElement: PsiElement): String? {
        return when (psiElement) {
            is MethodReference -> {
                if (!possibleMethods.containsKey(psiElement.name)) {
                    return null
                }

                psiElement.signature
            }
            else -> {
                val firstChild = psiElement.firstChild
                if (firstChild is Variable) {
                    firstChild.signature
                } else if (firstChild is PhpReference) {
                    firstChild.signature
                } else {
                    null
                }
            }
        }
    }

    override fun complete(expression: String?, project: Project?): PhpType? {
        if (expression == null || project == null || !expression.contains(key))
            return null

        val (entityTypeId, methodName) = expression.replace("#$key", "").split(splitKey)

        val entityTypeIndex = FileBasedIndex.getInstance()
            .getValues(ContentEntityIndex.KEY, entityTypeId, GlobalSearchScope.allScope(project))

        return if (entityTypeIndex.isEmpty()) {
            null
        } else {
            var fqn = entityTypeIndex.first().fqn
            if (methodName.isNotEmpty()) {
                fqn += possibleMethods[methodName]
            }

            PhpType().add(fqn)
        }
    }

    override fun getBySignature(
        expr: String,
        visited: Set<String?>?,
        depth: Int,
        project: Project?
    ): Collection<PhpClass> {
        return emptyList()
    }


}