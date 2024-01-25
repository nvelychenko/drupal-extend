package com.github.nvelychenko.drupalextend.util.yml

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.yaml.psi.YAMLKeyValue

class YAMLKeyValueFinder(private val keyPath: String) : PsiRecursiveElementVisitor() {
    private var kv: YAMLKeyValue? = null

    override fun visitElement(element: PsiElement) {
        if (element is YAMLKeyValue && element.keyPath == keyPath) {
            kv = element
        }

        if (kv == null) {
            super.visitElement(element)
        }
    }

    fun findIn(element: PsiElement): YAMLKeyValue? {
        element.accept(this)
        return kv
    }
}
