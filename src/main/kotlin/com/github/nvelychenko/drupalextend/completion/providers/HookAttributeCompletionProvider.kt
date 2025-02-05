package com.github.nvelychenko.drupalextend.completion.providers

import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.configurationStore.askToRestart
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.*
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.text.NameUtilCore
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.drupal.DrupalVersion
import com.jetbrains.php.drupal.hooks.DrupalHooksIndex
import com.jetbrains.php.drupal.settings.DrupalDataService
import com.jetbrains.php.lang.psi.PhpPsiElementFactory
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.impl.MethodImpl
import com.jetbrains.php.lang.psi.elements.impl.PhpAttributesListImpl
import com.jetbrains.php.lang.psi.elements.impl.PhpClassImpl
import com.jetbrains.php.lang.psi.stubs.indexes.PhpFunctionNameIndex
import com.jetbrains.php.refactoring.PhpFunctionCodeGenerator
import com.jetbrains.php.refactoring.PhpFunctionTemplate

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
        if (!service.isEnabled) return;
        if (service.version == null) return;

        val element = leaf.parent as? StringLiteralExpression ?: return
        val parameterList = PsiTreeUtil.getParentOfType(leaf, ParameterList::class.java) ?: return;
        if (!parameterList.parameters.first().isEquivalentTo(element)) return;
        val attribute = PsiTreeUtil.getParentOfType(leaf, PhpAttribute::class.java) ?: return;
        if (attribute.fqn != "\\Drupal\\Core\\Hook\\Attribute\\Hook") return;
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
            return hookName;
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
            if (method.attributes.size > 1 && secondLeaf is PsiWhiteSpace
                && Regex("\\R").findAll(secondLeaf.text).count() == 1
                && secondLeaf.nextSibling is PhpAttributesList
            ) {
                return;
            }
            addMethodImpl(context)
        }

        private fun addMethodImpl(context: InsertionContext) {
            val editor = context.editor
            val project = editor.project ?: return;
            val element = context.file.findElementAt(context.startOffset) ?: return
            val doc: Document = editor.document
            val docManager = PsiDocumentManager.getInstance(project)
            val attributeList = PsiTreeUtil.getParentOfType(element, PhpAttributesListImpl::class.java) ?: return
            val simpleContext = SimpleDataContext.getProjectContext(project)
            val currentCaret = editor.caretModel.currentCaret

            // Get a parameter list from the hook example in api.php file if present.
            val docFunction = getDescriptiveFunctionByHookName(hookName, context.project)
            var parameterListText = ""
            if (docFunction != null) {
                val parameterList =
                    PhpPsiUtil.getChildByCondition<PsiElement>(docFunction, ParameterList.INSTANCEOF) as ParameterList?
                if (parameterList != null) {
                    parameterListText = parameterList.text
                }
            }

            val functionName = convertToCamelCaseString(hookName)

            attributeList.parent.addAfter(
                PhpPsiElementFactory.createMethod(project, "public function $functionName($parameterListText) {}"),
                attributeList
            )
            if (docManager.isDocumentBlockedByPsi(doc)) {
                docManager.doPostponedOperationsAndUnblockDocument(doc)
            }
            docManager.commitDocument(doc)

            editor.caretModel.moveToOffset(attributeList.textRange.endOffset, true)
            EditorActionManager.getInstance().getActionHandler("EditorEnter").execute(
                editor,
                currentCaret,
                simpleContext
            )
            docManager.commitDocument(doc)
            val position = element.parentOfType<MethodImpl>()?.childrenOfType<GroupStatement>()?.first()?.textOffset ?: return
            editor.caretModel.moveToOffset(position + 1)
            EditorActionManager.getInstance().getActionHandler("EditorEnter").execute(editor, currentCaret, simpleContext)
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