package com.github.nvelychenko.drupalextend.index.types

import kotlinx.serialization.Serializable
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.util.*

@Serializable
data class SdcComponent(val props: Array<Props>, val slots: Array<Slots>) {
    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other != null && other is SdcComponent) {
            if (this.props.size != other.props.size || this.slots.size != other.slots.size) {
                return false
            }

            var equals = true

            for (i in 0..<this.props.size) {
                if (this.props[i] != other.props[i]) {
                    equals = false
                }
            }

            for (i in 0..<this.slots.size) {
                if (this.slots[i] != other.slots[i]) {
                    equals = false
                }
            }
            equals
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        val builder = HashCodeBuilder()
        this.slots.forEach(builder::append)
        this.props.forEach(builder::append)
        return builder.hashCode()
    }

    @Serializable
    data class Props(
        val id: String,
        val type: String? = null,
        val enums: Array<String>? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            return if (this === other) {
                true
            } else if (other != null && other is SdcComponent.Props) {
                this.id == other.id && this.type == other.type && this.enums.contentEquals(other.enums)
            } else {
                false
            }
        }

        override fun hashCode(): Int {
            return Objects.hash(
                *arrayOf(
                    this.id,
                    this.type,
                    this.enums,
                )
            )
        }
    }

    @Serializable
    data class Slots(
        val id: String,
        val required: Boolean,
    ) {
        override fun equals(other: Any?): Boolean {
            return if (this === other) {
                true
            } else if (other != null && other is SdcComponent.Slots) {
                this.id == other.id && this.required == other.required
            } else {
                false
            }
        }

        override fun hashCode(): Int {
            return Objects.hash(
                *arrayOf(
                    this.id,
                    this.required,
                )
            )
        }
    }



}
