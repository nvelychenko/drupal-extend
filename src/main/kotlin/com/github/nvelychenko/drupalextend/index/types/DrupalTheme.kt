package com.github.nvelychenko.drupalextend.index.types

import kotlinx.serialization.Serializable
import org.apache.commons.lang3.builder.HashCodeBuilder

@Serializable
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
        return HashCodeBuilder()
            .append(this.themeName)
            .append(this.variables)
            .hashCode()

    }

}