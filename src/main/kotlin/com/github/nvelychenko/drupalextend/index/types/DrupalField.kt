package com.github.nvelychenko.drupalextend.index.types

import java.util.*

data class DrupalField(val entityType: String, val fieldType: String, val fieldName: String) {
    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other != null && this.javaClass == other.javaClass) {
            val data = other as DrupalField
            this.entityType == data.entityType && this.fieldType == data.fieldType && this.fieldName == data.fieldName
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            *arrayOf<Any>(
                this.entityType,
                this.fieldType,
                this.fieldName,
            )
        )
    }

}
