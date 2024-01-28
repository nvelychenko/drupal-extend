package com.github.nvelychenko.drupalextend.symfonyIntegration.type

import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import fr.adrienbrault.idea.symfony2plugin.dic.SymfonyContainerTypeProvider
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil


class DrupalContainerTypeProvider : SymfonyContainerTypeProvider() {

    private val trimKey = '\u0182'

    override fun getKey(): Char {
        return '\u9955'
    }

    override fun getType(e: PsiElement): PhpType? {
        if (e !is MethodReference || !PhpElementsUtil.isMethodWithFirstStringOrFieldReference(e, "service")) {
            return null
        }

        ServiceContainerUtil.SERVICE_GET_SIGNATURES += MethodMatcher.CallToSignature("\\Drupal", "service")
        val signature = PhpTypeProviderUtil.getReferenceSignatureByFirstParameter(e, trimKey)
        return if (signature == null) null else PhpType().add("#" + this.key + signature)
    }
}