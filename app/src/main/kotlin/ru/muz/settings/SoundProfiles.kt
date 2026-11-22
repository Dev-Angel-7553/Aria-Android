package ru.muz.settings

import android.content.Context

internal enum class SoundOutput(val title: String) {
    HEADPHONES("Наушники"),
    SPEAKER("Колонка"),
    CAR("Машина"),
}

internal data class SoundProfile(val bands: List<Int>, val normalize: Boolean)

internal class SoundProfiles(context: Context) {
    private val prefs = context.getSharedPreferences("sound_profiles", Context.MODE_PRIVATE)

    fun load(output: SoundOutput): SoundProfile? {
        val bands = prefs.getString(output.name, null)?.split(',')
            ?.mapNotNull { it.toIntOrNull()?.coerceIn(-12, 12) }
            ?.takeIf { it.size == 5 } ?: return null
        return SoundProfile(bands, prefs.getBoolean(output.name + "_normalize", true))
    }

    fun save(output: SoundOutput, settings: AppSettings): SoundProfile {
        val profile = SoundProfile(settings.eqBands, settings.normalizeVolume)
        prefs.edit()
            .putString(output.name, profile.bands.joinToString(","))
            .putBoolean(output.name + "_normalize", profile.normalize)
            .apply()
        return profile
    }
}
