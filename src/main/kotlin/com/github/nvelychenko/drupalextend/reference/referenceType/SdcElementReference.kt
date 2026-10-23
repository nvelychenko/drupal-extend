package com.github.nvelychenko.drupalextend.reference.referenceType

import com.github.nvelychenko.drupalextend.index.ComponentsIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult.createResults
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class SdcElementReference(element: PsiElement, private val componentId: String) :
    PsiPolyVariantReferenceBase<PsiElement>(element) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (!element.project.drupalExtendSettings.isEnabled) return emptyArray()

        return createResults(ComponentsIndex.getComponentFiles(componentId, element.project))
    }
}
