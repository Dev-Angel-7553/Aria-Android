package ru.muz.yandex

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamUrlTest {
    @Test
    fun `tag based signing response produces official direct link`() {
        val xml = """<download-info><host>storage.example</host><path>/abc/file.mp3</path><ts>123</ts><s>salt</s></download-info>"""
        assertEquals(
            "https://storage.example/get-mp3/40daf1dcc7704611f6906a378e2a3046/123/abc/file.mp3",
            buildDirectStreamUrl(xml),
        )
    }

    @Test
    fun `attribute response remains supported`() {
        val xml = """<download-info host="storage.example" path="/abc/file.mp3" ts="123" s="salt"/>"""
        assertEquals(
            "https://storage.example/get-mp3/40daf1dcc7704611f6906a378e2a3046/123/abc/file.mp3",
            buildDirectStreamUrl(xml),
        )
    }
}
