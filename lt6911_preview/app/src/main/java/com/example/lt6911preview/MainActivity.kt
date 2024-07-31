package com.example.lt6911preview

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.*
import android.view.TextureView
import java.nio.ByteBuffer
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.media.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioRecord.*
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.annotation.RequiresApi

import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private val cameraManager by lazy { getSystemService(CAMERA_SERVICE) as CameraManager }
    private var cameraDevice: CameraDevice? = null
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private var captureSession: CameraCaptureSession? = null

    private lateinit var gAudioRecord: AudioRecord
    private lateinit var gAudioTrack: AudioTrack
    private var isRecordingNau8824 = false
    private var isRecordingLt6911 = false

    companion object {
        private const val CAMERA_REQUEST_CODE = 101
        private const val AUDIO_REQUEST_CODE = 102
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceView = findViewById(R.id.surfaceView)
        surfaceView.holder.addCallback(surfaceCallback)

        val btnRecordAudioNau8824 = findViewById<Button>(R.id.btnRecordAudioNau8824)
        btnRecordAudioNau8824.setOnClickListener {

            if (isRecordingNau8824) {
                stopRecording()
            } else {
                if (isRecordingLt6911) {
                    stopRecording()
                }
                startRecording(0)
            }
        }
        val btnRecordAudioLt6911 = findViewById<Button>(R.id.btnRecordAudioLt6911)
        btnRecordAudioLt6911.setOnClickListener {
            if (isRecordingLt6911) {
                stopRecording()
            } else {
                if (isRecordingNau8824) {
                    stopRecording()
                }
                startRecording(1)
            }
        }

        requestPermissions()
        //startRecording(1)
    }

    private fun listAudioInputDevices(context: Context): List<AudioDeviceInfo> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audioDevices = mutableListOf<AudioDeviceInfo>()

        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        audioDevices.addAll(devices)
        for (device in devices) {
            val deviceType = when (device.type) {
                AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Built-in Mic"
                AudioDeviceInfo.TYPE_FM_TUNER -> "FM Tuner"
                AudioDeviceInfo.TYPE_TV_TUNER -> "TV Tuner"
                AudioDeviceInfo.TYPE_TELEPHONY -> "Telephony"
                AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Device"
                AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB Accessory"
                AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
                AudioDeviceInfo.TYPE_AUX_LINE -> "Aux Line"
                AudioDeviceInfo.TYPE_IP -> "IP"
                AudioDeviceInfo.TYPE_BUS -> "Bus"
                else -> "Unknown"
            }
            val deviceName = device.productName?.toString() ?: "Unknown Name"
            Log.d("AudioDevice", "Input Device: $deviceName, Type: $deviceType(${device.type}), ID: ${device.id}")
        }
        return audioDevices
    }

    private fun listAudioOutputDevices(context: Context): List<AudioDeviceInfo> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audioDevices = mutableListOf<AudioDeviceInfo>()

        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        audioDevices.addAll(devices)
        for (device in devices) {
            val deviceType = when (device.type) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in Speaker"
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
                AudioDeviceInfo.TYPE_HDMI -> "HDMI"
                AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Device"
                AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
                AudioDeviceInfo.TYPE_LINE_ANALOG -> "Line Analog"
                AudioDeviceInfo.TYPE_LINE_DIGITAL -> "Line Digital"
                else -> "Unknown"
            }
            val deviceName = device.productName?.toString() ?: "Unknown Name"
            Log.d("AudioDevice", "Output Device: $deviceName, Type: $deviceType, ID: ${device.id}")
        }
        return audioDevices
    }

    private fun setupAudioRecord(selectedDevice: AudioDeviceInfo) {
        val sampleRate = 48000 // Sample rate in Hz
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO // Input channel configuration
        val audioFormat = AudioFormat.ENCODING_PCM_32BIT // Audio data format

        val bufferSize = getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (bufferSize == ERROR || bufferSize == ERROR_BAD_VALUE) {
            Log.e("AudioRecord", "Invalid buffer size")
            return
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gAudioRecord = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()

        gAudioRecord.preferredDevice = selectedDevice

        if (gAudioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioRecord", "AudioRecord initialization failed")
            return
        }

        Log.d("AudioRecord", "Success Return Audio Record ....")
   }
    private fun setupAudioTrack(selectedDevice: AudioDeviceInfo) {
        val sampleRate = 48000
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_32BIT

        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        gAudioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()

        gAudioTrack.preferredDevice = selectedDevice

    }

    private fun startAudioProcessing(selectedInputDevice: AudioDeviceInfo, selectedOutputDevice: AudioDeviceInfo) {
        setupAudioRecord(selectedInputDevice)
        setupAudioTrack(selectedOutputDevice)

        val bufferSize = getMinBufferSize(
            48000,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_32BIT
        )

        val audioBuffer = ByteArray(bufferSize)

        gAudioRecord?.startRecording()
        gAudioTrack.play()

        Thread {
            while (isRecordingNau8824 || isRecordingLt6911) {
                val readBytes = gAudioRecord?.read(audioBuffer, 0, bufferSize)
                //Log.d("Read Audio", "readBytes=${readBytes}")

                if (readBytes!! > 0) {
                    gAudioTrack.write(audioBuffer, 0, readBytes)
                }


            }
        }.start()
    }

    private fun requestPermissions() {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS)
        if (permissions.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(this, permissions, CAMERA_REQUEST_CODE)
        }
     }

    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            openCamera()
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            captureSession?.close()
            cameraDevice?.close()
        }
    }

    private fun openCamera() {
        try {
            val cameraId = cameraManager.cameraIdList[0]
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, null)
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error accessing camera", e)
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            startCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    private fun startCameraPreview() {
        val surface = surfaceView.holder.surface

        try {
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)

            cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    try {
                        captureSession?.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                    } catch (e: CameraAccessException) {
                        Log.e(TAG, "Error setting up camera preview", e)
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Failed to configure camera")
                }
            }, null)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error starting camera preview", e)
        }
    }

    private fun startRecording(index: Int) {
        isRecordingNau8824 = false
        isRecordingLt6911 = false
        stopRecordingString()
        if (index==0) {
            findViewById<Button>(R.id.btnRecordAudioNau8824).text = "Stop Audio from NAU8824"
            isRecordingNau8824 = true
        } else {
            findViewById<Button>(R.id.btnRecordAudioLt6911).text = "Stop Audio from LT6911"
            isRecordingLt6911 = true
        }

        val sampleRate = 48000
        val pcmbit = AudioFormat.ENCODING_PCM_32BIT

        getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO, pcmbit)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        //Log.d("startRecording", "audioSource = ${audioSource}")
        val inputDevices = listAudioInputDevices(this)
        val outputDevices = listAudioOutputDevices(this)

        if (inputDevices.isNotEmpty() && outputDevices.isNotEmpty()) {
            val selectedInputDevice = inputDevices[index]
            val selectedOutputDevice = outputDevices[0]

            startAudioProcessing(selectedInputDevice, selectedOutputDevice)
        } else {
            Toast.makeText(this, "No audio input/output devices found", Toast.LENGTH_LONG).show()
        }
    }
    private fun stopRecordingString() {
        this.findViewById<Button>(R.id.btnRecordAudioNau8824).text = "Audio from NAU8824"
        this.findViewById<Button>(R.id.btnRecordAudioLt6911).text = "Audio from LT6911"
    }
    private fun stopRecording() {
        stopRecordingString()

        stopAudioProcessing()
    }

    private fun stopAudioProcessing() {
        isRecordingNau8824 = false
        isRecordingLt6911 = false

        try {
            gAudioRecord?.let {
                /*
                if (it.state == AudioRecord.STATE_INITIALIZED) {
                    it.stop()
                }

                 */
                Log.d("Stop", "[Kevin] AudioRecord.state=(${it.state})")
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recording", e)
        }

        try {
            gAudioTrack?.let {
                /*
                if (it.state == AudioTrack.STATE_INITIALIZED) {
                    it.stop()
                }
                */
                Log.d("Stop", "[Kevin] gAudioTrack.state=(${it.state}")

                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio track", e)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                // Handle permission denial
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}