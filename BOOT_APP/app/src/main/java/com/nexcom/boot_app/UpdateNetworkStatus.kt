package com.nexcom.boot_app

import android.content.Context
import android.widget.ImageView

fun updateNetworkStatus(context: Context, wifiIcon: ImageView, mobileIcon: ImageView, btIcon: ImageView, ethernetIcon: ImageView, gpsIcon: ImageView) {
    val wifiSignal = getWifiStatus(context)
    val mobileSignal = getMobileStatus(context)
    val btStatus = getBluetoothStatus()
    val ethernetStatus = getEthernetStatus(context)
    val gpsStatus = getGpsStatus(context)

    // 根据状态和信号强度更新图标
    wifiIcon.setImageResource(if (wifiSignal > -70) R.drawable.ic_wifi_strong else R.drawable.ic_wifi_weak)
    mobileIcon.setImageResource(if (mobileSignal > 2) R.drawable.ic_mobile_strong else R.drawable.ic_mobile_weak)
    btIcon.setImageResource(if (btStatus) R.drawable.ic_bluetooth_on else R.drawable.ic_bluetooth_off)
    ethernetIcon.setImageResource(if (ethernetStatus) R.drawable.ic_ethernet_connected else R.drawable.ic_ethernet_disconnected)
    gpsIcon.setImageResource(if (gpsStatus) R.drawable.ic_gps_on else R.drawable.ic_gps_off)
}
