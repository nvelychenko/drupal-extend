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
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*
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
        val project = psiElement.project
        if (
            !project.drupalExtendSettings.isEnabled
            || DumbService.getInstance(project).isDumb
        ) {
            return null
        }

        val (signature, name) = getSignature(psiElement) ?: return null

        if (StringUtils.isBlank(signature)) {
            return null
        }

        val classReference = when (psiElement) {
            is MethodReference -> psiElement.classReference
            is FieldReference -> psiElement.classReference
            else -> return null
        }
        if (classReference !is Variable) return null

        val variableSignature = classReference.type.toString()
        return PhpType().add("#$key$variableSignature$endKey$name")
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


    override fun complete(expression: String, project: Project): PhpType? {
        if (!expression.contains(endKey)) return null
        val (signatures, fieldName) = expression.substring(2).split(endKey)

        val entityTypes = mutableListOf<String>()
        if (signatures.contains("#" + EntityFromStorageTypeProvider.KEY) && signatures.contains(
                EntityFromStorageTypeProvider.SPLIT_KEY
            )
        ) {
            val entityTypeId = signatures.substringAfter(EntityFromStorageTypeProvider.KEY)
                .substringBefore(EntityFromStorageTypeProvider.SPLIT_KEY)
            entityTypes.add(entityTypeId)
        } else {
            val objects = mutableListOf<PhpNamedElement>()

            val index = PhpIndex.getInstance(project)

            for (signature in signatures.split('|')) {
                if (signature.startsWith('#')) {
                    objects.addAll(index.getBySignature(signature))
                } else {
                    objects.addAll(index.getAnyByFQN(signature))
                }
            }


            for (clazz in objects) {
                if (clazz !is PhpClass) continue

                val entity = FileBasedIndex.getInstance().getValue(ContentEntityFqnIndex.KEY, clazz.fqn, project)
                    ?: continue

                val entityTypeId = entity.entityTypeId
                entityTypes.add(entityTypeId)
            }
        }

        val type = PhpType()

        entityTypes.forEach {
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

    override fun getBySignature(
        expression: String,
        visited: Set<String?>?,
        depth: Int,
        project: Project?
    ): Collection<PhpClass> {
        return emptyList()
    }


}