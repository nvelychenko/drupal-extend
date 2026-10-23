package com.github.nvelychenko.drupalextend.reference.referenceProvider

import com.github.nvelychenko.drupalextend.extensions.getArrayStringLiteralValue
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.extensions.hasDrupalComponent
import com.github.nvelychenko.drupalextend.index.ComponentsIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.reference.referenceType.SdcElementReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class SdcReferenceProvider : PsiReferenceProvider() {
    /**
     * Find ['#component' => 'theme:button']
     */
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (!element.project.drupalExtendSettings.isEnabled) return emptyArray()

        element as StringLiteralExpression

        if (!element.hasDrupalComponent()) return emptyArray()

        val value = element.getArrayStringLiteralValue() ?: return emptyArray()

        FileBasedIndex.getInstance()
            .getValue(ComponentsIndex.KEY, value.contents, element.project)
            ?: return emptyArray()

        return arrayOf(SdcElementReference(element, value.contents))
    }
}
