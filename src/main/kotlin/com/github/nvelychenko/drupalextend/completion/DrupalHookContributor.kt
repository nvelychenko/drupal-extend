package com.github.nvelychenko.drupalextend.completion

import com.github.nvelychenko.drupalextend.completion.providers.HookAttributeCompletionProvider
import com.github.nvelychenko.drupalextend.completion.providers.HookAttributeMethodCompletionProvider
import com.github.nvelychenko.drupalextend.patterns.Patterns.HOOK_ATTRIBUTE_PARAMETER_STRING_LITERAL
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PlatformPatterns.psiElement
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass



class DrupalHookContributor: CompletionContributor() {
    init {
        // #[Hook('here')]
        extend(CompletionType.BASIC, HOOK_ATTRIBUTE_PARAMETER_STRING_LITERAL, HookAttributeCompletionProvider())

        // #[Hook('here')]
        extend(CompletionType.BASIC, PlatformPatterns.or(psiElement().withParent(PhpClass::class.java), psiElement().withParent(Method::class.java)), HookAttributeMethodCompletionProvider())
    }
}
