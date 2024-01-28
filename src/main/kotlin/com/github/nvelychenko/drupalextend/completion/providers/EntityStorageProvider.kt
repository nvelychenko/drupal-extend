package com.github.nvelychenko.drupalextend.completion.providers

import com.github.nvelychenko.drupalextend.extensions.getAllProjectKeys
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.extensions.isSuperInterfaceOf
import com.github.nvelychenko.drupalextend.index.ConfigEntityIndex
import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*

class EntityStorageProvider : CompletionProvider<CompletionParameters>() {
    public override fun addCompletions(
        completionParameters: CompletionParameters,
        processingContext: ProcessingContext,
        completionResultSet: CompletionResultSet
    ) {
        val leaf = completionParameters.originalPosition ?: return
        val element = leaf.parent as? StringLiteralExpression
        if (element == null || element.contents.isEmpty()) return

        val parameterList = element.parent as ParameterList
        if (!parameterList.parameters.first().isEquivalentTo(element)) {
            return
        }

        val methodReference = parameterList.parent as MethodReference

        if (methodReference.isStatic) {
            return
        }

        val allowedMethods = arrayOf(
            "getAccessControlHandler",
            "getStorage",
            "getViewBuilder",
            "getListBuilder",
            "getFormObject",
            "getRouteProviders",
            "hasHandler",
            // @todo getDefinition does not work
        )

        if (!allowedMethods.contains(methodReference.name)) return

        val method = methodReference.resolve() ?: return

        if (method !is Method) return

        val methodClass = method.containingClass as PhpClass
        val project = method.project

        val entityReferences =
            PhpIndex.getInstance(project)
                .getInterfacesByFQN("\\Drupal\\Core\\Entity\\EntityTypeManagerInterface").firstOrNull()
                ?: return

        if (!methodClass.isSuperInterfaceOf(entityReferences)) return

        val instance = FileBasedIndex.getInstance()

        instance
            .getAllProjectKeys(ContentEntityIndex.KEY, project)
            .filter { !it.contains("\\") }
            .forEach {
                val contentEntity = instance.getValue(ContentEntityIndex.KEY, it, project) ?: return@forEach

                completionResultSet.addElement(
                    LookupElementBuilder.create(it)
                        .withTypeText(contentEntity.fqn, true)
                )
            }

        instance
            .getAllProjectKeys(ConfigEntityIndex.KEY, project)
            .forEach {
                val configEntityFqn =
                    instance.getValue(ConfigEntityIndex.KEY, it, project) ?: return@forEach

                completionResultSet.addElement(
                    LookupElementBuilder.create(it)
                        .withTypeText(configEntityFqn, true)
                )
            }
    }

}