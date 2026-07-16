package com.metrolist.desktop.personalization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePersonalizationTest {
    @Test
    fun `recent searches lead and duplicate searches are removed`() {
        val seeds = HomePersonalization.seeds(
            PersonalizationSignals(
                recentSearches = listOf("Jiwa Yang Bersedih", "jiwa yang bersedih", "Adele"),
                likedArtists = listOf("Hindia"),
            ),
        )

        assertEquals(
            listOf("Jiwa Yang Bersedih", "Adele"),
            seeds.take(2).map(HomeSeed::query),
        )
        assertTrue(seeds.take(2).all { it.reason == HomeSeedReason.SEARCH })
    }

    @Test
    fun `strong artist signals are ranked after searches`() {
        val seeds = HomePersonalization.seeds(
            PersonalizationSignals(
                recentSearches = listOf("Indonesian pop"),
                likedArtists = listOf("Hindia", "Hindia"),
                recentlyPlayedArtists = listOf("NOAH", "Hindia"),
                libraryArtists = listOf("Tulus"),
            ),
        )

        assertEquals(listOf("Indonesian pop", "Hindia", "NOAH", "Tulus"), seeds.map(HomeSeed::query))
        assertEquals(HomeSeedReason.ARTIST, seeds[1].reason)
    }

    @Test
    fun `seed list is capped and ignores unknown artists`() {
        val seeds = HomePersonalization.seeds(
            PersonalizationSignals(
                recentSearches = listOf("one", "two", "three"),
                likedArtists = listOf("Unknown artist", "A", "B", "C"),
            ),
        )

        assertEquals(4, seeds.size)
        assertTrue(seeds.none { it.query.equals("Unknown artist", ignoreCase = true) })
    }

    @Test
    fun `home blend uses four personal and two discovery sections`() {
        val result = HomePersonalization.blend(
            personal = listOf("p1", "p2", "p3", "p4", "p5"),
            discovery = listOf("d1", "d2", "d3"),
        )

        assertEquals(listOf("p1", "p2", "p3", "p4", "d1", "d2"), result)
    }

    @Test
    fun `home blend falls back to discovery before preferences exist`() {
        val result = HomePersonalization.blend(
            personal = emptyList<String>(),
            discovery = listOf("d1", "d2", "d3", "d4", "d5", "d6", "d7"),
        )

        assertEquals(listOf("d1", "d2", "d3", "d4", "d5", "d6"), result)
    }
}
