package com.nexcom.boot_app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class GridAdapter(private val context: Context, private val appInfoList: List<AppInfo>) : BaseAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = appInfoList.size

    override fun getItem(position: Int): Any = appInfoList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: inflater.inflate(R.layout.grid_item, parent, false)
        val imageView: ImageView = view.findViewById(R.id.iconImageView)
        val textView: TextView = view.findViewById(R.id.appNameTextView)

        val appInfo = appInfoList[position]
        imageView.setImageResource(appInfo.iconResId)
        textView.text = appInfo.appName

        // 设置点击事件以启动相应的应用
        view.setOnClickListener {
            try {
                when (appInfo.type) {
                    AppType.URL -> {
                        // 启动浏览器并传递 URL
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appInfo.url))
                        intent.setPackage(appInfo.packageName)
                        context.startActivity(intent)
                    }
                    AppType.WIFI -> {
                        // 打开 WiFi 设置
                        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                        context.startActivity(intent)
                    }
                    AppType.BLUETOOTH -> {
                        // 打开 Bluetooth 设置
                        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                        context.startActivity(intent)
                    }
                    else -> {
                        // 启动其他应用
                        val intent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                        if (intent != null) {
                            context.startActivity(intent)
                        } else {
                            // 显示应用未找到的消息
                            Toast.makeText(context, "App not found: ${appInfo.appName}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to launch app: ${appInfo.appName}", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
