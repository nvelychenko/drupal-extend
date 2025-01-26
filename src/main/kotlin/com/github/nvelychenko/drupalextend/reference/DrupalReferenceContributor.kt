package com.github.nvelychenko.drupalextend.reference

import com.github.nvelychenko.drupalextend.patterns.Patterns.SIMPLE_ARRAY_VALUE_ASSIGNMENT
import com.github.nvelychenko.drupalextend.patterns.Patterns.STRING_INSIDE_METHOD_PARAMETER
import com.github.nvelychenko.drupalextend.patterns.Patterns.STRING_IN_SIMPLE_ARRAY_VALUE
import com.github.nvelychenko.drupalextend.reference.referenceProvider.*
import com.intellij.patterns.PlatformPatterns.or
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class DrupalReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {

        // $account->hasPermission('|
        registrar.registerReferenceProvider(STRING_INSIDE_METHOD_PARAMETER, PermissionsReferenceProvider())

        // \Drupal::entityTypeManager()->getStorage('node')
        //                                             ↑
        registrar.registerReferenceProvider(STRING_INSIDE_METHOD_PARAMETER, EntityStorageReferenceProvider())

        // $node->get('field_body')
        //                ↑
        registrar.registerReferenceProvider(STRING_INSIDE_METHOD_PARAMETER, FieldReferenceProvider())

        // ['#type' => 'checkbox']
        //                 ↑
        // $type['#type'] = 'type';
        //                    ↑
        registrar.registerReferenceProvider(
            or(STRING_IN_SIMPLE_ARRAY_VALUE, SIMPLE_ARRAY_VALUE_ASSIGNMENT),
            RenderElementTypeReferenceProvider()
        )

        // ['#theme' => 'checkbox']
        //                 ↑
        // $theme['#theme'] = 'type';
        //                    ↑
        registrar.registerReferenceProvider(
            or(STRING_IN_SIMPLE_ARRAY_VALUE, SIMPLE_ARRAY_VALUE_ASSIGNMENT),
            ThemeReferenceProvider()
        )
    }
}