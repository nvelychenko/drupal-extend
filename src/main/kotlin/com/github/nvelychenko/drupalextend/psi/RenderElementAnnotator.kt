package com.github.nvelychenko.drupalextend.psi

import com.github.nvelychenko.drupalextend.extensions.containsRenderElement
import com.github.nvelychenko.drupalextend.extensions.containsTheme
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.RenderElementIndex
import com.github.nvelychenko.drupalextend.index.ThemeIndex
import com.github.nvelychenko.drupalextend.patterns.Patterns.STRING_IN_SIMPLE_ARRAY_VALUE
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class RenderElementAnnotator : Annotator {

    private val fileBasedIndex: FileBasedIndex by lazy { FileBasedIndex.getInstance() }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!element.project.drupalExtendSettings.isEnabled) return

        if (!STRING_IN_SIMPLE_ARRAY_VALUE.accepts(element)) {
            return
        }

        val hash = PsiTreeUtil.getParentOfType(element, ArrayHashElement::class.java) ?: return


        val tooltip = if (hash.containsRenderElement()) {
            fileBasedIndex.getValue(
                RenderElementIndex.KEY,
                (hash.value as StringLiteralExpression).contents,
                element.project
            )?.renderElementType
        } else if (hash.containsTheme()) {
            fileBasedIndex.getValue(
                ThemeIndex.KEY,
                (hash.value as StringLiteralExpression).contents,
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