package com.github.nvelychenko.drupalextend.project.configurable

import com.github.nvelychenko.drupalextend.DrupalExtendBundle
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.util.clearPluginIndexes
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*

class DrupalConfig(
    val project: Project,
) : BoundConfigurable(DrupalExtendBundle.message("settings.drupal_extend.title")) {
    private val settings = project.drupalExtendSettings

    override fun createPanel(): DialogPanel = panel {
        group(DrupalExtendBundle.message("settings.title")) {
            lateinit var isEnabledCheckbox: Cell<JBCheckBox>
            row {
                isEnabledCheckbox = checkBox(DrupalExtendBundle.message("settings.drupal_extend.is_enabled"))
                    .bindSelected(settings::isEnabled)
                    .onApply { clearPluginIndexes() }
            }

            row {
                val fileChooserDescriptor =
                    FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withFileFilter { it.isDirectory }

                textFieldWithBrowseButton(
                    project = project,
                    fileChooserDescriptor = fileChooserDescriptor,
                    fileChosen = {
                        return@textFieldWithBrowseButton it.path
                    })
                    .enabledIf(isEnabledCheckbox.selected)
                    .comment(DrupalExtendBundle.message("settings.drupal_extend.config_directory.title"))
                    .resizableColumn()
                    .align(Align.FILL)
                    .bindText(settings::configDirectory)
                    .onApply {
                        clearPluginIndexes()
                    }

                button(DrupalExtendBundle.message("settings.drupal_extend.clear_index")) { clearPluginIndexes() }
                    .enabledIf(isEnabledCheckbox.selected)
                    .align(AlignX.RIGHT)

            }
        }
    }

}
