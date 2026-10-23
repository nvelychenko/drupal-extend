package com.github.nvelychenko.drupalextend.index.types

import kotlinx.serialization.Serializable

@Serializable
data class SdcComponent(val props: List<Props>, val slots: List<Slots>) {
    @Serializable
    data class Props(
        val id: String,
        val type: String? = null,
        val enums: List<String>? = null,
        val required: Boolean = false,
    )

    @Serializable
    data class Slots(
        val id: String,
        val title: String,
    )
}
