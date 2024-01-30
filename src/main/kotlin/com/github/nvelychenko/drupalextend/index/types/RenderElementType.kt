package com.github.nvelychenko.drupalextend.index.types

import org.apache.commons.lang3.builder.HashCodeBuilder
import java.io.Serializable
import java.util.*

@kotlinx.serialization.Serializable
data class RenderElementType(
    val typeId: String,
    val typeClass: String,
    val properties: Array<RenderElementTypeProperty>
): Serializable {
    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other != null && other is RenderElementType) {
            if (this.properties.size != other.properties.size) {
                return false
            }

            var equals = true

            for (i in 0..<this.properties.size) {
                if (this.properties[i] != other.properties[i]) {
                    equals = false
                }
            }
            this.typeId == other.typeId && this.typeClass == other.typeClass && equals
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        val builder = HashCodeBuilder()
            .append(this.typeId)
            .append(this.typeClass)

        this.properties.forEach(builder::append)

        return builder.hashCode()
    }

    @kotlinx.serialization.Serializable
    data class RenderElementTypeProperty(val id: String, val type: String? = null, val doc: String? = null) {
        override fun equals(other: Any?): Boolean {
            return if (this === other) {
                true
            } else if (other != null && other is RenderElementTypeProperty) {
                this.id == other.id && this.type == other.type && this.doc == other.doc
            } else {
                false
            }
        }

        override fun hashCode(): Int {
            return Objects.hash(
                *arrayOf(
                    this.id,
                    this.type,
                    this.doc,
                )
            )
        }

    }

}
