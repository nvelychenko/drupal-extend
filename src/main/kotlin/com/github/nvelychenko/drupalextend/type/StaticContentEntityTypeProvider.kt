package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.index.ContentEntityFqnIndex
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4


/**
 * $node = Node::load();
 *   ↑
 */
class StaticContentEntityTypeProvider : PhpTypeProvider4 {

    override fun getKey(): Char {
        return '\u0434'
    }

    private val splitKey = '\u0435'

    private val possibleMethods = mutableMapOf(
        Pair("load", ""),
        Pair("create", ""),
        Pair("loadMultiple", "[]"),
    )

    override fun getType(psiElement: PsiElement): PhpType? {
        if (DumbService.getInstance(psiElement.project).isDumb) {
            return null
        }

        if (psiElement !is MethodReference) return null

        if (!possibleMethods.containsKey(psiElement.name)) return null

        return PhpType().add("#$key${psiElement.signature}$splitKey${psiElement.name}")
    }

    override fun complete(expression: String?, project: Project?): PhpType? {
        if (expression == null || project == null || !expression.contains(splitKey))
            return null

        val (signature, methodName) = expression.substring(2).split(splitKey)

        val fileBasedIndex = FileBasedIndex.getInstance()

        for (partialSignature in signature.split("|")) {
            if (partialSignature.startsWith("#M#C")) {
                val contentEntity = fileBasedIndex.getValue(ContentEntityFqnIndex.KEY, partialSignature.substring(4).substringBefore('.'), project) ?: continue

                val methodType = possibleMethods[methodName] ?: ""
                return PhpType().add(contentEntity.fqn + methodType)
            }
        }

        return null
    }

    override fun getBySignature(
        expression: String,
        visited: Set<String?>?,
        depth: Int,
        project: Project?
    ): Collection<PhpNamedElement> {
        return emptyList()
    }


}