package com.github.nvelychenko.drupalextend.reference.referenceProvider

import com.github.nvelychenko.drupalextend.extensions.getArrayStringLiteralValue
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.extensions.hasDrupalRenderElement
import com.github.nvelychenko.drupalextend.index.RenderElementIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.reference.referenceType.RenderElementReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class RenderElementTypeReferenceProvider : PsiReferenceProvider() {
    /**
     * Find ['#type' => 'container']
     */
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val project = element.project
        if (!project.drupalExtendSettings.isEnabled) return emptyArray()

        element as StringLiteralExpression

        if (!element.hasDrupalRenderElement()) return emptyArray()

        val value = element.getArrayStringLiteralValue() ?: return emptyArray()

        val renderElement = FileBasedIndex.getInstance()
            .getValue(RenderElementIndex.KEY, value.contents, project)
            ?: return emptyArray()

        return arrayOf(RenderElementReference(element, renderElement))
    }

}
