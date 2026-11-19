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
                ScorePage("sources/a.pdf", 2, PageCrop(0f, 0f, 1f, 1f)),
                ScorePage("sources/b.jpg", 0, PageCrop(0f, 0f, 2f, 2f), Normalization("pages/c.png", 0.2f, listOf(EraserStroke(listOf(Offset(1f, 2f)), 3f)))),
            ),
        )

        assertEquals(content, AppJson.decodeFromString<ScoreContent>(AppJson.encodeToString(content)))
    }

    @Test
    fun `attachments are saved under their name with the stored extension`() {
        assertEquals("Aufnahme 1" to "m4a", Attachment("audio/1.m4a", "Aufnahme 1").let { it.baseName to it.extension })
        assertEquals("Fingersatz" to "pdf", Attachment("files/2.pdf", "Fingersatz.PDF").let { it.baseName to it.extension })
        assertEquals("notes" to "", Attachment("files/3", "notes").let { it.baseName to it.extension })
    }
}
