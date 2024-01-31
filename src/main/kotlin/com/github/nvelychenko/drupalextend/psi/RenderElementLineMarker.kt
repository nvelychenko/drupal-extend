package com.github.nvelychenko.drupalextend.psi

import com.github.nvelychenko.drupalextend.extensions.containsRenderElement
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.icon.DrupalIcons.HASH_ICON
import com.github.nvelychenko.drupalextend.index.RenderElementIndex
import com.github.nvelychenko.drupalextend.patterns.Patterns.STRING_IN_SIMPLE_ARRAY_VALUE
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.patterns.PlatformPatterns.or
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import javax.swing.Icon


class RenderElementLineMarker : RelatedItemLineMarkerProvider() {

    private val fileBasedIndex: FileBasedIndex by lazy { FileBasedIndex.getInstance() }

    override fun getIcon(): Icon {
        return HASH_ICON
    }

    override fun getName(): String {
        return "Render element type"
    }

    override fun collectNavigationMarkers(
        element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (!or(
                psiElement(PhpTokenTypes.STRING_LITERAL).withParent(STRING_IN_SIMPLE_ARRAY_VALUE),
                psiElement(PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE).withParent(STRING_IN_SIMPLE_ARRAY_VALUE)
            ).accepts(element)
        ) {
            return
        }

        val hash = PsiTreeUtil.getParentOfType(element, ArrayHashElement::class.java) ?: return

        if (!hash.containsRenderElement()) {
            return
        }

        val project = element.project

        val renderElement = fileBasedIndex.getValue(RenderElementIndex.KEY, (hash.value as StringLiteralExpression).contents, project) ?: return

        val renderElementClass =
            PhpIndex.getInstance(project).getClassesByFQN(renderElement.typeClass).firstOrNull() ?: return
        val itemToNavigate =
            renderElementClass.docComment?.getTagElementsByName("@${renderElement.renderElementType}") ?: return

        val builder: NavigationGutterIconBuilder<PsiElement> =
            NavigationGutterIconBuilder.create(HASH_ICON).setTargets(itemToNavigate.toMutableList())
                .setTooltipText("Navigate to ${renderElement.renderElementType} ")
        result.add(builder.createLineMarkerInfo(element))
    }
}