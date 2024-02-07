package com.github.nvelychenko.drupalextend.extensions

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.lang.PhpLangUtil
import com.jetbrains.php.lang.psi.elements.*

fun PsiElement.findVariablesByName(variableName: String): Array<Variable> {
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
    return variables.toTypedArray()
}

fun PhpClass.isSuperInterfaceOf(interfaze: PhpClass): Boolean {
    return isSuperInterfacesOf(arrayOf(interfaze))
}

fun PhpClass.isSuperInterfacesOf(interfaces: Array<PhpClass>): Boolean {
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

fun LeafPsiElement.hasDrupalTheme(): Boolean {
    return (parent as? StringLiteralExpression)?.hasDrupalTheme() ?: false
}

fun LeafPsiElement.hasDrupalRenderElement(): Boolean {
    return (parent as? StringLiteralExpression)?.hasDrupalRenderElement() ?: false
}

fun StringLiteralExpression.hasDrupalTheme(): Boolean {
    return "#theme" == getThemeOrRenderElementKey()?.contents
}

fun StringLiteralExpression.hasDrupalRenderElement(): Boolean {
    return "#type" == getThemeOrRenderElementKey()?.contents
}

fun StringLiteralExpression.getThemeOrRenderElementKey(): StringLiteralExpression? {
    return when (val subParent = parent) {
        is AssignmentExpression -> {
            (subParent.variable as ArrayAccessExpression).index?.value as? StringLiteralExpression ?: return null
        }

        is PhpPsiElement -> {
            val arrayHash = PsiTreeUtil.getParentOfType(subParent, ArrayHashElement::class.java) ?: return null
            arrayHash.key as StringLiteralExpression
        }

        else -> return null
    }
}

fun StringLiteralExpression.getThemeOrRenderElementValue(): StringLiteralExpression? {
    return when (val subParent = parent) {
        is AssignmentExpression -> {
            subParent.value as? StringLiteralExpression
        }

        is PhpPsiElement -> {
            val arrayHash = PsiTreeUtil.getParentOfType(subParent, ArrayHashElement::class.java) ?: return null
            arrayHash.value as? StringLiteralExpression
        }

        else -> return null
    }
}
