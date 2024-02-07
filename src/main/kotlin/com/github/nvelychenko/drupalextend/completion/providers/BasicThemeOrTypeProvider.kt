package com.github.nvelychenko.drupalextend.completion.providers

import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder.create
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class BasicThemeOrTypeProvider : CompletionProvider<CompletionParameters>() {
    public override fun addCompletions(
        completionParameters: CompletionParameters,
        processingContext: ProcessingContext,
        result: CompletionResultSet
    ) {
        val leaf = completionParameters.originalPosition ?: return

        if (!leaf.project.drupalExtendSettings.isEnabled) return

        val containString = leaf.parent as StringLiteralExpression

        if (!containString.contents.startsWith("#")) return

        val array = PsiTreeUtil.getParentOfType(leaf, ArrayCreationExpression::class.java)!!

        val themeOrType = arrayOf("#theme", "#type")
        val hasThemeOrType =
            array.hashElements.find { themeOrType.contains((it.key as? StringLiteralExpression)?.contents) }

        if (hasThemeOrType != null) return

        themeOrType
            .map { it.substring(1) }
            .map(::create)
            .forEach(result::addElement)
    }

}