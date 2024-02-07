package com.github.nvelychenko.drupalextend.reference.referenceProvider

import com.github.nvelychenko.drupalextend.extensions.containsRenderElement
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.RenderElementIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.reference.referenceType.RenderElementReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class RenderElementTypeReferenceProvider : PsiReferenceProvider() {
    /**
     * Find ['#type' => 'container']
     */
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val project = element.project
        if (!project.drupalExtendSettings.isEnabled) return emptyArray()

        val psiReferences = PsiReference.EMPTY_ARRAY

        val hash = PsiTreeUtil.getParentOfType(element, ArrayHashElement::class.java) ?: return psiReferences

        if (!hash.containsRenderElement()) {
            return psiReferences
        }

        val renderElement = FileBasedIndex.getInstance()
            .getValue(RenderElementIndex.KEY, (hash.value as StringLiteralExpression).contents, project)
            ?: return psiReferences

        return arrayOf(RenderElementReference(element, renderElement))
    }

}
