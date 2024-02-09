@file:Suppress("unused")

package com.github.nvelychenko.drupalextend.type

import com.github.nvelychenko.drupalextend.extensions.getKey
import com.github.nvelychenko.drupalextend.extensions.keyForProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private const val SIGNATURE_SPLIT_KEY = '\u3336'
private const val SIGNATURE_HASH = '\u3337'

private fun compressString(str: String): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    GZIPOutputStream(byteArrayOutputStream).bufferedWriter(Charsets.UTF_8).use { it.write(str) }
    return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
}

private fun decompressString(compressedStr: String): String {
    val bytes = Base64.getDecoder().decode(compressedStr)
    return GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(Charsets.UTF_8).use { it.readText() }
}

fun compressSignature(string: String): String {
    return string.replace('|', SIGNATURE_SPLIT_KEY).replace('#', SIGNATURE_HASH)
}

fun decompressSignature(string: String): String {
    return string.replace(SIGNATURE_SPLIT_KEY, '|').replace(SIGNATURE_HASH, '#')
}

fun getClassesFromSignature(signatures: String, project: Project): List<PhpClass> {
    val potentialClasses = mutableListOf<PhpNamedElement>()
    val foundClasses = mutableSetOf<String>()
    val index = PhpIndex.getInstance(project)

    // Mama-mia
    for (signature in signatures.split('|')) {
        if (signature.contains("#M#C")) {
            val className = signature.substringAfter("#M#C").substringBefore(".")
            index.getAnyByFQN(className)
                .firstOrNull { !foundClasses.contains(it.fqn) }
                ?.let {
                    foundClasses.add(it.fqn)
                    potentialClasses.add(it)
                }
        } else if (signature.startsWith('#')) {
            index.getBySignature(signature)
                .forEach {
                    if (!foundClasses.contains(it.fqn)) {
                        foundClasses.add(it.fqn)
                        potentialClasses.add(it)
                    }
                }
        } else {
            if (!foundClasses.contains(signature)) {
                potentialClasses.addAll(index.getAnyByFQN(signature))
                foundClasses.add(signature)
            }
        }
    }

    return potentialClasses
        .filterIsInstance<PhpClass>()
        .toList()
}

@Synchronized
fun putTypeInCache(
    project: Project,
    expression: String,
    type: PhpType
): String {
    if (keyForProvider.count() > 300) {
        keyForProvider.clear()
    }

    return CachedValuesManager.getManager(project).getCachedValue(
        project,
        getKey(expression.hashCode().toString()),
        {
            CachedValueProvider.Result.create(
                type.filterPrimitives().toString(),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        },
        false
    )
}

@Synchronized
fun returnCachedType(
    project: Project,
    expression: String,
    additionalType: String? = null
): PhpType {
    val type = PhpType();
    keyForProvider[expression.hashCode().toString()]
        ?.let { project.getUserData(it) }
        ?.let { cachedValue ->
            (cachedValue.value as String).split("|").forEach(type::add)
            additionalType?.let { type.add(it) }
            return type
        }

    return type.add(expression)
}