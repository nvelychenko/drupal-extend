package com.github.nvelychenko.drupalextend.patterns

import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern.Capture
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.parser.PhpElementTypes
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

object Patterns {
    /**
     * $method->get('blabla
     *                 ↑
     */
    val LEAF_INSIDE_METHOD_PARAMETER: Capture<LeafPsiElement> by lazy {
        psiElement(LeafPsiElement::class.java)
            .withParent(
                psiElement(StringLiteralExpression::class.java)
                    .withParent(
                        psiElement(PhpElementTypes.PARAMETER_LIST)
                            .withParent(psiElement(PhpElementTypes.METHOD_REFERENCE))
                    )
            )
            .withLanguage(PhpLanguage.INSTANCE)
    }

    /**
     * $method->get('blabla')
     *                  ↑
     */
    val STRING_INSIDE_METHOD_PARAMETER: Capture<StringLiteralExpression> by lazy {
        psiElement(StringLiteralExpression::class.java)
            .withParent(
                psiElement(PhpElementTypes.PARAMETER_LIST)
                    .withParent(psiElement(PhpElementTypes.METHOD_REFERENCE))
            )
            .withLanguage(PhpLanguage.INSTANCE)
    }

    /**
     * $method->get('d
     *     ↑
     */
    val METHOD_WITH_FIRST_STRING_PARAMETER: Capture<PsiElement> by lazy {
        psiElement(PhpElementTypes.METHOD_REFERENCE)
            .withChild(
                psiElement(PhpElementTypes.PARAMETER_LIST)
                    .withFirstChild(
                        psiElement(StringLiteralExpression::class.java),
                    )
            )
            .withLanguage(PhpLanguage.INSTANCE)
    }

    /**
     * ['string' => 'string']
     *                  ↑
     */
    val STRING_IN_SIMPLE_ARRAY_VALUE: Capture<StringLiteralExpression> by lazy {
        psiElement(StringLiteralExpression::class.java)
            .withParent(
                psiElement(PhpElementTypes.ARRAY_VALUE)
                    .withParent(
                        psiElement(PhpElementTypes.HASH_ARRAY_ELEMENT)
                            .withFirstChild(
                                psiElement(PhpElementTypes.ARRAY_KEY)
                                    .withChild(psiElement(StringLiteralExpression::class.java))
                            )
                    )
            )
    }

}