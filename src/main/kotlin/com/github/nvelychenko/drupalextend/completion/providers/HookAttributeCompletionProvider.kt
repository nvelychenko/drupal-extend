package com.github.nvelychenko.drupalextend.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.drupal.DrupalVersion
import com.jetbrains.php.drupal.hooks.DrupalHooksIndex
import com.jetbrains.php.drupal.settings.DrupalDataService
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.PhpAttribute
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.stubs.indexes.PhpFunctionNameIndex

class HookAttributeCompletionProvider: CompletionProvider<CompletionParameters>() {
    public override fun addCompletions(
        completionParameters: CompletionParameters,
        processingContext: ProcessingContext,
        completionResultSet: CompletionResultSet
    ) {
        val leaf = completionParameters.originalPosition ?: return
        val element = leaf.parent as? StringLiteralExpression ?: return
        val parameterList = PsiTreeUtil.getParentOfType(leaf, ParameterList::class.java) ?: return;
        if (!parameterList.parameters.first().isEquivalentTo(element)) return;
        val attribute = PsiTreeUtil.getParentOfType(leaf, PhpAttribute::class.java) ?: return;
        if (attribute.fqn != "\\Drupal\\Core\\Hook\\Attribute\\Hook") return;
        val project = completionParameters.originalFile.project
        val service = DrupalDataService.getInstance(project)
        if (!service.isEnabled) return;
        if (service.version == null) return;
        val prefixMatcher: PrefixMatcher = completionResultSet.prefixMatcher

        val functionNamesFromIndex = getAllHooksInvocationsFromIndex(
            prefixMatcher,
            service.version,
            project
        )
        val functionNamesFromDocs = getAllHooksInvocationsFromDocs(
            prefixMatcher,
            project,
        )
        val hookNames: HashSet<String> = HashSet()
        hookNames.addAll(functionNamesFromIndex)
        hookNames.addAll(functionNamesFromDocs)

        for (name in hookNames) {
            completionResultSet.addElement(LookupElementBuilder.create(name))
        }
    }

    private fun getAllHooksInvocationsFromIndex(
        prefixMatcher: PrefixMatcher,
        version: DrupalVersion,
        project: Project
    ): Collection<String> {
        val index = FileBasedIndex.getInstance()
        val indexKeys = index.getAllKeys(DrupalHooksIndex.KEY, project);
        val filtered: ArrayList<String> = ArrayList()
        val searchScope = GlobalSearchScope.allScope(project)

        for (result in indexKeys) {
            if (result.suits(version)) {
                val hookImplementationText = result.name
                if (prefixMatcher.prefixMatches(hookImplementationText)) {
                    val fileCollection = index.getContainingFiles(DrupalHooksIndex.KEY, result, searchScope)
                    if (!fileCollection.isEmpty()) {
                        filtered.add(hookImplementationText)
                    }
                }
            }
        }

        return filtered
    }

    private fun getAllHooksInvocationsFromDocs(
        prefixMatcher: PrefixMatcher,
        project: Project
    ): Collection<String> {

        val results = FileBasedIndex.getInstance().getAllKeys(PhpFunctionNameIndex.KEY, project)
        val filtered: ArrayList<String> = ArrayList()

        for (key in results) {
            if (key.startsWith("hook_")) {
                val hookImplementationText = key.substring("hook_".length)
                if (prefixMatcher.prefixMatches(hookImplementationText)) {
                    filtered.add(hookImplementationText)
                }
            }
        }

        return filtered
    }
}