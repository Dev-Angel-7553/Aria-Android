package ru.muz.update

import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdateIntegrityTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun acceptsExpectedDownloadAndRejectsTruncationOrAlteredContent() {
        val file = temporary.newFile().apply { writeText("abc") }
        val digest = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        UpdateIntegrity.verify(file, 3, digest)
        assertThrows(IllegalArgumentException::class.java) { UpdateIntegrity.verify(file, 4, digest) }
        file.writeText("abd")
        assertThrows(IllegalArgumentException::class.java) { UpdateIntegrity.verify(file, 3, digest) }
        assertThrows(IllegalArgumentException::class.java) { UpdateIntegrity.verify(file, 3, "invalid") }
        file.writeText("")
        assertThrows(IllegalArgumentException::class.java) { UpdateIntegrity.verify(file, 0, null) }
    }

    @Test
    fun parsesChecksumPublishedByGitHub() {
        val release = AppUpdate.parseLatest("""{"tag_name":"v0.41","assets":[{"name":"Aria-0.41.apk","size":3,"browser_download_url":"https://example.com/app.apk","digest":"sha256:abc"}]}""")!!
        assertEquals("abc", release.sha256)
    }
}
