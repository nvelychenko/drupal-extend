package com.github.nvelychenko.drupalextend.completion.providers

import com.github.nvelychenko.drupalextend.completion.utils.toLookupElement
import com.github.nvelychenko.drupalextend.extensions.getSdcComponentId
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.extensions.hasDrupalVariant
import com.github.nvelychenko.drupalextend.index.ComponentsIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

/**
 * $build = [
 *   '#type' => 'component',
 *   '#component' => 'theme:button',
 *   '#variant' => '|',
 * ];
 */
class SdcVariantCompletionProvider : CompletionProvider<CompletionParameters>() {
    private val fileBasedIndex by lazy { FileBasedIndex.getInstance() }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val string = parameters.position.parent as? StringLiteralExpression ?: return
        val project = string.project

        if (!project.drupalExtendSettings.isEnabled) return
        if (!string.hasDrupalVariant()) return

        val componentId = PsiTreeUtil.getParentOfType(string, ArrayCreationExpression::class.java)
            ?.getSdcComponentId() ?: return
        val component = fileBasedIndex.getValue(ComponentsIndex.KEY, componentId, project) ?: return

        component.variants.forEach { result.addElement(it.toLookupElement()) }
    }
}
