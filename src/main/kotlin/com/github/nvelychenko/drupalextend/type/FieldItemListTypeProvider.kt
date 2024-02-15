package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.ContentEntityFqnIndex
import com.github.nvelychenko.drupalextend.index.FieldTypeIndex
import com.github.nvelychenko.drupalextend.index.FieldsIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4
import com.github.nvelychenko.drupalextend.type.EntityFromStorageTypeProvider.Companion.SPLIT_KEY as EntitySplitKey


/**
 * $item_list = $entity->get('field_foo');
 *     ↑
 */
class FieldItemListTypeProvider : PhpTypeProvider4 {

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
        ) {
            return null
        }
        val firstParameter = psiElement.parameters.firstOrNull() ?: return null
        if (psiElement.name != "get" || firstParameter !is StringLiteralExpression || firstParameter.contents.isBlank()) return null
        val name = firstParameter.contents

        val classReference = (psiElement as MemberReference).classReference

        val signature = when (classReference) {
            is Variable -> classReference.type
            is MethodReference -> classReference.type
            else -> return null
        }.toString()

        return PhpType().add("#$key${compressSignature(signature)}$END_KEY$name")
    }

    override fun complete(expression: String, project: Project): PhpType? {
        if (!expression.contains(END_KEY)) return null
        val (signature, fieldName) = expression.substring(2).split(END_KEY)
        val type = PhpType()

        getEntityTypes(decompressSignature(signature), project).forEach {
            val field = fileBasedIndex.getValue(FieldsIndex.KEY, "${it}|${fieldName}", project) ?: return null
            val fieldType = fileBasedIndex.getValue(FieldTypeIndex.KEY, field.fieldType, project) ?: return null

            type.add(
                if (fieldType.listClassFqn != FieldTypeIndex.DUMMY_LIST_CLASS) {
                    fieldType.listClassFqn
                } else {
                    "\\Drupal\\Core\\Field\\FieldItemList"
                }
            )

            type.add("${fieldType.fqn}[]")
        }

        return type
    }

    private fun getEntityTypes(
        signatures: String,
        project: Project
    ): Set<String> {
        val entityTypes = mutableListOf<String>()
        if (signatures.contains(EntitySplitKey)) {
            signatures
                .substringAfter(EntityFromStorageTypeProvider.KEY)
                .substringBefore(EntitySplitKey)
                .let { entityTypes.add(it) }

            return entityTypes.toSet()
        }

        getClassesFromSignature(signatures, project)
            .mapNotNull { fileBasedIndex.getValue(ContentEntityFqnIndex.KEY, it.fqn, project) }
            .forEach { entityTypes.add(it.entityTypeId) }

        return entityTypes.toSet()
    }

    override fun getBySignature(
        expression: String,
        visited: Set<String?>?,
        depth: Int,
        project: Project?
    ): Collection<PhpClass> {
        return emptyList()
    }

    companion object {
        const val END_KEY = '\u3339'
        const val KEY = '\u3338'
    }

}