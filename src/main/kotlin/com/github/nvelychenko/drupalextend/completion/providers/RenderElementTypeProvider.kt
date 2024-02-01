package com.github.nvelychenko.drupalextend.completion.providers

import com.github.nvelychenko.drupalextend.extensions.getAllProjectKeys
import com.github.nvelychenko.drupalextend.index.RenderElementIndex
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class RenderElementTypeProvider : CompletionProvider<CompletionParameters>() {
    public override fun addCompletions(
        completionParameters: CompletionParameters,
        processingContext: ProcessingContext,
        completionResultSet: CompletionResultSet
    ) {

        val leaf = completionParameters.originalPosition ?: return

        val hash = PsiTreeUtil.getParentOfType(leaf, ArrayHashElement::class.java)!!

        val key = hash.key as StringLiteralExpression

        if (key.contents != "#type") {
            return
        }
        FileBasedIndex.getInstance().getAllProjectKeys(RenderElementIndex.KEY, hash.project)
            .filter { !it.startsWith("\\") }
            .forEach {
                completionResultSet.addElement(
                    LookupElementBuilder.create(it)
                )
            }


    }

}