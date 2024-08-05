package com.nexcom.ufc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends Activity {

    private static final String LOG_TAG = "[ UFC100 ]";
    private static final String Version = "0.0.1.0510";
    private static final int REQUEST_PERMISSION = 200;

    private ImageButton iNexcomLogo;
    private ImageButton iMenuIcon;
    private ImageButton iSettings;
    private ImageButton iHome;
    private ImageButton iReturn;
    private ImageButton iFan;
    private ImageButton iVolume;
    private ImageButton iVideo;
    private ImageButton iGradientDecrease;
    private ImageButton iGradientIncrease;
    private ImageButton iStop;
    private ImageButton iSpeedDecrease;
    private ImageButton iSpeedIncrease;
    private ImageButton iBluetooth;

    private TextView tHeartRate;
    private TextView tTotalDistance;
    private TextView tGradient;
    private TextView tSpeed;

    private TextureView textureView;
    private Surface mSurface;
    private MediaPlayer mediaPlayer;

    private int gradientData = 0;
    private int speedData = 1;
    private int fanCount = 0;
    private int volumeCount = 0;

    private final int[] distance = {0};
    private final int[] heartRate = {0};

    private boolean bEStop = false;
    private boolean bVideoSource = false;   // true > HDMI In first , false > Mediaplayer first
    private boolean bMediaStop = true;
    private boolean bCameraStop = true;
    private boolean cameraInit = false;
    private static boolean buttonLock = false;
    private static boolean bluetoothSwitch = false;

    protected CameraDevice mCameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    protected CameraCharacteristics characteristics;
    protected StreamConfigurationMap map;
    protected CameraManager manager;
    private String cameraId;
    private Size imageDimension;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private AudioManager audioManager = null;

    private BluetoothAdapter BTAdapter = null;

    private File file = null;
    private String videoSource = "";
    private AlertDialog versionDialog = null;
    private static final int REQUEST_CODE_PERMISSIONS = 123;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 添加窗口?志以禁用?屏
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        // 全屏?示
        hideSystemUI();

        if (!hasAllPermissions()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        //checkPermission();
        findView();
        initTask();

    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE
        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        // Hide the nav bar and status bar
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private boolean hasAllPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        iFan.setImageDrawable(getDrawable(R.drawable.button_fan_off));

        iVolume.setImageDrawable(getDrawable(R.drawable.button_volume_off));
        setTinymixControl(false);

        volumeCount = 0;
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);

        if (BTAdapter.isEnabled()) {
            iMenuIcon.setImageDrawable(getDrawable(R.drawable.menu_bluetooth_on));
            bluetoothSwitch = true;
        } else {
            iMenuIcon.setImageDrawable(getDrawable(R.drawable.menu_bluetooth_off));
            bluetoothSwitch = false;
        }

        if (textureView.isAvailable()) {
            videoSourceFunction();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bVideoSource) {
            mediaStop();
            bVideoSource = false;
        } else {
            if (cameraInit) {
                stopBackgroundThread();
            }
            bVideoSource = true;
        }
    }

    protected void checkPermission() {
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.INTERNET}, REQUEST_PERMISSION);
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                // All permissions were granted, proceed with your functionality
                finish();
            }
        }
    }

    protected void findView() {
        iNexcomLogo = (ImageButton) findViewById(R.id.nexcomLogo);
        iNexcomLogo.setOnClickListener(clickNexcomLogo);
        iMenuIcon = (ImageButton) findViewById(R.id.menuIcon);
        iSettings = (ImageButton) findViewById(R.id.settings);
        iSettings.setOnClickListener(clickSettings);
        iHome = (ImageButton) findViewById(R.id.btnHome);
        iHome.setOnClickListener(clickHome);
        iReturn = (ImageButton) findViewById(R.id.btnReturn);
        iReturn.setOnClickListener(clickReturn);
        iFan = (ImageButton) findViewById(R.id.btnFan);
        iFan.setOnClickListener(clickFan);
        iVolume = (ImageButton) findViewById(R.id.btnVolume);
        iVolume.setOnClickListener(clickVolume);
        iVideo = (ImageButton) findViewById(R.id.btnVideo);
        iVideo.setOnClickListener(clickVideo);
        iGradientDecrease = (ImageButton) findViewById(R.id.btnGradientDecrease);
        iGradientDecrease.setOnClickListener(clickGradientDecrease);
        iGradientIncrease = (ImageButton) findViewById(R.id.btnGradientIncrease);
        iGradientIncrease.setOnClickListener(clickGradientIncrease);
        iStop = (ImageButton) findViewById(R.id.btnStop);
        iStop.setOnClickListener(clickStop);
        iSpeedDecrease = (ImageButton) findViewById(R.id.btnSpeedDecrease);
        iSpeedDecrease.setOnClickListener(clickSpeedDecrease);
        iSpeedIncrease = (ImageButton) findViewById(R.id.btnSpeedIncrease);
        iSpeedIncrease.setOnClickListener(clickSpeedIncrease);
        iBluetooth = (ImageButton) findViewById(R.id.btnBluetooth);
        iBluetooth.setOnClickListener(clickBluetooth);

        tHeartRate = (TextView) findViewById(R.id.textHeartRate);
        tTotalDistance = (TextView) findViewById(R.id.textTotalDistance);
        tGradient = (TextView) findViewById(R.id.textGradient);
        tSpeed = (TextView) findViewById(R.id.textSpeed);

        textureView = (TextureView) findViewById(R.id.textureViewUFC);
        textureView.setSurfaceTextureListener(textureListener);
    }

    private void unLockButton(int sec) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(sec * 1000);
                    buttonLock = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    View.OnClickListener clickSettings = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            settingsFunction();
        }
    };

    View.OnClickListener clickGradientDecrease = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            gradientDecreaseFunction();
        }
    };

    View.OnClickListener clickGradientIncrease = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            gradientIncreaseFunction();
        }
    };

    View.OnClickListener clickSpeedDecrease = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            speedDecreaseFunction();
        }
    };

    View.OnClickListener clickSpeedIncrease = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            speedIncreaseFunction();
        }
    };

    View.OnClickListener clickVideo = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!buttonLock) {
                buttonLock = true;
                videoSourceFunction();
                unLockButton(3);
            }
        }
    };

    View.OnClickListener clickStop = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            eStopFunction();
        }
    };

    View.OnClickListener clickFan = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            fanControlFunction();
        }
    };

    View.OnClickListener clickVolume = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            volumeControlFunction();
        }
    };

    View.OnClickListener clickBluetooth = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!buttonLock) {
                buttonLock = true;
                bluetoothFunction();
                unLockButton(1);
            }
        }
    };

    View.OnClickListener clickHome = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            homeFunction();
        }
    };

    View.OnClickListener clickReturn = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            returnFunction();
        }
    };

    View.OnClickListener clickNexcomLogo = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            nexcomLogoFunction();
        }
    };

    protected void settingsFunction() {
        Intent intent = new Intent("/");
        ComponentName componentName = new ComponentName("com.android.settings", "com.android.settings.Settings");
        intent.setComponent(componentName);
        intent.setAction("android.intent.action.View");
        startActivityForResult(intent, 0);
    }

    protected void videoControl() {
        if (bVideoSource) { // true > video ; false > HDMI
            float speed;
            speed = 1.0f + (((float) (speedData - 1) / 100) * 6) - ((float) gradientData / 20);
            if (speedData == 1 && gradientData == 0) {
                speed = 1;
            }
            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
        }
    }

    protected void gradientDecreaseFunction() {
        if (gradientData > 0) {
            gradientData--;
            tGradient.setText(String.valueOf(gradientData).toString());
            videoControl();
        }
    }

    protected void gradientIncreaseFunction() {
        if (gradientData < 10) {
            gradientData++;
            tGradient.setText(String.valueOf(gradientData).toString());
            videoControl();
        }
    }

    protected void speedDecreaseFunction() {
        if (speedData > 1) {
            speedData--;
            tSpeed.setText(String.valueOf(speedData).toString());
            videoControl();
        }
    }

    protected void speedIncreaseFunction() {
        if (speedData < 10) {
            speedData++;
            tSpeed.setText(String.valueOf(speedData).toString());
            videoControl();
        }
    }

    protected void initTask() {
        Handler hDistance = new Handler();
        Handler hHeartRate = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (!bEStop) {
                            distance[0]++;
                            if (distance[0] > 10000) {
                                distance[0] = 1;
                            }
                            hDistance.post(new Runnable() {
                                @Override
                                public void run() {
                                    tTotalDistance.setText(String.valueOf(distance[0]).toString());
                                }
                            });
                        } else {
                            Thread.sleep(100);
                            distance[0] = 0;
                        }
                        Thread.sleep((1 * 1020) - (speedData * 100) + (gradientData * 80));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    int radom = (int) (Math.random() * 20);

                    if (!bEStop) {
                        heartRate[0] = radom + 110 + (speedData * 2) + (gradientData * 2);
                        hHeartRate.post(new Runnable() {
                            @Override
                            public void run() {
                                tHeartRate.setText(String.valueOf(heartRate[0]).toString());
                            }
                        });
                    }
                    try {
                        Thread.sleep(radom * 100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    Runnable mediaRunnable = new Runnable() {
        @Override
        public void run() {
            //mediaStop();

            AssetFileDescriptor afd = null;
            try {
                afd = getAssets().openFd("vtc_7270.mp4");
                Log.i("[mediaRunnable]", "afd=" + afd.toString() + ", " + afd.getFileDescriptor());
                //file = new File(videoSource);
                //if (file.exists()) {
                    mediaPlayer.setSurface(mSurface);
                    //mediaPlayer.setDataSource(file.getAbsolutePath());
                    mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                //mediaPlayer.prepare();
                    mediaPlayer.prepareAsync();
                    mediaPlayer.setLooping(true);
                    mediaPlayer.start();
                bMediaStop = false;
                    videoControl();
                    textureView.requestFocus();
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            textureView.requestFocus();
                            mediaPlayer.setLooping(true);
                            mediaPlayer.start();
                            bMediaStop = false;
                        }
                    });
                /*
                } else {

                    Log.d(LOG_TAG, "File Can't Open......");
                    videoSource = "android.resource://" + getPackageName() + "/" + R.raw.vtc_7270;
                    Log.d(LOG_TAG, String.format("videoSource=%s", videoSource));
                    restartNewVideo();
                }
                */
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            mSurface = new Surface(surfaceTexture);
            videoSourceFunction();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

        }
    };

    protected void mediaStop() {
        Log.i("[mediaStop]", "bMediaStop = " + bMediaStop);
        if (!bMediaStop) {
            bMediaStop = true;
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
        }
    }
    protected void videoSourceFunction() {
        mediaStop();
        if (bVideoSource) {
            // Video Stop
            // HDMI In
            startBackgroundThread();
            openCamera();
            textureView.requestFocus();
            bVideoSource = false;

        } else {
            // HDMI Out
            if (cameraInit) {
                stopBackgroundThread();
            }
            // Video Play (Default)
            mediaPlayer = new MediaPlayer();
            new Thread(mediaRunnable).start();
            textureView.requestFocus();
            bVideoSource = true;
        }
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera_Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        Log.d(LOG_TAG, "startBackgroundThread");
    }

    protected void stopBackgroundThread() {
        if (bCameraStop) return;
        bCameraStop=true;

        try {
            mBackgroundThread.quitSafely();
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            cameraCaptureSessions.stopRepeating();
            mCameraDevice.close();
            captureRequestBuilder.removeTarget(mSurface);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.d(LOG_TAG, "stopBackgroundThread");
    }

    private void updatePreview() {
        if (mCameraDevice == null) {
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void createCameraPreview() {
        try {
            captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(mSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (mCameraDevice == null) {
                        return;
                    }
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.d(LOG_TAG, "onOpened: CameraDevice");
            mCameraDevice = cameraDevice;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    };

    private void openCamera() {
        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.d(LOG_TAG, "openCamera");
        try {
            cameraId = manager.getCameraIdList()[0];
            characteristics = manager.getCameraCharacteristics(cameraId);
            map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.d(LOG_TAG, "openCamera: Request Permission.");
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
            bCameraStop = false;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.d(LOG_TAG, "openCamera: open Camera successed.");
        cameraInit = true;
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice = null;
        }
        Log.d(LOG_TAG, "close Camera");
    }

    private void eStopFunction() {
        if (bEStop) {
            if (!cameraInit) {
                mediaPlayer.start();
                bMediaStop = false;
            }
            tHeartRate.setText("---");
            tTotalDistance.setText("---");
            tSpeed.setText("1");
            tGradient.setText("0");
            speedData = 1;
            gradientData = 0;
            videoControl();
            bEStop = false;
            Toast.makeText(this, "[Demo] E-STOP to re-active video and data .", Toast.LENGTH_SHORT).show();
        } else {
            if (!cameraInit) {
                if (!bMediaStop) mediaPlayer.pause();
            }
            bEStop = true;
            Toast.makeText(this, "[Demo] E-STOP to STOP video and data .", Toast.LENGTH_SHORT).show();
        }
    }

    private void fanControlFunction() {
        if (fanCount < 7) {
            fanCount++;
        } else {
            fanCount = 0;
        }
        Log.d(LOG_TAG, "fanControlFunction: " + fanCount);
        switch (fanCount) {
            case 4:
                iFan.setImageDrawable(getDrawable(R.drawable.button_fan_4));
                break;
            case 3:
            case 5:
                iFan.setImageDrawable(getDrawable(R.drawable.button_fan_3));
                break;
            case 2:
            case 6:
                iFan.setImageDrawable(getDrawable(R.drawable.button_fan_2));
                break;
            case 1:
            case 7:
                iFan.setImageDrawable(getDrawable(R.drawable.button_fan_1));
                break;
            case 0:
            default:
                iFan.setImageDrawable(getDrawable(R.drawable.button_fan_off));
                break;
        }
        Toast.makeText(this, "[Demo] Change FAN Level to [" + fanCount + "]", Toast.LENGTH_SHORT).show();
    }

    private void setTinymixControl(boolean control) {
        String value = "0";
        String onOff = "0";
        String strOnOff = "";
        Process process;
        int exitCode;

        if (control) {
            value = "100";
            onOff = "1";
            strOnOff = "ON";
        } else {
            value = "0";
            onOff = "0";
            strOnOff = "OFF";
        }

        try {
            process = new ProcessBuilder().command("/system/bin/tinymix", "47", onOff).redirectErrorStream(true).start();
            exitCode = process.waitFor();
            process.destroy();

            process = new ProcessBuilder().command("/system/bin/tinymix", "53", onOff).redirectErrorStream(true).start();
            exitCode = process.waitFor();
            process.destroy();

            process = new ProcessBuilder().command("/system/bin/tinymix", "36", value).redirectErrorStream(true).start();
            exitCode = process.waitFor();
            process.destroy();

            process = new ProcessBuilder().command("/system/bin/tinymix", "38", value).redirectErrorStream(true).start();
            exitCode = process.waitFor();
            process.destroy();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        //Toast.makeText(this, "[Demo] Turn Audio " + strOnOff + " .", Toast.LENGTH_SHORT).show();
    }

    private void volumeControlFunction() {
        if (volumeCount < 5) {
            volumeCount++;
        } else {
            volumeCount = 0;
        }
        Log.d(LOG_TAG, "volumeControlFunction: " + volumeCount);
        switch (volumeCount) {
            case 3:
                iVolume.setImageDrawable(getDrawable(R.drawable.button_volume_3));
                setTinymixControl(true);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 30, 0);
                break;
            case 2:
            case 4:
                iVolume.setImageDrawable(getDrawable(R.drawable.button_volume_2));
                setTinymixControl(true);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, 0);
                break;
            case 1:
            case 5:
                iVolume.setImageDrawable(getDrawable(R.drawable.button_volume_1));
                setTinymixControl(true);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 10, 0);
                break;
            case 0:
            default:
                iVolume.setImageDrawable(getDrawable(R.drawable.button_volume_off));
                setTinymixControl(false);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                break;
        }
        Toast.makeText(this, "[Demo] Turn Audio volumn to level [" + volumeCount + "] .", Toast.LENGTH_SHORT).show();
    }

    private void bluetoothFunction() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
             return;
        }
        if (!bluetoothSwitch && !BTAdapter.isEnabled()) {
            BTAdapter.enable();
            iMenuIcon.setImageDrawable(getDrawable(R.drawable.menu_bluetooth_on));
            bluetoothSwitch = true;
            Toast.makeText(this, "[Demo] Bluetooth ON .", Toast.LENGTH_SHORT).show();
        } else {
            BTAdapter.disable();
            iMenuIcon.setImageDrawable(getDrawable(R.drawable.menu_bluetooth_off));
            bluetoothSwitch = false;
            Toast.makeText(this, "[Demo] Bluetooth OFF .", Toast.LENGTH_SHORT).show();

        }
    }

    private void returnFunction() {
        tTotalDistance.setText("0");
        distance[0] = 0;
        tHeartRate.setText("---");
        tSpeed.setText("1");
        tGradient.setText("0");
        speedData = 1;
        gradientData = 0;
        videoControl();
        Toast.makeText(this, "[Demo] Reset calculate value .", Toast.LENGTH_SHORT).show();
    }

    private void homeFunction() {
        Intent intent = this.getPackageManager().getLaunchIntentForPackage("com.android.settings");
        if (intent != null) {
            this.startActivity(intent);
            Toast.makeText(this, "[Demo] Active Setting Page .", Toast.LENGTH_SHORT).show();
        } else {
            // 显示应用未找到的消息
            Toast.makeText(this, "App not found: com.android.settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void nexcomLogoFunction() {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View video = inflater.inflate(R.layout.version_dialog, (ViewGroup)null);
        final Button btnDefault = (Button) video.findViewById(R.id.buttonDefault);
        final Button btnVideo01 = (Button) video.findViewById(R.id.buttonVideo01);
        final Button btnVideo02 = (Button) video.findViewById(R.id.buttonVideo02);
        final Button btnVideo03 = (Button) video.findViewById(R.id.buttonVideo03);
        final Button btnVideo04 = (Button) video.findViewById(R.id.buttonVideo04);

        btnDefault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                versionDialog.cancel();
                videoSource = "vtc_7270.mp4";
                restartNewVideo();
            }
        });
        btnVideo01.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                versionDialog.cancel();
                videoSource = "vtc_7270.mp4";
                restartNewVideo();
            }
        });
        btnVideo02.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                versionDialog.cancel();
                videoSource = "vtc_7270.mp4";
                restartNewVideo();
            }
        });
        btnVideo03.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                versionDialog.cancel();
                videoSource = "vtc_7270.mp4";
                restartNewVideo();
            }
        });
        btnVideo04.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                versionDialog.cancel();
                videoSource = "vtc_7270.mp4";
                restartNewVideo();
            }
        });

        versionDialog = new AlertDialog.Builder(this)
                .setTitle("UFC Version")
                .setMessage("v " + Version)
                .setView(video)
                .show();
    }

    private void restartNewVideo() {
        if (cameraInit) {
            // HDMI Out
            stopBackgroundThread();
        } else {
            // Video Stop
            mediaStop();
        }
        // Video Play (Default)
        mediaPlayer = new MediaPlayer();
        new Thread(mediaRunnable).start();
        textureView.requestFocus();
        bVideoSource = true;
    }
}