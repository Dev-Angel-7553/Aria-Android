package ru.muz.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object NetworkMonitor {
    private val state = MutableStateFlow(true)
    private var started = false

    fun online(context: Context): StateFlow<Boolean> {
        start(context.applicationContext)
        return state
    }

    private fun start(context: Context) {
        if (started) return
        started = true
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        fun check() {
            val caps = manager.activeNetwork?.let(manager::getNetworkCapabilities)
            state.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
        check()
        runCatching {
            manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = check()
                override fun onLost(network: Network) = check()
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = check()
            })
        }
    }
}
