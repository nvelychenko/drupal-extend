package com.github.nvelychenko.drupalextend.reference.referenceType

import com.github.nvelychenko.drupalextend.index.types.RenderElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult.createResults
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.jetbrains.php.PhpIndex

class RenderElementReference(element: PsiElement, private val renderElement: RenderElementType) :
    PsiPolyVariantReferenceBase<PsiElement>(element) {

    private val project = element.project
    private val phpIndex by lazy {
        PhpIndex.getInstance(project)
    }
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val renderElementClass =
            phpIndex.getClassesByFQN(renderElement.typeClass).firstOrNull() ?: return emptyArray()
        return renderElementClass.docComment?.getTagElementsByName("@${renderElement.renderElementType}")
            ?.takeIf { it.isNotEmpty() }
            ?.mapNotNull { it }
            .let(::createResults)
    }
}
