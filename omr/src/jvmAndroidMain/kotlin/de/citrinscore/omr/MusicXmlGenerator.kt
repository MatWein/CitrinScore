package de.citrinscore.omr

import org.apache.commons.numbers.fraction.Fraction
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element

private val logger = LoggerFactory.getLogger("de.citrinscore.omr.MusicXmlGenerator")

private const val DURATION_OF_QUARTER = 16
private const val POSITION_ATTRIBUTE = "homr-position"
private const val VOICES_PER_STAFF = 4

private val LIFT_TO_ALTER = mapOf("N" to 0, "#" to 1, "##" to 2, "b" to -1, "bb" to -2)
private val DURATION_NAMES = mapOf(0 to "breve", 1 to "whole", 2 to "half", 4 to "quarter", 8 to "eighth", 16 to "16th", 32 to "32nd", 64 to "64th", 128 to "128th")
private val BEAM_TYPES = mapOf("beamStart" to "begin", "beamContinue" to "continue", "beamStop" to "end")
private val BEAMS_PER_TYPE = mapOf("eighth" to 1, "16th" to 2, "32nd" to 3, "64th" to 4, "128th" to 5)
private val BEFORE_NOTES_ORDER = listOf("clef", "keySignature", "timeSignature")

internal typealias TimeSignature = Pair<String, String>

private fun Fraction.toIntTruncated() = numerator / denominator

private fun List<Fraction>.medianFraction(): Fraction {
    val sorted = sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle] else sorted[middle - 1].add(sorted[middle]).divide(2)
}

private class ConversionState(val division: Int, var nominator: Fraction) {
    var beats = 4 * DURATION_OF_QUARTER
    private var tremoloState = "stop"
    private var voltaNumber = 1
    private var lastVoltaMeasure = -10
    val clefOctaves = mutableMapOf<Int, Int>()
    var shiftOctaves = mutableMapOf<Int, Int>()

    fun soundingPitch(writtenPitch: String, staff: Int) = transposeOctaves(writtenPitch, (clefOctaves[staff] ?: 0) + (shiftOctaves[staff] ?: 0))

    fun startVolta(measure: Int): Int {
        voltaNumber = if (measure == lastVoltaMeasure + 1) voltaNumber + 1 else 1
        return voltaNumber
    }

    fun stopVolta(measure: Int): Int {
        lastVoltaMeasure = measure
        return voltaNumber
    }

    fun toggleTremoloState(): String {
        tremoloState = if (tremoloState == "start") "stop" else "start"
        return tremoloState
    }
}

private class SymbolChord(val symbols: List<EncodedSymbol>, val tupletMarks: MutableMap<String, String> = mutableMapOf()) {
    fun markFor(symbol: EncodedSymbol) = tupletMarks[symbol.position] ?: ""

    val isBarline get() = symbols.isNotEmpty() && symbols[0].isBarlineOrRepeat

    fun intoPositions(): List<SymbolChord> = symbols.groupBy { it.position }.values.map { SymbolChord(it, tupletMarks) }
        .sortedByDescending { chord -> chord.symbols.all { it.rhythm.startsWith("rest") } }
}

private class GroupClock {
    var time: Fraction = Fraction.ZERO
    private var sounding = mutableListOf<Fraction>()
    private val voiceFree = mutableMapOf<String, Fraction>()

    private fun timed(group: SymbolChord) = group.symbols.filter { it.isNoteOrRest && it.duration.fraction > Fraction.ZERO }

    fun waitForVoices(group: SymbolChord): Fraction {
        val free = timed(group).map { voiceFree[it.position] ?: Fraction.ZERO }.maxOrNull() ?: Fraction.ZERO
        val gap = maxOf(free.subtract(time), Fraction.ZERO)
        time = time.add(gap)
        sounding = sounding.filter { it > time }.toMutableList()
        return gap
    }

    fun advance(group: SymbolChord): Fraction {
        val shortest = LinkedHashMap<String, Fraction>()
        timed(group).forEach { symbol ->
            val duration = symbol.duration.fraction
            shortest[symbol.position] = minOf(duration, shortest[symbol.position] ?: duration)
        }
        shortest.forEach { (position, duration) -> voiceFree[position] = time.add(duration) }
        val timedDurations = group.symbols.filter { it.isNoteOrRest }.map { it.duration.fraction }.filter { it > Fraction.ZERO }
        val advance = if (timedDurations.isEmpty()) {
            Fraction.ZERO
        } else {
            sounding += timedDurations.map { time.add(it) }
            sounding.filter { it > time }.min().subtract(time)
        }
        time = time.add(advance)
        sounding = sounding.filter { it > time }.toMutableList()
        return advance
    }
}

internal fun generateMusicXml(voices: List<List<EncodedSymbol>>, inheritedTimes: List<TimeSignature?>?): Document {
    val document = newDocument()
    val root = document.createElement("score-partwise").apply { setAttribute("version", "4.0") }
    document.appendChild(root)
    root.add("work").add("work-title", "")
    root.add("identification").add("encoding").add("software", "homr")
    root.add("defaults")
    val hasTwoStaves = voices.map { voice -> voice.any { isLowerPosition(it.position) } }
    val partList = root.add("part-list")
    hasTwoStaves.forEachIndexed { index, twoStaves -> addScorePart(partList, index, twoStaves) }
    val parts = voices.mapIndexed { index, voice -> buildPart(root, voice, index, hasTwoStaves[index], inheritedTimes?.getOrNull(index)) }
    shortenMeasureRestsInPickup(parts)
    return document
}

private fun partId(index: Int) = "P${index + 1}"

private fun addScorePart(partList: Element, index: Int, twoStaves: Boolean) {
    val (name, sound, program) = if (twoStaves) Triple("Piano", "keyboard.piano", 1) else Triple("Voice", "voice", 54)
    val id = partId(index)
    val scorePart = partList.add("score-part", null, "id" to id)
    scorePart.add("part-name", name)
    scorePart.add("score-instrument", null, "id" to "$id-I1").apply {
        add("instrument-name", name)
        add("instrument-sound", sound)
    }
    scorePart.add("midi-instrument", null, "id" to "$id-I1").apply {
        add("midi-channel", (index + 1).toString())
        add("midi-program", program.toString())
        add("volume", "100")
        add("pan", "0")
    }
}

private fun measureLength(measure: Element, divisions: Int): Fraction {
    var position = 0
    var end = 0
    for (child in measure.elements) {
        val duration = child.findText("duration", "0").ifEmpty { "0" }.toInt()
        when {
            child.tagName == "note" && child.find("chord") == null -> position += duration
            child.tagName == "forward" -> position += duration
            child.tagName == "backup" -> position -= duration
        }
        end = maxOf(end, position)
    }
    return Fraction.of(end, maxOf(divisions, 1))
}

private fun shortenMeasureRestsInPickup(parts: List<Element>) {
    val firsts = parts.map { part ->
        val measure = part.find("measure")
        val divisions = part.findText("measure/attributes/divisions", "0").ifEmpty { "0" }.toInt()
        if (measure == null || divisions <= 0) return
        measure to divisions
    }
    val lengths = firsts.map { (measure, divisions) -> measureLength(measure, divisions) }
    val played = firsts.zip(lengths).filter { (first, _) -> first.first.children("note").any { it.find("rest") == null } }.map { it.second }
    if (played.isEmpty()) return
    val pickup = played.max()
    firsts.zip(lengths).forEach { (first, length) ->
        val (measure, divisions) = first
        val notes = measure.children("note")
        if (notes.isEmpty()) {
            addPickupRests(measure, pickup, divisions)
            return@forEach
        }
        val onlyMeasureRests = notes.all { it.find("rest")?.getAttribute("measure") == "yes" }
        if (!onlyMeasureRests || length <= pickup) return@forEach
        val duration = maxOf(1, pickup.multiply(divisions).toIntTruncated()).toString()
        measure.elements.filter { it.tagName == "note" || it.tagName == "backup" }.forEach { it.find("duration")!!.textContent = duration }
    }
}

private fun addPickupRests(measure: Element, pickup: Fraction, divisions: Int) {
    val staves = measure.findText("attributes/staves", "1").ifEmpty { "1" }.toInt()
    val duration = maxOf(1, pickup.multiply(divisions).toIntTruncated()).toString()
    for (staff in 1..staves) {
        if (staff > 1) measure.add("backup").add("duration", duration)
        measure.add("note").apply {
            add("rest", null, "measure" to "yes")
            add("duration", duration)
            add("voice", if (staff == 1) "1" else "5")
            add("staff", staff.toString())
        }
    }
}

internal fun lastTimeSignatures(root: Element): List<TimeSignature?> = root.descendants("part").map { part ->
    part.descendants("time").lastOrNull()?.let { it.findText("beats") to it.findText("beat-type") }
}

private fun buildPart(root: Element, voice: List<EncodedSymbol>, index: Int, hasTwoStaves: Boolean, inheritedTime: TimeSignature?): Element {
    val part = root.add("part", null, "id" to partId(index))
    MeasureBuilder(part, voice, hasTwoStaves, inheritedTime).build().forEach { part.appendChild(it) }
    convertTies(part)
    addStems(part)
    convertBeams(part)
    part.descendants("note").forEach { it.removeAttribute(POSITION_ATTRIBUTE) }
    return part
}

private class MeasureBuilder(private val part: Element, voice: List<EncodedSymbol>, private val hasTwoStaves: Boolean, private var inheritedTime: TimeSignature?) {
    private val groups = splitMixedChords(TupletParser.parse(sortTokenChords(voice).map { SymbolChord(it) }))
    private val division: Int
    private val nominators: List<Fraction>
    private val state: ConversionState
    private var clock = GroupClock()
    private var measureNumber = 1
    private var restMeasures = 0
    private val measures = mutableListOf<Element>()
    private var current: Element

    init {
        val (division, nominators) = divisionAndNominators(groups)
        this.division = division
        this.nominators = nominators
        state = ConversionState(division, nominators[0])
        current = newMeasure()
    }

    private fun newMeasure() = part.element("measure", null, "number" to measureNumber.toString())

    private fun measureRests(measure: Element) {
        val positions = if (hasTwoStaves) listOf("upper", "lower") else listOf("upper")
        positions.forEachIndexed { staffNo, position ->
            if (staffNo > 0) measure.appendChild(backup(measure, state.nominator))
            measure.appendChild(measureRest(measure, EncodedSymbol("measureRest", position = position)))
        }
    }

    private fun closeCurrentMeasure() {
        rebalanceMeasureVoices(current)
        measures += current
        clock = GroupClock()
        repeat(restMeasures) {
            measureNumber += 1
            measures += newMeasure().also(::measureRests)
        }
        restMeasures = 0
    }

    private fun startNextMeasure() {
        closeCurrentMeasure()
        measureNumber += 1
        current = newMeasure()
    }

    private fun attributes(last: Element?, forceNew: Boolean = false) = if (last != null && !forceNew) last else current.add("attributes")

    fun build(): List<Element> {
        val inheritedLength = timeSignatureLength(inheritedTime)
        if (inheritedLength != null && !startsWithTimeSignature(groups)) state.nominator = inheritedLength else inheritedTime = null
        var timeSignatureNo = 0
        val firstAttributes = current.add("attributes")
        firstAttributes.add("divisions", (division / 4).toString())
        if (hasTwoStaves) {
            firstAttributes.add("staves", "2")
            firstAttributes.add("part-symbol", "brace")
        }
        var attributes: Element? = firstAttributes
        groups.forEachIndexed { groupNo, group ->
            val symbol = group.symbols[0]
            val rhythm = symbol.rhythm
            val lastAttributes = attributes
            attributes = null
            when {
                symbol.isNoteOrRest -> {
                    if (group.symbols.size == 1 && rhythm.endsWith("m")) {
                        val target = attributes(lastAttributes)
                        attributes = target
                        val duration = multiMeasureRest(symbol, target)
                        if (duration > 0) {
                            measureRests(current)
                            restMeasures = duration - 1
                        }
                    } else {
                        val positions = group.intoPositions()
                        val gap = clock.waitForVoices(group)
                        if (gap > Fraction.ZERO) current.appendChild(forward(current, gap))
                        val advance = clock.advance(group)
                        positions.forEachIndexed { positionNo, chord ->
                            val chordDuration = if (positionNo == positions.lastIndex) advance else Fraction.ZERO
                            noteChord(chord, chordDuration).forEach { current.appendChild(it) }
                        }
                    }
                }
                rhythm == "measureRest" -> group.symbols.sortedBy(::staffOf).forEachIndexed { staffNo, rest ->
                    if (staffNo > 0) current.appendChild(backup(current, state.nominator))
                    current.appendChild(measureRest(current, rest))
                }
                rhythm == "newline" -> {
                    stopOctaveShifts(current, state)
                    if (groupNo != groups.lastIndex) current.add("print", null, "new-system" to "yes")
                }
                rhythm.startsWith("clef") -> {
                    val target = attributes(lastAttributes, forceNew = true)
                    attributes = target
                    group.symbols.filter { it.rhythm.startsWith("clef") }.forEach { clef ->
                        addClef(clef, target)
                        state.clefOctaves[staffOf(clef)] = octavesOfClef(clef.rhythm)
                    }
                }
                rhythm.startsWith("keySignature") -> {
                    val target = attributes(lastAttributes)
                    attributes = target
                    target.add("key").add("fifths", rhythm.split("_")[1])
                }
                rhythm.startsWith("timeSignature") -> {
                    val target = attributes(lastAttributes)
                    attributes = target
                    state.nominator = nominators[minOf(timeSignatureNo, nominators.lastIndex)]
                    timeSignatureNo += 1
                    addTimeSignature(symbol, target)
                }
                "barline" in rhythm -> {
                    val emptyPickup = measures.isEmpty() && !hasNotes(current)
                    val ending = if (hasNotes(current) || emptyPickup) current else measures.lastOrNull()
                    if (rhythm != "barline" && ending != null) {
                        barline(ending, "right").add("bar-style", if (rhythm == "bolddoublebarline") "heavy-heavy" else "light-light")
                    }
                    if (ending === current) startNextMeasure()
                }
                rhythm == "repeatStart" -> {
                    if (hasNotes(current)) startNextMeasure()
                    addRepeat("repeatStart", barline(current, "left"))
                }
                rhythm == "repeatEnd" || rhythm == "repeatEndStart" -> {
                    val ending = if (hasNotes(current)) current else measures.lastOrNull()
                    if (ending != null) addRepeat("repeatEnd", barline(ending, "right"))
                    if (ending === current) startNextMeasure()
                    if (rhythm == "repeatEndStart") addRepeat("repeatStart", barline(current, "left"))
                }
                rhythm.startsWith("voltaStart") -> addEnding(symbol, barline(current, "left"), state.startVolta(measureNumber))
                rhythm.startsWith("voltaStop") || rhythm.startsWith("voltaDiscontinue") -> {
                    val stopped = if (hasNotes(current)) current else measures.lastOrNull()
                    if (stopped != null) {
                        val number = state.stopVolta(stopped.getAttribute("number").ifEmpty { "0" }.toInt())
                        addEnding(symbol, barline(stopped, "right"), number)
                    }
                }
                rhythm.startsWith("dynamic") || rhythm.startsWith("octaveShift") || symbol.articulation.startsWith("dynamic") -> group.symbols.forEach { marking ->
                    if (marking.rhythm.startsWith("octaveShift")) addOctaveShift(marking, current, state) else addDynamic(marking, current)
                }
                else -> logger.debug("Symbol isn't supported yet: {}", symbol)
            }
        }
        if (hasNotes(current) || measures.isEmpty()) {
            stopOctaveShifts(current, state)
            closeCurrentMeasure()
        } else {
            stopOctaveShifts(measures.last(), state)
        }
        if (measures[0].find("attributes/time") == null) {
            val time = firstAttributes.add("time")
            val (beats, beatType) = inheritedTime ?: (maxOf(state.nominator.multiply(4).toIntTruncated(), 1).toString() to "4")
            time.add("beats", beats)
            time.add("beat-type", beatType)
        }
        return measures
    }

    private fun addTimeSignature(symbol: EncodedSymbol, attributes: Element) {
        val denominator = symbol.rhythm.split("/")[1]
        val beats = maxOf(state.nominator.multiply(denominator.toInt()).toIntTruncated(), 1)
        attributes.add("time").apply {
            add("beats", beats.toString())
            add("beat-type", denominator)
        }
        state.beats = beats
    }

    private fun noteChord(chord: SymbolChord, chordDuration: Fraction): List<Element> {
        val byDuration = groupNotes(chord.symbols)
        val result = mutableListOf<Element>()
        byDuration.entries.forEachIndexed { i, (groupDuration, groupNotes) ->
            val notes = groupNotes.filter { it.pitch != EMPTY && it.pitch != NO_NOTE }
            val rests = groupNotes.filter { it.pitch == EMPTY || it.pitch == NO_NOTE }
            notes.forEachIndexed { index, note -> result += noteOrRest(note, i, index > 0, chord.markFor(note)) }
            if (rests.isNotEmpty() && groupDuration > Fraction.ZERO) {
                if (notes.isNotEmpty()) result += backup(current, groupDuration)
                result += noteOrRest(rests[0], i, false, chord.markFor(rests[0]))
            }
            if (i != byDuration.size - 1 && groupDuration > Fraction.ZERO) result += backup(current, groupDuration)
        }
        val longest = byDuration.keys.max()
        if (chordDuration < longest) result += backup(current, longest.subtract(chordDuration))
        return result
    }

    private fun noteOrRest(symbol: EncodedSymbol, layer: Int, isChord: Boolean, tupletMark: String): Element {
        val note = current.element("note")
        if (isChord) note.add("chord")
        val duration = symbol.duration
        val isGrace = "G" in symbol.rhythm
        if (isGrace) note.add("grace")
        when (symbol.pitch) {
            EMPTY -> if (duration.fraction.numerator == 0) note.add("rest", null, "measure" to "yes") else note.add("rest")
            NO_NOTE -> note.add("rest")
            else -> {
                val sounding = state.soundingPitch(symbol.pitch, staffOf(symbol))
                val pitch = note.add("pitch")
                pitch.add("step", sounding.substring(0, 1))
                pitch.add("octave", sounding.substring(1))
                if (symbol.lift != NO_NOTE && symbol.lift != EMPTY) pitch.add("alter", LIFT_TO_ALTER.getValue(symbol.lift).toString())
            }
        }
        when {
            isGrace -> note.add("type", DURATION_NAMES.getValue(duration.kern))
            duration.fraction.numerator > 0 -> {
                note.add("duration", maxOf(1, duration.fraction.multiply(state.division).toIntTruncated()).toString())
                note.add("type", DURATION_NAMES.getValue(if (duration.kern == 0) 1 else duration.kern))
            }
            else -> {
                note.add("duration", state.beats.toString())
                note.add("type", DURATION_NAMES.getValue(0))
            }
        }
        repeat(duration.dots) { note.add("dot") }
        if (duration.isTuplet) {
            note.add("time-modification").apply {
                add("actual-notes", duration.actualNotes.toString())
                add("normal-notes", duration.normalNotes.toString())
            }
        }
        val staff = staffOf(symbol)
        note.add("voice", xmlVoice(staff, layer).toString())
        note.add("staff", staff.toString())
        val (slurs, beam) = splitSlurAndBeam(symbol.slur)
        note.setAttribute(POSITION_ATTRIBUTE, symbol.position)
        val beamType = BEAM_TYPES[beam]
        if (beamType != null && symbol.pitch != EMPTY && symbol.pitch != NO_NOTE) note.add("beam", beamType, "number" to "1")
        addArticulations(note, symbol.articulation, if (duration.isTuplet) tupletMark else "")
        addSlurs(note, slurs, staff)
        return note
    }

    private fun addArticulations(note: Element, articulations: String, tupletMark: String) {
        val notations = note.add("notations")
        val xmlArticulations = mutableListOf<Element>()
        val ornaments = mutableListOf<Element>()
        if (!articulations.startsWith("dynamic")) {
            for (articulation in articulations.split("_")) {
                when (articulation) {
                    "" -> Unit
                    NO_NOTE -> logger.debug("Note without valid articulation: {}", articulations)
                    "fermata" -> notations.add("fermata")
                    "arpeggiate" -> notations.add("arpeggiate")
                    "accent" -> xmlArticulations += note.element("accent")
                    "accentStrong" -> xmlArticulations += note.element("strong-accent")
                    "mordent" -> xmlArticulations += note.element("mordent")
                    "staccato" -> xmlArticulations += note.element("staccato")
                    "staccatissimo" -> xmlArticulations += note.element("staccatissimo")
                    "tenuto" -> xmlArticulations += note.element("tenuto")
                    "tremolo" -> ornaments += note.element("tremolo", "3", "type" to state.toggleTremoloState())
                    "trill" -> ornaments += note.element("trill-mark")
                    "breathMark" -> xmlArticulations += note.element("breath-mark")
                    "turn" -> ornaments += note.element("inverted-turn")
                    "caesura" -> xmlArticulations += note.element("caesura")
                    "doit" -> xmlArticulations += note.element("doit")
                    "slurStart" -> notations.add("slur", null, "type" to "start")
                    "slurStop" -> notations.add("slur", null, "type" to "stop")
                    "tieStart" -> notations.add("tied", null, "type" to "start")
                    "tieStop" -> notations.add("tied", null, "type" to "stop")
                    else -> throw IllegalArgumentException("Unsupported articulation $articulation")
                }
            }
        }
        tupletMark.split("_").filter { it.isNotEmpty() }.forEach { notations.add("tuplet", null, "type" to it) }
        if (xmlArticulations.isNotEmpty()) notations.add("articulations").also { parent -> xmlArticulations.forEach { parent.appendChild(it) } }
        if (ornaments.isNotEmpty()) notations.add("ornaments").also { parent -> ornaments.forEach { parent.appendChild(it) } }
    }

    private fun addSlurs(note: Element, slurs: String, number: Int) {
        val notations = note.find("notations") ?: note.add("notations")
        when (slurs) {
            EMPTY, "" -> Unit
            NO_NOTE -> logger.debug("Note without valid slur: {}", slurs)
            "slurStart" -> notations.add("slur", null, "type" to "start", "number" to number.toString())
            "slurStop" -> notations.add("slur", null, "type" to "stop", "number" to number.toString())
            "slurStart_slurStop" -> {
                notations.add("slur", null, "type" to "stop", "number" to number.toString())
                notations.add("slur", null, "type" to "start", "number" to number.toString())
            }
            else -> throw IllegalArgumentException("Unsupported slur $slurs")
        }
    }

    private fun measureRest(owner: Element, symbol: EncodedSymbol) = owner.element("note").apply {
        add("rest", null, "measure" to "yes")
        add("duration", maxOf(1, state.nominator.multiply(state.division).toIntTruncated()).toString())
        add("voice", "1")
        add("staff", staffOf(symbol).toString())
    }

    private fun forward(owner: Element, duration: Fraction) = owner.element("forward").apply {
        add("duration", maxOf(1, duration.multiply(state.division).toIntTruncated()).toString())
    }

    private fun backup(owner: Element, duration: Fraction): Element {
        require(duration > Fraction.ZERO) { "Backup duration must be positive" }
        return owner.element("backup").apply { add("duration", maxOf(1, duration.multiply(state.division).toIntTruncated()).toString()) }
    }
}

private fun staffOf(symbol: EncodedSymbol) = if (isLowerPosition(symbol.position)) 2 else 1

private fun xmlVoice(staff: Int, layer: Int) = (staff - 1) * VOICES_PER_STAFF + layer + 1

private fun hasNotes(measure: Element) = measure.find("note") != null

private fun barline(measure: Element, location: String) =
    measure.elements.firstOrNull { it.tagName == "barline" && it.getAttribute("location") == location } ?: measure.add("barline", null, "location" to location)

private fun addRepeat(rhythm: String, barline: Element) {
    if (barline.find("repeat") != null) return
    barline.add("repeat", null, "direction" to if (rhythm == "repeatStart") "forward" else "backward")
}

private fun addEnding(volta: EncodedSymbol, barline: Element, number: Int) {
    val type = when {
        volta.rhythm.startsWith("voltaStart") -> "start"
        volta.rhythm.startsWith("voltaStop") -> "stop"
        else -> "discontinue"
    }
    barline.add("ending", null, "type" to type, "number" to number.toString())
}

private fun addClef(clef: EncodedSymbol, attributes: Element) {
    val signAndLine = clef.rhythm.split("_")[1]
    attributes.add("clef", null, "number" to staffOf(clef).toString()).apply {
        add("sign", signAndLine.substring(0, 1))
        add("line", signAndLine.substring(1, 2))
        val octaves = octavesOfClef(clef.rhythm)
        if (octaves != 0) add("clef-octave-change", octaves.toString())
    }
}

private fun addDynamic(marking: EncodedSymbol, measure: Element) {
    val marks = mutableListOf<Element>()
    if (marking.rhythm.startsWith("dynamic")) {
        val name = marking.rhythm.split("_", limit = 2)[1]
        if (name != "empty") marks += measure.element(name)
    }
    if (marking.articulation.startsWith("dynamic")) {
        marks += measure.element("other-dynamics", marking.articulation.split("_", limit = 2)[1].replace("piu", "più"))
    }
    if (marks.isEmpty()) return
    val direction = measure.add("direction", null, "placement" to "below")
    val dynamics = direction.add("direction-type").add("dynamics")
    marks.forEach { dynamics.appendChild(it) }
    direction.add("staff", staffOf(marking).toString())
}

private fun addOctaveShift(shift: EncodedSymbol, measure: Element, state: ConversionState) {
    val staff = staffOf(shift)
    if ((state.shiftOctaves[staff] ?: 0) != 0) {
        addOctaveShiftDirection(measure, staff, "stop", "above")
        state.shiftOctaves[staff] = 0
    }
    if (shift.rhythm == OCTAVE_SHIFT_STOP) return
    val octaves = octavesOfShift(shift.rhythm)
    if (octaves == 0) return
    val (type, placement) = if (octaves > 0) "down" to "above" else "up" to "below"
    addOctaveShiftDirection(measure, staff, type, placement)
    state.shiftOctaves[staff] = octaves
}

private fun stopOctaveShifts(measure: Element, state: ConversionState) {
    state.shiftOctaves.forEach { (staff, octaves) -> if (octaves != 0) addOctaveShiftDirection(measure, staff, "stop", "above") }
    state.shiftOctaves = mutableMapOf()
}

private fun addOctaveShiftDirection(measure: Element, staff: Int, type: String, placement: String) {
    val direction = measure.element("direction", null, "placement" to placement)
    direction.add("direction-type").add("octave-shift", null, "type" to type, "size" to "8", "number" to staff.toString())
    direction.add("staff", staff.toString())
    val children = measure.elements
    if (type == "stop" && children.isNotEmpty() && children.last().tagName == "backup") measure.insertAt(children.size - 1, direction) else measure.appendChild(direction)
}

private fun multiMeasureRest(symbol: EncodedSymbol, attributes: Element): Int {
    if (attributes.find("measure-style") != null) return 0
    val duration = symbol.rhythm.split("_")[1].replace("m", "").toInt()
    attributes.add("measure-style").add("multiple-rest", duration.toString())
    return duration
}

private fun groupNotes(notes: List<EncodedSymbol>): Map<Fraction, List<EncodedSymbol>> {
    val maxDuration = notes.maxOf { it.duration.fraction }
    val groups = LinkedHashMap<Fraction, MutableList<EncodedSymbol>>()
    notes.forEach { note ->
        val fraction = when {
            "G" in note.rhythm -> Fraction.ZERO
            note.duration.fraction.numerator == 0 -> maxDuration
            else -> note.duration.fraction
        }
        groups.getOrPut(fraction) { mutableListOf() } += note
    }
    return groups.toSortedMap()
}

private fun timeSignatureLength(time: TimeSignature?): Fraction? {
    time ?: return null
    val beats = time.first.toIntOrNull() ?: return null
    val beatType = time.second.toIntOrNull() ?: return null
    if (beatType == 0) return null
    return Fraction.of(beats, beatType).takeIf { it > Fraction.ZERO }
}

private fun startsWithTimeSignature(groups: List<SymbolChord>): Boolean {
    for (group in groups) {
        val rhythm = group.symbols.firstOrNull()?.rhythm ?: ""
        if (rhythm.startsWith("timeSignature")) return true
        if (rhythm.startsWith("note") || rhythm.startsWith("rest") || rhythm.startsWith("measureRest") || group.isBarline) return false
    }
    return false
}

private fun commonDivision(durations: List<Fraction>): Int {
    val denominators = durations.filter { it > Fraction.ZERO }.map { it.denominator }
    if (denominators.isEmpty()) return 1
    return denominators.drop(1).fold(denominators[0]) { common, d -> common / gcd(common, d) * d }
}

private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) kotlin.math.abs(a) else gcd(b, a % b)

private fun divisionAndNominators(groups: List<SymbolChord>): Pair<Int, List<Fraction>> {
    val durations = mutableListOf(Fraction.of(1, 4))
    var durationInMeasure = Fraction.ZERO
    val segments = mutableListOf(mutableListOf<Fraction>())
    var hasTimeSignature = false
    var clock = GroupClock()
    for (chord in groups) {
        if (chord.symbols.isNotEmpty() && chord.symbols[0].rhythm.startsWith("timeSignature")) {
            if (durationInMeasure > Fraction.ZERO) {
                segments.last() += durationInMeasure
                durationInMeasure = Fraction.ZERO
                clock = GroupClock()
            }
            if (hasTimeSignature) segments += mutableListOf<Fraction>()
            hasTimeSignature = true
        } else if (chord.isBarline && durationInMeasure > Fraction.ZERO) {
            segments.last() += durationInMeasure
            durationInMeasure = Fraction.ZERO
            clock = GroupClock()
        } else {
            val timed = chord.symbols.filter { it.isNoteOrRest }.map { it.duration.fraction }.filter { it > Fraction.ZERO }
            durations += timed
            if (timed.isNotEmpty() && !(chord.symbols.size == 1 && chord.symbols[0].rhythm.endsWith("m"))) {
                clock.waitForVoices(chord)
                val end = clock.time.add(timed.max())
                clock.advance(chord)
                durationInMeasure = maxOf(durationInMeasure, end)
            }
        }
    }
    if (durationInMeasure > Fraction.ZERO) segments.last() += durationInMeasure
    val allMeasures = segments.flatten()
    if (allMeasures.isEmpty()) return commonDivision(durations) to List(segments.size) { Fraction.ONE }
    var following = allMeasures.medianFraction()
    val nominators = segments.reversed().map { segment ->
        if (segment.isNotEmpty()) following = segment.medianFraction()
        following
    }.reversed()
    return commonDivision(durations) to nominators
}

private fun splitMixedChord(symbols: List<EncodedSymbol>): List<List<EncodedSymbol>> {
    val notes = symbols.filter { it.isNoteOrRest }
    if (notes.isEmpty() || notes.size == symbols.size) return listOf(symbols)
    val others = symbols.filter { !it.isNoteOrRest }
    val after = others.filter { it.isBarlineOrRepeat }
    val before = LinkedHashMap<String, MutableList<EncodedSymbol>>()
    others.filter { it !in after }.forEach { before.getOrPut(it.rhythm.split("_")[0].split("/")[0]) { mutableListOf() } += it }
    val ordered = before.keys.sortedBy { kind -> BEFORE_NOTES_ORDER.indexOf(kind).takeIf { it >= 0 } ?: BEFORE_NOTES_ORDER.size }
    return ordered.map { before.getValue(it) } + listOf(notes) + after.map { listOf(it) }
}

private fun splitMixedChords(groups: List<SymbolChord>): List<SymbolChord> {
    val result = mutableListOf<SymbolChord>()
    var splitBarline = false
    for (group in groups) {
        val parts = splitMixedChord(group.symbols)
        if (splitBarline && group.isBarline && parts.size == 1) {
            if (group.symbols[0].rhythm == "barline" && result.last().symbols[0].rhythm != "barline") {
                splitBarline = false
                continue
            }
            result.removeAt(result.lastIndex)
        }
        parts.forEach { part -> result += SymbolChord(part, if (part[0].isNoteOrRest) group.tupletMarks else mutableMapOf()) }
        splitBarline = parts.size > 1 && result.last().isBarline
    }
    return result
}

private object TupletParser {
    fun parse(groups: List<SymbolChord>): List<SymbolChord> {
        for (measure in splitIntoMeasures(groups)) {
            val positions = measure.flatMap { group -> group.symbols.filter(::isTuplet).map { it.position } }.toSortedSet()
            positions.forEach { addTuplets(measure, it) }
        }
        return groups
    }

    private fun isTuplet(symbol: EncodedSymbol) = symbol.isNoteOrRest && "G" !in symbol.rhythm && symbol.duration.isTuplet

    private fun durationAt(group: SymbolChord, position: String): SymbolDuration? {
        val timed = group.symbols.filter { it.position == position && it.isNoteOrRest && "G" !in it.rhythm }
        return (timed.firstOrNull(::isTuplet) ?: timed.firstOrNull())?.duration
    }

    private fun splitIntoMeasures(groups: List<SymbolChord>): List<List<SymbolChord>> {
        val measures = mutableListOf<List<SymbolChord>>()
        var current = mutableListOf<SymbolChord>()
        for (group in groups) {
            current += group
            if (group.isBarline) {
                measures += current
                current = mutableListOf()
            }
        }
        if (current.isNotEmpty()) measures += current
        return measures
    }

    private fun startTimes(groups: List<SymbolChord>): List<Fraction> {
        val clock = GroupClock()
        return groups.map { group ->
            if (group.symbols.any { it.isNoteOrRest }) {
                clock.waitForVoices(group)
                clock.time.also { clock.advance(group) }
            } else {
                clock.time
            }
        }
    }

    private fun addTuplets(groups: List<SymbolChord>, position: String) {
        val times = startTimes(groups)
        var run = mutableListOf<Triple<Int, Fraction, Fraction>>()
        var format = 1 to 1
        groups.forEachIndexed { index, group ->
            val duration = durationAt(group, position) ?: return@forEachIndexed
            val current = duration.actualNotes to duration.normalNotes
            if (current != format && run.isNotEmpty()) {
                markRun(groups, position, run)
                run = mutableListOf()
            }
            format = current
            if (current.first != current.second) run += Triple(index, times[index], duration.fraction)
        }
        if (run.isNotEmpty()) markRun(groups, position, run)
    }

    private fun markRun(groups: List<SymbolChord>, position: String, run: List<Triple<Int, Fraction, Fraction>>) {
        val byLength = split(run) { _, total, _ -> total }
        val byGrid = split(run) { _, _, end -> end }
        val (tuplets, leftOver) = if (byLength.second.size <= byGrid.second.size) byLength else byGrid
        val all = if (leftOver.isNotEmpty()) tuplets + (leftOver.first() to leftOver.last()) else tuplets
        all.forEach { (first, last) ->
            if (first == last) {
                groups[first].tupletMarks[position] = "start_stop"
            } else {
                groups[first].tupletMarks[position] = "start"
                groups[last].tupletMarks[position] = "stop"
            }
        }
    }

    private fun split(run: List<Triple<Int, Fraction, Fraction>>, measure: (Fraction, Fraction, Fraction) -> Fraction): Pair<List<Pair<Int, Int>>, List<Int>> {
        val tuplets = mutableListOf<Pair<Int, Int>>()
        var pending = mutableListOf<Int>()
        var total = Fraction.ZERO
        for ((index, start, duration) in run) {
            pending += index
            total = total.add(duration)
            val denominator = measure(start, total, start.add(duration)).denominator
            if (denominator > 0 && denominator and (denominator - 1) == 0) {
                tuplets += pending.first() to index
                pending = mutableListOf()
                total = Fraction.ZERO
            }
        }
        return tuplets to pending
    }
}

private class TimedNoteEvent(val staff: Int, val start: Int, val end: Int, val notes: MutableList<Element>)

private fun isSecondVoice(note: Element) = note.getAttribute(POSITION_ATTRIBUTE).endsWith("2")

private fun rebalanceMeasureVoices(measure: Element) {
    val events = mutableListOf<TimedNoteEvent>()
    var currentTime = 0
    var lastNoteStart = 0
    for (child in measure.elements) {
        when (child.tagName) {
            "backup" -> child.find("duration")?.let { currentTime -= it.textContent.toInt() }
            "forward" -> child.find("duration")?.let { currentTime += it.textContent.toInt() }
            "note" -> {
                val duration = child.find("duration")?.textContent?.toInt() ?: 0
                val staff = child.find("staff")?.textContent?.toInt() ?: 1
                val isChordTone = child.find("chord") != null
                val start = if (isChordTone) lastNoteStart else currentTime
                val end = start + duration
                val last = events.lastOrNull()
                if (isChordTone && last != null && last.staff == staff && last.start == start && last.end == end) {
                    last.notes += child
                } else if (isChordTone) {
                    events += TimedNoteEvent(staff, start, end, mutableListOf(child))
                } else {
                    lastNoteStart = start
                    currentTime += duration
                    events += TimedNoteEvent(staff, start, end, mutableListOf(child))
                }
            }
        }
    }
    events.groupBy { it.staff }.forEach { (staff, staffEvents) ->
        var active = listOf<Pair<Int, Int>>()
        staffEvents.sortedWith(compareBy({ it.start }, { it.end })).forEach { event ->
            active = active.filter { (activeEnd, _) -> activeEnd > event.start }
            val used = active.map { it.second }.toSet()
            var voice = if (isSecondVoice(event.notes[0])) 2 else 1
            if (voice in used) {
                voice = 1
                while (voice in used) voice++
            }
            active = active + (event.end to voice)
            val xmlVoice = xmlVoice(staff, voice - 1).toString()
            event.notes.forEach { note -> note.find("voice")?.textContent = xmlVoice }
        }
    }
}

private fun addStems(part: Element) {
    for (measure in part.children("measure")) {
        val notes = measure.children("note").filter { it.find("rest") == null }
        for (staff in notes.map { it.findText("staff", "1") }.toSet()) {
            val onStaff = notes.filter { it.findText("staff", "1") == staff }
            val voices = onStaff.filter { it.hasAttribute(POSITION_ATTRIBUTE) && it.getAttribute(POSITION_ATTRIBUTE).isNotEmpty() }.map(::isSecondVoice).toSet()
            if (voices.size < 2) continue
            onStaff.forEach { note ->
                val stem = note.element("stem", if (isSecondVoice(note)) "down" else "up")
                val staffElement = note.find("staff")
                note.insertAt(if (staffElement != null) note.indexOf(staffElement) else note.elements.size, stem)
            }
        }
    }
}

private fun groupIntoEvents(measure: Element): List<List<Element>> {
    val events = mutableListOf<MutableList<Element>>()
    measure.children("note").forEach { note ->
        if (note.find("chord") != null && events.isNotEmpty()) events.last() += note else events += mutableListOf(note)
    }
    return events
}

private fun notePitch(note: Element): String? {
    val pitch = note.find("pitch") ?: return null
    return listOf("step", "alter", "octave").joinToString("|") { pitch.findText(it) }
}

private fun slurOf(note: Element, type: String): Pair<Element, Element>? {
    for (notation in note.children("notations")) {
        for (slur in notation.children("slur")) {
            if (slur.getAttribute("type") == type) return slur to notation
        }
    }
    return null
}

private fun addTie(note: Element, type: String, notation: Element) {
    val tie = note.element("tie", null, "type" to type)
    val duration = note.find("duration")
    note.insertAt(if (duration != null) note.indexOf(duration) + 1 else 0, tie)
    notation.insertAt(0, note.element("tied", null, "type" to type))
}

private fun hasTie(note: Element, type: String) = note.children("tie").any { it.getAttribute("type") == type }

private fun notationsOf(note: Element) = note.find("notations") ?: note.add("notations")

private fun convertTies(part: Element) {
    val previous = mutableMapOf<Pair<String, String>, List<Element>>()
    for (measure in part.children("measure")) {
        for (event in groupIntoEvents(measure)) {
            val first = event[0]
            val key = first.findText("staff", "1") to first.findText("voice", "1")
            val before = previous[key]
            previous[key] = event
            if (before != null) tieEvent(before, event)
        }
    }
}

private fun tieEvent(before: List<Element>, after: List<Element>) {
    var isTied = false
    for (startNote in before) {
        val begins = slurOf(startNote, "start") ?: continue
        val pitch = notePitch(startNote) ?: continue
        for (stopNote in after) {
            if (notePitch(stopNote) != pitch) continue
            val ends = slurOf(stopNote, "stop") ?: continue
            begins.second.removeChild(begins.first)
            ends.second.removeChild(ends.first)
            addTie(startNote, "start", begins.second)
            addTie(stopNote, "stop", ends.second)
            isTied = true
            break
        }
    }
    if (isTied) tieRestOfChord(before, after)
}

private fun tieRestOfChord(before: List<Element>, after: List<Element>) {
    if (before.size < 2 || after.size < 2) return
    val afterByPitch = after.mapNotNull { note -> notePitch(note)?.let { it to note } }.toMap()
    for (startNote in before) {
        val pitch = notePitch(startNote) ?: continue
        val stopNote = afterByPitch[pitch] ?: continue
        if (hasTie(startNote, "start") || hasTie(stopNote, "stop")) continue
        addTie(startNote, "start", notationsOf(startNote))
        addTie(stopNote, "stop", notationsOf(stopNote))
    }
}

private data class Moment(val measure: Int, val time: Int) : Comparable<Moment> {
    override fun compareTo(other: Moment) = compareValuesBy(this, other, { it.measure }, { it.time })
}

private class BeamEvent(val notes: List<Element>, val position: String, val isGrace: Boolean, val state: String?, val start: Moment, val end: Moment)

private fun beamCount(note: Element) = BEAMS_PER_TYPE[note.findText("type")] ?: 0

private fun noteTimes(measure: Element): Map<Element, Pair<Int, Int>> {
    val result = HashMap<Element, Pair<Int, Int>>()
    var time = 0
    var lastStart = 0
    for (child in measure.elements) {
        when (child.tagName) {
            "backup" -> time -= child.findText("duration", "0").toInt()
            "forward" -> time += child.findText("duration", "0").toInt()
            "note" -> {
                val duration = child.findText("duration", "0").toInt()
                if (child.find("chord") != null) {
                    result[child] = lastStart to lastStart + duration
                } else {
                    lastStart = time
                    result[child] = time to time + duration
                    time += duration
                }
            }
        }
    }
    return result
}

private fun beamEvents(part: Element): List<BeamEvent> {
    val events = mutableListOf<BeamEvent>()
    part.children("measure").forEachIndexed { measureNo, measure ->
        val times = noteTimes(measure)
        for (event in groupIntoEvents(measure)) {
            val first = event[0]
            val position = first.getAttribute(POSITION_ATTRIBUTE).takeIf { first.hasAttribute(POSITION_ATTRIBUTE) }
            var state: String? = null
            for (note in event) {
                for (beam in note.children("beam")) {
                    if (beam.getAttribute("number") == "1" && state == null) state = beam.textContent
                    note.removeChild(beam)
                }
            }
            if (position == null || first.find("rest") != null) continue
            val (start, end) = times.getValue(first)
            events += BeamEvent(event, position, first.find("grace") != null, state, Moment(measureNo, start), Moment(measureNo, end))
        }
    }
    return events.withIndex().sortedWith(compareBy({ it.value.start }, { it.index })).map { it.value }
}

private fun convertBeams(part: Element) {
    val openGroups = LinkedHashMap<Pair<String, Boolean>, MutableList<BeamEvent>>()
    val groups = mutableListOf<List<BeamEvent>>()
    fun close(key: Pair<String, Boolean>) {
        val group = openGroups.remove(key) ?: return
        if (group.size >= 2) groups += group
    }
    for (event in beamEvents(part)) {
        val key = event.position to event.isGrace
        if (event.state == null || beamCount(event.notes[0]) == 0) {
            close(key)
            continue
        }
        if (event.state == "begin") {
            close(key)
        } else if (key !in openGroups) {
            val staff = event.notes[0].findText("staff", "1")
            val candidates = openGroups.filter { (otherKey, group) ->
                otherKey.second == event.isGrace && group.last().notes[0].findText("staff", "1") != staff && group.last().end <= event.start
            }.keys
            val other = candidates.singleOrNull()
            if (other != null) {
                val group = openGroups.getValue(other)
                val voice = group[0].notes[0].findText("voice", "1")
                event.notes.forEach { note -> note.find("voice")?.textContent = voice }
                group += event
                if (event.state == "end") close(other)
                continue
            }
        }
        openGroups.getOrPut(key) { mutableListOf() } += event
        if (event.state == "end") close(key)
    }
    openGroups.keys.toList().forEach(::close)
    groups.forEach { group -> writeBeams(group.map { it.notes }) }
}

private fun writeBeams(group: List<List<Element>>) {
    val counts = group.map { beamCount(it[0]) }
    group.forEachIndexed { index, event ->
        val note = event[0]
        val staff = note.find("staff")
        var position = if (staff != null) note.indexOf(staff) + 1 else note.elements.size
        for (level in 1..counts[index]) {
            val hasPrevious = index > 0 && counts[index - 1] >= level
            val hasNext = index < group.lastIndex && counts[index + 1] >= level
            val type = when {
                hasPrevious && hasNext -> "continue"
                hasPrevious -> "end"
                hasNext -> "begin"
                index == 0 -> "forward hook"
                else -> "backward hook"
            }
            note.insertAt(position, note.element("beam", type, "number" to level.toString()))
            position += 1
        }
    }
}
