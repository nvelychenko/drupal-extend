package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.extensions.isValidForIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.VoidDataExternalizer
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping

class LibrariesIndex : FileBasedIndexExtension<String, Void?>() {
    override fun getIndexer(): DataIndexer<String, Void?, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, Void?>()

            val yamlFile = inputData.psiFile as YAMLFile
            val file = inputData.file
            val fileName = file.name

            if (!inputData.project.drupalExtendSettings.isEnabled) return@DataIndexer map
            if (!fileName.endsWith(".libraries.yml")) return@DataIndexer map
            if (!inputData.isValidForIndex()) return@DataIndexer map

            val moduleName = file.parent?.name
            if (moduleName != fileName.substringBefore(".libraries.yml")) return@DataIndexer map

            PsiTreeUtil.findChildOfType(yamlFile, YAMLMapping::class.java)
                ?.keyValues
                ?.forEach { map["$moduleName/${it.keyText}"] = null }

            map
        }
    }

    private val myKeyDescriptor: KeyDescriptor<String> = EnumeratorStringDescriptor()

    private val myDataExternalizer = VoidDataExternalizer.INSTANCE

    override fun getName(): ID<String, Void?> {
        return KEY
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = myKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<Void?> = myDataExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file: VirtualFile -> file.fileType == YAMLFileType.YML }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 0

    companion object {
        val KEY = ID.create<String, Void?>("com.github.nvelychenko.drupalextend.index.libraries")
    }

}