package com.nexcom.gsensor_lsm6ds

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CubeRenderer(private val context: Context) : GLSurfaceView.Renderer, SensorEventListener {
    private lateinit var cube: Cube
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)
    private val deltaRotationVector = FloatArray(4)
    private val currentRotationMatrix = FloatArray(16)
    private var timestamp: Long = 0
    private var sensorManager: SensorManager? = null
    private var gyroscope: Sensor? = null

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        cube = Cube()
        //cube.run { createDigitTextures() }
        sensorManager?.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI)
        Matrix.setIdentityM(currentRotationMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -5f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, currentRotationMatrix, 0)
        cube.draw(mvpMatrix)
        // Draw the x, y, z coordinates
        drawText("X", -1.5f, 0f, 0f)
        drawText("Y", 0f, -1.5f, 0f)
        drawText("Z", 0f, 0f, -1.5f)
    }

    private fun drawText(text: String, x: Float, y: Float, z: Float) {
        val charWidth = 0.1f // Width of each character
        val charHeight = 0.2f // Height of each character

        val vertexBuffer = ByteBuffer.allocateDirect(12 * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(floatArrayOf(
                    x, y, z,
                    x, y + charHeight, z,
                    x + charWidth, y + charHeight, z,
                    x + charWidth, y, z
                ))
                position(0)
            }
        }

        // Set up shader program, color attribute, etc.
        // This part depends on your OpenGL ES setup

        // Draw the quad with color
        // This part also depends on your OpenGL ES setup
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Draw additional geometry (lines, triangles) to represent the character
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio: Float = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            val dT = (event.timestamp - timestamp) * 1.0e-9f
            timestamp = event.timestamp

            var axisX = event.values[0]
            var axisY = event.values[1]
            var axisZ = event.values[2]

            val omegaMagnitude = Math.sqrt((axisX * axisX + axisY * axisY + axisZ * axisZ).toDouble()).toFloat()
            if (omegaMagnitude > 0.0001f) {
                axisX /= omegaMagnitude
                axisY /= omegaMagnitude
                axisZ /= omegaMagnitude
            }

            val thetaOverTwo = omegaMagnitude * dT / 2.0f
            val sinThetaOverTwo = Math.sin(thetaOverTwo.toDouble()).toFloat()
            val cosThetaOverTwo = Math.cos(thetaOverTwo.toDouble()).toFloat()

            deltaRotationVector[0] = sinThetaOverTwo * axisX
            deltaRotationVector[1] = sinThetaOverTwo * axisY
            deltaRotationVector[2] = sinThetaOverTwo * axisZ
            deltaRotationVector[3] = cosThetaOverTwo

            val deltaRotationMatrix = FloatArray(16)
            SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector)

            Matrix.multiplyMM(currentRotationMatrix, 0, currentRotationMatrix, 0, deltaRotationMatrix, 0)
        }
    }
}
