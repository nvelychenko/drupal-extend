package com.github.nvelychenko.drupalextend.reference.referenceType

import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.PhpClass

class ContentEntityReference(element: PsiElement, private val entityTypeId: String) :
    PsiPolyVariantReferenceBase<PsiElement>(element) {

    private val project = element.project
    private val scope = GlobalSearchScope.allScope(project)

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val entityTypes = FileBasedIndex.getInstance().getValues(
            ContentEntityIndex.KEY, entityTypeId, scope
        )

        val resolveResults = mutableListOf<ResolveResult>()

        return if (entityTypes.isEmpty()) {
            ResolveResult.EMPTY_ARRAY
        } else {
            val clazz: MutableCollection<PhpClass> =
                PhpIndex.getInstance(project).getClassesByFQN(entityTypes.first().fqn)
            resolveResults.add(PsiElementResolveResult(clazz.first()))
            return resolveResults.toTypedArray()
        }
    }
}
