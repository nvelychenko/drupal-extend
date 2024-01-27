package com.github.nvelychenko.drupalextend.extensions

import com.intellij.psi.PsiElement


val PsiElement.parents: Iterable<PsiElement>
    get() = object : Iterable<PsiElement> {
        override fun iterator(): Iterator<PsiElement> {
            var file = this@parents

            return object : Iterator<PsiElement> {
                override fun hasNext() = file.parent != null
                override fun next(): PsiElement {
                    file = file.parent
                    return file
                }
            }
        }
    }

