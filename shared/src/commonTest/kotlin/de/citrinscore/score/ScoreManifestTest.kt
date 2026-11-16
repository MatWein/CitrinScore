package de.citrinscore.score

import de.citrinscore.serialization.AppJson
import kotlin.test.Test
import kotlin.test.assertEquals

class ScoreManifestTest {

    @Test
    fun `pdf pages without explicit index keep their document order`() {
        val json = """
            {"metadata":{"title":"Sonate"},"createdAt":"2026-01-01T00:00:00Z",
             "content":{"type":"pdf","path":"score.pdf","pages":[
               {"left":1.0,"top":2.0,"right":3.0,"bottom":4.0},
               {"left":5.0,"top":6.0,"right":7.0,"bottom":8.0}]}}
        """.trimIndent()

        val content = AppJson.decodeFromString<ScoreManifest>(json).content

        assertEquals(
            ScoreContent.Pdf("score.pdf", listOf(PdfPage(0, PageCrop(1f, 2f, 3f, 4f)), PdfPage(1, PageCrop(5f, 6f, 7f, 8f)))),
            content,
        )
    }

    @Test
    fun `reordered pdf pages survive a round trip`() {
        val content = ScoreContent.Pdf("score.pdf", listOf(PdfPage(2, PageCrop(0f, 0f, 1f, 1f)), PdfPage(0, PageCrop(0f, 0f, 2f, 2f))))

        assertEquals(content, AppJson.decodeFromString<ScoreContent>(AppJson.encodeToString<ScoreContent>(content)))
    }
}
