package com.github.nvelychenko.drupalextend.completion.providers

import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.nextLeaf
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.text.NameUtilCore
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.codeInsight.PhpCodeInsightUtil
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil
import com.jetbrains.php.drupal.DrupalVersion
import com.jetbrains.php.drupal.hooks.DrupalHooksIndex
import com.jetbrains.php.drupal.settings.DrupalDataService
import com.jetbrains.php.lang.psi.PhpCodeEditUtil
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.impl.MethodImpl
import com.jetbrains.php.lang.psi.elements.impl.ParameterListImpl
import com.jetbrains.php.lang.psi.elements.impl.PhpClassImpl
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.stubs.indexes.PhpFunctionNameIndex
import com.jetbrains.php.refactoring.PhpAliasImporter

class HookAttributeCompletionProvider : CompletionProvider<CompletionParameters>() {
    public override fun addCompletions(
        completionParameters: CompletionParameters,
        processingContext: ProcessingContext,
        completionResultSet: CompletionResultSet
    ) {
        val leaf = completionParameters.originalPosition ?: return
        val project = leaf.project

        if (!project.drupalExtendSettings.isEnabled) return
        val service = DrupalDataService.getInstance(project)
        if (!service.isEnabled) return
        if (service.version == null) return

        val element = leaf.parent as? StringLiteralExpression ?: return
        val parameterList = PsiTreeUtil.getParentOfType(leaf, ParameterList::class.java) ?: return
        if (!parameterList.parameters.first().isEquivalentTo(element)) return
        val attribute = PsiTreeUtil.getParentOfType(leaf, PhpAttribute::class.java) ?: return
        if (attribute.fqn != "\\Drupal\\Core\\Hook\\Attribute\\Hook") return
        val prefixMatcher: PrefixMatcher = completionResultSet.prefixMatcher

        val functionNamesFromIndex = getAllHooksInvocationsFromIndex(
            prefixMatcher, service.version, project
        )
        val functionNamesFromDocs = getAllHooksInvocationsFromDocs(
            prefixMatcher,
            project,
        )
        val hookNames: HashSet<String> = HashSet()
        hookNames.addAll(functionNamesFromIndex)
        hookNames.addAll(functionNamesFromDocs)

        for (name in hookNames) {
            completionResultSet.addElement(HookImplementationLookupElement(name))
        }
    }

    private fun getAllHooksInvocationsFromIndex(
        prefixMatcher: PrefixMatcher, version: DrupalVersion, project: Project
    ): Collection<String> {
        val index = FileBasedIndex.getInstance()
        val indexKeys = index.getAllKeys(DrupalHooksIndex.KEY, project)
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
        prefixMatcher: PrefixMatcher, project: Project
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

    class HookImplementationLookupElement(private val hookName: String) : LookupElement() {

        override fun getLookupString(): String {
            return hookName
        }

        override fun handleInsert(context: InsertionContext) {
            val element = context.file.findElementAt(context.startOffset) ?: return
            val currentAttribute = PsiTreeUtil.getParentOfType(element, PhpAttribute::class.java) ?: return
            val attributeListImpl = currentAttribute.parent
            // Do not insert method if hook attribute is on class.
            val attributeParent = attributeListImpl.parent
            if (attributeParent is PhpClassImpl) return
            val method = attributeParent as MethodImpl

            val secondLeaf = currentAttribute.nextLeaf()?.nextLeaf()
            // Some piece of actual shit.
            val onlyWhiteSpace = secondLeaf is PsiWhiteSpace && Regex("\\R").findAll(secondLeaf.text).count() == 1
            if (
                (method.attributes.size > 1 && onlyWhiteSpace && secondLeaf?.nextSibling is PhpAttributesList)
                || (onlyWhiteSpace && secondLeaf?.nextSibling?.parent is Method)
            ) {
                return
            }

            addMethodImpl(context, currentAttribute)
        }

        private fun addMethodImpl(context: InsertionContext, attribute: PhpAttribute) {
            val editor = context.editor
            val file: PsiFile = context.file
            val project = editor.project ?: return
            val targetClass: PhpClass = PhpCodeEditUtil.findClassAtCaret(editor, file) ?: return
            val element = context.file.findElementAt(context.startOffset) ?: return
            val doc: Document = editor.document
            val attributeList = PsiTreeUtil.getParentOfType(element, PhpAttributesList::class.java) ?: return

            val docManager = PsiDocumentManager.getInstance(project)

            // Get a parameter list from the hook example in api.php file if present.
            val parameterList = getDescriptiveFunctionByHookName(
                hookName,
                context.project
            )?.let { PhpPsiUtil.getChildByCondition<PsiElement>(it, ParameterList.INSTANCEOF) as ParameterList? }

            val classesToImport = mutableListOf<String>()
            if (parameterList is ParameterListImpl) {
                for (parameter in parameterList.parameters) {
                    val classReference = PsiTreeUtil.findChildOfType(parameter, ClassReference::class.java)
                    val fqn = classReference?.fqn

                    if (fqn == null || fqn == "" || PhpType.isPrimitiveType(fqn)) {
                        continue
                    }

                    if (fqn.contains("\\")) {
                        classesToImport.add(fqn)
                    }

                    val classes = PhpIndex.getInstance(project).getClassesByName(fqn)
                    if (classes.size == 1) {
                        classesToImport.add(classes.first().fqn)
                    }
                }
            }

            val parameterListText = parameterList?.text ?: ""

            val functionName = convertToCamelCaseString(hookName)
            val offset = attributeList.nextSibling.textRange.startOffset
            val template = "\npublic function $functionName($parameterListText) {\n}"

            editor.caretModel.moveToOffset(offset, true)
            docManager.commitDocument(doc)
            val rangeMarker: RangeMarker = editor.document.createRangeMarker(offset + 1, offset + 1)
            rangeMarker.isGreedyToRight = false

            PhpInsertHandlerUtil.insertStringAtCaret(editor, template)
            CodeStyleManager.getInstance(project).reformatText(file, offset, rangeMarker.endOffset)

            val scope = PhpCodeInsightUtil.findScopeForUseOperator(targetClass) ?: return

            classesToImport.forEach {
                PhpAliasImporter.insertUseStatement(it, scope)
            }
        }

        private fun getDescriptiveFunctionByHookName(hookName: String, project: Project): Function? {
            val dataService = DrupalDataService.getInstance(project)
            if (dataService.isEnabled && dataService.isVersionValid) {
                val functionName = "hook_$hookName"

                for (suitableFunction in PhpIndex.getInstance(project).getFunctionsByName(functionName)) {
                    val containingFile = suitableFunction.containingFile
                    if (containingFile != null) {
                        val fileName = containingFile.name
                        if (fileName.endsWith(".api.php")) {
                            return suitableFunction
                        }
                    }
                }

                return null
            } else {
                return null
            }
        }

        private fun convertToCamelCaseString(text: String): String {
            val strings: Array<String> = NameUtilCore.nameToWords(text)
            if (strings.isNotEmpty()) {
                val buf = StringBuilder()
                buf.append(StringUtil.toLowerCase(strings[0]))

                for (i in 1..<strings.size) {
                    val string = strings[i]
                    if (Character.isLetterOrDigit(string[0])) {
                        buf.append(StringUtil.capitalize(StringUtil.toLowerCase(string)))
                    }
                }

                return buf.toString()
            } else {
                return ""
            }
        }

    }
}