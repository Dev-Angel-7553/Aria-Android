package ru.muz.scrobble

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class ScrobbleApiTest {
    private val account = ScrobbleAccount(ScrobbleService.LASTFM, name = "user", token = "session", apiKey = "key", secret = "secret")
    private val play = ScrobblePlay("play", ScrobbleTrack("1", "Song & Song", "Tyler, The Creator", 120000, artists = listOf("Tyler, The Creator", "Other")), 1700000000)
    private fun api(code: Int, body: String, inspect: (Request) -> Unit = {}): ScrobbleApi = ScrobbleApi(
        OkHttpClient.Builder().addInterceptor { chain ->
            inspect(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("fixture").body(body.toResponseBody()).build()
        }.build())

    @Test fun signaturesSortNamesAndExcludeTransportParameters() {
        val params = linkedMapOf("z" to "3", "a" to "1", "format" to "json", "callback" to "ignored", "api_sig" to "old")
        assertEquals("20787e75aa414463da465bf95d45d06d", ScrobbleApi.signature(params, "secret"))
    }

    @Test fun listenBrainzOmitsTimeForNowPlayingAndPreservesCredits() {
        val now = ScrobbleApi.listenBrainzPayload(play, true)
        val entry = now["payload"]!!.jsonArray.single().jsonObject
        assertNull(entry["listened_at"])
        assertEquals("Tyler, The Creator", entry["track_metadata"]!!.jsonObject["artist_name"]!!.jsonPrimitive.content)
        val single = ScrobbleApi.listenBrainzPayload(play, false)
        assertEquals(play.startedAt, single["payload"]!!.jsonArray.single().jsonObject["listened_at"]!!.jsonPrimitive.long)
    }

    @Test fun onlyAcknowledgedScrobblesAreAccepted() = runBlocking {
        assertEquals(ScrobbleApi.Delivery.Accepted, api(200, """{"scrobbles":{"@attr":{"accepted":"1","ignored":"0"}}}""").deliver(account, play, false))
        assertEquals(ScrobbleApi.Delivery.Retry, api(200, "{}").deliver(account, play, false))
        assertTrue(api(200, """{"scrobbles":{"@attr":{"accepted":"0","ignored":"1"}}}""").deliver(account, play, false) is ScrobbleApi.Delivery.Rejected)
    }

    @Test fun authFailuresAndTransientErrorsHaveDifferentPolicies() = runBlocking {
        assertEquals(ScrobbleApi.Delivery.Unauthorized, api(200, """{"error":9}""").deliver(account, play, false))
        assertEquals(ScrobbleApi.Delivery.Retry, api(429, "{}").deliver(account, play, false))
        assertEquals(ScrobbleApi.Delivery.Retry, api(503, "{}").deliver(account, play, false))
        assertTrue(api(400, "{}").deliver(account, play, false) is ScrobbleApi.Delivery.Rejected)
    }

    @Test fun tokenValidationUsesAnAuthorizationHeaderAndChecksValidity() = runBlocking {
        val client = api(200, """{"valid":true,"user_name":"listener"}""") { request ->
            assertEquals("Token personal", request.header("Authorization"))
            assertNull(request.url.query)
        }
        assertEquals("listener", client.listenBrainzUser("personal"))
        assertTrue(runCatching { api(200, """{"valid":false}""").listenBrainzUser("bad") }.isFailure)
    }
}
