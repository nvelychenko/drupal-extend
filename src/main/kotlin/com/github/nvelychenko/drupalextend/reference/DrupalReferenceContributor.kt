package com.github.nvelychenko.drupalextend.reference

import com.github.nvelychenko.drupalextend.patterns.Patterns.STRING_INSIDE_METHOD_PARAMETER
import com.github.nvelychenko.drupalextend.patterns.Patterns.STRING_IN_SIMPLE_ARRAY_VALUE
import com.github.nvelychenko.drupalextend.reference.referenceProvider.EntityStorageReferenceProvider
import com.github.nvelychenko.drupalextend.reference.referenceProvider.FieldReferenceProvider
import com.github.nvelychenko.drupalextend.reference.referenceProvider.RenderElementTypeReferenceProvider
import com.github.nvelychenko.drupalextend.reference.referenceProvider.ThemeReferenceProvider
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class DrupalReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // \Drupal::entityTypeManager()->getStorage('node')
        //                                             ↑
        registrar.registerReferenceProvider(STRING_INSIDE_METHOD_PARAMETER, EntityStorageReferenceProvider())

        // $node->get('field_body')
        //                ↑
        registrar.registerReferenceProvider(STRING_INSIDE_METHOD_PARAMETER, FieldReferenceProvider())

        // ['#type' => 'checkbox']
        //                 ↑
        registrar.registerReferenceProvider(STRING_IN_SIMPLE_ARRAY_VALUE, RenderElementTypeReferenceProvider())

        registrar.registerReferenceProvider(STRING_IN_SIMPLE_ARRAY_VALUE, ThemeReferenceProvider())
    }
}