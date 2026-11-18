package de.citrinscore.score.playback

import kotlinx.serialization.Serializable

@Serializable
enum class Instrument(val program: Int) {
    Piano(0),
    Organ(19),
    Guitar(24),
    Harp(46),
    Violin(40),
    Viola(41),
    Cello(42),
    DoubleBass(43),
    Flute(73),
    Recorder(74),
    Oboe(68),
    Clarinet(71),
    Bassoon(70),
    Saxophone(65),
    Horn(60),
    Trumpet(56),
    Trombone(57),
    Choir(52),
}
