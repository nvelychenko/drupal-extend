package com.github.nvelychenko.drupalextend.completion.utils

import com.github.nvelychenko.drupalextend.index.types.SdcComponent
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder

/**
 * SDC props and slots are listed above other suggestions, same as field properties.
 */
private const val SDC_PRIORITY = 100.0

/**
 * Required props are bold and listed first.
 */
fun SdcComponent.Props.toLookupElement(): LookupElement {
    val lookup = LookupElementBuilder.create(this, id).withTypeText(type, true).withBoldness(required)
    return PrioritizedLookupElement.withPriority(lookup, if (required) SDC_PRIORITY + 1 else SDC_PRIORITY)
}

fun SdcComponent.Slots.toLookupElement(): LookupElement =
    PrioritizedLookupElement.withPriority(LookupElementBuilder.create(id).withTypeText(title, true), SDC_PRIORITY)

fun SdcComponent.Variants.toLookupElement(): LookupElement =
    PrioritizedLookupElement.withPriority(LookupElementBuilder.create(id).withTypeText(title, true), SDC_PRIORITY)
