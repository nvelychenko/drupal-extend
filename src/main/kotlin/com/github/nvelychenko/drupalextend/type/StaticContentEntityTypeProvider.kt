package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.ContentEntityFqnIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
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

    private val fileBasedIndex: FileBasedIndex by lazy {
        FileBasedIndex.getInstance()
    }

    override fun getKey(): Char {
        return '\u0434'
    }

    private val splitKey = '\u0435'

    private val possibleMethods = mutableMapOf(
        Pair("load", ""),
        Pair("create", ""),
        Pair("loadMultiple", "[]"),
        Pair("create", ""),
    )

    override fun getType(psiElement: PsiElement): PhpType? {
        val project = psiElement.project
        if (!project.drupalExtendSettings.isEnabled || DumbService.getInstance(project).isDumb) {
            return null
        }

        if (psiElement !is MethodReference) return null

        if (!possibleMethods.containsKey(psiElement.name)) return null

        return PhpType().add("#$key${compressSignature(psiElement.signature)}$splitKey${psiElement.name}")
    }

    override fun complete(expression: String, project: Project): PhpType? {
        if (!expression.contains(splitKey))
            return null

        val (signature, methodName) = expression.substring(2).split(splitKey)

        for (partialSignature in decompressSignature(signature).split("|")) {
            if (partialSignature.startsWith("#M#C")) {
                val contentEntity = fileBasedIndex.getValue(
                    ContentEntityFqnIndex.KEY,
                    partialSignature.substring(4).substringBefore('.'),
                    project
                ) ?: continue

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