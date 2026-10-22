package ru.muz.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenInputTest {
    @Test
    fun extractsTokenFromRedirectAddress() {
        val url = "https://music.yandex.ru/#access_token=y0_AgAAAAAbCdEfGhIjKlMnOpQrStUvWxYz123&token_type=bearer&expires_in=31536000"
        assertEquals("y0_AgAAAAAbCdEfGhIjKlMnOpQrStUvWxYz123", TokenInput.extract(url))
    }

    @Test
    fun cleansPlainToken() {
        assertEquals("y0_abcdefghijklmnopqrstuvwxyz", TokenInput.extract("  \"OAuth y0_abcdefghijklmnopqrstuvwxyz\"\n"))
        assertEquals("abcdefghijklmnopqrstuvwxyz", TokenInput.extract("abcdefghij klmnopqrstuvwxyz"))
    }

    @Test
    fun validatesShape() {
        assertTrue(TokenInput.looksValid("y0_AgAAAAAbCdEfGhIjKlMnOpQrStUvWxYz123"))
        assertFalse(TokenInput.looksValid("короткий"))
        assertFalse(TokenInput.looksValid("https://music.yandex.ru/"))
    }
}
