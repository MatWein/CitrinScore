package de.citrinscore.score

import androidx.compose.ui.geometry.Offset
import de.citrinscore.score.scan.EraserStroke
import de.citrinscore.serialization.AppJson
import kotlin.test.Test
import kotlin.test.assertEquals

class ScoreManifestTest {

    @Test
    fun `pages survive a round trip`() {
        val content: ScoreContent = ScoreContent.Pages(
            listOf(
                ScorePage("sources/a.pdf", 2, PageCrop(0f, 0f, 1f, 1f), Normalization("pages/a.png", 0.15f)),
                ScorePage("sources/b.jpg", 0, PageCrop(0f, 0f, 2f, 2f), Normalization("pages/c.png", 0.2f, listOf(EraserStroke(listOf(Offset(1f, 2f)), 3f)))),
            ),
        )

        assertEquals(content, AppJson.decodeFromString<ScoreContent>(AppJson.encodeToString(content)))
    }

    @Test
    fun `pages offer the original and the black and white display and the recognized music when present`() {
        val part = ScorePart("parts/a", "", ScoreContent.Pages(emptyList()))
        val recognized = part.copy(recognition = Recognition(), display = PartDisplay.MusicXml)

        assertEquals(listOf(PartDisplay.Original, PartDisplay.Normalized) to PartDisplay.Original, part.displays to part.shownDisplay)
        assertEquals(PartDisplay.MusicXml, recognized.shownDisplay)
        assertEquals(PartDisplay.Original, recognized.copy(recognition = null).shownDisplay)
        assertEquals(listOf(PartDisplay.MusicXml), ScorePart("parts/b", "", ScoreContent.MusicXml("score.mxl")).displays)
    }

    @Test
    fun `attachments are saved under their name with the stored extension`() {
        assertEquals("Aufnahme 1" to "m4a", Attachment("audio/1.m4a", "Aufnahme 1").let { it.baseName to it.extension })
        assertEquals("Fingersatz" to "pdf", Attachment("files/2.pdf", "Fingersatz.PDF").let { it.baseName to it.extension })
        assertEquals("notes" to "", Attachment("files/3", "notes").let { it.baseName to it.extension })
    }
}
