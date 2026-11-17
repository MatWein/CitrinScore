package de.citrinscore.score.playback

import de.citrinscore.score.musicxml.ScorePerformance
import kotlinx.coroutines.flow.StateFlow

interface ScoreSynthesizer : AutoCloseable {
    val position: StateFlow<Double>

    val isPlaying: StateFlow<Boolean>

    suspend fun play(from: Double)

    suspend fun pause()

    suspend fun sound(pitch: Int)

    fun setTempo(factor: Float)
}

expect suspend fun openScoreSynthesizer(performance: ScorePerformance, instrument: Instrument?): ScoreSynthesizer
