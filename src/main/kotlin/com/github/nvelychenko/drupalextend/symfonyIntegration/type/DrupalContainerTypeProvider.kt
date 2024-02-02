package com.github.nvelychenko.drupalextend.symfonyIntegration.type

import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.dic.SymfonyContainerTypeProvider
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil


/**
 * Add support for \Drupal::service('database')->|
 */
class DrupalContainerTypeProvider : SymfonyContainerTypeProvider() {

    private val trimKey = '\u0182'

    override fun getKey(): Char {
        return '\u9955'
    }

    override fun getType(psiElement: PsiElement): PhpType? {
        if (!Symfony2ProjectComponent.isEnabled(psiElement.project) || !psiElement.project.drupalExtendSettings.isEnabled) {
            return null
        }

        if (psiElement !is MethodReference || !PhpElementsUtil.isMethodWithFirstStringOrFieldReference(psiElement, "service")) {
            return null
        }

        val signature = PhpTypeProviderUtil.getReferenceSignatureByFirstParameter(psiElement, trimKey)
        return if (signature == null) null else PhpType().add("#" + this.key + signature)
    }
}