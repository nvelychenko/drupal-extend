package com.github.nvelychenko.drupalextend.index.types

import kotlinx.serialization.Serializable
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.util.*

@Serializable
data class RenderElementType(
    val typeId: String,
    val typeClass: String,
    val properties: Array<RenderElementTypeProperty>,
    val renderElementType: String,
) {
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
            this.typeId == other.typeId && this.typeClass == other.typeClass && equals && this.renderElementType == other.renderElementType
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        val builder = HashCodeBuilder()
            .append(this.typeId)
            .append(this.typeClass)
            .append(this.renderElementType)

        this.properties.forEach(builder::append)

        return builder.hashCode()
    }

    @Serializable
    data class RenderElementTypeProperty(
        val id: String,
        val priority: Double = 0.0,
        val type: String? = null,
        val doc: String? = null
    ) {
        override fun equals(other: Any?): Boolean {
            return if (this === other) {
                true
            } else if (other != null && other is RenderElementTypeProperty) {
                this.id == other.id && this.type == other.type && this.doc == other.doc && this.priority == other.priority
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
                    this.priority,
                )
            )
        }

    }

}
