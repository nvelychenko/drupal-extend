package com.github.nvelychenko.drupalextend.reference.referenceType

import com.github.nvelychenko.drupalextend.index.PermissionsIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.util.yml.YAMLKeyValueFinder
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.xml.model.gotosymbol.GoToSymbolProvider
import com.jetbrains.php.lang.PhpFileType
import icons.DrupalIcons
import org.jetbrains.yaml.YAMLFileType

class PermissionsReference(element: PsiElement, private val permissionName: String) : PsiPolyVariantReferenceBase<PsiElement>(element) {

    private val psiManager: PsiManager by lazy { PsiManager.getInstance(project) }
    private val project = element.project
    private val scope = GlobalSearchScope.getScopeRestrictedByFileTypes(
        GlobalSearchScope.allScope(project),
        XmlFileType.INSTANCE,
        YAMLFileType.YML,
        PhpFileType.INSTANCE
    )

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (!project.drupalExtendSettings.isEnabled) return emptyArray()
        val files = HashMap<String, VirtualFile>()
        val processor = FileBasedIndex.ValueProcessor<String> { file, value -> files[value] = file; true }

        FileBasedIndex
            .getInstance()
            .processValues(PermissionsIndex.KEY, permissionName, null, processor, scope)

        val navigationItems = mutableListOf<PsiElement>()
        files.forEach {
            val path = it.key
            val virtualFile = it.value
            val yamlFile = psiManager.findFile(virtualFile)
            val psiElement = YAMLKeyValueFinder(path).findIn(yamlFile as PsiElement)?.value as PsiElement
            val navigationItem = GoToSymbolProvider.BaseNavigationItem(psiElement, path, DrupalIcons.ImplementedHook)
            navigationItems.add(navigationItem)

        }
        return PsiElementResolveResult.createResults(navigationItems)
    }

}
