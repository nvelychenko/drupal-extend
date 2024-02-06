package com.github.nvelychenko.drupalextend.completion.utils

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement

class RenderElementTypeInsertionHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.document
        val editor = context.editor


        val fileText: String = editor.document.text
        if (fileText.isEmpty()) {
            return
        }

        if (document.charsSequence[context.startOffset - 1] != '#') {
            context.document.insertString(context.startOffset, "#")
        }
    }
}
