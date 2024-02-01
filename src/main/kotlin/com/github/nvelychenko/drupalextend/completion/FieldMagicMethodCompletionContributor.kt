package com.github.nvelychenko.drupalextend.completion

import com.github.nvelychenko.drupalextend.completion.providers.MagicPropertyFieldCompletionProvider
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.jetbrains.php.lang.PhpLanguage

class FieldMagicMethodCompletionContributor : CompletionContributor() {

    init {
        // $node->field_|
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(PhpLanguage.INSTANCE),
            MagicPropertyFieldCompletionProvider()
        )
    }

}