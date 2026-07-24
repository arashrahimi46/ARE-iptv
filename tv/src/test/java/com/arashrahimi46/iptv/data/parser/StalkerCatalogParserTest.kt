package com.arashrahimi46.iptv.data.parser

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Fixture-driven coverage of the pure Stalker catalog parsers (the fragile portal-JSON mapping),
 * exercised without a live portal — the same "no probing providers from the shell" rule as Xtream.
 * Robolectric-backed because the parsers use `org.json`, an unmocked stub on a plain JVM test.
 */
@RunWith(RobolectricTestRunner::class)
class StalkerCatalogParserTest {

    @Test
    fun `parses genres and drops the synthetic all pseudo-genre`() {
        val arr = JSONArray(
            """[{"id":"*","title":"All"},{"id":"1","title":"News"},{"id":"2","name":"Sports"}]""",
        )
        val cats = parseStalkerCategories(arr)
        assertEquals(listOf("News", "Sports"), cats.map { it.title })
        assertEquals(listOf("1", "2"), cats.map { it.id })
    }

    @Test
    fun `parses channels, keeps cmd, skips cmd-less rows`() {
        val data = JSONArray(
            """[
              {"id":"10","name":"BBC One","number":"101","cmd":"ffmpeg http://p/live/10","logo":"l.png","xmltv_id":"bbc1","tv_genre_id":"1"},
              {"id":"11","name":"No Cmd"}
            ]""",
        )
        val channels = parseStalkerChannels(data)
        assertEquals(1, channels.size)
        val c = channels.single()
        assertEquals("BBC One", c.name)
        assertEquals("ffmpeg http://p/live/10", c.cmd)
        assertEquals("bbc1", c.xmltvId)
        assertEquals("1", c.categoryId)
        assertEquals("101", c.number)
    }

    @Test
    fun `vod falls back to id for missing cmd`() {
        val items = listOf(
            JSONObject("""{"id":"5","name":"Dune","category_id":"3","screenshot_uri":"d.jpg","year":"2021","cmd":"/media/5.mpg"}"""),
            JSONObject("""{"id":"6","o_name":"Arrival","category_id":"3"}"""),
        )
        val vod = parseStalkerVod(items)
        assertEquals("Dune", vod[0].name)
        assertEquals("/media/5.mpg", vod[0].cmd)
        assertEquals("2021", vod[0].year)
        assertEquals("Arrival", vod[1].name)
        assertEquals("6", vod[1].cmd) // fell back to id
    }

    @Test
    fun `series parses parent rows`() {
        val items = listOf(
            JSONObject("""{"id":"20","name":"The Wire","category_id":"9","cover":"w.jpg"}"""),
        )
        val series = parseStalkerSeries(items)
        assertEquals("The Wire", series.single().name)
        assertEquals("20", series.single().id)
    }

    @Test
    fun `episodes expand the per-season series array sharing the season cmd`() {
        val seasons = listOf(
            JSONObject("""{"id":"1","name":"Season 2","cmd":"/media/s2","series":[1,2,3]}"""),
        )
        val eps = parseStalkerEpisodes(seasons)
        assertEquals(3, eps.size)
        assertTrue(eps.all { it.season == 2 && it.cmd == "/media/s2" })
        assertEquals(listOf(1, 2, 3), eps.map { it.episode })
    }

    @Test
    fun `a season with no episode array becomes a single entry`() {
        val seasons = listOf(JSONObject("""{"id":"1","name":"Miniseries","cmd":"/media/m"}"""))
        val eps = parseStalkerEpisodes(seasons)
        assertEquals(1, eps.size)
        assertEquals(1, eps.single().episode)
        assertEquals("/media/m", eps.single().cmd)
    }
}
