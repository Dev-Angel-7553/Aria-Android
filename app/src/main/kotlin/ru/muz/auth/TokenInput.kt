package ru.muz.auth

object TokenInput {
    const val GUIDE_URL = "https://ym.marshal.dev/token"
    const val OAUTH_URL = "https://oauth.yandex.ru/authorize?response_type=token&client_id=23cabbbdc6cd418abb4b39c32c41195d"
    const val EXTENSION_CHROME = "https://chrome.google.com/webstore/detail/yandex-music-token/lcbjeookjibfhjjopieifgjnhlegmkib"
    const val EXTENSION_FIREFOX = "https://addons.mozilla.org/en-US/firefox/addon/yandex-music-token/"
    const val ANDROID_APP = "https://github.com/MarshalX/yandex-music-token/releases"

    fun extract(raw: String): String {
        var value = raw.trim().trim('"', '\'', '`', ' ')
        Regex("access_token=([^&\\s#]+)").find(value)?.let { value = it.groupValues[1] }
        value = value.removePrefix("OAuth ").removePrefix("oauth ").removePrefix("Bearer ").trim()
        return value.filterNot { it.isWhitespace() }
    }

    fun looksValid(token: String): Boolean = token.length >= 20 && token.all { it.isLetterOrDigit() || it in "-_." }
}
