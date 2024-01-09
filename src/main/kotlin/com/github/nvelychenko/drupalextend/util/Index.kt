package com.github.nvelychenko.drupalextend.util

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.indexing.FileContent

fun isValidForIndex(inputData: FileContent): Boolean {

    val fileName = inputData.psiFile.name
    if (fileName.startsWith(".") || fileName.endsWith("Test")) {
        return false
    }

    // possible fixture or test file
    // to support also library paths, only filter them on project files
    val relativePath = VfsUtil.getRelativePath(inputData.file, inputData.project.baseDir, '/')
    return !(relativePath != null && (relativePath.contains("/Test/")
            || relativePath.contains("/Tests/") || relativePath.contains("/Fixture/")
            || relativePath.contains("/tests/") || relativePath.contains("/fixtures/")
            || relativePath.contains("/tests/") || relativePath.contains("/fixture/")
            || relativePath.contains("/Fixtures/")))
}