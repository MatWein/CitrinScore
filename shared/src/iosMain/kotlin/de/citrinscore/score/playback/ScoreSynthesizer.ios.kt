package de.citrinscore.score.playback

import de.citrinscore.score.musicxml.ScorePerformance

actual suspend fun openScoreSynthesizer(performance: ScorePerformance, instrument: Instrument?): ScoreSynthesizer =
    throw UnsupportedOperationException("Score playback is not available on iOS yet")
