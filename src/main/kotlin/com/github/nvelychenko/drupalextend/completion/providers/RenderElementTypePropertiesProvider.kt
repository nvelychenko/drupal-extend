package com.github.nvelychenko.drupalextend.completion.providers

import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.RenderElementIndex
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class RenderElementTypePropertiesProvider : CompletionProvider<CompletionParameters>() {

    private val fileBasedIndex by lazy { FileBasedIndex.getInstance() }

    public override fun addCompletions(
        completionParameters: CompletionParameters,
        processingContext: ProcessingContext,
        completionResultSet: CompletionResultSet
    ) {
        val leaf = completionParameters.originalPosition ?: return

        val array = PsiTreeUtil.getParentOfType(leaf, ArrayCreationExpression::class.java)!!

        val typeElement = array.hashElements.find {
            val key = it.key
            key is StringLiteralExpression && key.contents == "#type" && it.value is StringLiteralExpression
        } ?: return

        val type =
            (typeElement.value as StringLiteralExpression).contents.takeIf { it.isNotEmpty() } ?: return

        val project = array.project

        val renderElementType = fileBasedIndex.getValue(RenderElementIndex.KEY, type, project) ?: return

        val elementClass =
            PhpIndex.getInstance(project).getClassesByFQN(renderElementType.typeClass).firstOrNull()
                ?: return

        var properties = renderElementType.properties

        PhpClassHierarchyUtils.processSuperClasses(elementClass, false, true) { phpClass ->
            fileBasedIndex.getValue(RenderElementIndex.KEY, phpClass.fqn, project)
                ?.let { properties = arrayOf(*properties, *it.properties) }
            if (phpClass.fqn == "\\Drupal\\Core\\Render\\Element\\RenderElement") {
                return@processSuperClasses false
            }
            true
        }

        properties.forEach {
            completionResultSet.addElement(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder.create(it.id.replace("#", ""))
                        .withTypeText(it.type)
                        .withBoldness(it.priority > 0.0)
                        .withInsertHandler(RenderElementTypeInsertionHandler()),
                    it.priority
                )
            )
        }
    }

    private class RenderElementTypeInsertionHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val document = context.document
            val editor = context.editor


            val fileText: String = editor.document.text
            if (fileText.isEmpty()) {
                return
            }

            if (document.charsSequence[context.startOffset - 1] != '#') {
                context.document.insertString(context.startOffset, "#")
            }
        }
    }

}