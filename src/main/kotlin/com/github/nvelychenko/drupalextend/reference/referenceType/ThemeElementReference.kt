package com.github.nvelychenko.drupalextend.reference.referenceType

import com.github.nvelychenko.drupalextend.index.ThemeIndex
import com.github.nvelychenko.drupalextend.index.types.DrupalTheme
import com.github.nvelychenko.drupalextend.patterns.Patterns.ARRAY_KEY_WITH_ARRAY_CREATION_EXPRESSION
import com.github.nvelychenko.drupalextend.patterns.Patterns.SIMPLE_FUNCTION
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.util.yml.FunctionFinderInContext
import com.github.nvelychenko.drupalextend.util.yml.StringFinderInContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult.createResults
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex

class ThemeElementReference(element: PsiElement, private val theme: DrupalTheme) :
    PsiPolyVariantReferenceBase<PsiElement>(element) {

    private val project = element.project
    private val scope = GlobalSearchScope.allScope(project)
    private val fileBasedIndex by lazy { FileBasedIndex.getInstance() }
    private val stringFinder = StringFinderInContext(theme.themeName, ARRAY_KEY_WITH_ARRAY_CREATION_EXPRESSION)

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (!project.drupalExtendSettings.isEnabled) return emptyArray()

        return multiResolve() ?: emptyArray()
    }

    private fun multiResolve(): Array<ResolveResult>? = mutableListOf<VirtualFile>()
        .apply {
            val processor: (file: VirtualFile, value: DrupalTheme) -> Boolean =
                { containFile, _ -> this.add(containFile) }
            fileBasedIndex.processValues(ThemeIndex.KEY, theme.themeName, null, processor, scope)
        }
        .firstNotNullOfOrNull { it.findPsiFile(project) }
        ?.let { it ->
            val methodFinder = FunctionFinderInContext(arrayOf(theme.hookName), SIMPLE_FUNCTION)
            methodFinder.findIn(it)
                ?.let { mutableListOf(stringFinder.findIn(it)) }
                ?.let(::createResults)
        }


}
