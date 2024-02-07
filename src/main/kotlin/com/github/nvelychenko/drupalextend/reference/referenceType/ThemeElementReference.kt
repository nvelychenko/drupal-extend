package com.github.nvelychenko.drupalextend.reference.referenceType

import com.github.nvelychenko.drupalextend.index.ThemeIndex
import com.github.nvelychenko.drupalextend.index.types.DrupalTheme
import com.github.nvelychenko.drupalextend.patterns.Patterns.ARRAY_KEY_WITH_ARRAY_CREATION_EXPRESSION
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.util.yml.StringFinderInContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult.createResults
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.Function

class ThemeElementReference(element: PsiElement, private val renderElement: DrupalTheme) :
    PsiPolyVariantReferenceBase<PsiElement>(element) {

    private val project = element.project
    private val scope = GlobalSearchScope.allScope(project)
    private val fileBasedIndex by lazy { FileBasedIndex.getInstance() }
    private val finder = StringFinderInContext(renderElement.themeName, ARRAY_KEY_WITH_ARRAY_CREATION_EXPRESSION)

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (!project.drupalExtendSettings.isEnabled) return emptyArray()

        var result: Array<ResolveResult> = emptyArray()
        val processor: (file: VirtualFile, value: DrupalTheme) -> Boolean = { containFile, _ ->
            val moduleName = containFile.name.substringBefore(".module")

            val allowedHookNames = arrayOf(moduleName + "_theme", "drupal_common_theme")
            val psiFile = containFile.findPsiFile(project)
            psiFile?.accept(object : PsiRecursiveElementWalkingVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element is Function && (allowedHookNames.contains(element.name))) {
                        val stringLiteralExpression = finder.findIn(element)
                        if (stringLiteralExpression != null) {
                            result = createResults(mutableListOf(stringLiteralExpression))
                        }
                    }

                    super.visitElement(element)
                }
            })
            false
        }

        fileBasedIndex.processValues(ThemeIndex.KEY, renderElement.themeName, null, processor, scope)

        return result
    }
}
