package com.github.nvelychenko.drupalextend.reference.referenceType

import com.github.nvelychenko.drupalextend.icon.DrupalIcons
import com.github.nvelychenko.drupalextend.index.ThemeIndex
import com.github.nvelychenko.drupalextend.index.types.DrupalTheme
import com.github.nvelychenko.drupalextend.patterns.Patterns.ARRAY_KEY_INSIDE_ASSIGNMENT_EXPRESSION
import com.github.nvelychenko.drupalextend.patterns.Patterns.ARRAY_KEY_WITH_ARRAY_CREATION_EXPRESSION
import com.github.nvelychenko.drupalextend.patterns.Patterns.FUNCTION_OR_METHOD
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.util.yml.FunctionFinderInContext
import com.github.nvelychenko.drupalextend.util.yml.StringFinderInContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PlatformPatterns.or
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult.createResults
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.xml.model.gotosymbol.GoToSymbolProvider.BaseNavigationItem

class ThemeElementReference(element: PsiElement, private val theme: DrupalTheme) : PsiPolyVariantReferenceBase<PsiElement>(element) {

    private val project = element.project
    private val scope = GlobalSearchScope.allScope(project)
    private val fileBasedIndex by lazy { FileBasedIndex.getInstance() }
    private val stringFinder = StringFinderInContext(theme.themeName,
        or(ARRAY_KEY_WITH_ARRAY_CREATION_EXPRESSION,
            ARRAY_KEY_INSIDE_ASSIGNMENT_EXPRESSION)
    )

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (!project.drupalExtendSettings.isEnabled) return emptyArray()

        return multiResolve()
    }

    private fun multiResolve(): Array<ResolveResult> {
        val resultList = mutableListOf<PsiElement>()
        getThemeStringPsiElement()?.let { resultList.add(it) }

        val templateName = theme.themeName.replace('_', '-')

        FilenameIndex
            .getVirtualFilesByName("$templateName.html.twig", scope)
            .map {
                it.findPsiFile(project)
                    ?.let { file -> resultList.add(file) }
            }

        return resultList.let(::createResults)
    }

    private fun getThemeStringPsiElement(): PsiElement? {
        var list: VirtualFile? = null

        val processor: (file: VirtualFile, value: DrupalTheme) -> Boolean = { containFile, _ ->
            list = containFile
            true
        }

        fileBasedIndex.processValues(ThemeIndex.KEY, theme.themeName, null, processor, scope)

        val file = list?.findPsiFile(project) ?: return null

        val method = FunctionFinderInContext(arrayOf(theme.hookName), FUNCTION_OR_METHOD).findIn(file) ?: return null
        return stringFinder.findIn(method)?.let { BaseNavigationItem(it, it.text.replace(Regex("[\"']"), ""), DrupalIcons.HASH_ICON) }
    }


}
