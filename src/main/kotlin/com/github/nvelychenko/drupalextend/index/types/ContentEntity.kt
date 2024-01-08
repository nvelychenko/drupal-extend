package com.github.nvelychenko.drupalextend.index.types

import java.util.*

data class ContentEntity(val entityTypeId: String, val fqn: String) {
    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other != null && this.javaClass == other.javaClass) {
            val data = other as ContentEntity
            this.entityTypeId == data.entityTypeId && this.fqn == data.fqn
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            *arrayOf<Any>(
                this.entityTypeId,
                this.fqn,
            )
        )
    }

}
