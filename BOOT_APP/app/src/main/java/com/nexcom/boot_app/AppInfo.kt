package com.nexcom.boot_app

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppInfo(
    val iconResId: Int,
    val appName: String,
    val packageName: String,
    val url: String? = null,
    val type: AppType = AppType.APP // 添加 type 字段，默认为 APP
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        AppType.valueOf(parcel.readString() ?: AppType.APP.name)
    )

    companion object : Parceler<AppInfo> {

        override fun AppInfo.write(parcel: Parcel, flags: Int) {
            parcel.writeInt(iconResId)
            parcel.writeString(appName)
            parcel.writeString(packageName)
            parcel.writeString(url)
            parcel.writeString(type.name)
        }

        override fun create(parcel: Parcel): AppInfo {
            return AppInfo(parcel)
        }
    }
}

enum class AppType {
    APP,
    URL,
    WIFI,
    BLUETOOTH
}