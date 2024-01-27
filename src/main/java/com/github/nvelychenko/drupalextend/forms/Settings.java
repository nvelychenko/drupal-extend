package com.github.nvelychenko.drupalextend.forms;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "com.github.nvelychenko.drupalextend.Settings",
        storages = @Storage("DrupalExtendSettings.xml")
)
public class Settings implements PersistentStateComponent<Settings> {

    private boolean pluginEnabled = false;

    private String configDir = "config/sync";

    @Override
    public @Nullable Settings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull Settings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public static Settings getInstance(@NotNull Project project) {
        return project.getService(Settings.class);
    }

    public boolean isPluginEnabled() {
        return pluginEnabled;
    }

    public void setPluginEnabled(boolean pluginEnabled) {
        this.pluginEnabled = pluginEnabled;
    }

    public String getConfigDir() {
        return configDir;
    }

    public void setConfigDir(String configDir) {
        this.configDir = configDir;
    }
}
