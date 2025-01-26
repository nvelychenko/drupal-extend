package com.github.nvelychenko.drupalextend.completion.providers

import com.github.nvelychenko.drupalextend.extensions.isSuperInterfaceOf
import com.github.nvelychenko.drupalextend.index.PermissionsIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*

class PermissionsCompletionProvider : CompletionProvider<CompletionParameters>() {

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

    public override fun addCompletions(
        completionParameters: CompletionParameters,
        processingContext: ProcessingContext,
        completionResultSet: CompletionResultSet
    ) {
        val leaf = completionParameters.originalPosition ?: return
        val project = leaf.project
        if (!project.drupalExtendSettings.isEnabled) return
        val element = leaf.parent as StringLiteralExpression

        // Triggers only for the first parameter.
        val parameterList = element.parent as ParameterList
        if (!parameterList.parameters.first().isEquivalentTo(element)) {
            return
        }

        val methodReference = parameterList.parent as MethodReference
        if (!allowedMethods.containsKey(methodReference.name)) return

        val methodResolvers = methodReference.multiResolve(false)
        if (methodResolvers.isEmpty()) return
        for (methodResolver in methodResolvers) {
            val methodDefinition = methodResolver.element
            if (methodDefinition !is Method) continue
            val methodClass = methodDefinition.containingClass as PhpClass

            for (allowedClass in allowedMethods[methodReference.name]!!) {
                val allowedClassReferences =
                    PhpIndex.getInstance(project)
                        .getInterfacesByFQN(allowedClass).firstOrNull()
                        ?: continue
                if (!methodClass.isSuperInterfaceOf(allowedClassReferences)) continue

                FileBasedIndex.getInstance()
                    .getAllKeys(PermissionsIndex.KEY, project)
                    .forEach {
                        completionResultSet.addElement(LookupElementBuilder.create(it))
                    }
                return
            }
        }
    }

}
