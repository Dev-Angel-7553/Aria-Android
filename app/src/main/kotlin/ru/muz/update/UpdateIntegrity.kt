package ru.muz.update

import java.io.File
import java.security.MessageDigest

internal object UpdateIntegrity {
    fun verify(file: File, expectedSize: Long, sha256: String?) {
        require(file.length() > 0 && (expectedSize <= 0 || file.length() == expectedSize)) { "Обновление скачано не полностью. Повторите загрузку" }
        if (sha256 != null) {
            require(sha256.matches(Regex("[a-fA-F0-9]{64}"))) { "Некорректная контрольная сумма обновления" }
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            require(actual.equals(sha256, true)) { "Файл обновления повреждён. Повторите загрузку" }
        }
    }
}
