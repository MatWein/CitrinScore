package de.citrinscore.score.audio

import kotlinx.coroutines.flow.Flow

internal const val SAMPLE_SCALE = 32768f

expect fun microphoneSamples(sampleRate: Int): Flow<FloatArray>
