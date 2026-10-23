package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.extensions.isValidForIndex
import com.github.nvelychenko.drupalextend.index.dataExternalizer.SerializedObjectDataExternalizer
import com.github.nvelychenko.drupalextend.index.types.SdcComponent
import com.github.nvelychenko.drupalextend.index.types.SdcComponent.Props
import com.github.nvelychenko.drupalextend.index.types.SdcComponent.Slots
import com.github.nvelychenko.drupalextend.index.types.SdcComponent.Variants
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.util.yml.YAMLKeyValueFinder
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import kotlinx.serialization.serializer
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence

class ComponentsIndex : FileBasedIndexExtension<String, SdcComponent>() {
    override fun getIndexer(): DataIndexer<String, SdcComponent, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, SdcComponent>()

            val file = inputData.file
            val fileName = file.name

            if (!inputData.project.drupalExtendSettings.isEnabled) return@DataIndexer map
            if (!inputData.isValidForIndex()) return@DataIndexer map
            val yamlFile = inputData.psiFile as? YAMLFile ?: return@DataIndexer map

            val componentName = file.parent?.name
            if (componentName != fileName.substringBefore(".component.yml")) return@DataIndexer map

            val props = YAMLKeyValueFinder("props.properties").findIn(yamlFile)
            val themeOrModuleFile = findDirectoryWithInfoYmlFile(file)?.name ?: return@DataIndexer map
            val aggregatedProps = ArrayList<Props>()
            val required = (YAMLKeyValueFinder("props.required").findIn(yamlFile)?.value as? YAMLSequence)
                ?.items?.mapNotNull { (it.value as? YAMLScalar)?.textValue }?.toSet()
                ?: emptySet()

            if (props is YAMLKeyValue && props.value is YAMLMapping) {
                (props.value as YAMLMapping).keyValues.forEach {
                    val value = it.value
                    if (value is YAMLMapping) {
                        val id = it.keyText
                        // Scalars are unquoted; union types like [string, 'null'] are kept as raw text.
                        val type = value.getKeyValueByKey("type")?.value
                            ?.let { v -> (v as? YAMLScalar)?.textValue ?: v.text } ?: ""

                        // @todo Process enum
                        aggregatedProps.add(Props(id, type, required = id in required))
                    }

                }
            }

            val slots = YAMLKeyValueFinder("slots").findIn(yamlFile)
            val aggregatedSlots = ArrayList<Slots>()

            if (slots is YAMLKeyValue && slots.value is YAMLMapping) {
                (slots.value as YAMLMapping)
                    .keyValues.forEach {
                        val value = it.value
                        if (value is YAMLMapping) {
                            val id = it.keyText
                            val title = value.getKeyValueByKey("title")?.valueText ?: ""

                            aggregatedSlots.add(Slots(id, title))
                        }

                    }
            }

            val variants = YAMLKeyValueFinder("variants").findIn(yamlFile)
            val aggregatedVariants = ArrayList<Variants>()

            if (variants is YAMLKeyValue && variants.value is YAMLMapping) {
                (variants.value as YAMLMapping)
                    .keyValues.forEach {
                        val value = it.value
                        if (value is YAMLMapping) {
                            val id = it.keyText
                            val title = value.getKeyValueByKey("title")?.valueText ?: ""

                            aggregatedVariants.add(Variants(id, title))
                        }

                    }
            }

            map["$themeOrModuleFile:$componentName"] = SdcComponent(aggregatedProps, aggregatedSlots, aggregatedVariants)

            return@DataIndexer map
        }
    }

    fun findDirectoryWithInfoYmlFile(startingPosition: VirtualFile): VirtualFile? {
        var currentDirectory: VirtualFile? = startingPosition

        while (currentDirectory != null) {
            val currentDirectoryName = currentDirectory.name
            val targetFileName = "$currentDirectoryName.info.yml"

            val targetFile = currentDirectory.findChild(targetFileName)
            if (targetFile != null && !targetFile.isDirectory) {
                return currentDirectory
            }

            currentDirectory = currentDirectory.parent
        }

        return null
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
        return FileBasedIndex.InputFilter { file: VirtualFile ->
            file.fileType == YAMLFileType.YML && file.name.endsWith(".component.yml")
        }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 3

    companion object {
        val KEY = ID.create<String, SdcComponent>("com.github.nvelychenko.drupalextend.index.sdc_components")

        /**
         * Returns *.component.yml and the sibling template of the component with the given id.
         */
        fun getComponentFiles(id: String, project: Project): List<PsiFile> {
            val psiManager = PsiManager.getInstance(project)
            return FileBasedIndex.getInstance()
                .getContainingFiles(KEY, id, GlobalSearchScope.allScope(project))
                .flatMap { yml ->
                    val template = yml.parent?.findChild(yml.name.removeSuffix(".component.yml") + ".twig")
                    listOfNotNull(yml, template)
                }
                .mapNotNull(psiManager::findFile)
        }
    }

}
