package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.index.dataExternalizer.SerializedObjectDataExternalizer
import com.github.nvelychenko.drupalextend.extensions.findVariablesByName
import com.github.nvelychenko.drupalextend.extensions.isValidForIndex
import com.github.nvelychenko.drupalextend.index.types.DrupalTheme
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function
import kotlinx.serialization.serializer

class ThemeIndex : FileBasedIndexExtension<String, DrupalTheme>() {

    override fun getIndexer(): DataIndexer<String, DrupalTheme, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, DrupalTheme>()

            if (!inputData.isValidForIndex()) {
                return@DataIndexer map
            }

            val fileName = inputData.fileName
            val moduleName = fileName.substringBefore(".")
            if (!fileName.endsWith(".module") && fileName != "theme.inc") {
                return@DataIndexer map
            }

            val themeHook = getThemeHook(inputData, moduleName) ?: return@DataIndexer map
            when (val returnType = PsiTreeUtil.findChildOfType(themeHook, PhpReturn::class.java)?.firstPsiChild) {
                is ArrayCreationExpression -> processArrayCreationExpression(returnType, map)
                is Variable -> processVariable(returnType, themeHook, map)
                is FunctionReference -> {
                    if (returnType.name == "array_merge") {
                        returnType.parameters.forEach {
                            if (it is ArrayCreationExpression) {
                                processArrayCreationExpression(it, map)
                            }
                        }
                    }
                }
            }

            return@DataIndexer map
        }
    }

    private fun processVariable(returnVariable: Variable, themeHook: Function, map: HashMap<String, DrupalTheme>) {
        for (variable in themeHook.findVariablesByName(returnVariable.name)) {
            val assignment = PhpPsiUtil.getParentOfClass(variable, AssignmentExpression::class.java) ?: continue

            val propertiesHash = assignment.variable as? ArrayCreationExpression ?: continue

            // $theme['foo']
            val themeName = ((assignment.value as? ArrayAccessExpression)?.index as? StringLiteralExpression)?.contents ?: continue

            map[themeName] = DrupalTheme(themeName, getThemeVariables(propertiesHash))
        }
    }

    private fun processArrayCreationExpression(returnType: ArrayCreationExpression, map: HashMap<String, DrupalTheme>) {
        top@ for (topHash in returnType.hashElements) {
            val propertiesHash = topHash.value
            val themeNamePsi = topHash.key
            if (themeNamePsi !is StringLiteralExpression || themeNamePsi.contents.isEmpty() || propertiesHash !is ArrayCreationExpression) continue

            map[themeNamePsi.contents] = DrupalTheme(themeNamePsi.contents, getThemeVariables(propertiesHash))

        }
    }

    private fun getThemeVariables(propertiesHash: ArrayCreationExpression): Array<String> {
        val variables = mutableListOf<String>()
        for (it in propertiesHash.hashElements) {
            val variablesHash = it.value
            if ((it.key as? StringLiteralExpression)?.contents != "variables" || variablesHash !is ArrayCreationExpression) continue

            variablesHash.hashElements.forEach {
                val themeName = it.key
                if (themeName is StringLiteralExpression && themeName.contents.isNotEmpty()) {
                    variables.add(themeName.contents)
                }
            }

            return variables.toTypedArray()
        }

        return emptyArray()
    }

    private fun getThemeHook(inputData: FileContent, moduleName: String): Function? {
        var themeHook: Function? = null
        val allowedHookNames = arrayOf(moduleName + "_theme", "drupal_common_theme")
        inputData.psiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is Function && (allowedHookNames.contains(element.name))) {
                    themeHook = element
                }

                super.visitElement(element)
            }
        })

        return themeHook
    }

    private val myKeyDescriptor: KeyDescriptor<String> = EnumeratorStringDescriptor()

    private val myDataExternalizer: DataExternalizer<DrupalTheme> =
        SerializedObjectDataExternalizer(serializer<DrupalTheme>())

    override fun getName(): ID<String, DrupalTheme> {
        return KEY
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = myKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<DrupalTheme> = myDataExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file -> file.fileType == PhpFileType.INSTANCE }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 0

    companion object {
        val KEY = ID.create<String, DrupalTheme>("com.github.nvelychenko.drupalextend.index.hook_themes")
    }

}