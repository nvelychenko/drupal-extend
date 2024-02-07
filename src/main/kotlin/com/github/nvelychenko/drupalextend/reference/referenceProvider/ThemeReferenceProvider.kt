package com.github.nvelychenko.drupalextend.reference.referenceProvider

import com.github.nvelychenko.drupalextend.extensions.containsTheme
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.ThemeIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.reference.referenceType.ThemeElementReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class ThemeReferenceProvider : PsiReferenceProvider() {
    /**
     * Find ['#theme' => 'container']
     */
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val project = element.project
        if (!project.drupalExtendSettings.isEnabled) return emptyArray()

        val hash = PsiTreeUtil.getParentOfType(element, ArrayHashElement::class.java) ?: return emptyArray()

        if (!hash.containsTheme()) {
            return emptyArray()
        }

        val renderElement = FileBasedIndex.getInstance()
            .getValue(ThemeIndex.KEY, (hash.value as StringLiteralExpression).contents, project)
            ?: return emptyArray()

        return arrayOf(ThemeElementReference(element, renderElement))
    }

}
