package com.github.nvelychenko.drupalextend.reference.referenceProvider

import com.github.nvelychenko.drupalextend.completion.providers.PermissionsCompletionProvider.Companion.allowedMethods
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.reference.referenceType.PermissionsReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.impl.MethodImpl

class PermissionsReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (!element.project.drupalExtendSettings.isEnabled) {
            return emptyArray()
        }

        element as StringLiteralExpression
        val permissionName = element.contents
        if (permissionName.isEmpty()) {
            return emptyArray()
        }

        // Triggers only for the first parameter.
        val parameterList = element.parent as ParameterList
        val methodReference = parameterList.parent as MethodReference
        // Triggers only for the first parameter.

        val methodName = methodReference.name
        val position = allowedMethods.keys.find { it == methodName } ?: return emptyArray()
        val methodsToCheck = allowedMethods[position]!!
        val currentElementMethodFqn = (methodReference.resolve() as? MethodImpl)?.fqn ?: return emptyArray()
        val methodToCheck = methodsToCheck.keys.find { it == currentElementMethodFqn } ?: return emptyArray()
        if (parameterList.parameters[methodsToCheck[methodToCheck]!!]?.isEquivalentTo(element) == false) return emptyArray()

        return arrayOf(PermissionsReference(element, permissionName))
    }

}
