package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.extensions.isValidForIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.PhpAttribute
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class HookAttributesIndex : FileBasedIndexExtension<String, String>() {
    override fun getIndexer(): DataIndexer<String, String, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, String>()
            if (!inputData.isValidForIndex()) return@DataIndexer map
            if (!inputData.project.drupalExtendSettings.isEnabled) return@DataIndexer map

            val phpFile = inputData.psiFile as PhpFile
            val clazz = PsiTreeUtil.findChildOfType(phpFile, PhpClass::class.java) ?: return@DataIndexer map

            if (!clazz.namespaceName.startsWith("\\Drupal\\") || !clazz.namespaceName.endsWith("\\Hook\\")) return@DataIndexer map

            val attributes = PsiTreeUtil.findChildrenOfType(clazz, PhpAttribute::class.java)

            for (attribute in attributes) {
                if (attribute.fqn != "\\Drupal\\Core\\Hook\\Attribute\\Hook") continue
                val hookName = attribute.parameters.first() ?: continue

                if (hookName is StringLiteralExpression && hookName.contents.isNotEmpty()) {
                    map[hookName.contents] = clazz.fqn
                }
            }

            map
        }
    }

    private val myKeyDescriptor: KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE
    private val myDataExternalizer = EnumeratorStringDescriptor.INSTANCE

    override fun getName(): ID<String, String> {
        return KEY
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = myKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<String> = myDataExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file: VirtualFile -> file.fileType == PhpFileType.INSTANCE }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 0

    companion object {
        val KEY = ID.create<String, String>("com.github.nvelychenko.drupalextend.index.hook_attributes")

        fun getHookAttributesByName(psiElement: PsiElement, name: String): Array<PhpAttribute> {
            val result = mutableListOf<PhpAttribute>()
            val attributes = PsiTreeUtil.findChildrenOfType(psiElement, PhpAttribute::class.java)

            for (attribute in attributes) {
                if (attribute.fqn != "\\Drupal\\Core\\Hook\\Attribute\\Hook") continue
                val hookName = attribute.parameters.first() ?: continue
                if (hookName is StringLiteralExpression && hookName.contents == name) {
                    result.add(attribute)
                }
            }

            return result.toTypedArray()
        }
    }

}