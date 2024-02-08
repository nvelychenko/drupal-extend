package com.github.nvelychenko.drupalextend.index.types

import kotlinx.serialization.Serializable
import org.apache.commons.lang3.builder.HashCodeBuilder

@Serializable
data class DrupalField(val entityType: String, val fieldType: String, val fieldName: String, val path: String, val targetType: String? = null) {
    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other != null && this.javaClass == other.javaClass) {
            val data = other as DrupalField
            this.entityType == data.entityType &&
                    this.fieldType == data.fieldType &&
                    this.fieldName == data.fieldName &&
                    this.path == data.path &&
                    this.targetType == data.targetType
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(this.entityType)
                .append(this.fieldType)
                .append(this.fieldName)
                .append(this.path)
                .append(this.targetType)
                .hashCode()
    }

}
