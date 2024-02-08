package com.github.nvelychenko.drupalextend.completion.providers

import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

/**
 * Node::create([
 *   'fiel|
 * ]);
 */
open class FieldInCreateMethodCompletionCompletionProvider : FieldCompletionCompletionProvider() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = (parameters.originalPosition ?: return).parent as StringLiteralExpression

        if (!element.project.drupalExtendSettings.isEnabled) return

        val methodReference = PsiTreeUtil.getParentOfType(element, MethodReference::class.java) ?: return

        if (methodReference.name != "create") return

        getContentEntityFromReferenceAndBuildAutocomplete(methodReference, result)
    }

}