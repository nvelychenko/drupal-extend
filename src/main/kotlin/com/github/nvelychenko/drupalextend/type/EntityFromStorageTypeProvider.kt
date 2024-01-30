package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.github.nvelychenko.drupalextend.index.ConfigEntityIndex
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.jsonSchema.impl.nestedCompletions.letIf
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4

/**
 * $paragraph = \Drupal::entityTypeManager()->getStorage('paragraph')->load(1);
 *     ↑
 */
class EntityFromStorageTypeProvider : PhpTypeProvider4 {

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
        if (
            DumbService.getInstance(psiElement.project).isDumb
            || psiElement !is MethodReference || psiElement.isStatic
            || !possibleMethods.containsKey(psiElement.name)
            ) {
            return null
        }

        val signature = psiElement.signature

        if (!signature.contains(EntityStorageTypeProvider.Util.SPLIT_KEY)) {
            return null
        }

        val entityTypeId = signature.substringAfter(EntityStorageTypeProvider.Util.SPLIT_KEY).substringBefore(".load")

        return PhpType().add("#$key$entityTypeId$splitKey${psiElement.name}")
    }

    override fun complete(expression: String?, project: Project?): PhpType? {
        if (expression == null || project == null || !expression.contains(key))
            return null

        val (entityTypeId, methodName) = expression.replace("#$key", "").split(splitKey)

        val fileBasedIndex = FileBasedIndex.getInstance()

        val contentEntity =  fileBasedIndex.getValue(ContentEntityIndex.KEY, entityTypeId, project)
        if (contentEntity != null) {
            return PhpType().add(contentEntity.fqn.letIf(possibleMethods.containsKey(methodName)) { fqn -> fqn + possibleMethods[methodName] })
        }

        val configEntity = fileBasedIndex.getValue(ConfigEntityIndex.KEY, entityTypeId, project)
        if (configEntity != null) {
            return PhpType().add(configEntity.letIf(possibleMethods.containsKey(methodName)) { fqn -> fqn + possibleMethods[methodName] })
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