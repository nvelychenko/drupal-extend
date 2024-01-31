package com.github.nvelychenko.drupalextend.reference.referenceProvider

import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.RenderElementIndex
import com.github.nvelychenko.drupalextend.reference.referenceType.RenderElementReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class RenderElementTypeProvider : PsiReferenceProvider() {
    /**
     * Find ['#type' => 'container']
     */
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val psiReferences = PsiReference.EMPTY_ARRAY

        val hash = PsiTreeUtil.getParentOfType(element, ArrayHashElement::class.java) ?: return psiReferences

        val key = hash.key as StringLiteralExpression
        val value = hash.value as StringLiteralExpression

        if (key.contents != "#type" || value.contents.isEmpty()) {
            return psiReferences
        }

        val project = element.project
        val renderElement = FileBasedIndex.getInstance().getValue(RenderElementIndex.KEY, value.contents, project)
            ?: return psiReferences

        return arrayOf(RenderElementReference(element, renderElement))
    }

}