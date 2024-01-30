package com.github.nvelychenko.drupalextend.psi

import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.RenderElementIndex
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.parser.PhpElementTypes
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class RenderElementAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is ArrayHashElement) return

        if (!psiElement(PhpElementTypes.HASH_ARRAY_ELEMENT)
                .withFirstChild(
                    psiElement(PhpElementTypes.ARRAY_KEY)
                        .withChild(psiElement(StringLiteralExpression::class.java))
                )
                .withLastChild(
                    psiElement(PhpElementTypes.ARRAY_VALUE)
                        .withChild(psiElement(StringLiteralExpression::class.java))
                )
                .accepts(element)
        ) {
            return
        }

        val key = element.key as StringLiteralExpression
        val value = element.value as StringLiteralExpression

        if (key.contents != "#type" && value.contents.isEmpty()) {
            return
        }

        val project = element.project
        val renderElement =
            FileBasedIndex.getInstance().getValue(RenderElementIndex.KEY, value.contents, project) ?: return

        val renderElementClass =
            PhpIndex.getInstance(project).getClassesByFQN(renderElement.typeClass).firstOrNull() ?: return

        var isFormElement = false
        PhpClassHierarchyUtils.processSuperClasses(renderElementClass, false, true) {
            isFormElement = it.fqn == "\\Drupal\\Core\\Render\\Element\\FormElement"

            if (isFormElement) {
                return@processSuperClasses false
            }

            if (it.fqn == "\\Drupal\\Core\\Render\\Element\\RenderElement") {
                return@processSuperClasses false
            }

            true
        }

        val tooltip = if (isFormElement) {
            "FormElement"
        } else {
            "RenderElement"
        }

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element.textRange).textAttributes(DefaultLanguageHighlighterColors.IDENTIFIER)
            .tooltip(tooltip)
            .create()
    }
}