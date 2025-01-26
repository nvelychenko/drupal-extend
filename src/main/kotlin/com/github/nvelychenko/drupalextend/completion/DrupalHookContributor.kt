package com.github.nvelychenko.drupalextend.completion

import com.github.nvelychenko.drupalextend.completion.providers.HookAttributeCompletionProvider
import com.github.nvelychenko.drupalextend.patterns.Patterns.HOOK_ATTRIBUTE_PARAMETER_STRING_LITERAL
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType

class DrupalHookContributor: CompletionContributor() {
    init {
        // #[Hook('here')]
        extend(CompletionType.BASIC, HOOK_ATTRIBUTE_PARAMETER_STRING_LITERAL, HookAttributeCompletionProvider())
    }
}