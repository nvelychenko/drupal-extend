package com.github.nvelychenko.drupalextend.index.types

import java.util.*

data class DrupalFieldType(val fieldTypeId: String, val fqn: String, val listClassFqn: String, val properties: HashMap<String, String>) {
    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other != null && other is DrupalFieldType) {
            this.fieldTypeId == other.fieldTypeId && this.fqn == other.fqn && this.listClassFqn == other.listClassFqn && this.properties == other.properties
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            *arrayOf(
                this.fieldTypeId,
                this.fqn,
                this.properties,
                this.listClassFqn
            )
        )
    }

}
