package com.github.nvelychenko.drupalextend.index.dataExternalizer

import com.intellij.util.io.DataExternalizer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.DataInput
import java.io.DataOutput

class SerializedObjectDataExternalizer<T : Any>(
    private val serializer: KSerializer<T>,
) : DataExternalizer<T> {

    override fun save(out: DataOutput, value: T) {
        out.writeUTF(Json.encodeToString(serializer, value))
    }

    override fun read(input: DataInput): T {
        return Json.decodeFromString(serializer, input.readUTF())
    }
}
