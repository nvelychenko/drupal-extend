package com.github.nvelychenko.drupalextend.util

import com.github.nvelychenko.drupalextend.index.ContentEntityFqnIndex
import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.github.nvelychenko.drupalextend.index.FieldTypeIndex
import com.github.nvelychenko.drupalextend.index.FieldsIndex
import com.intellij.util.indexing.FileBasedIndex

fun clearPluginIndexes() {
    arrayOf(
        ContentEntityFqnIndex.KEY,
        ContentEntityIndex.KEY,
        FieldsIndex.KEY,
        FieldTypeIndex.KEY,
    ).forEach { FileBasedIndex.getInstance().requestRebuild(it) }
}
