package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.extensions.isSuperInterfaceOf
import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.github.nvelychenko.drupalextend.patterns.Patterns.METHOD_WITH_FIRST_STRING_PARAMETER
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.type.EntityStorageTypeProvider.Util.SPLIT_KEY
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4


/**
 * $entity_storage = \Drupal::entityTypeManager()->getStorage('node');
 *       ↑
 */
class EntityStorageTypeProvider : PhpTypeProvider4 {

    private val entityTypeManagerInterface = "\\Drupal\\Core\\Entity\\EntityTypeManagerInterface"

    private val fileBasedIndex: FileBasedIndex by lazy {
        FileBasedIndex.getInstance()
    }

    override fun getKey(): Char {
        return Util.KEY
    }

    override fun getType(methodReference: PsiElement): PhpType? {
        val project = methodReference.project
        if (
            !project.drupalExtendSettings.isEnabled
            || DumbService.getInstance(project).isDumb
            || !METHOD_WITH_FIRST_STRING_PARAMETER.accepts(methodReference)
            || methodReference !is MethodReference
            || methodReference.name != "getStorage"
        ) {
            return null
        }

        val parameterList = methodReference.childrenOfType<ParameterList>().first()
        val firstParameter = parameterList.getParameter(0) as StringLiteralExpression

        if (firstParameter.contents.isBlank()) return null

        return PhpType().add("#$key${compressSignature(methodReference.signature)}$SPLIT_KEY${firstParameter.contents}")
    }

    override fun complete(expression: String, project: Project): PhpType? {
        if (!expression.contains(SPLIT_KEY)) return null

        val (originalSignature, entityTypeId) = expression.substring(2).split(SPLIT_KEY)

        val entityType = fileBasedIndex.getValue(ContentEntityIndex.KEY, entityTypeId, project) ?: return null

        val phpIndex = PhpIndex.getInstance(project)
        val namedCollection = mutableListOf<PhpNamedElement>()
        for (partialSignature in decompressSignature(originalSignature).split('|')) {
            namedCollection.addAll(phpIndex.getBySignature(partialSignature))
        }

        val methods = namedCollection.filterIsInstance<Method>()

        if (methods.isEmpty()) return null

        val entityTypeManagerInterface =
            phpIndex.getInterfacesByFQN(entityTypeManagerInterface).takeIf { it.isNotEmpty() }?.first() ?: return null

        val entityStorageClass = phpIndex.getClassesByFQN(entityType.storageHandler)
            .takeIf { it.isNotEmpty() }
            ?.first()
            ?: return null

        val type = PhpType()
        methods.forEach { method ->
            val clazz = method.containingClass ?: return@forEach
            if (clazz.fqn == entityTypeManagerInterface.fqn || clazz.isSuperInterfaceOf(entityTypeManagerInterface)) {
                entityStorageClass.implementsList.referenceElements
                    .map { it.fqn }
                    .forEach(type::add)
            }
        }

        return type
    }

    override fun getBySignature(
        expression: String, visited: Set<String?>?, depth: Int, project: Project?
    ): Collection<PhpNamedElement>? {
        return null
    }

    object Util {
        const val SPLIT_KEY = '\u3333'
        const val KEY = '\u3334'
    }


}