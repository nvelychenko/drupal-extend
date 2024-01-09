package com.github.nvelychenko.drupalextend.extensions

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.jetbrains.php.lang.PhpLangUtil
import com.jetbrains.php.lang.psi.elements.Variable

fun PsiElement.findVariablesByName(variableName: String): List<Variable> {
    val variables = mutableListOf<Variable>()
    this.acceptChildren(object : PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is Variable) {
                if (PhpLangUtil.equalsVariableNames(element.name, variableName)) {
                    variables.add(element)
                }
                return
            }

            super.visitElement(element)
        }
    })
    variables.reverse()
    return variables
}
