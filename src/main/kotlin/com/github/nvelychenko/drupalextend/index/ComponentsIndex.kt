package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.extensions.isValidForIndex
import com.github.nvelychenko.drupalextend.index.dataExternalizer.SerializedObjectDataExternalizer
import com.github.nvelychenko.drupalextend.index.types.SdcComponent
import com.github.nvelychenko.drupalextend.index.types.SdcComponent.Props
import com.github.nvelychenko.drupalextend.index.types.SdcComponent.Slots
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.util.yml.YAMLKeyValueFinder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import kotlinx.serialization.serializer
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping

class ComponentsIndex : FileBasedIndexExtension<String, SdcComponent>() {
    override fun getIndexer(): DataIndexer<String, SdcComponent, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, SdcComponent>()

            val yamlFile = inputData.psiFile as YAMLFile
            val file = inputData.file
            val fileName = file.name

            if (!inputData.project.drupalExtendSettings.isEnabled) return@DataIndexer map
            if (!fileName.endsWith(".component.yml")) return@DataIndexer map
            if (!inputData.isValidForIndex()) return@DataIndexer map

            val componentName = file.parent?.name
            if (componentName != fileName.substringBefore(".component.yml")) return@DataIndexer map

            val props = YAMLKeyValueFinder("props.properties").findIn(yamlFile)
            val aggregatedProps = ArrayList<Props>()

            if (props is YAMLMapping) {
                props.keyValues.forEach {
                    val value = it.value
                    val key = it.key
                    if (value is YAMLMapping && key?.text != null) {
                        val id = key.text
                        val type = value.keyValues
                            .find { keys -> keys.key?.text == "type" }
                            ?.value?.text ?: ""

                        // @todo Process enum, probably there is also "required" statement that we need to process
                        aggregatedProps.add(Props(id, type))
                    }

                }
            }

            // @todo Process slots
            val slots = YAMLKeyValueFinder("slots").findIn(yamlFile)
            val aggregatedSlots = ArrayList<Slots>()

            map[componentName] = SdcComponent(aggregatedProps.toTypedArray(), aggregatedSlots.toTypedArray())

            map
        }
    }

    private val myKeyDescriptor: KeyDescriptor<String> = EnumeratorStringDescriptor()

    private val myDataExternalizer: DataExternalizer<SdcComponent> =
        SerializedObjectDataExternalizer(serializer<SdcComponent>())

    override fun getName(): ID<String, SdcComponent> {
        return KEY
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = myKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<SdcComponent> = myDataExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file: VirtualFile -> file.fileType == YAMLFileType.YML }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 0

    companion object {
        val KEY = ID.create<String, SdcComponent>("com.github.nvelychenko.drupalextend.index.sdc_components")
    }

}