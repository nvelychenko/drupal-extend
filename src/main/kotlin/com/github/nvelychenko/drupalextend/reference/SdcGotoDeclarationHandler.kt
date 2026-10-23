package com.github.nvelychenko.drupalextend.reference

import com.github.nvelychenko.drupalextend.index.ComponentsIndex
import com.github.nvelychenko.drupalextend.patterns.Patterns.TWIG_SDC_COMPONENT_NAME
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement

/**
 * Navigates from an SDC component id in Twig to its *.component.yml and template.
 *
 * Twig string literals are leaf tokens that don't receive contributed references,
 * so navigation goes through GotoDeclarationHandler instead of PsiReferenceContributor.
 */
class SdcGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(element: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
        element ?: return null
        if (!element.project.drupalExtendSettings.isEnabled) return null
        if (!TWIG_SDC_COMPONENT_NAME.accepts(element)) return null

        return ComponentsIndex.getComponentFiles(element.text, element.project).toTypedArray()
    }
}
