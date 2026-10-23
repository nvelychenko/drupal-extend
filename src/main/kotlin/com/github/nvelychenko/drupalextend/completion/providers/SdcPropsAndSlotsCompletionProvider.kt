package com.github.nvelychenko.drupalextend.completion.providers

import com.github.nvelychenko.drupalextend.completion.utils.toLookupElement
import com.github.nvelychenko.drupalextend.extensions.getSdcComponentId
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.ComponentsIndex
import com.github.nvelychenko.drupalextend.index.types.SdcComponent
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

/**
 * $build = [
 *   '#type' => 'component',
 *   '#component' => 'theme:button',
 *   '#props' => ['|' => ...],
 *   '#slots' => ['|' => ...],
 * ];
 */
class SdcPropsAndSlotsCompletionProvider : CompletionProvider<CompletionParameters>() {
    private val fileBasedIndex by lazy { FileBasedIndex.getInstance() }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val project = position.project

        if (!project.drupalExtendSettings.isEnabled) return

        // '#props' => [ ... ]  ←  ARRAY_VALUE  ←  ArrayHashElement
        val section = PsiTreeUtil.getParentOfType(position, ArrayCreationExpression::class.java)
            ?.parent?.parent as? ArrayHashElement ?: return
        val sectionName = (section.key as? StringLiteralExpression)?.contents ?: return
        if (sectionName != "#props" && sectionName != "#slots") return

        val renderArray = section.parent as? ArrayCreationExpression ?: return
        val componentId = renderArray.getSdcComponentId() ?: return

        val component = fileBasedIndex.getValue(ComponentsIndex.KEY, componentId, project) ?: return

        // Only our items are listed here (see stopHere below), so ignore IDE usage statistics:
        // required props first, then alphabetically.
        val sorted = result.withRelevanceSorter(
            CompletionSorter.emptySorter()
                .weigh(object : LookupElementWeigher("sdcRequiredFirst") {
                    override fun weigh(element: LookupElement) =
                        (element.`object` as? SdcComponent.Props)?.required != true
                })
                .weigh(object : LookupElementWeigher("sdcAlphabetical") {
                    override fun weigh(element: LookupElement) = element.lookupString
                })
        )

        if (sectionName == "#props") {
            component.props.forEach { sorted.addElement(it.toLookupElement()) }
        } else {
            component.slots.forEach { sorted.addElement(it.toLookupElement()) }
        }

        // Only props/slots are valid here; without this PhpStorm mixes in class and namespace names.
        result.stopHere()
    }
}
