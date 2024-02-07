package com.github.nvelychenko.drupalextend.index.types

import kotlinx.serialization.Serializable
import org.apache.commons.lang3.builder.HashCodeBuilder

@Serializable
data class DrupalContentEntity(
    val entityTypeId: String,
    val fqn: String,
    val keys: HashMap<String, String>,
    val storageHandler: String,
    val isEck: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other != null && other is DrupalContentEntity) {
            this.entityTypeId == other.entityTypeId
                    && this.fqn == other.fqn
                    && this.keys == other.keys
                    && this.storageHandler == other.storageHandler
                    && this.isEck == other.isEck
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return HashCodeBuilder()
            .append(this.entityTypeId)
            .append(this.fqn)
            .append(this.keys)
            .append(this.storageHandler)
            .append(this.isEck)
            .hashCode()
    }

}
