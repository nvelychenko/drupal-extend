package com.github.nvelychenko.drupalextend.reference.referenceType

import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.ConfigEntityIndex
import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpIndex

class DrupalStorageEntityReference(element: PsiElement, private val entityTypeId: String) :
    PsiPolyVariantReferenceBase<PsiElement>(element) {

    private val project = element.project

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val fileBasedIndex = FileBasedIndex.getInstance()
        val phpIndex = PhpIndex.getInstance(project)

        val resolveResults = mutableListOf<ResolveResult>()

        fileBasedIndex.getValue(ContentEntityIndex.KEY, entityTypeId, project)
            ?.let { phpIndex.getClassesByFQN(it.fqn).firstOrNull() }
            ?.let { resolveResults.add(PsiElementResolveResult(it)) }

        // @todo Refactor config entity index to be able to use content entity index class
        fileBasedIndex.getValue(ConfigEntityIndex.KEY, entityTypeId, project)
            ?.let { phpIndex.getClassesByFQN(it).firstOrNull() }
            ?.let { resolveResults.add(PsiElementResolveResult(it)) }

        return resolveResults.toTypedArray()
    }
}
