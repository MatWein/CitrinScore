package de.citrinscore.library

import de.citrinscore.score.ScoreMetadata
import io.github.vinceglb.filekit.PlatformFile
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class LibrarySearchTest {

    private val folder = PlatformFile(File("library"))

    private fun score(title: String, composer: String = "", description: String = "", lastActivity: String? = null) = SearchEntry(
        LibraryItem.Score(PlatformFile(File(folder.file, "$title.citscore")), lastActivity?.let(Instant::parse), ScoreMetadata(title, composer, description)),
        folder,
    )

    private fun List<SearchEntry>.titles(query: String, sort: LibrarySort = LibrarySort.Title) = search(query, sort).map { it.item.displayName }

    @Test
    fun `every word has to occur in some field`() {
        val entries = listOf(score("Spring", composer = "Vivaldi"), score("Winter", composer = "Vivaldi"), score("Spring Song", composer = "Mendelssohn"))

        assertEquals(listOf("Spring"), entries.titles("vivaldi spring"))
        assertEquals(emptyList(), entries.titles("vivaldi summer"))
    }

    @Test
    fun `case, diacritics and punctuation do not matter`() {
        val entries = listOf(score("Übungen"), score("Violin Concerto - 1st Mov't."), score("Straße"))

        assertEquals(listOf("Übungen"), entries.titles("UBUNGEN"))
        assertEquals(listOf("Violin Concerto - 1st Mov't."), entries.titles("movt"))
        assertEquals(listOf("Straße"), entries.titles("strasse"))
    }

    @Test
    fun `title prefixes come first, then title, composer and description matches`() {
        val entries = listOf(
            score("Nocturne", description = "a quiet piece for the evening"),
            score("Lieder", composer = "Ludwig van Beethoven"),
            score("Für Elise von Beethoven"),
            score("Beethoven Sonata"),
        )

        assertEquals(listOf("Beethoven Sonata", "Für Elise von Beethoven", "Lieder"), entries.titles("beethoven"))
        assertEquals(listOf("Nocturne"), entries.titles("evening"))
    }

    @Test
    fun `equal matches keep folders first and the chosen order`() {
        val folderEntry = SearchEntry(LibraryItem.Folder(PlatformFile(File(folder.file, "Bach"))), folder)
        val entries = listOf(
            score("Bach Air", lastActivity = "2026-01-01T00:00:00Z"),
            score("Bach Arioso", lastActivity = "2026-05-01T00:00:00Z"),
            folderEntry,
        )

        assertEquals(listOf("Bach", "Bach Air", "Bach Arioso"), entries.titles("bach"))
        assertEquals(listOf("Bach", "Bach Arioso", "Bach Air"), entries.titles("bach", LibrarySort.RecentlyUsed))
    }

    @Test
    fun `matches are highlighted in the original text`() {
        val hit = listOf(score("Für Elise", composer = "Ludwig van Beethoven")).search("fur beethoven", LibrarySort.Title).single()

        assertEquals(Highlighted("Für Elise", listOf(0..2)), hit.title)
        assertEquals(Highlighted("Ludwig van Beethoven", listOf(11..19)), hit.detail)
    }

    @Test
    fun `title matches need no detail and long descriptions are cut around the match`() {
        val description = "This arrangement was written for a small ensemble.\nIt ends with a long and very quiet coda in the strings."
        val entries = listOf(score("Elegy", description = description))

        assertNull(entries.search("elegy", LibrarySort.Title).single().detail)
        val detail = entries.search("coda", LibrarySort.Title).single().detail!!
        assertEquals("…long and very quiet coda in the strings.", detail.text)
        assertEquals("coda", detail.text.substring(detail.matches.single()))
    }
}
