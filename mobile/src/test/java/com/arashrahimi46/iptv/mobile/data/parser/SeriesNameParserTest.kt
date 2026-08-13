package com.arashrahimi46.iptv.mobile.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeriesNameParserTest {

    @Test
    fun `parses standard SxxExx with episode title`() {
        val info = parseSeriesEpisode("Breaking Bad S01E02 - Cat's in the Bag")!!
        assertEquals("Breaking Bad", info.seriesName)
        assertEquals(1, info.season)
        assertEquals(2, info.episode)
        assertEquals("Cat's in the Bag", info.episodeTitle)
    }

    @Test
    fun `parses spaced and separated SxxExx variants`() {
        assertEquals("Money Heist", parseSeriesEpisode("Money Heist S01 E01")!!.seriesName)
        assertEquals("The Office", parseSeriesEpisode("The Office - s2e10")!!.seriesName)
        assertEquals(2, parseSeriesEpisode("The Office - s2e10")!!.season)
        assertEquals(10, parseSeriesEpisode("The Office - s2e10")!!.episode)
    }

    @Test
    fun `parses NxM form`() {
        val info = parseSeriesEpisode("Lost 3x08")!!
        assertEquals("Lost", info.seriesName)
        assertEquals(3, info.season)
        assertEquals(8, info.episode)
    }

    @Test
    fun `parses spelled out season episode`() {
        val info = parseSeriesEpisode("Dark Season 2 Episode 5")!!
        assertEquals("Dark", info.seriesName)
        assertEquals(2, info.season)
        assertEquals(5, info.episode)
    }

    @Test
    fun `synthesizes an episode title when none is present`() {
        assertEquals("Episode 2", parseSeriesEpisode("Breaking Bad S01E02")!!.episodeTitle)
    }

    @Test
    fun `returns null when no season episode marker is present`() {
        assertNull(parseSeriesEpisode("The Matrix"))
        assertNull(parseSeriesEpisode("Planet Earth Documentary"))
    }

    @Test
    fun `does not misread a resolution as a season marker`() {
        // 1920x1080 -- four/three-digit numbers must not be captured as season/episode.
        assertNull(parseSeriesEpisode("Nature 1920x1080"))
    }

    @Test
    fun `returns null when the series name would be empty`() {
        assertNull(parseSeriesEpisode("S01E02"))
    }

    @Test
    fun `collapses all seasons of a show to one series name`() {
        val s3 = parseSeriesEpisode("12 Monkeys 2015 (Persian Series Foreign) S03 12 Monkeys S03E05")!!
        val s4 = parseSeriesEpisode("12 Monkeys 2015 (Persian Series Foreign) S04 12 Monkeys S04E01")!!
        // Same series name -> both seasons group under one entry.
        assertEquals(s3.seriesName, s4.seriesName)
        // Cut at the first (mid-name) season token also drops the trailing duplicate title.
        assertEquals("12 Monkeys 2015 (Persian Series Foreign)", s4.seriesName)
        // But each episode keeps its real season number.
        assertEquals(3, s3.season)
        assertEquals(5, s3.episode)
        assertEquals(4, s4.season)
        assertEquals(1, s4.episode)
    }

    @Test
    fun `mid-name season token does not leak into the series name`() {
        val info = parseSeriesEpisode("Money Heist S02 The Heist S02E03")!!
        assertEquals("Money Heist", info.seriesName)
        assertEquals(2, info.season)
        assertEquals(3, info.episode)
    }
}
