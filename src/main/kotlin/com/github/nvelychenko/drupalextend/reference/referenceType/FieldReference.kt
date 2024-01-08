package com.github.nvelychenko.drupalextend.reference.referenceType

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.ProjectScope

class FieldReference(element: PsiElement, public val entityTypeId: String, public val fieldName: String) : PsiPolyVariantReferenceBase<PsiElement>(element) {

    private val project = element.project
    private val scope = ProjectScope.getAllScope(project)

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return ResolveResult.EMPTY_ARRAY
    }
}
