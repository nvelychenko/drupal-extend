package com.github.nvelychenko.drupalextend.extensions

import com.github.nvelychenko.drupalextend.project.DEFAULT_CONFIG_SYNC_DIRECTORY
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.ConcurrencyUtil
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Excludes possible test directories, files from index.
 */
fun FileContent.isValidForIndex(): Boolean {
    val fileName = psiFile.name
    if (fileName.startsWith(".") || fileName.endsWith("Test")) {
        return false
    }

    val projectDir = project.guessProjectDir() ?: return true

    val relativePath = VfsUtil.getRelativePath(file, projectDir, '/')
    return !(relativePath != null && (relativePath.contains("/Test/")
            || relativePath.contains("/Tests/") || relativePath.contains("/Fixture/")
            || relativePath.contains("/tests/") || relativePath.contains("/fixtures/")
            || relativePath.contains("/tests/") || relativePath.contains("/fixture/")
            || relativePath.contains("/Fixtures/")))
}

fun FileContent.isInConfigurationDirectory(): Boolean {
    val configDirectory = project.drupalExtendSettings.configDirectory
    if (configDirectory == DEFAULT_CONFIG_SYNC_DIRECTORY) {
        val baseDir = project.guessProjectDir() ?: return true

        val relativePath =
            VfsUtil.getRelativePath(file, baseDir, '/') ?: return true

        return relativePath.contains(DEFAULT_CONFIG_SYNC_DIRECTORY)
    }

    return VfsUtil.isEqualOrAncestor(configDirectory, file.path)
}

private val keyForProvider: ConcurrentMap<String, Key<CachedValue<*>>> = ConcurrentHashMap()

private fun <K> getKey(name: String): Key<CachedValue<K>> {
    @Suppress("UNCHECKED_CAST")
    return (keyForProvider[name] ?: ConcurrencyUtil.cacheOrGet(
        keyForProvider,
        name,
        Key.create(name)
    )) as Key<CachedValue<K>>
}

fun <K : Any, V> FileBasedIndex.getValue(
    id: ID<K, V>,
    key: K,
    project: Project
): V? {
    return getValues(id, key, GlobalSearchScope.allScope(project))
        .takeIf { it.isNotEmpty() }
        ?.first()
}

@Synchronized
fun <K : Any, V> FileBasedIndex.getAllProjectKeys(
    id: ID<K, V>,
    project: Project,
): List<K> {
    return CachedValuesManager.getManager(project).getCachedValue(
        project,
        getKey(id.name),
        {
            CachedValueProvider.Result.create(
                getAllIndexKeys(id, project, this),
                getModificationTrackerForIndexId(project, id, this)
            )
        },
        false
    )
}

@Synchronized
fun <K : Any, V> FileBasedIndex.getAllValuesWithKeyPrefix(
    id: ID<K, V>,
    prefix: String,
    project: Project
): List<V> {
    return CachedValuesManager.getManager(project).getCachedValue(
        project,
        getKey("${id.name}${prefix}"),
        {
            val result = getAllKeys(id, project)
                .filter { it is String && it.contains(prefix) }
                .mapNotNull {
                    getValues(id, it, GlobalSearchScope.allScope(project)).firstOrNull()
                }
                .toList()
            CachedValueProvider.Result.create(
                result, getModificationTrackerForIndexId(project, id, this)
            )
        },
        false
    )
}

@Synchronized
private fun <K : Any, V> getAllIndexKeys(
    id: ID<K, V>,
    project: Project,
    index: FileBasedIndex
): List<K> {
    val items = mutableSetOf<K>()
    for (indexKey in index.getAllKeys(id, project)) {
        val inScope = !index.processValues(id, indexKey, null, { _, _ -> false }, GlobalSearchScope.allScope(project))

        if (inScope) {
            items.add(indexKey)
        }
    }
    return items.toList()
}

@Synchronized
fun <K : Any, V> getModificationTrackerForIndexId(
    project: Project,
    id: ID<K, V>,
    index: FileBasedIndex
): ModificationTracker {
    return ModificationTracker {
        index.getIndexModificationStamp(id, project)
    }
}

