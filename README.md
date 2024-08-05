# Nexcom MCS VMC320 Machine - Android Demo APP

Welcome to the GitLab repository for the Android Demo APPs developed for the Nexcom MCS VMC320 Machine. This repository contains several demo applications showcasing various functionalities of the VMC320 machine.

## VMC320 Machine Introduction

The Nexcom VMC320 is a versatile vehicle mount computer designed for mobile computing solutions. It offers robust performance and reliability for a wide range of applications in vehicle environments. The VMC320 supports Android 13 OS, providing an advanced and modern operating system environment for enhanced functionality and user experience.

For more detailed information, please visit the [official product page](https://www.nexcom.com.tw/Products/mobile-computing-solutions/vehicle-mount-computer/vmc-10-inch/vmc-320-vehicle-mount-computer/OrderInginformation).

![VMC320 Machine](vmc320.png)


## Included Demo Applications

### 1. BOOT_APP

This application serves as the initial home launcher for the VMC320. It demonstrates:
- Initial setup and configuration
- Key event handling
- Video playback
- Network status monitoring
Repository: [BOOT_APP](BOOT_APP/README.md)

### 2. NexcomGPS

The GPS demo application displays the current location using OpenStreetMap and includes a satellite map view. It also shows GPS coordinates and other relevant satellite information.
Repository: [NexcomGPS](NexcomGPS/README.md)

### 3. gsensor-lsm6ds

This demo showcases the G-sensor capabilities of the VMC320. It uses a 3D cube to visually represent the rotation along the x, y, and z axes.
Repository: [gsensor-lsm6ds](gsensor-lsm6ds/README.md)

### 4. lt6911_preview

This application demonstrates the HDMI-IN functionality and audio source capabilities of the VMC320. It supports:
- HDMI-IN video preview
- Audio capture from HDMI-IN and the built-in microphone
Repository: [lt6911_preview](lt6911_preview/README.md)

### 5. object_detect

Using TensorFlow Lite, this application performs object detection on video input from the HDMI-IN source. It showcases the machine's ability to process and identify objects in real-time.
Repository: [object_detect](AI-TLite/object_detect/README.md)

### 6. posenet

Similar to the object detection demo, this application uses TensorFlow Lite for pose estimation. It detects and tracks human poses in real-time using video input from the HDMI-IN source.
Repository: [posenet](AI-TLite/posenet/README.md)

### 7. style_transfer - HDMI-IN Style Transfer Application

The style_transfer application captures video input from the HDMI-IN source of the Nexcom VMC320 vehicle mount computer and applies various artistic styles to the captured images. This demo showcases the capability of performing style transfer using advanced neural network techniques.
Repository: [style_transfer](AI-TLite/style_transfer/README.md)

### 8. image_segmentation - HDMI-IN Image Segmentation Application

The image_segmentation application captures video input from the HDMI-IN source of the Nexcom VMC320 vehicle mount computer and performs real-time image segmentation to identify and classify objects within the captured images. This demo showcases the capability of performing object recognition and segmentation using advanced neural network techniques.
Repository: [image_segmentation](AI-TLite/image_segmentation/README.md)



### 9. UFC 100

UFC100 is an Android application designed to simulate the interface of a treadmill head unit. This app offers various controls and simulations, including different video playback speeds, app calls, Bluetooth, and sound controls.
Repository: [UFC-100](UFC/README.md)



---

Feel free to explore the source code and modify it to suit your specific needs. Contributions and feedback are always welcome.

Repository: [VMC320.Android](https://github.com/mcsnexcom/VMC320.Android)
