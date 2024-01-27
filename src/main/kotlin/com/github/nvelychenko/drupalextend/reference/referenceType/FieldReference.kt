package com.github.nvelychenko.drupalextend.reference.referenceType

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

@Suppress("unused")
class FieldReference(element: PsiElement, val entityTypeId: String, val fieldName: String) :
    PsiPolyVariantReferenceBase<PsiElement>(element) {

    private val project = element.project

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return ResolveResult.EMPTY_ARRAY
    }
}
