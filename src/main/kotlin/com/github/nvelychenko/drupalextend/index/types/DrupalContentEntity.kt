package com.github.nvelychenko.drupalextend.index.types

import java.util.*
import kotlin.collections.HashMap

data class DrupalContentEntity(val entityType: String, val fqn: String, val keys: HashMap<String, String>) {
    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other != null && other is DrupalContentEntity) {
            this.entityType == other.entityType && this.fqn == other.fqn && this.keys == other.keys
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            *arrayOf(
                this.entityType,
                this.fqn,
                this.keys
            )
        )
    }

}
