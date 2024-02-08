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
        extend(CompletionType.BASIC, STRING_LITERAL_INSIDE_METHOD_PARAMETER, EntityStorageCompletionProvider())

        // \Drupal::entityTypeManager->getStorage('no|
        extend(CompletionType.BASIC, STRING_LITERAL_INSIDE_METHOD_PARAMETER, StaticEntityQueryCompletionProvider())

        // \Drupal::entityTypeManager->getQuery()->condition('
        extend(CompletionType.BASIC, STRING_LITERAL_INSIDE_METHOD_PARAMETER, EntityQueryConditionFieldCompletionProvider())

        // $node->set('fi
        // $node->get('fi|
        extend(CompletionType.BASIC, STRING_LITERAL_INSIDE_METHOD_PARAMETER, FieldCompletionCompletionProvider())

        // $render = ['#them|
        extend(CompletionType.BASIC, STRING_LEAF_IN_ARRAY_KEY_OR_ONLY_VALUE, BasicThemeOrTypeCompletionProvider())

        // $render = [
        //    '#type' => '|
        // ];
        extend(CompletionType.BASIC, LEAF_STRING_IN_SIMPLE_ARRAY_VALUE, RenderElementTypeCompletionProvider())

        // $render = [
        //    '#theme' => '|
        // ];
        extend(CompletionType.BASIC, LEAF_STRING_IN_SIMPLE_ARRAY_VALUE, ThemeCompletionProvider())

        // $render = [
        //    '#type' => 'checkbox',
        //    '#|
        // ];
        extend(CompletionType.BASIC, STRING_LEAF_IN_ARRAY_KEY_OR_ONLY_VALUE, RenderElementTypePropertiesCompletionProvider())

        // $render = [
        //    '#theme' => 'item_list',
        //    '#|
        // ];
        extend(CompletionType.BASIC, STRING_LEAF_IN_ARRAY_KEY_OR_ONLY_VALUE, ThemePropertiesCompletionProvider())

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