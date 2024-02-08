package com.github.nvelychenko.drupalextend.completion.providers

import com.github.nvelychenko.drupalextend.index.LibrariesIndex
import com.github.nvelychenko.drupalextend.patterns.Patterns.TRIPLE_ARRAY_WITH_STRING_VALUE
import com.github.nvelychenko.drupalextend.patterns.Patterns.TRIPLE_NESTED_STRING_ARRAY_VALUE
import com.github.nvelychenko.drupalextend.patterns.Patterns.TRIPLE_SIMPLE_STRING_ARRAY_VALUE
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder.create
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.AssignmentExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class LibrariesCompletionProvider : CompletionProvider<CompletionParameters>() {
    public override fun addCompletions(
        completionParameters: CompletionParameters,
        processingContext: ProcessingContext,
        result: CompletionResultSet
    ) {
        val leaf = completionParameters.originalPosition ?: return

        val project = leaf.project
        if (!project.drupalExtendSettings.isEnabled) return

        val containString = leaf.parent as StringLiteralExpression

        val shouldEnableAutocomplete = when {
            TRIPLE_NESTED_STRING_ARRAY_VALUE.accepts(containString) -> isTripleNestedArrayAttachedLibrary(containString)
            TRIPLE_SIMPLE_STRING_ARRAY_VALUE.accepts(containString) -> isTripleSimpleAttachedLibrary(containString)
            TRIPLE_ARRAY_WITH_STRING_VALUE.accepts(containString) -> isTripleArrayAttachedLibrary(containString)
            else -> return
        }

        if (shouldEnableAutocomplete) {
            FileBasedIndex.getInstance()
                .getAllKeys(LibrariesIndex.KEY, project)
                .mapNotNull(::create)
                .forEach { result.addElement(it) }
        }
    }

    private fun isTripleNestedArrayAttachedLibrary(containString: StringLiteralExpression): Boolean {
        val hash = PsiTreeUtil.getParentOfType(containString, ArrayHashElement::class.java) ?: return false

        if ((hash.key as? StringLiteralExpression)?.contents != "library") return false

        val assignmentExpression = hash.parent.parent as AssignmentExpression
        val indexValue =
            (assignmentExpression.variable as? ArrayAccessExpression)?.index?.value as? StringLiteralExpression
                ?: return false

        return indexValue.contents == "#attached"
    }

    private fun isTripleSimpleAttachedLibrary(containString: StringLiteralExpression): Boolean {
        val hash = PsiTreeUtil.getParentOfType(containString, ArrayHashElement::class.java) ?: return false
        if ((hash.key as? StringLiteralExpression)?.contents != "library") return false
        val attachedHash = PsiTreeUtil.getParentOfType(hash, ArrayHashElement::class.java) ?: return false
        return (attachedHash.key as? StringLiteralExpression)?.contents == "#attached"
    }

    private fun isTripleArrayAttachedLibrary(containString: StringLiteralExpression): Boolean {
        val parent = containString.parent as AssignmentExpression
        val main = parent.variable as ArrayAccessExpression
        val libraryElement = main.value as ArrayAccessExpression

        if ((libraryElement.index?.value as? StringLiteralExpression)?.contents != "library") return false

        val attachmentsElement = libraryElement.value as ArrayAccessExpression

        return (attachmentsElement.index?.value as? StringLiteralExpression)?.contents == "#attached"
    }

}