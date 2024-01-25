package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.github.nvelychenko.drupalextend.util.getValue
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.jsonSchema.impl.nestedCompletions.letIf
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4


class ContentEntityFieldTypeProvider : PhpTypeProvider4 {

    private val possibleMethods = mutableMapOf(
        Pair("load", ""),
        Pair("loadByProperties", "[]"),
        Pair("loadMultiple", "[]"),
    )

    private val splitKey = '\u0421'

    override fun getKey(): Char {
        return '\u0420'
    }

    override fun getType(psiElement: PsiElement): PhpType? {
        if (DumbService.getInstance(psiElement.project).isDumb) {
            return null
        }

        if (psiElement !is MethodReference) return null

        if (!possibleMethods.containsKey(psiElement.name)) return null

        val signature = psiElement.signature

        if (!signature.contains(EntityStorageTypeProvider.Util.SPLIT_KEY)) {
            return null
        }

        val entityTypeId = signature.substringAfter(EntityStorageTypeProvider.Util.SPLIT_KEY).substringBefore(".load")

        if (entityTypeId.isEmpty()) return null

        return PhpType().add("#$key$entityTypeId$splitKey${psiElement.name}")
    }

    override fun complete(expression: String?, project: Project?): PhpType? {
        if (expression == null || project == null || !expression.contains(key))
            return null

        val (entityTypeId, methodName) = expression.replace("#$key", "").split(splitKey)

        return FileBasedIndex.getInstance().getValue(ContentEntityIndex.KEY, entityTypeId, project)
            ?.let {
                PhpType().add(it.fqn.letIf(possibleMethods.containsKey(methodName)) { fqn -> fqn + possibleMethods[methodName] })
            }
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