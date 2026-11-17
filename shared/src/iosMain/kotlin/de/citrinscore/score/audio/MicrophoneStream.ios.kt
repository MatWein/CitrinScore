package de.citrinscore.score.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual fun microphoneSamples(sampleRate: Int): Flow<FloatArray> =
    flow { throw UnsupportedOperationException("Listening to the microphone is not available on iOS yet") }
