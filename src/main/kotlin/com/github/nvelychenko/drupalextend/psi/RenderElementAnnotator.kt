package com.github.nvelychenko.drupalextend.psi

import com.github.nvelychenko.drupalextend.extensions.getThemeOrRenderElementValue
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.extensions.hasDrupalRenderElement
import com.github.nvelychenko.drupalextend.extensions.hasDrupalTheme
import com.github.nvelychenko.drupalextend.index.RenderElementIndex
import com.github.nvelychenko.drupalextend.index.ThemeIndex
import com.github.nvelychenko.drupalextend.patterns.Patterns.SIMPLE_ARRAY_VALUE_ASSIGNMENT
import com.github.nvelychenko.drupalextend.patterns.Patterns.STRING_IN_SIMPLE_ARRAY_VALUE
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.patterns.PlatformPatterns.or
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class RenderElementAnnotator : Annotator {

    private val fileBasedIndex: FileBasedIndex by lazy { FileBasedIndex.getInstance() }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!element.project.drupalExtendSettings.isEnabled) return

        if (!or(STRING_IN_SIMPLE_ARRAY_VALUE, SIMPLE_ARRAY_VALUE_ASSIGNMENT).accepts(element)) {
            return
        }

        element as StringLiteralExpression

        val tooltip = if (element.hasDrupalRenderElement()) {
            val key = element.getThemeOrRenderElementValue() ?: return
            fileBasedIndex.getValue(
                RenderElementIndex.KEY,
                key.contents,
                element.project
            )?.renderElementType
        } else if (element.hasDrupalTheme()) {
            val key = element.getThemeOrRenderElementValue() ?: return
            fileBasedIndex.getValue(
                ThemeIndex.KEY,
                key.contents,
                element.project
            )?.themeName
                ?: return

            "Theme"
        } else {
            return
        }

        if (tooltip == null) return


        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element.textRange).textAttributes(DefaultLanguageHighlighterColors.IDENTIFIER)
            .tooltip(tooltip)
            .create()

    }
}