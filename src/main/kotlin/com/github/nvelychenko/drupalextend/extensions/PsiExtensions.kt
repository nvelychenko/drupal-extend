package com.github.nvelychenko.drupalextend.extensions

import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.PhpLangUtil
import com.jetbrains.php.lang.psi.elements.Variable
import com.jetbrains.php.lang.psi.visitors.PhpRecursiveElementVisitor

fun PsiElement.findVariablesByName(variableName: String): List<Variable> {
    val variables = mutableListOf<Variable>()
    this.acceptChildren(object : PhpRecursiveElementVisitor() {
        override fun visitPhpVariable(variable: Variable) {
            if (PhpLangUtil.equalsVariableNames(variable.name, variableName)) {
                variables.add(variable)
            }
        }
    })
    variables.reverse()
    return variables
}
