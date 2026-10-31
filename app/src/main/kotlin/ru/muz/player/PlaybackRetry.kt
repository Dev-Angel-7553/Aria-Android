package ru.muz.player

internal object PlaybackRetry {
    fun delayMs(errorCode: Int, attempt: Int, httpStatus: Int? = null): Long? {
        if (attempt !in 0..2 || errorCode !in setOf(2000, 2001, 2002, 2004)) return null
        if (errorCode == 2004 && httpStatus != 408 && httpStatus != 429 && httpStatus !in 500..599) return null
        return 1_000L shl attempt
    }
}
