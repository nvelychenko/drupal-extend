package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.ConfigEntityIndex
import com.github.nvelychenko.drupalextend.index.ContentEntityFqnIndex
import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.github.nvelychenko.drupalextend.index.FieldsIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.type.EntityStorageTypeProvider.Util.SPLIT_KEY
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.FieldReference
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.PhpTypedElement
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4

/**
 * $user = $node->get('user')->entity();
 *    ↑
 */
class EntityReferenceFieldTypeProvider : PhpTypeProvider4 {

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
            || psiElement.signature.isBlank()
            || psiElement.classReference !is PhpTypedElement
        ) {
            return null
        }

        val classReference = psiElement.classReference as PhpTypedElement

        val types = classReference.type.types

        val keyTypes = types.filter { it.contains(FieldItemListTypeProvider.END_KEY) }

        if (keyTypes.isEmpty()) return null

        val signature = compressSignature(types.joinToString("|"))
        val type =
            PhpType().add("#$key$signature$END_KEY${keyTypes.last().substringAfter(FieldItemListTypeProvider.END_KEY)}")

        return type
    }

    override fun complete(expression: String, project: Project): PhpType? {
        if (!expression.startsWith("#$key"))
            return null

        val (rawSignature, fieldName) = expression.substring(2).split(END_KEY)
        val signature = decompressSignature(rawSignature)

        val type = PhpType()
        if (signature.contains(SPLIT_KEY)) {
            val contentEntityId = signature.substringAfter(SPLIT_KEY).substringBefore(".")
            addType(contentEntityId, fieldName, project, type)
            return type
        }

        getClassesFromSignature(signature, project)
            .mapNotNull { fileBasedIndex.getValue(ContentEntityFqnIndex.KEY, it.fqn, project) }
            .forEach { contentEntity ->
                if (addType(contentEntity.entityTypeId, fieldName, project, type)) return@forEach
            }

        return type

    }

    private fun addType(
        contentEntityId: String,
        fieldName: String,
        project: Project,
        type: PhpType
    ): Boolean {
        val field = fileBasedIndex
            .getValue(FieldsIndex.KEY, "${contentEntityId}|${fieldName}", project)
            ?: return true

        if (field.targetType.isNullOrEmpty()) return true
        fileBasedIndex
            .getValue(ContentEntityIndex.KEY, field.targetType, project)
            ?.let { type.add(it.fqn) }

        fileBasedIndex
            .getValue(ConfigEntityIndex.KEY, field.targetType, project)
            ?.let { type.add(it) }
        return false
    }

    override fun getBySignature(
        expression: String,
        visited: Set<String?>?,
        depth: Int,
        project: Project?
    ): Collection<PhpNamedElement>? {
        return null
    }

    companion object {
        const val END_KEY = '\u0428'
        const val KEY = '\u0427'
    }


}