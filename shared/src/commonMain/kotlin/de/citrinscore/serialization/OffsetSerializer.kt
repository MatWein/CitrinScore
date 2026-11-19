package de.citrinscore.serialization

import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object OffsetSerializer : KSerializer<Offset> {
    private val delegate = FloatArraySerializer()

    override val descriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Offset) = encoder.encodeSerializableValue(delegate, floatArrayOf(value.x, value.y))

    override fun deserialize(decoder: Decoder): Offset = decoder.decodeSerializableValue(delegate).let { (x, y) -> Offset(x, y) }
}
