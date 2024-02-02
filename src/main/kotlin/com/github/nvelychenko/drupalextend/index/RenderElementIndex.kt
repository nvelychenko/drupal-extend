package com.github.nvelychenko.drupalextend.index

import com.github.nvelychenko.drupalextend.extensions.findVariablesByName
import com.github.nvelychenko.drupalextend.extensions.isValidForIndex
import com.github.nvelychenko.drupalextend.index.types.RenderElementType
import com.github.nvelychenko.drupalextend.index.types.RenderElementType.RenderElementTypeProperty
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.DataInput
import java.io.DataOutput

class RenderElementIndex : FileBasedIndexExtension<String, RenderElementType>() {
    private val myKeyDescriptor: KeyDescriptor<String> = EnumeratorStringDescriptor()

    private val myDataExternalizer: DataExternalizer<RenderElementType> =
        object : DataExternalizer<RenderElementType> {
            override fun save(out: DataOutput, value: RenderElementType) {
                out.writeUTF(Json.encodeToString(value))
            }

            override fun read(input: DataInput): RenderElementType {
                return Json.decodeFromString<RenderElementType>(input.readUTF())
            }
        }

    override fun getName(): ID<String, RenderElementType> {
        return KEY
    }

    override fun getIndexer(): DataIndexer<String, RenderElementType, FileContent> {
        return DataIndexer { inputData ->
            val map = hashMapOf<String, RenderElementType>()
            val psiFile = inputData.psiFile

            if (!inputData.isValidForIndex()) {
                return@DataIndexer map
            }

            val phpClass = PsiTreeUtil.findChildOfType(psiFile, PhpClass::class.java) ?: return@DataIndexer map

            val specialClasses = arrayOf(
                "\\Drupal\\Core\\Render\\Element\\FormElement",
                "\\Drupal\\Core\\Render\\Element\\RenderElement",
            )

            val docComment = phpClass.docComment ?: return@DataIndexer map
            val docCommentText = docComment.text ?: return@DataIndexer map

            if (docCommentText.isEmpty()) return@DataIndexer map

            if (specialClasses.contains(phpClass.fqn) && docCommentText.contains(" * - #")) {
                var properties = getAdditionalParametersFromDoc(docCommentText)

                val isFormElement = phpClass.fqn == "\\Drupal\\Core\\Render\\Element\\FormElement"
                if (isFormElement) {
                    properties += arrayOf(RenderElementTypeProperty("name", type = "string"))
                }

                val renderElementType = if (isFormElement) {
                    "FormElement"
                } else {
                    "RenderElement"
                }
                map[phpClass.fqn] = RenderElementType(phpClass.fqn, phpClass.fqn, properties, renderElementType)
            } else {
                processRenderElement(phpClass, map, docCommentText)
            }

            map
        }
    }

    private fun processRenderElement(
        phpClass: PhpClass,
        map: HashMap<String, RenderElementType>,
        docCommentString: String
    ) {
        val docComment = phpClass.docComment!!
        var renderElement = docComment.getTagElementsByName("@RenderElement").firstOrNull()
        var type = "RenderElement"

        if (renderElement == null) {
           renderElement = docComment.getTagElementsByName("@FormElement").firstOrNull() ?: return
            type = "FormElement"
        }

        val getInfoMethod = phpClass.findOwnMethodByName("getInfo") ?: return

        var parameters = getInfoMethod
            .let { PsiTreeUtil.findChildOfType(it, PhpReturn::class.java)?.firstPsiChild }
            .takeIf { it is Variable || it is ArrayCreationExpression || (it as? BinaryExpression)?.operationType == PhpTokenTypes.opPLUS }
            ?.let {
                when (it) {
                    is Variable -> processVariables(getInfoMethod, it)
                    is ArrayCreationExpression -> processArrayCreationExpression(it)
                    is BinaryExpression -> {
                        var value = mutableListOf<RenderElementTypeProperty>()
                        if (it.leftOperand is ArrayCreationExpression) {
                            value = processArrayCreationExpression(it.leftOperand as ArrayCreationExpression).toMutableList()
                        }

                        if (it.rightOperand is ArrayCreationExpression) {
                            value.addAll(processArrayCreationExpression(it.rightOperand as ArrayCreationExpression).toMutableList())
                        }
                        value.toTypedArray()
                    }
                    else -> null
                }
            } ?: return

        if (docCommentString.contains(" * - #")) {
            parameters = getAdditionalParametersFromDoc(docCommentString, 10.0) + parameters
        }

        val renderElementIdId = renderElement.text.substringAfter('"').substringBefore('"')

        map[renderElementIdId] = RenderElementType(renderElementIdId, phpClass.fqn, parameters, type)
    }

    private fun processArrayCreationExpression(expression: ArrayCreationExpression): Array<RenderElementTypeProperty> {
        val parameters = ArrayList<RenderElementTypeProperty>(5)

        expression.hashElements.forEach { hashElement ->
            val key = hashElement.key
            if (key is StringLiteralExpression) {
                parameters.add(RenderElementTypeProperty(key.contents, 10.0,null, null))
            }

        }

        return parameters.toTypedArray()
    }

    private fun processVariables(method: Method, returnType: Variable): Array<RenderElementTypeProperty> {
        val parameters = ArrayList<RenderElementTypeProperty>(5)
        method.findVariablesByName(returnType.name).forEach { variable ->
            val assignment = PhpPsiUtil.getParentOfClass(variable, AssignmentExpression::class.java) ?: return@forEach

            val index = (assignment.variable as? ArrayAccessExpression)?.index ?: return@forEach
            val indexValue = index.value

            if (indexValue is StringLiteralExpression) {
                parameters.add(RenderElementTypeProperty(indexValue.contents, 10.0,null, null))
            }
        }

        return parameters.toTypedArray()
    }

    private fun getAdditionalParametersFromDoc(docCommentText: String, defaultPriority: Double = 0.0): Array<RenderElementTypeProperty> {
        val doc = docCommentText.replace(docCommentText.substringBefore(" * - #"), "")


        val parameters = ArrayList<RenderElementTypeProperty>(8)
        // @todo Write wtf is going on here
        doc.split(" * - #").forEach { rowDocument ->
            if (rowDocument.trim().isEmpty()) return@forEach
            // @todo Implement documentation.
//            val document = rowDocument.replace("*", "").replace("\\s{2,}".toRegex(), " ")
            val id = rowDocument.substringBefore(":")
            val hasType = rowDocument.startsWith("${id}: (")

            val type = rowDocument.takeIf { hasType }?.substringAfter("(")?.substringBefore(")")
    //
    //            val documentation = if (hasDocument) {
    //                document.substringAfter("${type})")
    //            } else {
    //                document.substringAfter("${id}: ").trim()
    //            }

            parameters.add(RenderElementTypeProperty(id, defaultPriority ,type, null))
        }
        return parameters.toTypedArray()
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = myKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<RenderElementType> = myDataExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file -> file.fileType == PhpFileType.INSTANCE }
    }

    override fun dependsOnFileContent(): Boolean = true

    override fun getVersion(): Int = 2

    companion object {
        val KEY = ID.create<String, RenderElementType>("com.github.nvelychenko.drupalextend.index.render_element")
    }

}