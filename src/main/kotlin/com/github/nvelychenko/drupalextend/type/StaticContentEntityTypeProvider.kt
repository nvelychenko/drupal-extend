package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.index.ContentEntityFqnIndex
import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.github.nvelychenko.drupalextend.util.getAllProjectKeys
import com.github.nvelychenko.drupalextend.util.getValue
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.jsonSchema.impl.nestedCompletions.letIf
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4


class StaticContentEntityTypeProvider : PhpTypeProvider4 {

    override fun getKey(): Char {
        return '\u0434'
    }

    override fun getType(psiElement: PsiElement): PhpType? {
        if (DumbService.getInstance(psiElement.project).isDumb) {
            return null
        }

        if (psiElement !is MethodReference) return null

        if (psiElement.name != "load" || !psiElement.isStatic) return null

        return PhpType().add("#$key${psiElement.signature}")
    }

    override fun complete(expression: String?, project: Project?): PhpType? {
        if (expression == null || project == null || !expression.contains(key))
            return null

        val signature = expression.substring(2)

        val fileBasedIndex = FileBasedIndex.getInstance()

        for (partialSignature in signature.split("|")) {
            if (partialSignature.startsWith("#M#C")) {
                val contentEntity = fileBasedIndex.getValue(ContentEntityFqnIndex.KEY, partialSignature.substring(4).substringBefore('.'), project) ?: continue

                return PhpType().add(contentEntity.fqn)
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