package com.github.nvelychenko.drupalextend.patterns

import com.intellij.patterns.PlatformPatterns.or
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern.Capture
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.jetbrains.php.lang.parser.PhpElementTypes
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

object Patterns {
    private val phpLanguage by lazy { PhpLanguage.INSTANCE }

    /**
     * $method->get('blabla
     *                 ↑
     */
    @Suppress("unused")
    val LEAF_INSIDE_METHOD_PARAMETER: Capture<LeafPsiElement> by lazy {
        psiElement(LeafPsiElement::class.java)
            .withParent(
                psiElement(StringLiteralExpression::class.java)
                    .withParent(
                        psiElement(PhpElementTypes.PARAMETER_LIST)
                            .withParent(psiElement(PhpElementTypes.METHOD_REFERENCE))
                    )
            )
            .withLanguage(phpLanguage)
    }

    val STRING_LITERAL_INSIDE_METHOD_PARAMETER by lazy {
        or(
            psiElement(PhpTokenTypes.STRING_LITERAL).withParent(STRING_INSIDE_METHOD_PARAMETER),
            psiElement(PhpTokenTypes.chRSINGLE_QUOTE).withParent(STRING_INSIDE_METHOD_PARAMETER),
            psiElement(PhpTokenTypes.chRDOUBLE_QUOTE).withParent(STRING_INSIDE_METHOD_PARAMETER),
            psiElement(PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE).withParent(STRING_INSIDE_METHOD_PARAMETER)
        )
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
            .withLanguage(phpLanguage)
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
            .withLanguage(phpLanguage)
    }

    val ARRAY_KEY_WITH_ARRAY_CREATION_EXPRESSION: Capture<StringLiteralExpression> by lazy {
        psiElement(StringLiteralExpression::class.java)
            .withParent(
                psiElement(PhpElementTypes.ARRAY_KEY)
                    .withParent(
                        psiElement(PhpElementTypes.HASH_ARRAY_ELEMENT)
                            .withChild(psiElement(PhpElementTypes.ARRAY_VALUE).withChild(psiElement(PhpElementTypes.ARRAY_CREATION_EXPRESSION)))
                    )
            )
            .withLanguage(phpLanguage)
    }

    val ARRAY_KEY_INSIDE_ASSIGNMENT_EXPRESSION: Capture<StringLiteralExpression> by lazy {
        psiElement(StringLiteralExpression::class.java)
            .withParent(
                psiElement(PhpElementTypes.ARRAY_INDEX)
                    .withParent(
                        psiElement(PhpElementTypes.ARRAY_ACCESS_EXPRESSION)
                            .withParent(
                                psiElement(PhpElementTypes.ASSIGNMENT_EXPRESSION)
                            )
                    )
            )
            .withLanguage(phpLanguage)
    }

    val SIMPLE_FUNCTION: Capture<PsiElement> by lazy { psiElement(PhpElementTypes.FUNCTION).withLanguage(phpLanguage) }

    val FUNCTION_OR_METHOD by lazy {
        or(
            psiElement(PhpElementTypes.FUNCTION).withLanguage(phpLanguage),
            psiElement(PhpElementTypes.CLASS_METHOD).withLanguage(phpLanguage),
        )
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
            .withLanguage(phpLanguage)
    }

    /**
     * $array['#theme'] = 'kek'
     *                      ↑
     */
    val SIMPLE_ARRAY_VALUE_ASSIGNMENT: Capture<StringLiteralExpression> by lazy {
        psiElement(StringLiteralExpression::class.java)
            .withParent(
                psiElement(PhpElementTypes.ASSIGNMENT_EXPRESSION)
                    .withChild(
                        psiElement(PhpElementTypes.ARRAY_ACCESS_EXPRESSION)
                            .withChild(
                                psiElement(PhpElementTypes.ARRAY_INDEX)
                                    .withChild(psiElement(StringLiteralExpression::class.java))
                            )
                    )
            )
            .withLanguage(phpLanguage)
    }

    val LEAF_STRING_IN_SIMPLE_ARRAY_VALUE by lazy {
        or(
            psiElement(LeafPsiElement::class.java).withParent(STRING_IN_SIMPLE_ARRAY_VALUE).withLanguage(phpLanguage),
            psiElement(LeafPsiElement::class.java).withParent(SIMPLE_ARRAY_VALUE_ASSIGNMENT).withLanguage(phpLanguage),
        )
    }

    /**
     * @sample
     *
     * ['value']
     *     ↑
     *
     * or
     *
     * ['key' => 'value']
     *    ↑
     * or
     * [
     *   ['key' => 'value']
     *      ↑
     * ]
     */
    val STRING_LEAF_IN_ARRAY_KEY_OR_ONLY_VALUE: Capture<LeafPsiElement> by lazy {
        psiElement(LeafPsiElement::class.java)
            .withParent(STRING_LITERAL_IN_ARRAY_KEY_OR_ONLY_VALUE)
            .withLanguage(phpLanguage)
    }

    val STRING_INSIDE_ARRAY_METHOD: Capture<StringLiteralExpression> by lazy {
        psiElement(StringLiteralExpression::class.java)
            .withParent(
                or(
                    psiElement(PhpElementTypes.ARRAY_KEY)
                        .withParent(
                            or(
                                psiElement(PhpElementTypes.HASH_ARRAY_ELEMENT)
                                    .withParent(
                                        psiElement(PhpElementTypes.ARRAY_CREATION_EXPRESSION)
                                            .withParent(
                                                psiElement(PhpElementTypes.PARAMETER_LIST)
                                                    .withParent(psiElement(PhpElementTypes.METHOD_REFERENCE))
                                            )
                                    ),
                                psiElement(PhpElementTypes.ARRAY_CREATION_EXPRESSION)
                                    .withParent(
                                        psiElement(PhpElementTypes.PARAMETER_LIST)
                                            .withParent(psiElement(PhpElementTypes.METHOD_REFERENCE))
                                    )

                            )
                        ),
                    psiElement(PhpElementTypes.ARRAY_VALUE)
                        .withParent(
                            psiElement(PhpElementTypes.ARRAY_CREATION_EXPRESSION)
                                .withParent(
                                    psiElement(PhpElementTypes.PARAMETER_LIST)
                                        .withParent(psiElement(PhpElementTypes.METHOD_REFERENCE))
                                )
                        )
                )

            )
            .withLanguage(phpLanguage)
    }

    /**
     * @sample
     *
     * $build['#attached']['library'][] = 'here';
     */
    val TRIPLE_ARRAY_WITH_STRING_VALUE: Capture<StringLiteralExpression> by lazy {
        psiElement(StringLiteralExpression::class.java)
            .withParent(
                psiElement(PhpElementTypes.ASSIGNMENT_EXPRESSION)
                    .withChild(
                        psiElement(PhpElementTypes.ARRAY_ACCESS_EXPRESSION)
                            .withChild(
                                psiElement(PhpElementTypes.ARRAY_ACCESS_EXPRESSION)
                                    .withChild(
                                        psiElement(PhpElementTypes.ARRAY_ACCESS_EXPRESSION)
                                            .withChild(psiElement(PhpElementTypes.VARIABLE))
                                    )
                            )
                    )
            ).withLanguage(phpLanguage)
    }

    /**
     * @sample
     *
     * $build['#attached'] = [
     *   'library' => [
     *     'here',
     *        ↑
     *   ],
     * ];
     */
    val TRIPLE_NESTED_STRING_ARRAY_VALUE: Capture<StringLiteralExpression> by lazy {
        psiElement(StringLiteralExpression::class.java)
            .withParent(
                psiElement(PhpElementTypes.ARRAY_VALUE)
                    .withParent(
                        psiElement(PhpElementTypes.ARRAY_CREATION_EXPRESSION)
                            .withParent(
                                psiElement(PhpElementTypes.ARRAY_VALUE)
                                    .withParent(
                                        psiElement(PhpElementTypes.HASH_ARRAY_ELEMENT)
                                            .withParent(
                                                psiElement(PhpElementTypes.ARRAY_CREATION_EXPRESSION)
                                                    .withParent(psiElement(PhpElementTypes.ASSIGNMENT_EXPRESSION))
                                            )
                                    )
                            )
                    )
            ).withLanguage(phpLanguage)
    }

    /**
     * @sample
     *
     * $build = [
     *   '#attached' => [
     *     'library' => [
     *       'here',
     *         ↑
     *     ],
     *   ],
     * ];
     */
    val TRIPLE_SIMPLE_STRING_ARRAY_VALUE: Capture<StringLiteralExpression> by lazy {
        psiElement(StringLiteralExpression::class.java)
            .withParent(
                psiElement(PhpElementTypes.ARRAY_VALUE)
                    .withParent(
                        psiElement(PhpElementTypes.ARRAY_CREATION_EXPRESSION)
                            .withParent(
                                psiElement(PhpElementTypes.ARRAY_VALUE)
                                    .withParent(
                                        psiElement(PhpElementTypes.HASH_ARRAY_ELEMENT)
                                            .withParent(
                                                psiElement(PhpElementTypes.ARRAY_CREATION_EXPRESSION)
                                                    .withParent(
                                                        psiElement(PhpElementTypes.ARRAY_VALUE)
                                                            .withParent(psiElement(PhpElementTypes.HASH_ARRAY_ELEMENT))
                                                    )
                                            )
                                    )
                            )
                    )
            ).withLanguage(phpLanguage)
    }

    /**
     * @sample
     *
     * #[Hook('here', 'here' ...)];
     */
    val HOOK_ATTRIBUTE_PARAMETER_STRING_LITERAL by lazy {
        psiElement(LeafPsiElement::class.java)
            .withParent(
                psiElement(StringLiteralExpression::class.java)
                    .withParent(
                        psiElement(PhpElementTypes.PARAMETER_LIST)
                            .withParent(psiElement(PhpElementTypes.ATTRIBUTE))
                    )
            )
            .withLanguage(phpLanguage)
    }

    private val STRING_LITERAL_IN_ARRAY_KEY_OR_ONLY_VALUE: Capture<StringLiteralExpression> by lazy {
        psiElement(StringLiteralExpression::class.java)
            .withParent(
                or(
                    psiElement(PhpElementTypes.ARRAY_KEY)
                        .withParent(
                            or(
                                psiElement(PhpElementTypes.HASH_ARRAY_ELEMENT)
                                    .withParent(psiElement(PhpElementTypes.ARRAY_CREATION_EXPRESSION)),
                                psiElement(PhpElementTypes.ARRAY_CREATION_EXPRESSION)
                            )
                        ),
                    psiElement(PhpElementTypes.ARRAY_VALUE)
                        .withParent(psiElement(PhpElementTypes.ARRAY_CREATION_EXPRESSION))
                )
            )
            .withLanguage(phpLanguage)

    }

}