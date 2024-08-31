package com.github.nvelychenko.drupalextend.completion

import com.github.nvelychenko.drupalextend.index.ComponentsIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder.create
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.twig.TwigLanguage


class DrupalTwigCompletionContributor : CompletionContributor() {

    val fileBasedIndex: FileBasedIndex by lazy {
        FileBasedIndex.getInstance()
    }

    init {
        // @todo Implement appropriate pattern for include, embed.
        val pattern = psiElement().withLanguage(TwigLanguage.INSTANCE)
        extend(CompletionType.BASIC, pattern,
            object : CompletionProvider<CompletionParameters>() {
                public override fun addCompletions(
                    parameters: CompletionParameters,
                    processingContext: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val element = parameters.originalPosition ?: return
                    val project = element.project

                    if (!project.drupalExtendSettings.isEnabled) return

                    // @todo Double verify if current element is appropriate one
                    FileBasedIndex.getInstance()
                        .getAllKeys(ComponentsIndex.KEY, project)
                        .mapNotNull(::create)
                        .forEach { result.addElement(it) }

                }

            })
    }
}
