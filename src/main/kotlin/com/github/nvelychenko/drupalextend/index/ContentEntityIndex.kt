package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.extensions.getModificationTrackerForIndexId
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.extensions.isInConfigurationDirectory
import com.github.nvelychenko.drupalextend.extensions.isValidForIndex
import com.github.nvelychenko.drupalextend.index.dataExternalizer.SerializedObjectDataExternalizer
import com.github.nvelychenko.drupalextend.index.types.DrupalContentEntity
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.util.getPhpDocParameter
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.PhpClass
import kotlinx.serialization.serializer
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

class ContentEntityIndex : FileBasedIndexExtension<String, DrupalContentEntity>() {
    private val myKeyDescriptor: KeyDescriptor<String> = EnumeratorStringDescriptor()

    override fun getIndexer(): DataIndexer<String, DrupalContentEntity, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, DrupalContentEntity>()

            if (!inputData.project.drupalExtendSettings.isEnabled) return@DataIndexer map

            val psiFile = inputData.psiFile

            if (!inputData.isValidForIndex()) {
                return@DataIndexer map
            }

            when (psiFile) {
                is PhpFile -> processPhp(map, psiFile)
                is YAMLFile -> {
                    if (inputData.isInConfigurationDirectory()) {
                        processYml(map, psiFile)
                    }
                }
            }

            map
        }
    }

    private fun processYml(map: HashMap<String, DrupalContentEntity>, psiFile: YAMLFile) {
        if (!psiFile.name.startsWith("eck.eck_entity_type.")) {
            return
        }

        PsiTreeUtil.getChildOfType(psiFile, YAMLDocument::class.java)?.topLevelValue?.children?.forEach { node ->
            if (node is YAMLKeyValue && node.keyText == "id") {
                map[node.valueText] = DrupalContentEntity(
                    node.valueText,
                    "\\Drupal\\eck\\Entity\\EckEntity",
                    hashMapOf(),
                    "\\Drupal\\Core\\Entity\\Sql\\SqlContentEntityStorage",
                    true,
                )
            }
        }
    }

    private fun processPhp(map: HashMap<String, DrupalContentEntity>, phpFile: PhpFile) {
        val phpClass = PsiTreeUtil.findChildOfType(phpFile, PhpClass::class.java) ?: return
        if (phpClass.docComment !is PhpDocComment) return

        val contentEntityTypes = (phpClass.docComment as PhpDocComment).getTagElementsByName("@ContentEntityType")
        if (contentEntityTypes.isEmpty()) {
            return
        }

        val contentEntityTypeDocText = contentEntityTypes[0].text

        val id = getPhpDocParameter(contentEntityTypeDocText, "id") ?: return

        val hardcodedKeys = arrayOf(
            "id",
            "revision",
            "bundle",
            "label",
            "langcode",
            "uuid",
            "status",
            "published",
            "uid",
            "owner",
            "revision_log_message",
            "revision_created",
            "revision_user",
        )

        val resolvedKeys = hashMapOf<String, String>()

        // @todo Implement better parsing for phpdoc.
        for (key in hardcodedKeys) {
            resolvedKeys[key] = (getPhpDocParameter(contentEntityTypeDocText, "\"${key}\"") ?: continue)
        }

        val sqlStorageHandler = getPhpDocParameter(contentEntityTypeDocText, "\"storage\"")
            ?: "\\Drupal\\Core\\Entity\\Sql\\SqlContentEntityStorage"

        map[id] = DrupalContentEntity(id, phpClass.fqn, resolvedKeys, sqlStorageHandler)
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = myKeyDescriptor

    private val myDataExternalizer: DataExternalizer<DrupalContentEntity> =
        SerializedObjectDataExternalizer(serializer<DrupalContentEntity>())

    override fun getName(): ID<String, DrupalContentEntity> {
        return KEY
    }

    override fun getValueExternalizer(): DataExternalizer<DrupalContentEntity> = myDataExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file -> file.fileType == YAMLFileType.YML || file.fileType == PhpFileType.INSTANCE }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 13

    companion object {
        val KEY = ID.create<String, DrupalContentEntity>("com.github.nvelychenko.drupalextend.index.content_types")

        private val index by lazy { FileBasedIndex.getInstance() }

        @Synchronized
        fun getAllHandlers(project: Project): HashMap<String, DrupalContentEntity> {
            return CachedValuesManager.getManager(project).getCachedValue(project) {
                val results = hashMapOf<String, DrupalContentEntity>()

                index.getAllKeys(KEY, project).forEach {
                    index.getValue(KEY, it, project)?.let { contentEntity ->
                        results[contentEntity.storageHandler] = contentEntity
                    }
                }

                CachedValueProvider.Result.create(results, getModificationTrackerForIndexId(project, KEY, index))
            }
        }

    }

}