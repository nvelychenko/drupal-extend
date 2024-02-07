package com.github.nvelychenko.drupalextend.completion

import com.github.nvelychenko.drupalextend.completion.providers.*
import com.github.nvelychenko.drupalextend.patterns.Patterns.LEAF_STRING_IN_SIMPLE_ARRAY_VALUE
import com.github.nvelychenko.drupalextend.patterns.Patterns.STRING_LEAF_IN_ARRAY_KEY_OR_ONLY_VALUE
import com.github.nvelychenko.drupalextend.patterns.Patterns.STRING_LITERAL_INSIDE_METHOD_PARAMETER
import com.github.nvelychenko.drupalextend.patterns.Patterns.TRIPLE_ARRAY_WITH_STRING_VALUE
import com.github.nvelychenko.drupalextend.patterns.Patterns.TRIPLE_NESTED_STRING_ARRAY_VALUE
import com.github.nvelychenko.drupalextend.patterns.Patterns.TRIPLE_SIMPLE_STRING_ARRAY_VALUE
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns.or
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.PhpLanguage


class DrupalCompletionContributor : CompletionContributor() {

    init {
        // \Drupal::entityTypeManager->getStorage('no|
        extend(CompletionType.BASIC, STRING_LITERAL_INSIDE_METHOD_PARAMETER, EntityStorageProvider())

        // \Drupal::entityTypeManager->getStorage('no|
        extend(CompletionType.BASIC, STRING_LITERAL_INSIDE_METHOD_PARAMETER, StaticEntityQueryProvider())

        // \Drupal::entityTypeManager->getQuery()->condition('
        extend(CompletionType.BASIC, STRING_LITERAL_INSIDE_METHOD_PARAMETER, EntityQueryConditionFieldProvider())

        // $node->set('fi
        // $node->get('fi|
        extend(CompletionType.BASIC, STRING_LITERAL_INSIDE_METHOD_PARAMETER, FieldCompletionProvider())

        // $render = ['#them|
        extend(CompletionType.BASIC, STRING_LEAF_IN_ARRAY_KEY_OR_ONLY_VALUE, BasicThemeOrTypeProvider())

        // $render = [
        //    '#type' => '|
        // ];
        extend(CompletionType.BASIC, LEAF_STRING_IN_SIMPLE_ARRAY_VALUE, RenderElementTypeProvider())

        // $render = [
        //    '#theme' => '|
        // ];
        extend(CompletionType.BASIC, LEAF_STRING_IN_SIMPLE_ARRAY_VALUE, ThemeProvider())

        // $render = [
        //    '#type' => 'checkbox',
        //    '#|
        // ];
        extend(CompletionType.BASIC, STRING_LEAF_IN_ARRAY_KEY_OR_ONLY_VALUE, RenderElementTypePropertiesProvider())

        // $render = [
        //    '#theme' => 'item_list',
        //    '#|
        // ];
        extend(CompletionType.BASIC, STRING_LEAF_IN_ARRAY_KEY_OR_ONLY_VALUE, ThemePropertiesProvider())

        // $build['#attached']['library'][] = 'he|
        extend(
            CompletionType.BASIC,
            or(
                psiElement(PsiElement::class.java).withParent(TRIPLE_ARRAY_WITH_STRING_VALUE),
                psiElement(PsiElement::class.java).withParent(TRIPLE_SIMPLE_STRING_ARRAY_VALUE),
                psiElement(PsiElement::class.java).withParent(TRIPLE_NESTED_STRING_ARRAY_VALUE),
            ),
            LibrariesCompletionProvider()
        )

        // $node->get('field_entity')->entity
        // $node->get('field_entity')[0]->entity
        // $node->get('field_entity')->first()->entity
        // $field->entity
        extend(CompletionType.BASIC, psiElement().withLanguage(PhpLanguage.INSTANCE), FieldPropertyCompletionProvider())
    }

}