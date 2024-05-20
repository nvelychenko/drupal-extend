package com.github.nvelychenko.drupalextend.symfonyIntegration.type

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher.CallToSignature
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil

/**
 * Add support for \Drupal::service('database')->|
 */
class DrupalContainerTypeProvider : PhpTypeProvider4 {

    private val trimKey = '\u9956'

    override fun getKey(): Char {
        return '\u9955'
    }

    override fun getType(psiElement: PsiElement): PhpType? {
        if (psiElement !is MethodReference) {
            return null
        }

        val project = psiElement.project
        if (!Settings.getInstance(project).pluginEnabled) {
            return null
        }

        if (!PhpElementsUtil.isMethodWithFirstStringOrFieldReference(psiElement, "service")) {
            return null
        }

        val signature = PhpTypeProviderUtil.getReferenceSignatureByFirstParameter(psiElement, trimKey)
        return if (signature == null) null else PhpType().add("#" + this.key + signature)
    }

    override fun complete(s: String?, project: Project?): PhpType? {
        return null
    }

    override fun getBySignature(
        expression: String,
        visited: Set<String?>,
        depth: Int,
        project: Project
    ): Collection<PhpNamedElement?> {
        val endIndex = expression.lastIndexOf(trimKey)

        if (endIndex == -1) {
            return emptySet()
        }

        val originalSignature = expression.substring(0, endIndex)
        var parameter: String = expression.substring(endIndex + 1)

        // search for called method
        val phpIndex: PhpIndex = PhpIndex.getInstance(project)
        val phpNamedElementCollections: Collection<PhpNamedElement?> =
            PhpTypeProviderUtil.getTypeSignature(phpIndex, originalSignature)

        if (phpNamedElementCollections.isEmpty()) {
            return emptySet()
        }

        // get first matched item
        val phpNamedElement: PhpNamedElement = phpNamedElementCollections.iterator().next() as? Method
            ?: return phpNamedElementCollections

        parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, parameter) ?: return phpNamedElementCollections

        // finally search the classes
        if (!PhpElementsUtil.isMethodInstanceOf(
                phpNamedElement as Method,
                CallToSignature("\\Drupal", "service")
            )
        ) {
            return phpNamedElementCollections
        }

        val containerService =
            ContainerCollectionResolver.getService(project, parameter) ?: return phpNamedElementCollections
        val phpClasses: MutableCollection<PhpNamedElement?> = HashSet()

        for (s in containerService.classNames) {
            phpClasses.addAll(PhpIndex.getInstance(project).getAnyByFQN(s))
        }

        return phpClasses
    }
}