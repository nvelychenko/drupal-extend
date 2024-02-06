package com.github.nvelychenko.drupalextend.completion.providers

import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.FieldTypeIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.jetbrains.php.lang.psi.elements.*

/**
 * Provides autocompletion for field properties.
 *
 * $node->get('field')->property|
 * $field|$fieldList->property|
 */
open class FieldPropertyCompletionProvider : CompletionProvider<CompletionParameters>() {

    val fileBasedIndex: FileBasedIndex by lazy {
        FileBasedIndex.getInstance()
    }

    override fun addCompletions(
        completionParameters: CompletionParameters,
        context: ProcessingContext,
        completionResultSet: CompletionResultSet
    ) {
        val psiElement = completionParameters.originalPosition ?: return

        if (!psiElement.project.drupalExtendSettings.isEnabled) return

        val parent: PsiElement
        val prevSibling = completionParameters.position.prevSibling
        parent = if (prevSibling != null && prevSibling.node.elementType === PhpTokenTypes.ARROW) {
            when (prevSibling.parent) {
                is FieldReference -> prevSibling.parent
                else -> return
            }
        } else {
            psiElement.parent
        }

        if (parent !is FieldReference) return
        val methodReference = when (val classReference = parent.classReference) {
            is MethodReference -> when (classReference.name) {
                "get" -> classReference
                "first" -> classReference.classReference
                else -> return
            }
            is Variable -> classReference

            is ArrayAccessExpression -> classReference.children.firstOrNull()
            else -> return
        }

        val phpTypes = when (methodReference) {
            is MethodReference -> methodReference.globalType
            is Variable -> methodReference.globalType
            else -> return
        }

        val project = methodReference.project
        val fieldTypes = FieldTypeIndex.getAllFqns(project)
        val fieldTypeFqn = phpTypes.types.find {
            val singularType = it.replace("[]", "")
            fieldTypes.containsKey(singularType)
        }?.replace("[]", "") ?: return

        val fieldType = fieldTypes[fieldTypeFqn] ?: return

        val properties = HashMap<String, String>()
        properties.putAll(fieldType.properties)


        val fieldClassInstance =
                PhpIndex.getInstance(project).getClassesByFQN(fieldType.fqn).firstOrNull() ?: return
        PhpClassHierarchyUtils.processSuperClasses(fieldClassInstance, false, true) {
            it.findOwnMethodByName("propertyDefinitions") ?: return@processSuperClasses true
            val superProperties = fileBasedIndex.getValue(FieldTypeIndex.KEY, it.fqn, project)
            superProperties?.let { properties.putAll(superProperties.properties) }
            return@processSuperClasses true
        }

        properties.forEach {
            completionResultSet.addElement(
                    PrioritizedLookupElement.withPriority(
                            LookupElementBuilder.create(it.key).withTypeText(it.value).withBoldness(true),
                            100.0,
                    ),
            )
        }
    }
}