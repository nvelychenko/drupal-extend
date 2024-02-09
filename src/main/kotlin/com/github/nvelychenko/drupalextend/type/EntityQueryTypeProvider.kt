package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.data.configEntitySqlFactoryClass
import com.github.nvelychenko.drupalextend.data.contentEntitySqlFactoryClass
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.ConfigEntityIndex
import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.type.EntityStorageTypeProvider.Util.SPLIT_KEY
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4

/**
 * $query = \Drupal::entityTypeManager()->getStorage('paragraph')->getQuery();
 *    ↑
 */
class EntityQueryTypeProvider : PhpTypeProvider4 {

    private val possibleMethods = arrayOf(
        "condition",
        "allRevisions",
        "latestRevision",
        "currentRevision",
        "accessCheck",
        "count",
        "tableSort",
        "sort",
        "range",
        "pager",
        "notExists",
        "exists",
        "entityQuery",
        "getQuery",
    )

    private val fileBasedIndex: FileBasedIndex by lazy {
        FileBasedIndex.getInstance()
    }

    override fun getKey(): Char {
        return KEY
    }

    override fun getType(psiElement: PsiElement): PhpType? {
        val project = psiElement.project
        if (
            !project.drupalExtendSettings.isEnabled
            || DumbService.getInstance(project).isDumb
            || psiElement !is MethodReference
            || !possibleMethods.contains(psiElement.name)
        ) {
            return null
        }

        val isStaticEntityQuery = psiElement.classReference?.name == "Drupal"
                && psiElement.name == "entityQuery"

        if (psiElement.isStatic && isStaticEntityQuery) {
            val firstParameter = psiElement.parameterList?.getParameter(0)
            if (firstParameter is StringLiteralExpression && firstParameter.contents.isNotEmpty()) {
                return PhpType().add("#$key${firstParameter.contents}$END_KEY")
            }
        }

        val signature = psiElement.signature

        val entityTypeId = if (signature.contains(SPLIT_KEY)) {
            signature.substringAfter(SPLIT_KEY).substringBefore(".")
        } else if (signature.contains(END_KEY)) {
            signature.substringAfter(KEY).substringBefore(END_KEY)
        } else {
            return null
        }

        return returnCachedType(project, "#$key$entityTypeId$END_KEY")
    }

    override fun complete(expression: String?, project: Project?): PhpType? {
        if (expression == null || project == null || !expression.startsWith("#$key"))
            return null

        val entityTypeId = expression.substring(2).substringBefore(END_KEY)

        val contentEntity = fileBasedIndex.getValue(ContentEntityIndex.KEY, entityTypeId, project)
        if (contentEntity != null) {
            val type = PhpType().add(contentEntitySqlFactoryClass)
            putTypeInCache(project, expression, type)
            return type
        }

        val configEntity = fileBasedIndex.getValue(ConfigEntityIndex.KEY, entityTypeId, project)
        if (configEntity != null) {
            val type = PhpType().add(configEntitySqlFactoryClass)
            putTypeInCache(project, expression, type)
            return type
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

    companion object {
        const val END_KEY = '\u0426'
        const val KEY = '\u0425'
    }


}