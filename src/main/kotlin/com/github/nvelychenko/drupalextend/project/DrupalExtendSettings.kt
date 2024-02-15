package com.github.nvelychenko.drupalextend.project

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

private const val SERVICE_NAME: String = "DrupalExtendSettings"

const val DEFAULT_CONFIG_SYNC_DIRECTORY = "config/sync"

val Project.drupalExtendSettings: DrupalExtendSettings
    get() = service<DrupalExtendSettings>()

@Service
@State(name = SERVICE_NAME, storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class DrupalExtendSettings :
    SimplePersistentStateComponent<DrupalExtendSettings.DrupalExtendState>(DrupalExtendState()) {

    var isEnabled: Boolean
        get() = state.isEnabled
        set(value) {
            state.isEnabled = value
        }

    var isEntityReferenceTypeResolverEnabled: Boolean
        get() = state.isEntityReferenceTypeResolverEnabled
        set(value) {
            state.isEntityReferenceTypeResolverEnabled = value
        }

    var configDirectory: String
        get() = state.configDirectory ?: DEFAULT_CONFIG_SYNC_DIRECTORY
        set(value) {
            state.configDirectory = value
        }

    class DrupalExtendState : BaseState() {
        // @todo Once we will implement balloon make it false by default
        var isEnabled: Boolean by property(true)
        var configDirectory by string()
        var isEntityReferenceTypeResolverEnabled: Boolean by property(false)
    }

}
