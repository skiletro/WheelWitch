package com.skiletro.wheelwitch.util.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Returns true when the device has an active network with internet capability. */
fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager =
        getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
