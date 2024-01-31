package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.github.nvelychenko.drupalextend.index.FieldTypeIndex
import com.github.nvelychenko.drupalextend.index.FieldsIndex
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.FieldReference
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4
import org.apache.commons.lang3.StringUtils


/**
 * $item_list = $entity->get('field_foo');
 *     ↑
 */
class FieldItemListTypeProvider : PhpTypeProvider4 {

    private val fileBasedIndex: FileBasedIndex by lazy {
        FileBasedIndex.getInstance()
    }

    private val endKey = '\u3339'

    override fun getKey(): Char {
        return '\u3338'
    }

    override fun getType(psiElement: PsiElement): PhpType? {
        if (DumbService.getInstance(psiElement.project).isDumb) {
            return null
        }

        val (signature, name) = getSignature(psiElement) ?: return null

        if (StringUtils.isBlank(signature)) {
            return null
        }

        if (!signature.contains(EntityStorageTypeProvider.Util.SPLIT_KEY)) {
            return null
        }
        val entityTypeId = signature.substringAfter(EntityStorageTypeProvider.Util.SPLIT_KEY).substringBefore('.')

        return PhpType().add("#$key$entityTypeId$endKey$name")
    }

    private fun getSignature(psiElement: PsiElement): Array<String>? {
        return when (psiElement) {
            is MethodReference -> {
                val parameters = psiElement.parameters
                if (psiElement.name != "get" || parameters.isEmpty()) {
                    return null
                }

                val firstParam = parameters[0]
                if (firstParam !is StringLiteralExpression) {
                    return null
                }

                arrayOf(psiElement.signature, firstParam.contents)
            }

            is FieldReference -> {
                arrayOf(psiElement.signature, psiElement.name ?: "")
            }

            else -> null
        }
    }


    override fun complete(expression: String?, project: Project?): PhpType? {
        if (expression == null || project == null || !expression.contains(key))
            return null

        val (entityTypeId, fieldName) = expression.replace("#$key", "").split(endKey)

        val allScope = GlobalSearchScope.allScope(project)

        val entityTypeIndex = fileBasedIndex.getValues(ContentEntityIndex.KEY, entityTypeId, allScope)

        if (entityTypeIndex.isEmpty()) return null

        val entityType = entityTypeIndex.first()

        val fieldIndex = fileBasedIndex
            .getValues(FieldsIndex.KEY, "${entityType.entityTypeId}|${fieldName}", allScope)

        if (fieldIndex.isEmpty()) return null

        val fieldTypeIndex = fileBasedIndex.getValues(FieldTypeIndex.KEY, fieldIndex.first().fieldType, allScope)

        if (fieldTypeIndex.isEmpty()) return null

        val listClassFqn = if (fieldTypeIndex.first().listClassFqn != FieldTypeIndex.DUMMY_LIST_CLASS) {
            fieldTypeIndex.first().listClassFqn
        } else {
            "\\Drupal\\Core\\Field\\FieldItemList"
        }

        return PhpType().add(listClassFqn)
    }

    override fun getBySignature(
        expression: String,
        visited: Set<String?>?,
        depth: Int,
        project: Project?
    ): Collection<PhpClass> {
        return emptyList()
    }


}