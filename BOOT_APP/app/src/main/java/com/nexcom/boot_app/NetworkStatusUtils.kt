package com.nexcom.boot_app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.location.LocationManager
import android.bluetooth.BluetoothAdapter
import android.telephony.TelephonyManager

fun getWifiStatus(context: Context): Int {
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    return wifiManager.connectionInfo.rssi
}

fun getMobileStatus(context: Context): Int {
    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    val signalStrength = telephonyManager.signalStrength
    return signalStrength?.level ?: 0
}

fun getBluetoothStatus(): Boolean {
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    return bluetoothAdapter?.isEnabled ?: false
}

fun getEthernetStatus(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)
    return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ?: false
}

fun getGpsStatus(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}
