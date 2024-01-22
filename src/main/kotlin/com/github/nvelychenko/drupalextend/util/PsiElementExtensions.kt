package com.github.nvelychenko.drupalextend.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ID


val PsiElement.parents: Iterable<PsiElement>
    get() = object : Iterable<PsiElement> {
        override fun iterator(): Iterator<PsiElement> {
            var file = this@parents

            return object : Iterator<PsiElement> {
                override fun hasNext() = file.parent != null
                override fun next(): PsiElement {
                    file = file.parent
                    return file
                }
            }
        }
    }

fun <K : Any, V> getIndexValueForKey(
    id: ID<K, V>,
    key: K,
    project: Project
): V? {
    return FileBasedIndex.getInstance().getValues(id, key, GlobalSearchScope.allScope(project))
        .takeIf { it.isNotEmpty() }
        ?.first()
}

fun getModificationTrackerForIndexId(project: Project, id: ID<*, *>): ModificationTracker {
    return ModificationTracker {
        FileBasedIndex.getInstance().getIndexModificationStamp(id, project)
    }
}

