package com.github.nvelychenko.drupalextend.util

import com.github.nvelychenko.drupalextend.index.*
import com.intellij.util.indexing.FileBasedIndex

fun clearPluginIndexes() {
    arrayOf(
        ConfigEntityIndex.KEY,
        ContentEntityIndex.KEY,
        ContentEntityFqnIndex.KEY,
        FieldsIndex.KEY,
        FieldTypeIndex.KEY,
        RenderElementIndex.KEY,
        ThemeIndex.KEY
    ).forEach { FileBasedIndex.getInstance().requestRebuild(it) }
}
