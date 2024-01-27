package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.util.isValidForIndex
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.PhpClass

class ConfigEntityIndex : FileBasedIndexExtension<String, String>() {
    private val myKeyDescriptor: KeyDescriptor<String> = EnumeratorStringDescriptor()

    private val myDataExternalizer: DataExternalizer<String> = EnumeratorStringDescriptor.INSTANCE;
    override fun getName(): ID<String, String> {
        return KEY
    }

    override fun getIndexer(): DataIndexer<String, String, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, String>()
            val psiFile = inputData.psiFile

            if (!isValidForIndex(inputData)) {
                return@DataIndexer map
            }

            when (psiFile) {
                is PhpFile -> processPhp(map, psiFile)
            }

            map
        }
    }


    private fun processPhp(map: HashMap<String, String>, phpFile: PhpFile) {
        val phpClass = PsiTreeUtil.findChildOfType(phpFile, PhpClass::class.java) ?: return
        if (phpClass.docComment !is PhpDocComment) return

        val contentEntityTypes = (phpClass.docComment as PhpDocComment).getTagElementsByName("@ConfigEntityType")
        if (contentEntityTypes.isEmpty()) {
            return
        }

        val contentEntityTypeDocText = contentEntityTypes[0].text

        val id = getPhpDocParameter(contentEntityTypeDocText, "id") ?: return

        map[id] = phpClass.fqn;
    }

    private fun getPhpDocParameter(phpDocText: String, id: String): String? {
        @Suppress("RegExpUnnecessaryNonCapturingGroup")
        val entityTypeMatch = Regex("${id}(?:\"?)\\s*=\\s*\"([^\"]+)\"").find(phpDocText)

        return entityTypeMatch?.groups?.get(1)?.value
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = myKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<String> = myDataExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file -> file.fileType == PhpFileType.INSTANCE }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 0

    companion object {
        val KEY = ID.create<String, String>("com.github.nvelychenko.drupalextend.index.config_types")
    }

}