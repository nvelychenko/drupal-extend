package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.index.types.DrupalContentEntity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.PhpClass
import java.io.DataInput
import java.io.DataOutput

class ContentEntityIndex : FileBasedIndexExtension<String, DrupalContentEntity>() {
    private val myKeyDescriptor: KeyDescriptor<String> = EnumeratorStringDescriptor()

    private val myDataExternalizer: DataExternalizer<DrupalContentEntity> =
        object : DataExternalizer<DrupalContentEntity> {
            override fun save(out: DataOutput, value: DrupalContentEntity) {
                out.writeUTF(value.entityType)
                out.writeUTF(value.fqn)
                out.writeInt(value.keys.size)
                for (key in value.keys) {
                    out.writeUTF(key.key)
                    out.writeUTF(key.value)
                }
            }

            override fun read(input: DataInput): DrupalContentEntity {
                val entityType = input.readUTF()
                val fqn = input.readUTF()

                val keys = hashMapOf<String, String>()
                for (i in 1..input.readInt()) {
                    keys[input.readUTF()] = input.readUTF()
                }

                return DrupalContentEntity(entityType, fqn, keys)
            }
        }

    override fun getName(): ID<String, DrupalContentEntity> {
        return KEY
    }

    override fun getIndexer(): DataIndexer<String, DrupalContentEntity, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, DrupalContentEntity>()
            val phpFile = inputData.psiFile as PhpFile

            val phpClass = PsiTreeUtil.findChildOfType(phpFile, PhpClass::class.java) ?: return@DataIndexer map
            if (phpClass.docComment !is PhpDocComment) return@DataIndexer map

            val contentEntityTypes = (phpClass.docComment as PhpDocComment).getTagElementsByName("@ContentEntityType")
            if (contentEntityTypes.isEmpty()) {
                return@DataIndexer map
            }

            val contentEntityTypeDocText = contentEntityTypes[0].text

            val id = getPhpDocParameter(contentEntityTypeDocText, "id") ?: return@DataIndexer map

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

            map[id] = DrupalContentEntity(id, phpClass.fqn, resolvedKeys)

            map
        }
    }

    private fun getPhpDocParameter(phpDocText: String, id: String): String? {
        val entityTypeMatch = Regex("${id}(?:\"?)\\s*=\\s*\"([^\"]+)\"").find(phpDocText)

        return entityTypeMatch?.groups?.get(1)?.value
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = myKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<DrupalContentEntity> = myDataExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file: VirtualFile -> file.fileType == PhpFileType.INSTANCE }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 0

    companion object {
        val KEY = ID.create<String, DrupalContentEntity>("com.github.nvelychenko.drupalextend.index.content_types")
    }

}