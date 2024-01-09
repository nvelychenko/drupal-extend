package com.github.nvelychenko.drupalextend.util.yml

import com.github.nvelychenko.drupalextend.util.parents
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue

val YAMLKeyValue.keyPath: String
    get() = this.parents.takeWhile { it !is YAMLDocument }
        .filterIsInstance<YAMLKeyValue>()
        .let { listOf(this, *it.toTypedArray()) }
        .map { it.keyText }
        .reversed()
        .joinToString(".")
