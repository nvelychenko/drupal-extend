package com.github.nvelychenko.drupalextend.forms;

import com.github.nvelychenko.drupalextend.index.ContentEntityFqnIndex;
import com.github.nvelychenko.drupalextend.index.ContentEntityIndex;
import com.github.nvelychenko.drupalextend.index.FieldTypeIndex;
import com.github.nvelychenko.drupalextend.index.FieldsIndex;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.*;

public class SettingsForm implements Configurable {

    private Project project;
    private JCheckBox pluginEnabled;
    private JPanel mainPanel;
    private TextFieldWithBrowseButton pathToConfigDirField;
    private JButton clearIndexButton;
    private JLabel configDirLabel;

    public SettingsForm(@NotNull final Project project) {
        this.project = project;
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Drupal Extend";
    }

    @Override
    public @Nullable JComponent createComponent() {
        pathToConfigDirField.addBrowseFolderListener(createBrowseFolderListener(pathToConfigDirField.getTextField(), FileChooserDescriptorFactory.createSingleFolderDescriptor()));
        updateElementsEnableStatus();

        pluginEnabled.addItemListener(e -> updateElementsEnableStatus());
        clearIndexButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ID<?, ?>[] indexIds = new ID<?, ?>[]{
                        ContentEntityFqnIndex.Companion.getKEY(),
                        ContentEntityIndex.Companion.getKEY(),
                        FieldsIndex.Companion.getKEY(),
                        FieldTypeIndex.Companion.getKEY(),
                };

                for (ID<?, ?> id : indexIds) {
                    FileBasedIndex.getInstance().requestRebuild(id);
                }
                super.mouseClicked(e);
            }
        });
        return mainPanel;
    }

    private void updateElementsEnableStatus() {
        pathToConfigDirField.setEnabled(pluginEnabled.isSelected());
        clearIndexButton.setEnabled(pluginEnabled.isSelected());
        configDirLabel.setEnabled(pluginEnabled.isSelected());
    }

    private TextBrowseFolderListener createBrowseFolderListener(final JTextField textField, final FileChooserDescriptor fileChooserDescriptor) {
        return new TextBrowseFolderListener(fileChooserDescriptor) {
            @Override
            public void actionPerformed(ActionEvent e) {
                VirtualFile projectDirectory = project.getBaseDir();
                VirtualFile selectedFile = FileChooser.chooseFile(
                        fileChooserDescriptor,
                        project,
                        VfsUtil.findRelativeFile(textField.getText(), projectDirectory)
                );

                if (null == selectedFile) {
                    return; // Ignore but keep the previous path
                }

                String path = VfsUtil.getRelativePath(selectedFile, projectDirectory, '/');
                if (null == path) {
                    path = selectedFile.getPath();
                }

                textField.setText(path);
            }
        };
    }

    @Override
    public void reset() {
        pluginEnabled.setSelected(getSettings().isPluginEnabled());
        pathToConfigDirField.setText(getSettings().getConfigDir());
    }

    @Override
    public boolean isModified() {
        return
                !pluginEnabled.isSelected() == getSettings().isPluginEnabled()
                        || !pathToConfigDirField.getText().equals(getSettings().getConfigDir());
    }

    @Override
    public void apply() throws ConfigurationException {
        getSettings().setPluginEnabled(pluginEnabled.isSelected());
        getSettings().setConfigDir(pathToConfigDirField.getText());
    }


    private Settings getSettings() {
        return Settings.getInstance(project);
    }

}
