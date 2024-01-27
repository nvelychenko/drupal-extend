package com.github.nvelychenko.drupalextend.index.types

import java.util.*

data class DrupalTheme(val themeName: String, val variables: Array<String>) {
    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other != null && this.javaClass == other.javaClass) {
            val data = other as DrupalTheme
            this.themeName == data.themeName && this.variables.contentEquals(data.variables)
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            *arrayOf<Any>(
                this.themeName,
                this.variables,
            )
        )
    }

}