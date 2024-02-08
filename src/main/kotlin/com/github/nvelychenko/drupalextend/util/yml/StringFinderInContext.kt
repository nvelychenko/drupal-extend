package com.github.nvelychenko.drupalextend.util.yml

import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class StringFinderInContext(
    private val stringToFind: String,
    private val condition: ElementPattern<StringLiteralExpression>
) : PsiRecursiveElementVisitor() {
    private var stringLiteralPsi: StringLiteralExpression? = null

    override fun visitElement(element: PsiElement) {
        if (condition.accepts(element) && element is StringLiteralExpression && element.contents == stringToFind) {
            stringLiteralPsi = element
        }

        if (stringLiteralPsi == null) {
            super.visitElement(element)
        }
    }

    fun findIn(element: PsiElement): StringLiteralExpression? {
        element.accept(this)
        return stringLiteralPsi
    }
}
