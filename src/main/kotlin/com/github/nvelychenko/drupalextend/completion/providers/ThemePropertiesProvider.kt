package com.github.nvelychenko.drupalextend.completion.providers

import com.github.nvelychenko.drupalextend.completion.utils.RenderElementTypeInsertionHandler
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.ThemeIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder.create
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class ThemePropertiesProvider : CompletionProvider<CompletionParameters>() {

    private val fileBasedIndex by lazy { FileBasedIndex.getInstance() }

    private val insertionHandler by lazy { RenderElementTypeInsertionHandler() }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val leaf = parameters.originalPosition ?: return
        val project = leaf.project

        if (!project.drupalExtendSettings.isEnabled) return

        val array = PsiTreeUtil.getParentOfType(leaf, ArrayCreationExpression::class.java)!!

        val typeElement = array.hashElements.find {
            val key = it.key
            key is StringLiteralExpression && key.contents == "#theme" && it.value is StringLiteralExpression
        } ?: return

        val type =
            (typeElement.value as StringLiteralExpression).contents.takeIf { it.isNotEmpty() } ?: return

        fileBasedIndex.getValue(ThemeIndex.KEY, type, project)
            ?.variables
            ?.map(::create)
            ?.forEach {
                result.addElement(it.withInsertHandler(insertionHandler))
            }

    }

}