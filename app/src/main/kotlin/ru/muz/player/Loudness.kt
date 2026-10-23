package ru.muz.player

object Loudness {
    const val TARGET_LUFS = -14f
    const val MAX_CUT_DB = -12f

    fun gainDb(integratedLufs: Float): Float {
        if (integratedLufs.isNaN() || integratedLufs > 0f || integratedLufs < -70f) return 0f
        return (TARGET_LUFS - integratedLufs).coerceIn(MAX_CUT_DB, 0f)
    }
}
