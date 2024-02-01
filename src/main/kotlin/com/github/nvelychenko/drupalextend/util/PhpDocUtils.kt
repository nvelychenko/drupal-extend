package com.github.nvelychenko.drupalextend.util

fun getPhpDocParameter(phpDocText: String, id: String): String? {
    val entityTypeMatch = Regex("${id}?\\s*=\\s*\"([^\"]+)\"").find(phpDocText)

    return entityTypeMatch?.groups?.get(1)?.value
}
