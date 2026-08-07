package com.soundlog.app.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class SongKeyNormalizerTest {

    @Test
    fun testNormalizeText_removesSpecialCharactersAndSpaces() {
        val input = "  BTS (방탄소년단) - Dynamite!!!  "
        val normalized = SongKeyNormalizer.normalizeText(input)
        assertEquals("bts방탄소년단dynamite", normalized)
    }

    @Test
    fun testGenerateSongKey_combinesArtistAndTitle() {
        val artist = "IU (아이유)"
        val title = "Celebrity 🎉"
        val songKey = SongKeyNormalizer.generateSongKey(artist, title)
        assertEquals("iu아이유|celebrity", songKey)
    }
}
