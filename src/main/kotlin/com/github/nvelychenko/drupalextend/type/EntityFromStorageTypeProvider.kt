package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.extensions.isSuperInterfaceOf
import com.github.nvelychenko.drupalextend.index.ConfigEntityIndex
import com.github.nvelychenko.drupalextend.index.ContentEntityFqnIndex
import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.jsonSchema.impl.nestedCompletions.letIf
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4

/**
 * $paragraph = \Drupal::entityTypeManager()->getStorage('paragraph')->load(1);
 *     ↑
 */
class EntityFromStorageTypeProvider : PhpTypeProvider4 {

    private val fileBasedIndex: FileBasedIndex by lazy {
        FileBasedIndex.getInstance()
    }

    private val possibleMethods = mutableMapOf(
        Pair("load", ""),
        Pair("loadByProperties", "[]"),
        Pair("loadMultiple", "[]"),
        Pair("create", ""),
    )

    private val unclearKey = '\u0422'

    override fun getKey(): Char {
        return KEY
    }

    override fun getType(psiElement: PsiElement): PhpType? {
        val project = psiElement.project
        if (
            !project.drupalExtendSettings.isEnabled
            || DumbService.getInstance(project).isDumb
            || psiElement !is MethodReference || psiElement.isStatic
            || psiElement.name.isNullOrEmpty()
            || !possibleMethods.containsKey(psiElement.name)
        ) {
            return null
        }

        val signature = psiElement.signature

        if (!signature.contains(EntityStorageTypeProvider.Util.SPLIT_KEY)) {
            return returnCachedType(project, "#$key${compressSignature(signature)}$unclearKey${psiElement.name}")
        }

        val entityTypeId =
            signature.substringAfter(EntityStorageTypeProvider.Util.SPLIT_KEY).substringBefore(".${psiElement.name!!}")

        return returnCachedType(project, "#$key$entityTypeId$SPLIT_KEY${psiElement.name}")
    }

    override fun complete(s: String, project: Project): PhpType? {
        val expression = s.substring(2)

        val entityTypeId: String?
        val methodName: String

        if (expression.contains(SPLIT_KEY)) {
            val split = expression.split(SPLIT_KEY)
            entityTypeId = split[0]
            methodName = split[1]
        } else if (expression.contains(unclearKey)) {
            val result = unknownStorageProcess(decompressSignature(expression), project) ?: return null
            entityTypeId = result.second
            methodName = result.first
        } else {
            return null
        }

        val type = PhpType()

        val contentEntity = fileBasedIndex.getValue(ContentEntityIndex.KEY, entityTypeId, project)
        if (contentEntity != null) {
            type.add(contentEntity.fqn.letIf(possibleMethods.containsKey(methodName)) { fqn -> fqn + possibleMethods[methodName] })
            if (!type.isEmpty) {
                putTypeInCache(project, expression, type)
            }

            return type
        }

        val configEntity = fileBasedIndex.getValue(ConfigEntityIndex.KEY, entityTypeId, project)
        if (configEntity != null) {
            type.add(configEntity.letIf(possibleMethods.containsKey(methodName)) { fqn -> fqn + possibleMethods[methodName] })
            if (!type.isEmpty) {
                putTypeInCache(project, expression, type)
            }

            return type
        }

        return null

    }

    /**
     * In case if only storage is present with getStorage.
     */
    private fun unknownStorageProcess(expression: String, project: Project): Pair<String, String>? {
        val (signature, methodName) = expression.split(unclearKey)

        val phpIndex = PhpIndex.getInstance(project)
        val classes = mutableSetOf<String>()
        for (partialSignature in signature.split('|')) {
            if (partialSignature.contains("#M#C") && partialSignature.contains(".$methodName")) {
                classes.add(partialSignature.substringAfter("#M#C").substringBefore(".$methodName"))
            }
        }

        if (classes.isEmpty()) return null

        val entityStorageInterface =
            phpIndex.getInterfacesByFQN("\\Drupal\\Core\\Entity\\EntityStorageInterface").firstOrNull() ?: return null

        val fileBasedIndex = FileBasedIndex.getInstance()

        classes.forEach { classFqn ->
            fileBasedIndex.getValue(ContentEntityFqnIndex.KEY, classFqn, project)
                ?.let { return Pair(methodName, it.entityTypeId) }

            if (classFqn == "\\Drupal\\Core\\Entity\\EntityStorageInterface") return@forEach

            var clazz = phpIndex.getAnyByFQN(classFqn).firstOrNull() ?: return@forEach

            if (clazz.isInterface && !classFqn.startsWith("\\Drupal\\Core\\Entity") && classFqn.contains("StorageInterface")) {
                PhpClassHierarchyUtils.getDirectSubclasses(clazz).takeIf { it.size == 1 }?.first()
                    ?.let { clazz = it }
            }

            if (clazz.isInterface || clazz.fqn == "\\Drupal\\Core\\Entity\\Sql\\SqlContentEntityStorage") {
                return@forEach
            }

            if (clazz.isSuperInterfaceOf(entityStorageInterface)) {
                val allHandlers = ContentEntityIndex.getAllHandlers(project)
                val fqn = clazz.fqn
                val contentEntity = allHandlers[fqn] ?: allHandlers[fqn.substring(1)] ?: return@forEach

                return Pair(methodName, contentEntity.entityTypeId)
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

    companion object {
        const val KEY = '\u0420'
        const val SPLIT_KEY = '\u0421'
    }
}