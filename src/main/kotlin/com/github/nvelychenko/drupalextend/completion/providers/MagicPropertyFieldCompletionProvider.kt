package com.github.nvelychenko.drupalextend.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.jetbrains.php.lang.psi.elements.FieldReference


/**
 * Provides autocompletion for magic properties.
 *
 * $node->field|
 */
class MagicPropertyFieldCompletionProvider : FieldCompletionProvider() {

    /**
     * I don't want to show magic field at the top, hence such priority.
     */
    override val priority = -20.0

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val originalPosition = parameters.originalPosition ?: return
        val prevSibling = parameters.position.prevSibling

        val memberReference = if (prevSibling != null && prevSibling.node.elementType === PhpTokenTypes.ARROW) {
            // $xex->
            prevSibling.parent
        }
        // Exclude "Node::" cases
        else if (prevSibling != null && prevSibling.node.elementType === PhpTokenTypes.SCOPE_RESOLUTION) {
            return
        } else {
            // $xex->ad
            originalPosition.parent
        }

        if (memberReference !is FieldReference) return
        
        processMemberReference(memberReference, result)
    }
}