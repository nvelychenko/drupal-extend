package com.github.nvelychenko.drupalextend.index.types

import java.util.*

data class DrupalContentEntity(val entityTypeId: String, val fqn: String, val keys: HashMap<String, String>, val storageHandler: String) {
    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other != null && other is DrupalContentEntity) {
            this.entityTypeId == other.entityTypeId && this.fqn == other.fqn && this.keys == other.keys && this.storageHandler == other.storageHandler
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            *arrayOf(
                this.entityTypeId,
                this.fqn,
                this.keys,
                this.storageHandler
            )
        )
    }

}
