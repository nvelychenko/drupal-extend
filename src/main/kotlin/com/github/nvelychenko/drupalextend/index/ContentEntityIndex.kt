package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.forms.Settings
import com.github.nvelychenko.drupalextend.index.types.DrupalContentEntity
import com.github.nvelychenko.drupalextend.util.isValidForIndex
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.PhpClass
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import java.io.DataInput
import java.io.DataOutput

class ContentEntityIndex : FileBasedIndexExtension<String, DrupalContentEntity>() {
    private val myKeyDescriptor: KeyDescriptor<String> = EnumeratorStringDescriptor()

    private val myDataExternalizer: DataExternalizer<DrupalContentEntity> =
        object : DataExternalizer<DrupalContentEntity> {
            override fun save(out: DataOutput, value: DrupalContentEntity) {
                out.writeUTF(value.entityTypeId)
                out.writeUTF(value.fqn)
                out.writeInt(value.keys.size)
                for (key in value.keys) {
                    out.writeUTF(key.key)
                    out.writeUTF(key.value)
                }

                out.writeUTF(value.storageHandler)
            }

            override fun read(input: DataInput): DrupalContentEntity {
                val entityType = input.readUTF()
                val fqn = input.readUTF()

                val keys = hashMapOf<String, String>()
                for (i in 1..input.readInt()) {
                    keys[input.readUTF()] = input.readUTF()
                }

                val sqlStorageHandler = input.readUTF()

                return DrupalContentEntity(entityType, fqn, keys, sqlStorageHandler)
            }
        }

    override fun getName(): ID<String, DrupalContentEntity> {
        return KEY
    }

    override fun getIndexer(): DataIndexer<String, DrupalContentEntity, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, DrupalContentEntity>()
            val psiFile = inputData.psiFile

            if (!isValidForIndex(inputData)) {
                return@DataIndexer map
            }

            when (psiFile) {
                is YAMLFile -> processYml(map, psiFile)
                is PhpFile -> processPhp(map, psiFile)
            }

            map
        }
    }

    private fun processYml(map: HashMap<String, DrupalContentEntity>, psiFile: YAMLFile) {
        val file = psiFile.virtualFile
        val baseDir = psiFile.project.guessProjectDir()

        if (baseDir != null && file != null) {
            val relativePath = VfsUtil.getRelativePath(file, baseDir, '/') ?: return

            val configDIr = Settings.getInstance(psiFile.project).configDir;
            if (!relativePath.contains(configDIr)) {
                return
            }
        }

        if (!psiFile.name.startsWith("eck.eck_entity_type.")) {
            return
        }

        PsiTreeUtil.getChildOfType(psiFile, YAMLDocument::class.java)?.topLevelValue?.children?.forEach { node ->
            if (node is YAMLKeyValue && node.keyText == "id") {
                map[node.valueText] = DrupalContentEntity(
                    node.valueText,
                    "\\Drupal\\eck\\Entity\\EckEntity",
                    hashMapOf(),
                    "\\Drupal\\Core\\Entity\\Sql\\SqlContentEntityStorage"
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
            resolvedKeys[key] = (getPhpDocParameter(contentEntityTypeDocText, key) ?: continue)
        }

        val sqlStorageHandler = getPhpDocParameter(contentEntityTypeDocText, "storage") ?: "\\Drupal\\Core\\Entity\\Sql\\SqlContentEntityStorage"

        map[id] = DrupalContentEntity(id, phpClass.fqn, resolvedKeys, sqlStorageHandler)
    }

    private fun getPhpDocParameter(phpDocText: String, id: String): String? {
        @Suppress("RegExpUnnecessaryNonCapturingGroup")
        val entityTypeMatch = Regex("${id}(?:\"?)\\s*=\\s*\"([^\"]+)\"").find(phpDocText)

        return entityTypeMatch?.groups?.get(1)?.value
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = myKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<DrupalContentEntity> = myDataExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file -> file.fileType == YAMLFileType.YML || file.fileType == PhpFileType.INSTANCE }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 11

    companion object {
        val KEY = ID.create<String, DrupalContentEntity>("com.github.nvelychenko.drupalextend.index.content_types")
    }

}