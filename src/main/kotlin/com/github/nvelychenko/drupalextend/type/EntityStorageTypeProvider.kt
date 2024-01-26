package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.extensions.isSuperInterfaceOf
import com.github.nvelychenko.drupalextend.index.ContentEntityIndex
import com.github.nvelychenko.drupalextend.type.EntityStorageTypeProvider.Util.SPLITER_KEY
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.parser.PhpElementTypes
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


class EntityStorageTypeProvider : PhpTypeProvider4 {

    private val entityTypeManagerInterface = "\\Drupal\\Core\\Entity\\EntityTypeManagerInterface"

    override fun getKey(): Char {
        return Util.KEY
    }

    override fun getType(psiElement: PsiElement): PhpType? {
        if (DumbService.getInstance(psiElement.project).isDumb) {
            return null
        }

        // container calls are only on "get" methods
        if (psiElement !is MethodReference) {
            return null
        }

        if (psiElement.name != "getStorage") {
            return null
        }

        if (!PlatformPatterns
                .psiElement(PhpElementTypes.METHOD_REFERENCE)
                .withChild(
                    PlatformPatterns
                        .psiElement(PhpElementTypes.PARAMETER_LIST)
                        .withFirstChild(
                            PlatformPatterns.psiElement(StringLiteralExpression::class.java),
                        )
                ).accepts(psiElement)
        ) {
            return null
        }

        val parameterList = PhpPsiUtil.getChildOfClass(psiElement, ParameterList::class.java)
        if (parameterList !is ParameterList) return null

        val firstParameter = parameterList.getParameter(0) as StringLiteralExpression
        if (firstParameter.contents.isEmpty()) return null

        val signature = compressString(psiElement.signature).replace('|', SPLITER_KEY)
        return PhpType().add("#$key${signature}${Util.SPLIT_KEY}${firstParameter.contents}")
    }

    private fun compressString(str: String): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        GZIPOutputStream(byteArrayOutputStream).bufferedWriter(Charsets.UTF_8).use { it.write(str) }
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
    }

    private fun decompressString(compressedStr: String): String {
        val bytes = Base64.getDecoder().decode(compressedStr)
        return GZIPInputStream(ByteArrayInputStream(bytes))
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    override fun complete(expression: String?, project: Project?): PhpType? {
        return null
    }

    override fun getBySignature(
        expression: String,
        visited: Set<String?>?,
        depth: Int,
        project: Project?
    ): Collection<PhpNamedElement> {
        // @todo Figure out why id doesn't work, here should be this.getBySignature method that should work
        //   but it brakes ContentEntityFieldTypeProvider type provider.
        return emptyList()
    }

    @Suppress("unused")
    private fun getBySignature(
        project: Project?,
        expression: String,
        visited: Set<String?>?,
        depth: Int
    ): Collection<PhpNamedElement> {
        if (project == null || !expression.contains(Util.SPLIT_KEY)) return emptyList()

        val parts = expression.split(Util.SPLIT_KEY)

        if (parts.size != 2) {
            return emptyList()
        }

        val (originalSignature, entityTypeId) = parts

        val entityType =
            FileBasedIndex.getInstance().getValue(ContentEntityIndex.KEY, entityTypeId, project) ?: return emptyList()

        val phpIndex = PhpIndex.getInstance(project)
        val namedCollection = mutableListOf<PhpNamedElement>()
        for (partialSignature in decompressString(originalSignature.replace(SPLITER_KEY, '|')).split('|')) {
            namedCollection.addAll(phpIndex.getBySignature(partialSignature, visited, depth))
        }

        val methods = namedCollection.filterIsInstance<Method>()

        if (methods.isEmpty()) return emptyList()

        val baseStorageClass = phpIndex.getClassesByFQN("\\Drupal\\Core\\Entity\\EntityStorageBase")

        methods.forEach {
            if (it.fqn == entityTypeManagerInterface) {
                return phpIndex.getClassesByFQN(entityType.storageHandler) + baseStorageClass
            }
        }

        val entityTypeManagerInterface =
            phpIndex.getInterfacesByFQN(entityTypeManagerInterface).takeIf { it.isNotEmpty() }?.first()
                ?: return emptyList()

        methods.forEach {
            if (it.containingClass?.isSuperInterfaceOf(entityTypeManagerInterface) == true) {
                return phpIndex.getClassesByFQN(entityType.storageHandler) + baseStorageClass
            }
        }

        return emptyList()
    }

    object Util {
        const val SPLIT_KEY = '\u3333'
        const val KEY = '\u3334'
        const val SPLITER_KEY = '\u3336'
    }


}