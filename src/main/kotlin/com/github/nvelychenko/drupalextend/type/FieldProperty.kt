package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.ProjectScope
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.ClassReference
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpReference
import com.jetbrains.php.lang.psi.elements.Variable
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4


class FieldProperty : PhpTypeProvider4 {

    private val trimKey = '\u3336'
    private val splitKey = '\u3337'

    override fun getKey(): Char {
        return '\u3335'
    }

    override fun getType(psiElement: PsiElement): PhpType? {
        if (DumbService.getInstance(psiElement.project).isDumb) {
            return null
        }
        val signature = getSignature(psiElement) ?: return null

        if (!signature.contains(DrupalEntityStorage.Util.SPLIT_KEY)) {
            return null
        }

        val entityTypeId = signature.substringAfter(DrupalEntityStorage.Util.SPLIT_KEY).substringBefore(".load")

        if (entityTypeId.isEmpty()) return null

        return PhpType().add("#$key$entityTypeId")
    }

    private fun getSignature(psiElement: PsiElement): String? {
        return when (psiElement) {
            is MethodReference -> {
                if (psiElement.name != "load") {
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

    override fun complete(expr: String?, project: Project?): PhpType? {
        if (expr == null || project == null || !expr.contains(key))
            return null

        val entityTypeId = expr.substringAfter(key)

        val entityTypeIndex = FileBasedIndex.getInstance()
            .getValues(ContentEntityIndex.KEY, entityTypeId, ProjectScope.getAllScope(project))
        return if (entityTypeIndex.isEmpty()) {
            null
        } else {
            PhpType().add(PhpIndex.getInstance(project).getAnyByFQN(entityTypeIndex.first().fqn).first())
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