package com.github.nvelychenko.drupalextend.extensions

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.lang.PhpLangUtil
import com.jetbrains.php.lang.psi.elements.PhpClass
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

fun PhpClass.isSuperInterfaceOf(interfaces: Array<PhpClass>): Boolean {
    var isInstanceOf = false
    PhpClassHierarchyUtils.processSuperInterfaces(this, true, true) {
        interfaces.forEach { currentInterface ->
            if (PhpClassHierarchyUtils.classesEqual(currentInterface, it)) {
                isInstanceOf = true
            }
        }

        return@processSuperInterfaces !isInstanceOf
    }

    return isInstanceOf
}