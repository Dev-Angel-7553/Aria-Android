package ru.muz.yandex

import android.content.Context

internal class AccountCache(context: Context) {
    private val prefs = context.getSharedPreferences("aria_account", Context.MODE_PRIVATE)

    fun save(account: YandexClient.Account) {
        prefs.edit()
            .putString("uid", account.uid)
            .putString("login", account.login)
            .putString("name", account.displayName)
            .putBoolean("service", account.serviceAvailable)
            .putBoolean("plus", account.hasPlus)
            .apply()
    }

    fun read(): YandexClient.Account? {
        val uid = prefs.getString("uid", null)?.takeIf { it.isNotBlank() } ?: return null
        return YandexClient.Account(
            uid = uid,
            login = prefs.getString("login", "").orEmpty(),
            displayName = prefs.getString("name", "").orEmpty(),
            serviceAvailable = prefs.getBoolean("service", true),
            hasPlus = prefs.getBoolean("plus", true),
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
