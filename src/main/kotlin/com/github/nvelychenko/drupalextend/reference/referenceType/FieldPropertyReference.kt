package com.github.nvelychenko.drupalextend.reference.referenceType

import com.github.nvelychenko.drupalextend.index.FieldsIndex
import com.github.nvelychenko.drupalextend.index.types.DrupalField
import com.github.nvelychenko.drupalextend.util.yml.YAMLKeyValueFinder
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult.createResults
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.PhpClass
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLFile

class FieldPropertyReference(element: PsiElement, val entityTypeId: String, val fieldName: String) :
    PsiPolyVariantReferenceBase<PsiElement>(element) {

    private val psiManager: PsiManager by lazy { PsiManager.getInstance(project) }
    private val project = element.project
    private val scope = GlobalSearchScope.getScopeRestrictedByFileTypes(
        GlobalSearchScope.allScope(project),
        XmlFileType.INSTANCE,
        YAMLFileType.YML,
        PhpFileType.INSTANCE
    )


    override fun multiResolve(incompleteCode: Boolean) = HashMap<VirtualFile, String>().apply {
        val processor = FileBasedIndex.ValueProcessor<DrupalField> { file, value -> put(file, value.path); true }
        FileBasedIndex.getInstance()
            .processValues(FieldsIndex.KEY, "${entityTypeId}|${fieldName}", null, processor, scope)
    }
        .mapNotNull { fileToElement(it) }
        .let(::createResults)

    private fun fileToElement(file: Map.Entry<VirtualFile, String>) = psiManager.findFile(file.key)?.let { fileToElement(it, file.value) }

    private fun fileToElement(file: PsiElement, path: String) = when (file) {
        is YAMLFile -> YAMLKeyValueFinder(path).findIn(file)?.value
        // @todo Implement ability to go to the PSI element.
        is PhpFile -> PsiTreeUtil.findChildOfType(file, PhpClass::class.java)?.findOwnMethodByName(path)
        else -> null
    }
}
