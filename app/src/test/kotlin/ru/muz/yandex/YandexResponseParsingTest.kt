package ru.muz.yandex

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class YandexResponseParsingTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `liked tracks are read from result library`() {
        val response = json.parseToJsonElement(
            """{"result":{"library":{"revision":7,"tracks":[{"id":"11","albumId":"2"}]}}}""",
        ).jsonObject

        assertEquals("11", response.likedTrackElements().single().jsonObject["id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `liked tracks are read from root library and direct fallback`() {
        val library = json.parseToJsonElement(
            """{"library":{"tracks":[{"id":"21"},{"id":"22"}]}}""",
        ).jsonObject
        val direct = json.parseToJsonElement(
            """{"result":{"likedTracks":[{"id":"31"}]}}""",
        ).jsonObject

        assertEquals(2, library.likedTrackElements().size)
        assertEquals("31", direct.likedTrackElements().single().jsonObject["id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `wave supports sequence and nested sequence responses`() {
        val direct = json.parseToJsonElement(
            """{"result":{"sequence":[{"track":{"id":"41"}}]}}""",
        ).jsonObject
        val nested = json.parseToJsonElement(
            """{"result":{"result":{"sequence":[{"track":{"id":"51"}},{"track":{"id":"52"}}]}}}""",
        ).jsonObject

        assertEquals(1, direct.waveTrackElements().size)
        assertEquals(2, nested.waveTrackElements().size)
    }
}
