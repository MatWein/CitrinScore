package de.citrinscore.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NameValidationTest {

    @Test
    fun `blank names are rejected`() {
        assertEquals(NameError.Empty, validateName("   ", emptyList()))
    }

    @Test
    fun `forbidden characters are rejected`() {
        listOf("a/b", "a\\b", "a:b", "a*b", "a?b", "a\"b", "a<b", "a>b", "a|b", "..").forEach {
            assertEquals(NameError.InvalidCharacters, validateName(it, emptyList()), it)
        }
    }

    @Test
    fun `names with the score extension are rejected`() {
        assertEquals(NameError.ReservedExtension, validateName("Etude.CitScore ", emptyList()))
    }

    @Test
    fun `taken names are rejected case insensitively`() {
        assertEquals(NameError.AlreadyExists, validateName(" Etude ", listOf("etude")))
    }

    @Test
    fun `valid names are accepted`() {
        assertNull(validateName("Für Elise – Op. 59", listOf("Etude")))
    }
}
