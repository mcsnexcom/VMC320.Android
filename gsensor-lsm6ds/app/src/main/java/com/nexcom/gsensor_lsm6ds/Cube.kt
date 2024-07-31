package com.nexcom.gsensor_lsm6ds

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Cube {
    private val vertexBuffer: FloatBuffer
    private val indexBuffer: ByteBuffer

    private val vertexShaderCode =
        """
        attribute vec4 vPosition;
        uniform mat4 uMVPMatrix;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
        }
        """

    private val fragmentShaderCode =
        """
        precision mediump float;
        uniform vec4 vColor;
        void main() {
            gl_FragColor = vColor;
        }
        """

    private val vertices = floatArrayOf(
        -1.0f,  1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        1.0f, -1.0f, -1.0f,
        1.0f,  1.0f, -1.0f,
        -1.0f,  1.0f,  1.0f,
        -1.0f, -1.0f,  1.0f,
        1.0f, -1.0f,  1.0f,
        1.0f,  1.0f,  1.0f
    )

    private val indices = byteArrayOf(
        0, 1, 2, 0, 2, 3, // back face
        4, 5, 6, 4, 6, 7, // front face
        0, 1, 5, 0, 5, 4, // left face
        3, 2, 6, 3, 6, 7, // right face
        0, 3, 7, 0, 7, 4, // top face
        1, 2, 6, 1, 6, 5  // bottom face
    )

    private val colors = arrayOf(
        floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f), // red
        floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f), // green
        floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f), // blue
        floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f), // yellow
        floatArrayOf(1.0f, 0.0f, 1.0f, 1.0f), // magenta
        floatArrayOf(0.0f, 1.0f, 1.0f, 1.0f)  // cyan
    )

    private var program: Int

    init {
        // Initialize vertex byte buffer for shape coordinates
        val bb = ByteBuffer.allocateDirect(vertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer().apply {
            put(vertices)
            position(0)
        }

        // Initialize byte buffer for the draw list
        indexBuffer = ByteBuffer.allocateDirect(indices.size).apply {
            put(indices)
            position(0)
        }

        // Prepare shaders and OpenGL program
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    private var positionHandle = 0
    private var colorHandle = 0
    private var mvpMatrixHandle = 0

    private val vertexStride = COORDS_PER_VERTEX * 4 // 4 bytes per vertex
    private val texCoords = floatArrayOf(
        0.0f, 0.0f,  // 左上
        0.0f, 1.0f,  // 左下
        1.0f, 1.0f,  // 右下
        1.0f, 0.0f   // 右上
    )

    fun draw(mvpMatrix: FloatArray) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(program)

        // Get handle to vertex shader's vPosition member
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition").also {
            GLES20.glEnableVertexAttribArray(it)
            GLES20.glVertexAttribPointer(
                it,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer
            )
        }

        // Get handle to shape's transformation matrix
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // Draw each face with different color
        for (i in 0..5) {
            colorHandle = GLES20.glGetUniformLocation(program, "vColor").also {
                GLES20.glUniform4fv(it, 1, colors[i], 0)
            }

            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                6,
                GLES20.GL_UNSIGNED_BYTE,
                indexBuffer.position(6 * i)
            )
        }

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                GLES20.glDeleteShader(shader)
                throw RuntimeException("Error creating shader.")
            }
        }
    }

    companion object {
        const val COORDS_PER_VERTEX = 3
    }
    private val digitTextures = IntArray(6)

    // 创建数字纹理
    fun createDigitTextures() {
        val digitBitmaps = arrayOf(
            createDigitBitmap(0),
            createDigitBitmap(1),
            createDigitBitmap(2),
            createDigitBitmap(3),
            createDigitBitmap(4),
            createDigitBitmap(5)
        )

        // 生成纹理
        GLES20.glGenTextures(6, digitTextures, 0)

        for (i in 0 until 6) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, digitTextures[i])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, digitBitmaps[i], 0)
            digitBitmaps[i].recycle()
        }
    }

    // 创建数字位图
    private fun createDigitBitmap(digit: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textSize = 80f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(digit.toString(), 50f, 70f, paint)
        return bitmap
    }

}
