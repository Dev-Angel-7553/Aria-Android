package ru.muz.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {
    @Test
    fun comparesNumericallyNotAsText() {
        assertTrue(AppUpdate.isNewer("0.19", "0.18"))
        assertTrue(AppUpdate.isNewer("0.100", "0.99"))
        assertTrue(AppUpdate.isNewer("1.0", "0.99.9"))
        assertFalse(AppUpdate.isNewer("0.18", "0.18"))
        assertFalse(AppUpdate.isNewer("0.18.0", "0.18"))
        assertFalse(AppUpdate.isNewer("v0.17", "0.18"))
        assertTrue(AppUpdate.isNewer("0.19-beta", "0.18"))
    }

    @Test
    fun parsesLatestRelease() {
        val body = """
            {"tag_name":"v0.19","draft":false,"prerelease":false,"html_url":"https://github.com/x/releases/tag/v0.19",
             "body":"Что нового","assets":[
               {"name":"notes.txt","browser_download_url":"https://x/notes.txt","size":3},
               {"name":"Aria-0.19.apk","browser_download_url":"https://x/Aria-0.19.apk","size":123}
             ]}
        """.trimIndent()
        val release = AppUpdate.parseLatest(body)!!
        assertEquals("0.19", release.version)
        assertEquals("https://x/Aria-0.19.apk", release.apkUrl)
        assertEquals(123L, release.apkSize)
        assertEquals("Что нового", release.notes)
    }

    @Test
    fun ignoresReleasesWithoutApkAndBrokenJson() {
        assertNull(AppUpdate.parseLatest("""{"tag_name":"v1","assets":[]}"""))
        assertNull(AppUpdate.parseLatest("""{"tag_name":"v1","prerelease":true,"assets":[{"name":"a.apk","browser_download_url":"u"}]}"""))
        assertNull(AppUpdate.parseLatest("not json"))
    }
}
