package com.github.nvelychenko.drupalextend.util.yml

import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class StringFinderInContext(
    private val stringToFind: String,
    private val condition: ElementPattern<StringLiteralExpression>
) : PsiRecursiveElementVisitor() {
    private var stringLitralPsi: StringLiteralExpression? = null

    override fun visitElement(element: PsiElement) {
        if (condition.accepts(element) && element is StringLiteralExpression && element.contents == stringToFind) {
            stringLitralPsi = element
        }

        if (stringLitralPsi == null) {
            super.visitElement(element)
        }
    }

    fun findIn(element: PsiElement): StringLiteralExpression? {
        element.accept(this)
        return stringLitralPsi
    }
}
