package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.extensions.isValidForIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.util.getPhpDocParameter
import com.intellij.openapi.util.text.StringUtil.unquoteString
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.psi.elements.PhpClass

class ConfigEntityIndex : FileBasedIndexExtension<String, String>() {
    private val myKeyDescriptor: KeyDescriptor<String> = EnumeratorStringDescriptor()

    private val myDataExternalizer: DataExternalizer<String> = EnumeratorStringDescriptor.INSTANCE
    override fun getName(): ID<String, String> {
        return KEY
    }

    override fun getIndexer(): DataIndexer<String, String, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, String>()
            val psiFile = inputData.psiFile

            if (!inputData.project.drupalExtendSettings.isEnabled) return@DataIndexer map

            if (!inputData.isValidForIndex()) return@DataIndexer map

            val phpClass = PsiTreeUtil.findChildOfType(psiFile, PhpClass::class.java) ?: return@DataIndexer map

            val id = phpClass.attributes.find { it.fqn == "\\Drupal\\Core\\Entity\\Attribute\\ConfigEntityType" }
                ?.arguments?.find { it.name == "id" }?.argument?.value?.let { unquoteString(it) }
            if (id != null) {
                map[id] = phpClass.fqn
                return@DataIndexer map
            }

            phpClass
                .takeIf {
                    it.docComment is PhpDocComment && it.docComment!!.getTagElementsByName("@ConfigEntityType")
                        .isNotEmpty()
                }
                ?.let { getPhpDocParameter(it.docComment!!.text, "id") }
                ?.let { map[it] = phpClass.fqn }

            map
        }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = myKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<String> = myDataExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file -> file.fileType == PhpFileType.INSTANCE }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 1

    companion object {
        val KEY = ID.create<String, String>("com.github.nvelychenko.drupalextend.index.config_types")
    }

}