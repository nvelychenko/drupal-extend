package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.data.configEntitySqlFactoryClass
import com.github.nvelychenko.drupalextend.data.contentEntitySqlFactoryClass
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.*
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.type.EntityStorageTypeProvider.Util.SPLIT_KEY
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4
import org.apache.commons.lang3.StringUtils

/**
 * $user = $node->get('user')->entity();
 *    ↑
 */
class EntityReferenceFieldTypeProvider : PhpTypeProvider4 {

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
            || psiElement !is FieldReference
            || psiElement.name != "entity"
        ) {
            return null
        }

        if (StringUtils.isBlank(psiElement.signature)) {
            return null
        }

        val parentSignature = when (val classReference = psiElement.classReference) {
            is Variable -> classReference.signature
            is MethodReference -> classReference.signature
            else -> return null
        }
        val entitySignature = parentSignature.substringAfter(FieldItemListTypeProvider.KEY).substringBefore(FieldItemListTypeProvider.END_KEY)
        val fieldName = parentSignature.substringAfter(FieldItemListTypeProvider.END_KEY).substringBefore('|')
        return PhpType().add("#$key$entitySignature$END_KEY$fieldName")
    }

    override fun complete(expression: String?, project: Project?): PhpType? {
        if (expression == null || project == null || !expression.startsWith("#$key"))
            return null

        val parentTypes = expression.substring(2).substringBefore(END_KEY);
        val fieldName = expression.substringAfter(END_KEY)


        val index = PhpIndex.getInstance(project)
        val entityTypes = PhpType();
        parentTypes.split('|').forEach {
            entityTypes.add(index.completeType(project, PhpType().add(it), mutableSetOf<String>()))
        }
        entityTypes.types.forEach {
            val entityType = fileBasedIndex.getValue(ContentEntityFqnIndex.KEY, it, project) ?: return@forEach
            val field = fileBasedIndex.getValue(FieldsIndex.KEY, "${entityType.entityTypeId}|${fieldName}", project) ?: return@forEach
            if (field.targetType.isNullOrEmpty()) return@forEach
            val targetEntity = fileBasedIndex.getValue(ContentEntityIndex.KEY, field.targetType, project) ?: return@forEach
            return PhpType().add(targetEntity.fqn)
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
        const val END_KEY = '\u0428'
        const val KEY = '\u0427'
    }


}