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
import com.intellij.openapi.util.text.StringUtil
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
import com.jetbrains.php.lang.psi.elements.*
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

        processAttributes(map, phpClass)
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

    private fun processAttributes(map: HashMap<String, DrupalContentEntity>, clazz: PhpClass) {
        val attribute =
            clazz.attributes.find { it.fqn == "\\Drupal\\Core\\Entity\\Attribute\\ContentEntityType" } ?: return
        val resolvedKeys = hashMapOf<String, String>()
        val rawId = attribute.arguments.find { it.name == "id" }?.argument?.value ?: return
        val id = StringUtil.unquoteString(rawId)
        processKeysInAttribute(resolvedKeys, "entity_keys", attribute)
        processKeysInAttribute(resolvedKeys, "revision_metadata_keys", attribute)

        var storage = "\\Drupal\\Core\\Entity\\Sql\\SqlContentEntityStorage"

        attribute.arguments.find { it.name == "handlers" }
            ?.argument?.argumentIndex
            ?.let {
                val value = attribute.parameters[it]
                if (value is ArrayCreationExpression) {
                    val storageValue = value.hashElements.find { current ->
                        val key = current.key
                        key is StringLiteralExpression && key.contents == "storage"
                    }
                        ?.value

                    if (storageValue is StringLiteralExpression) {
                        storage = storageValue.contents
                    }
                    if (storageValue is ClassConstantReference) {
                        val clazzSignature = storageValue.signature
                            .split('|')
                            .find { part -> part.contains("#K#C") && part.contains(".class") }

                            if (clazzSignature != null) {
                                storage = clazzSignature.substringAfter("#K#C").substringBefore(".class")
                            }
                    }
                }
            }

        map[id] = DrupalContentEntity(id, clazz.fqn, resolvedKeys, storage)
    }

    private fun processKeysInAttribute(resolvedKeys: HashMap<String, String>, name: String, attribute: PhpAttribute) {
        attribute.arguments.find { it.name == name }
            ?.argument?.argumentIndex
            ?.let {
                val value = attribute.parameters[it]
                if (value is ArrayCreationExpression) {
                    value.hashElements.map { element ->
                        val key = element.key
                        val arrayValue = element.value
                        if (key is StringLiteralExpression && arrayValue is StringLiteralExpression) {
                            resolvedKeys[key.contents] = arrayValue.contents
                        }
                    }
                }
            }
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

    override fun getVersion(): Int = 14

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