package com.github.nvelychenko.drupalextend.symfonyIntegration

import com.intellij.openapi.project.Project
import de.espend.idea.symfony.marketplace.LicenseUtil.PostStartupActivity
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher

class SymfonyIntegrationStartupActivity : PostStartupActivity() {

    override fun runActivity(project: Project) {
        ServiceContainerUtil.SERVICE_GET_SIGNATURES += MethodMatcher.CallToSignature("\\Drupal", "service")
    }
}