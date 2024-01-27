package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.index.types.DrupalTheme
import com.github.nvelychenko.drupalextend.util.isValidForIndex
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function
import java.io.DataInput
import java.io.DataOutput

class ThemeIndex : FileBasedIndexExtension<String, DrupalTheme>() {
    private val myKeyDescriptor: KeyDescriptor<String> = EnumeratorStringDescriptor()

    private val myDataExternalizer: DataExternalizer<DrupalTheme> =
        object : DataExternalizer<DrupalTheme> {
            override fun save(out: DataOutput, value: DrupalTheme) {
                out.writeUTF(value.themeName)

                out.writeInt(value.variables.size)
                for (key in value.variables) {
                    out.writeUTF(key)
                }
            }

            override fun read(input: DataInput): DrupalTheme {
                val theme = input.readUTF()

                val keys = mutableListOf<String>()
                for (i in 1..input.readInt()) {
                    keys.add(input.readUTF())
                }

                return DrupalTheme(theme, keys.toTypedArray())
            }
        }

    override fun getName(): ID<String, DrupalTheme> {
        return KEY
    }

    override fun getIndexer(): DataIndexer<String, DrupalTheme, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, DrupalTheme>()
            val psiFile = inputData.psiFile

            if (!isValidForIndex(inputData)) {
                return@DataIndexer map
            }

            val fileName = inputData.fileName
            val moduleName = fileName.substringBefore(".")
            if (!fileName.endsWith(".module")) {
                return@DataIndexer map
            }

            inputData.psiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element is Function && element.name == moduleName + "_theme") {
                        processPhp(map, psiFile as PhpFile, element)
                    }

                    super.visitElement(element)
                }
            })

            return@DataIndexer map
        }
    }

    private fun processPhp(map: HashMap<String, DrupalTheme>, phpFile: PhpFile, function: Function): HashMap<String, DrupalTheme> {

        val returnType = PsiTreeUtil.findChildOfType(function, PhpReturn::class.java)?.firstPsiChild
        if (returnType !is Variable && returnType !is ArrayCreationExpression) {
            return map
        }

        // @todo Implement ability to process ArrayCreationExpression
        if (returnType is ArrayCreationExpression) {
            for (templateName in returnType.children) {
                if (templateName is ArrayHashElement && templateName.key is StringLiteralExpression) {
                    val childValue = PsiTreeUtil.findChildOfType(templateName, ArrayCreationExpression::class.java) ?: continue

                    for (templateVariables in childValue.children) {
                        val key = templateName.key as StringLiteralExpression
                        var variables = mutableListOf<String>();

                        if (templateVariables is ArrayHashElement
                            && templateVariables.key is StringLiteralExpression
                            && (templateVariables.key as StringLiteralExpression).contents == "variables"
                            ) {
                            val variablesValue = PsiTreeUtil.findChildOfType(templateVariables, ArrayCreationExpression::class.java) ?: continue

                            variablesValue.children.forEach {
                                it as ArrayHashElement
                                val variablesKey = it.key
                                if (variablesKey is StringLiteralExpression) {
                                    variables.add(variablesKey.contents)
                                }
                            }

                        }

                        map[key.contents] = DrupalTheme(key.contents, variables.toTypedArray())

                    }

                }
            }
        }

        return map
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = myKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<DrupalTheme> = myDataExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file -> file.fileType == PhpFileType.INSTANCE }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 1

    companion object {
        val KEY = ID.create<String, DrupalTheme>("com.github.nvelychenko.drupalextend.index.hook_themes")
    }

    private data class FieldRepresentation(val key: String, val value: String, val path: String)

}