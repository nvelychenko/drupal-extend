package com.github.nvelychenko.drupalextend.completion.providers

import com.github.nvelychenko.drupalextend.extensions.getAllProjectKeys
import com.github.nvelychenko.drupalextend.extensions.hasDrupalComponent
import com.github.nvelychenko.drupalextend.index.ComponentsIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class SdcComponentCompletionProvider : CompletionProvider<CompletionParameters>() {
    private val fileBasedIndex by lazy { FileBasedIndex.getInstance() }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val string = parameters.position.parent as? StringLiteralExpression ?: return
        val project = string.project

        if (!project.drupalExtendSettings.isEnabled) return
        if (!string.hasDrupalComponent()) return

        fileBasedIndex.getAllProjectKeys(ComponentsIndex.KEY, project)
            .forEach { result.addElement(LookupElementBuilder.create(it)) }
    }
}
