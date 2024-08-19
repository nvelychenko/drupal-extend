package com.github.nvelychenko.drupalextend.type

import com.intellij.openapi.project.Project
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpNamedElement

private const val SIGNATURE_SPLIT_KEY = '\u3336'
private const val SIGNATURE_HASH = '\u3337'

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

inline fun <R, U : R, T : R> T.letIf(condition: Boolean, block: (T) -> U): R = if (condition) block(this) else this
