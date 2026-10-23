package com.github.nvelychenko.drupalextend.completion

import com.github.nvelychenko.drupalextend.completion.utils.toLookupElement
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.ComponentsIndex
import com.github.nvelychenko.drupalextend.index.types.SdcComponent
import com.github.nvelychenko.drupalextend.patterns.Patterns.TWIG_HASH_KEY
import com.github.nvelychenko.drupalextend.patterns.Patterns.TWIG_SDC_COMPONENT_NAME
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder.create
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import com.intellij.psi.tree.TokenSet
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.twig.elements.TwigElementTypes
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern


class DrupalTwigCompletionContributor : CompletionContributor() {

    val fileBasedIndex: FileBasedIndex by lazy {
        FileBasedIndex.getInstance()
    }

    init {
        // {% include 'theme:comp' %}, {% embed 'theme:comp' %}, {{ include('theme:comp') }}
        extend(
            CompletionType.BASIC,
            TWIG_SDC_COMPONENT_NAME,
            object: CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val project = parameters.position.project
                    if (!project.drupalExtendSettings.isEnabled) return

                    FileBasedIndex.getInstance()
                        .getAllKeys(ComponentsIndex.KEY, project)
                        .mapNotNull(::create)
                        .forEach { result.addElement(it) }
                }
            },
        )

        // {{ include('theme:comp', { | }) }}, {% include 'theme:comp' with { | } %}, {% embed 'theme:comp' with { | } %}
        extend(
            CompletionType.BASIC,
            TWIG_HASH_KEY,
            object: CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val element = parameters.position
                    if (!element.project.drupalExtendSettings.isEnabled) return

                    val component = findSdcComponent(element, PROPS_HOSTS) ?: return
                    component.props.forEach { result.addElement(it.toLookupElement()) }
                }
            },
        )

        // {% embed 'theme:comp' %} {% block | %} {% endblock %} {% endembed %}
        extend(
            CompletionType.BASIC,
            TwigPattern.getBlockTagPattern(),
            object: CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val element = parameters.position
                    if (!element.project.drupalExtendSettings.isEnabled) return

                    val component = findSdcComponent(element, SLOTS_HOSTS) ?: return
                    component.slots.forEach { result.addElement(it.toLookupElement()) }
                }
            },
        )
    }

    /**
     * Finds the nearest ancestor of one of [hosts] types and resolves the SDC component referenced in it.
     */
    private fun findSdcComponent(element: PsiElement, hosts: TokenSet): SdcComponent? {
        // ponytail: nearest host wins, so a slot block nested inside another block of an embed still resolves to that embed.
        val host = generateSequence(element) { it.parent }
            .takeWhile { it !is PsiFile }
            .firstOrNull { hosts.contains(it.node.elementType) }
            ?: return null

        val name = SyntaxTraverser.psiTraverser(host)
            .filter { TWIG_SDC_COMPONENT_NAME.accepts(it) }
            .first()
            ?.text
            ?: return null

        return fileBasedIndex.getValue(ComponentsIndex.KEY, name, element.project)
    }

    companion object {
        private val PROPS_HOSTS = TokenSet.create(
            TwigElementTypes.INCLUDE_TAG,
            TwigElementTypes.EMBED_TAG,
            TwigElementTypes.PRINT_BLOCK,
        )
        private val SLOTS_HOSTS = TokenSet.create(TwigElementTypes.EMBED_STATEMENT)
    }
}
