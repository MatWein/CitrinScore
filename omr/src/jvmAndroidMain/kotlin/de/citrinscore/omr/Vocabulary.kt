package de.citrinscore.omr

import org.apache.commons.numbers.fraction.Fraction

internal const val NO_NOTE = "."
internal const val EMPTY = "_"
internal const val OCTAVE_SHIFT_STOP = "octaveShiftStop"

private val TIME_SIGNATURE_DENOMINATORS = listOf(1, 2, 3, 4, 6, 8, 12, 16, 32, 48)
private val DYNAMICS = listOf("pppp", "ppp", "pp", "p", "mp", "mf", "f", "ff", "fff", "sfz", "sffz", "fp", "sf", "fz", "rf", "rfz", "sfp", "empty")
private val DYNAMICS_MODIFIER = listOf("piu", "dolce", "poco", "espress", "subito", "atempo", "leggiero", "pocopiu", "sempre")
private val OCTAVE_CLEFS = listOf("clef_G2_8vb", "clef_G2_8va", "clef_F4_8vb", "clef_F4_8va")
private val OCTAVE_SHIFTS = listOf("octaveShift_8va", "octaveShift_8vb")
private val NOTE_NAMES = listOf("C", "D", "E", "F", "G", "A", "B")
private val SLURS = listOf("slurStart_slurStop", "slurStart", "slurStop")
internal val BEAMS = listOf("beamStart", "beamContinue", "beamStop")

private val LIEDER_ARTICULATIONS = listOf(
    "accent", "accent_accentStrong", "accent_arpeggiate", "accent_arpeggiate_fermata", "accent_arpeggiate_staccato",
    "accent_arpeggiate_tenuto", "accent_breathMark", "accent_breathMark_fermata", "accent_fermata", "accent_fermata_tremolo",
    "accent_staccatissimo", "accent_staccato", "accent_staccato_tenuto", "accent_staccato_tremolo", "accent_staccato_trill",
    "accent_tenuto", "accent_tremolo", "accent_trill", "accentStrong", "accentStrong_arpeggiate", "accentStrong_arpeggiate_fermata",
    "accentStrong_arpeggiate_staccato", "accentStrong_arpeggiate_tenuto", "accentStrong_breathMark", "accentStrong_breathMark_fermata",
    "accentStrong_fermata", "accentStrong_fermata_tremolo", "accentStrong_staccatissimo", "accentStrong_staccato",
    "accentStrong_staccato_tenuto", "accentStrong_staccato_tremolo", "accentStrong_staccato_trill", "accentStrong_tenuto",
    "accentStrong_tremolo", "accentStrong_trill", "arpeggiate", "arpeggiate_breathMark", "arpeggiate_fermata",
    "arpeggiate_fermata_staccato", "arpeggiate_fermata_tenuto", "arpeggiate_staccatissimo", "arpeggiate_staccatissimo_staccato",
    "arpeggiate_staccato", "arpeggiate_staccato_tenuto", "arpeggiate_tenuto", "arpeggiate_tremolo", "arpeggiate_trill",
    "breathMark", "breathMark_fermata", "breathMark_fermata_tenuto", "breathMark_staccato", "breathMark_tenuto",
    "breathMark_tremolo", "breathMark_trill", "fermata", "fermata_staccato", "fermata_tenuto", "fermata_tremolo", "fermata_trill",
    "fermata_turn", "mordent", "staccatissimo", "staccatissimo_staccato", "staccatissimo_staccato_tenuto",
    "staccatissimo_staccato_tenuto_trill", "staccatissimo_tenuto", "staccato", "staccato_tenuto", "staccato_tremolo",
    "staccato_trill", "staccato_turn", "tenuto", "tenuto_tremolo", "tenuto_trill", "tremolo", "trill", "trill_turn", "turn",
)
private val ADDITIONAL_ARTICULATIONS = listOf(
    "accent_accentStrong_tremolo", "accent_arpeggiate_staccatissimo", "accent_fermata_trill", "fermata_mordent_staccato_trill",
    "fermata_staccatissimo", "mordent_staccato", "mordent_trill",
)

internal object Vocabulary {
    val rhythm: List<String> = buildList {
        addAll(listOf("PAD", "BOS", "EOS", "chord", "barline", "doublebarline", "bolddoublebarline"))
        addAll(listOf("repeatStart", "repeatEnd", "repeatEndStart", "voltaStart", "voltaStop", "voltaDiscontinue"))
        addAll((3..5).map { "clef_F$it" })
        addAll((1..5).map { "clef_C$it" })
        addAll((1..2).map { "clef_G$it" })
        add("clef_TAB5")
        addAll((-7..7).map { "keySignature_$it" })
        addAll(TIME_SIGNATURE_DENOMINATORS.map { "timeSignature/$it" })
        addAll((2..10).map { "rest_${it}m" })
        val kernValues = listOf(0, 1, 2, 3, 4, 5, 6, 8, 10, 12, 16, 32, 64, 128).flatMap { duration ->
            listOf("", "G").flatMap { grace -> listOf("", ".", "..").map { dots -> "$duration$grace$dots" } }
        }
        val irregular = listOf(7, 11, 13, 18, 20, 21, 22, 24, 26, 28, 30, 34, 36, 40, 48, 56, 96)
        addAll(kernValues.map { "note_$it" })
        addAll(irregular.map { "note_$it" })
        addAll(kernValues.map { "rest_$it" })
        addAll(irregular.map { "rest_$it" })
        addAll(DYNAMICS.map { "dynamic_$it" })
        addAll(OCTAVE_CLEFS)
        addAll(OCTAVE_SHIFTS)
        add(OCTAVE_SHIFT_STOP)
    }
    val lift = listOf(NO_NOTE, EMPTY, "#", "##", "N", "b", "bb")
    val position = listOf(NO_NOTE, "upper", "upper2", "lower", "lower2")
    val articulation = listOf(NO_NOTE, EMPTY) + LIEDER_ARTICULATIONS + DYNAMICS_MODIFIER.map { "dynamic_$it" } + ADDITIONAL_ARTICULATIONS
    val slur = listOf(NO_NOTE, EMPTY) + SLURS + BEAMS.flatMap { beam -> listOf(beam) + SLURS.map { "${beam}_$it" } }
    val pitch = listOf(NO_NOTE, EMPTY) + (0..9).flatMap { octave -> NOTE_NAMES.map { "$it$octave" } }.reversed()
    const val EOS_TOKEN = 2
}

internal fun isLowerPosition(position: String) = position.startsWith("lower")

internal fun octavesOfClef(clef: String) = when {
    clef.endsWith("_8vb") -> -1
    clef.endsWith("_8va") -> 1
    else -> 0
}

internal fun octavesOfShift(shift: String) = when (shift) {
    "octaveShift_8va" -> 1
    "octaveShift_8vb" -> -1
    else -> 0
}

internal fun transposeOctaves(pitch: String, octaves: Int): String {
    if (octaves == 0 || pitch.length != 2 || !pitch[1].isDigit()) return pitch
    return "${pitch[0]}${pitch[1].digitToInt() + octaves}"
}

internal fun splitSlurAndBeam(value: String): Pair<String, String> {
    if (value == NO_NOTE || value == EMPTY) return value to EMPTY
    val parts = value.split("_")
    val beams = parts.filter { it in BEAMS }
    val slurs = parts.filter { it !in BEAMS }
    return slurs.joinToString("_").ifEmpty { EMPTY } to (beams.firstOrNull() ?: EMPTY)
}

internal class SymbolDuration(val baseDuration: Fraction, val dots: Int, actualNotes: Int, normalNotes: Int, val kern: Int) {
    val actualNotes: Int
    val normalNotes: Int
    val fraction: Fraction

    init {
        val ratio = Fraction.of(actualNotes, normalNotes)
        this.actualNotes = ratio.numerator
        this.normalNotes = ratio.denominator
        var duration = baseDuration
        var add = duration.divide(2)
        repeat(dots) {
            duration = duration.add(add)
            add = add.divide(2)
        }
        fraction = if (this.actualNotes != this.normalNotes) duration.multiply(Fraction.of(this.normalNotes, this.actualNotes)) else duration
    }

    val isTuplet get() = actualNotes != normalNotes
}

private fun priorPowerOfTwo(n: Int) = if (n < 1) 1 else Integer.highestOneBit(n)

internal fun kernToSymbolDuration(kern: String): SymbolDuration {
    if (kern.endsWith("m")) return SymbolDuration(Fraction.ONE, 0, 1, 1, 4)
    val digits = kern.takeWhile { it.isDigit() }
    val base = digits.toIntOrNull() ?: 4
    val dots = kern.drop(digits.length).count { it == '.' }
    if ('G' in kern) return SymbolDuration(Fraction.ZERO, dots, 1, 1, base)
    if (base == 0) return SymbolDuration(Fraction.ONE, dots, 1, 1, base)
    if (base and (base - 1) == 0) return SymbolDuration(Fraction.of(1, base), dots, 1, 1, base)
    val normalNotes = priorPowerOfTwo(base)
    return SymbolDuration(Fraction.of(1, normalNotes), dots, base, normalNotes, normalNotes)
}

private val TUPLET_PATTERN = Regex("(note|rest)_(\\d+)(.*)")

internal data class EncodedSymbol(
    val rhythm: String,
    val pitch: String = NO_NOTE,
    val lift: String = NO_NOTE,
    val articulation: String = NO_NOTE,
    val slur: String = NO_NOTE,
    val position: String = NO_NOTE,
) {
    val duration: SymbolDuration by lazy {
        if (!isNoteOrRest) SymbolDuration(Fraction.ZERO, 0, 1, 1, 1) else kernToSymbolDuration(rhythm.split("_")[1])
    }

    val isNoteOrRest get() = rhythm.startsWith("note") || rhythm.startsWith("rest")

    val isBarlineOrRepeat get() = "barline" in rhythm || "repeat" in rhythm

    fun removeTuplet(): EncodedSymbol {
        val match = TUPLET_PATTERN.matchEntire(rhythm) ?: return this
        val value = match.groupValues[2].toInt()
        val duration = when {
            value % 3 == 0 -> value / 3 * 2
            value % 5 == 0 -> value / 5 * 4
            value % 7 == 0 -> value / 7 * 4
            else -> return this
        }
        return copy(rhythm = "${match.groupValues[1]}_$duration${match.groupValues[3]}")
    }

    fun toUpperPosition() = if (!isLowerPosition(position)) this else copy(position = position.replace("lower", "upper"))

    override fun toString() = listOf(rhythm, pitch, lift, articulation, slur, position).joinToString(" ")
}

internal fun sortTokenChords(symbols: List<EncodedSymbol>): List<List<EncodedSymbol>> = groupIntoChords(symbols).map { chord ->
    chord.sortedWith(compareByDescending { it.toString() })
}

internal fun groupIntoChords(symbols: List<EncodedSymbol>): List<List<EncodedSymbol>> {
    val chords = mutableListOf<MutableList<EncodedSymbol>>()
    var inChord = false
    for (symbol in symbols) {
        when {
            symbol.rhythm == "chord" -> inChord = true
            inChord && chords.isNotEmpty() -> {
                chords.last() += symbol
                inChord = false
            }
            else -> chords += mutableListOf(symbol)
        }
    }
    return chords
}
