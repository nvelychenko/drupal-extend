package com.github.nvelychenko.drupalextend.util.yml

import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.jetbrains.php.lang.psi.elements.Function

class FunctionFinderInContext(
    private val stringsToFind: Array<String>,
    private val condition: PsiElementPattern.Capture<PsiElement>
) : PsiRecursiveElementWalkingVisitor() {
    private var stringLiteralPsi: Function? = null

    override fun visitElement(element: PsiElement) {
        if (condition.accepts(element) && element is Function && stringsToFind.contains(element.name)) {
            stringLiteralPsi = element
        }

        if (stringLiteralPsi == null) {
            super.visitElement(element)
        }
    }

    fun findIn(element: PsiElement): Function? {
        element.accept(this)
        return stringLiteralPsi
    }
}
