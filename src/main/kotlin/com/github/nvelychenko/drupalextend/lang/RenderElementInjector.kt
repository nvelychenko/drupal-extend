package com.github.nvelychenko.drupalextend.lang

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement

@Suppress("unused")
class RenderElementInjector(private val psiElement: PsiElement) : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        TODO("Not yet implemented")
    }

    override fun elementsToInjectIn(): MutableList<out Class<out PsiElement>> {
        TODO("Not yet implemented")
    }
}