package com.github.nvelychenko.drupalextend.lineMarker

import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.daemon.DefaultGutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.drupal.settings.DrupalDataService
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.stubs.indexes.PhpFunctionIndex
import icons.DrupalIcons


class DrupalHookImplLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        return null
    }

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (elements.isEmpty()) return
        val dataService = DrupalDataService.getInstance(elements[0].project)
        if (!dataService.isEnabled || !dataService.isVersionValid) return

        val functionNamesCache: MutableList<String> = mutableListOf()

        for (element in elements) {
            handlePotentialDoc(element, result, functionNamesCache)
        }
    }

    private fun handlePotentialDoc(
        element: PsiElement,
        result: MutableCollection<in LineMarkerInfo<*>>,
        functionNamesCache: MutableList<String>,
    ) {
        if (element !is Method || element.attributes.isEmpty()) return
        val project = element.project

        val hookNames = mutableListOf<String>()
        for (attribute in element.attributes) {
            if (attribute.fqn != "\\Drupal\\Core\\Hook\\Attribute\\Hook" || attribute.parameters.isEmpty()) continue

            val hookNamePsi = attribute.parameters.first()
            if (hookNamePsi !is StringLiteralExpression || hookNamePsi.contents.isEmpty()) continue

            hookNames.add("hook_${hookNamePsi.contents}")
        }

        if (hookNames.isEmpty()) return

        if (functionNamesCache.isEmpty()) {
            functionNamesCache.addAll(PhpIndex.getInstance(project).getAllFunctionNames(null as PrefixMatcher?))
        }

        val implementations = ArrayList<Function>()

        val modulesFilesScope = GlobalSearchScope.allScope(project)
        functionNamesCache
            .filter { hookNames.contains(it) }
            .forEach {
                val functions = StubIndex.getElements(PhpFunctionIndex.KEY, it, project, modulesFilesScope, Function::class.java)

                for (function in functions) {
                    implementations.add(function)
                }
            }

        if (implementations.isEmpty()) return


        val anchor = PsiTreeUtil.getDeepestFirst(element)
        result.add(
            LineMarkerInfo(
                anchor,
                anchor.textRange,
                DrupalIcons.ImplementingHook,
                { "Maybe some day" },
                DefaultGutterIconNavigationHandler(
                    implementations,
                    "Hook Template"
                ),
                GutterIconRenderer.Alignment.LEFT,
                { "IM NOT USER FRIENDLY, OK?" }
            )
        )
    }
}
