package com.nexcom.boot_app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import me.relex.circleindicator.CircleIndicator
import android.widget.ImageView
import android.widget.TextView
import okhttp3.*
import android.widget.VideoView

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE_PERMISSIONS = 1
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.RECEIVE_BOOT_COMPLETED,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.DISABLE_KEYGUARD,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.INTERNET,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE
    )

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: FrameLayout
    private lateinit var wifiManager: WifiManager
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var viewPager: ViewPager

    private lateinit var contentFrame: FrameLayout
    private lateinit var f1Button: Button
    private lateinit var f2Button: Button
    private lateinit var f3Button: Button
    private lateinit var f4Button: Button
    private lateinit var f5Button: Button
    private val client = OkHttpClient()
    private lateinit var videoView: VideoView
    private var isPlaying = false

    private lateinit var weatherIcon: ImageView
    private lateinit var weatherText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 60000L // 1 minute in milliseconds
    private val updateStatusInterval = 15000L // 1 minute in milliseconds

    private lateinit var wifiIcon: ImageView
    private lateinit var mobileIcon: ImageView
    private lateinit var btIcon: ImageView
    private lateinit var ethernetIcon: ImageView
    private lateinit var gpsIcon: ImageView

    private val weatherData = listOf(
        WeatherInfo("Sunny", 25, R.drawable.sunny),
        WeatherInfo("Cloudy", 22, R.drawable.cloudy),
        WeatherInfo("Rainy", 18, R.drawable.rainy),
        WeatherInfo("Stormy", 20, R.drawable.stormy),
        WeatherInfo("Snowy", -5, R.drawable.snowy)
    )
    data class WeatherInfo(val description: String, val temperature: Int, val iconResId: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 添加窗口?志以禁用?屏
        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        // 全屏?示
        hideSystemUI()

        // Request necessary permissions
        if (allPermissionsGranted()) {
            checkOverlayPermission()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        viewPager = findViewById(R.id.viewPager)

        viewPager = findViewById(R.id.viewPager)

        val appInfoList1 = arrayListOf(
            AppInfo(R.drawable.ic_chrome, "Chrome", "org.chromium.webview_shell", "https://m.youtube.com", AppType.URL),
            AppInfo(R.drawable.ic_camera, "HDMI-IN", "com.example.lt6911preview"),
            AppInfo(R.drawable.ic_gsensor, "G-Sensor", "com.nexcom.gsensor_lsm6ds"),
            AppInfo(R.drawable.ic_gps, "GPS & Map", "com.nexcom.nexcomgps")
            // 添加更多应用信息
        )

        val appInfoList2 = arrayListOf(
            AppInfo(R.drawable.ic_wifi, "WiFi Setting", "", type = AppType.WIFI),
            AppInfo(R.drawable.ic_bluetooth, "Bluetooth Seting", "", type = AppType.BLUETOOTH)
            // 添加更多应用信息
        )

        val appInfoList3 = arrayListOf(
            AppInfo(R.drawable.ic_settings, "Settings", "com.android.settings"),
            AppInfo(R.drawable.ic_camera, "Camera", "com.example.lt6911preview"),
            AppInfo(R.drawable.ic_chrome, "Chrome", "org.chromium.webview_shell")
            // 添加更多应用信息
        )

        val adapter = object : FragmentStatePagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getCount(): Int = 3 // 示例中有三个页面

            override fun getItem(position: Int): Fragment {
                return when (position) {
                    0 -> PageFragment.newInstance(appInfoList1)
                    1 -> PageFragment.newInstance(appInfoList2)
                    2 -> PageFragment.newInstance(appInfoList3)
                    else -> PageFragment.newInstance(appInfoList1)
                }
            }
        }

        viewPager.adapter = adapter

        val indicator: CircleIndicator = findViewById(R.id.indicator)
        indicator.setViewPager(viewPager)

        //For Other Service
        boot_init()

        weather_init()
        keyevent_init()
        video_init()

        status_init()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
            } else {
                startOverlayService()
            }
        } else {
            startOverlayService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                checkOverlayPermission()
            } else {
                Toast.makeText(this, "必要权限未授予", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startOverlayService()
                } else {
                    Toast.makeText(this, "权限被拒绝，无法显示在其他应用之上", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    companion object {
        private const val ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1
    }

    private fun boot_init() {
        // 启动前台服务
        val serviceIntent = Intent(this, ForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
    private fun status_init() {
        wifiIcon = findViewById(R.id.wifi_icon)
        mobileIcon = findViewById(R.id.mobile_icon)
        btIcon = findViewById(R.id.bt_icon)
        ethernetIcon = findViewById(R.id.ethernet_icon)
        gpsIcon = findViewById(R.id.gps_icon)

        updateNetworkStatus(this@MainActivity, wifiIcon, mobileIcon, btIcon, ethernetIcon, gpsIcon)
    }

    private val updateTask = object : Runnable {
        override fun run() {
            updateNetworkStatus(this@MainActivity, wifiIcon, mobileIcon, btIcon, ethernetIcon, gpsIcon)
            handler.postDelayed(this, updateStatusInterval)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateTask)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateTask)
    }

    private fun video_init() {
        videoView = findViewById(R.id.videoView)
        val videoUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.vtc_7270)
        videoView.setVideoURI(videoUri)

        // 設置循環播放
        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = true
            videoView.start()
        }

        // 設置點擊事件處理程序
        videoView.setOnClickListener {
            if (isPlaying) {
                videoView.pause()
                isPlaying = false
            } else {
                videoView.start()
                isPlaying = true
            }
        }

        // 自動播放影片（可選）
        videoView.setOnPreparedListener {
            videoView.start()
            isPlaying = true
        }

        // 如果需要自動播放，請在這裡設置
        videoView.setOnCompletionListener {
            // 當影片播放完成後，您可以選擇重新播放影片或其他操作
            videoView.start()
            isPlaying = true
        }
    }
    private val updateWeatherRunnable = object : Runnable {
        override fun run() {
            fetchWeatherInfo()
            handler.postDelayed(this, updateInterval)
        }
    }
    private fun weather_init() {
        weatherIcon = findViewById(R.id.weatherIcon)
        weatherText = findViewById(R.id.weatherText)

        // 開始定時更新
        handler.post(updateWeatherRunnable)
    }

    private fun fetchWeatherInfo() {
        thread {
            //val url = "https://wttr.in/?format=%C+%t&lang=en"
            val url = "https://wttr.in/?format=4"
            val request = Request.Builder().url(url).build()

            try {
                val response = client.newCall(request).execute()
                val responseData = response.body?.string()?.trim() ?: "N/A"
                //val responseData = response.body?.string() ?: "N/A"

                val locationData = responseData.split(":")
                val weatherLocation = locationData[0]
                val weatherData = locationData[1].trim().split(" ")
                val weatherDescription = weatherData[0]
                val weatherTemperature = weatherData[weatherData.count()-2]
                val weatherSpeed = weatherData[weatherData.count()-1]
                //Log.i("[fetchWeatherInfo]", "weatherData=${weatherData}, weatherDescription=${weatherDescription}, weatherTemperature=${weatherTemperature}")
                //Log.i("[fetchWeatherInfo]", "weatherData=${weatherData}, weatherDescription=${weatherDescription}, weatherTemperature=${weatherTemperature}, weatherSpeed=${weatherSpeed}")

                runOnUiThread {
                    updateWeatherInfo(weatherDescription, weatherLocation, weatherTemperature, weatherSpeed)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun updateWeatherInfo(description: String, location: String, temperature: String, speed: String) {
        if (location != "not found")
            weatherText.text = "$location\n$temperature\n$speed"
        else
            weatherText.text = "$temperature\n$speed"
        Log.i("[fetchWeatherInfo]", "description=${description}, location=${location}, temperature=${temperature}, speed=${speed}")

        val iconResId = when (description.toLowerCase()) {
            "sunny" -> R.drawable.sunny
            "cloudy" -> R.drawable.cloudy
            "rainy" -> R.drawable.rainy
            "stormy" -> R.drawable.stormy
            "snowy" -> R.drawable.snowy
            "☀\uFE0F" -> R.drawable.sunny
            "☁\uFE0F" -> R.drawable.rainy
            "⛅\uFE0F" -> R.drawable.cloudy
            "⛈\uFE0F" -> R.drawable.stormy
            "❄\uFE0F" -> R.drawable.snowy
            "\uD83C\uDF26" -> R.drawable.rainy
            "\uD83C\uDF27" -> R.drawable.stormy
            "\uD83C\uDF28" -> R.drawable.stormy
            "\uD83C\uDF2B" -> R.drawable.stormy
            "\uD83C\uDF29" -> R.drawable.rainy
            else -> R.drawable.unknown
        }
        weatherIcon.setImageResource(iconResId)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止定時更新
        handler.removeCallbacks(updateWeatherRunnable)
    }

    private fun keyevent_init() {
        f1Button = findViewById(R.id.f1Button)
        f2Button = findViewById(R.id.f2Button)
        f3Button = findViewById(R.id.f3Button)
        f4Button = findViewById(R.id.f4Button)
        f5Button = findViewById(R.id.f5Button)
        val keyEventButtons = listOf(f1Button, f2Button, f3Button, f4Button, f5Button)
        keyEventButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                val keyCode = KeyEvent.KEYCODE_F1 + index
                val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
                dispatchKeyEvent(keyEvent)
                showButtonLight(button, keyCode)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        //Log.i("[onKeyDown]", "keyCode=${keyCode}")
        //if (event != null) {
        when (keyCode) {
            KeyEvent.KEYCODE_F1 -> showButtonLight(f1Button, keyCode)
            KeyEvent.KEYCODE_F2 -> showButtonLight(f2Button, keyCode)
            KeyEvent.KEYCODE_F3 -> showButtonLight(f3Button, keyCode)
            KeyEvent.KEYCODE_F4 -> showButtonLight(f4Button, keyCode)
            KeyEvent.KEYCODE_F5 -> showButtonLight(f5Button, keyCode)

            KeyEvent.KEYCODE_VOLUME_DOWN -> showButtonLight(f2Button, keyCode)
            KeyEvent.KEYCODE_VOLUME_UP -> showButtonLight(f1Button, keyCode)
            KeyEvent.KEYCODE_VOLUME_MUTE -> showButtonLight(f3Button, keyCode)
            KeyEvent.KEYCODE_BRIGHTNESS_UP -> showButtonLight(f4Button, keyCode)
            KeyEvent.KEYCODE_BRIGHTNESS_DOWN -> showButtonLight(f5Button, keyCode)
        }
        //}
        return super.onKeyDown(keyCode, event)
    }

    private fun showButtonLight(button: Button, keyCode: Int) {
        val mapKeScript = mapOf(
            KeyEvent.KEYCODE_VOLUME_UP to "KEYCODE_VOLUME_UP",
            KeyEvent.KEYCODE_VOLUME_DOWN to "KEYCODE_VOLUME_DOWN",
            KeyEvent.KEYCODE_VOLUME_MUTE to "KEYCODE_VOLUME_MUTE",
            KeyEvent.KEYCODE_BRIGHTNESS_UP to "KEYCODE_BRIGHTNESS_UP",
            KeyEvent.KEYCODE_BRIGHTNESS_DOWN to "KEYCODE_BRIGHTNESS_DOWN",
            KeyEvent.KEYCODE_F1 to "KEYCODE_F1",
            KeyEvent.KEYCODE_F2 to "KEYCODE_F2",
            KeyEvent.KEYCODE_F3 to "KEYCODE_F3",
            KeyEvent.KEYCODE_F4 to "KEYCODE_F4",
            KeyEvent.KEYCODE_F5 to "KEYCODE_F5"
        )
        val msgKey = "Get key code [${keyCode}], Key Name is ${mapKeScript[keyCode]} ..."
        Toast.makeText(this, msgKey, Toast.LENGTH_LONG).show()
        button.setBackgroundColor(Color.YELLOW)
        Handler(Looper.getMainLooper()).postDelayed({
            button.setBackgroundColor(Color.GRAY)
        }, 1000)
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the content
                // doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}
