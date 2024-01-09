package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.index.types.ContentEntity
import com.github.nvelychenko.drupalextend.util.isValidForIndex
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

class ContentEntityFqnIndex : FileBasedIndexExtension<String, ContentEntity>() {
    private val myKeyDescriptor: KeyDescriptor<String> = EnumeratorStringDescriptor()

    private val myDataExternalizer: DataExternalizer<ContentEntity> =
        object : DataExternalizer<ContentEntity> {
            override fun save(out: DataOutput, value: ContentEntity) {
                out.writeUTF(value.entityTypeId)
                out.writeUTF(value.fqn)
            }

            override fun read(input: DataInput): ContentEntity {
                val entityType = input.readUTF()
                val fqn = input.readUTF()

                return ContentEntity(entityType, fqn)
            }
        }

    override fun getName(): ID<String, ContentEntity> {
        return KEY
    }

    override fun getIndexer(): DataIndexer<String, ContentEntity, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, ContentEntity>()
            val phpFile = inputData.psiFile as PhpFile

            if (!isValidForIndex(inputData)) {
                return@DataIndexer map
            }

            val phpClass = PsiTreeUtil.findChildOfType(phpFile, PhpClass::class.java) ?: return@DataIndexer map
            if (phpClass.docComment !is PhpDocComment) return@DataIndexer map

            val contentEntityTypes = (phpClass.docComment as PhpDocComment).getTagElementsByName("@ContentEntityType")
            if (contentEntityTypes.isEmpty()) {
                return@DataIndexer map
            }

            val contentEntityTypeDocText = contentEntityTypes[0].text

            val id = getPhpDocParameter(contentEntityTypeDocText, "id") ?: return@DataIndexer map

            map[phpClass.fqn] = ContentEntity(id, phpClass.fqn)

            map
        }
    }

    private fun getPhpDocParameter(phpDocText: String, id: String): String? {
        val entityTypeMatch = Regex("${id}(?:\"?)\\s*=\\s*\"([^\"]+)\"").find(phpDocText)

        return entityTypeMatch?.groups?.get(1)?.value
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = myKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<ContentEntity> = myDataExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file: VirtualFile -> file.fileType == PhpFileType.INSTANCE }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 10

    companion object {
        val KEY = ID.create<String, ContentEntity>("com.github.nvelychenko.drupalextend.index.content_entity_fqn")
    }

}