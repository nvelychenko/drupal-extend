package com.github.nvelychenko.drupalextend.symfonyIntegration

import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher

class SymfonyIntegrationStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (project.drupalExtendSettings.isEnabled) {
            ServiceContainerUtil.SERVICE_GET_SIGNATURES += MethodMatcher.CallToSignature("\\Drupal", "service")
        }
    }
}