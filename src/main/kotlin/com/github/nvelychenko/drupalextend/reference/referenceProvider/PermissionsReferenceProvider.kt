package com.github.nvelychenko.drupalextend.reference.referenceProvider

import com.github.nvelychenko.drupalextend.extensions.isSuperInterfaceOf
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.reference.referenceType.PermissionsReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*

class PermissionsReferenceProvider : PsiReferenceProvider() {

    companion object {
        val allowedMethods: Map<String, List<String>> = mapOf(
            "hasPermission" to listOf(
                "\\Drupal\\Core\\Session\\AccountInterface",
            ),
            "grantPermission" to listOf(
                "\\Drupal\\user\\RoleInterface",
            ),
            "revokePermission" to listOf(
                "\\Drupal\\user\\RoleInterface",
            ),
            "allowedIfHasPermission" to listOf(
                "\\Drupal\\Core\\Access\\AccessResult",
                "\\Drupal\\user\\RoleInterface",
            ),
        )
    }

    /**
     * Finds \Drupal::entityTypeManager()->getStorage('ENTITY_TYPE') and adds reference to it
     */
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
        if (!parameterList.parameters.first().isEquivalentTo(element)) {
            return emptyArray()
        }

        val methodReference = parameterList.parent as MethodReference
        if (!allowedMethods.containsKey(methodReference.name)) {
            return emptyArray()
        }

        val methodResolvers = methodReference.multiResolve(false)
        if (methodResolvers.isEmpty()) {
            return emptyArray()
        }

        for (methodResolver in methodResolvers) {
            val methodDefinition = methodResolver.element
            if (methodDefinition !is Method) continue
            val methodClass = methodDefinition.containingClass as PhpClass

            for (allowedClass in allowedMethods[methodReference.name]!!) {
                val allowedClassReferences =
                    PhpIndex.getInstance(element.project)
                        .getInterfacesByFQN(allowedClass).firstOrNull()
                        ?: continue
                if (!methodClass.isSuperInterfaceOf(allowedClassReferences)) continue

                return arrayOf(PermissionsReference(element, permissionName))
            }
        }

        return emptyArray()
    }

}
