package com.farah.jewelryar.ar

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLU
import android.util.AttributeSet
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

/**
 * Custom GLSurfaceView for rendering 3D rings using OpenGL ES 2.0
 */
class Ring3DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {
    private val renderer = Ring3DGLRenderer()

    init {
        Log.d("Ring3DView", "Initializing Ring3DView")
        setEGLContextClientVersion(2) // OpenGL ES 2.0
        // Make the view transparent so camera preview shows
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(android.graphics.PixelFormat.TRANSPARENT)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        Log.d("Ring3DView", "Ring3DView initialized")
    }

    fun setRingStyle(style: RingStyle) {
        Log.d("Ring3DView", "Setting ring style: ${style.name}")
        renderer.setRingStyle(style)
    }

    fun setRingPosition(x: Float, y: Float, scale: Float, rotationZ: Float) {
        renderer.setRingPosition(x, y, scale, rotationZ)
    }

    fun cleanup() {
        Log.d("Ring3DView", "Cleaning up Ring3DView")
        renderer.cleanup()
    }

    private class Ring3DGLRenderer : GLSurfaceView.Renderer {
        private var ringShaderProgram: Int = 0
        private var torusVertexBuffer: Int = 0
        private var torusIndexBuffer: Int = 0
        private var torusIndexCount: Int = 0

        private var currentRingStyle: RingStyle = RingStyle.GOLD

        // Ring position and transformation
        private var ringX = 0f
        private var ringY = 0f
        private var ringScale = 1f
        private var ringRotationZ = 0f

        // Projection and view matrices
        private val projectionMatrix = FloatArray(16)
        private val viewMatrix = FloatArray(16)
        private val modelMatrix = FloatArray(16)
        private val mvMatrix = FloatArray(16)
        private val mvpMatrix = FloatArray(16)

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            Log.d("Ring3DGLRenderer", "========== Surface Created ==========")

            try {
                // Set background color to transparent
                GLES20.glClearColor(0f, 0f, 0f, 0f)

                // Enable depth testing
                GLES20.glEnable(GLES20.GL_DEPTH_TEST)
                GLES20.glDepthFunc(GLES20.GL_LEQUAL)

                // Create shader program
                ringShaderProgram = createShaderProgram()
                Log.d("Ring3DGLRenderer", "Shader program created: $ringShaderProgram")

                // Generate torus geometry
                generateTorusGeometry()
                Log.d("Ring3DGLRenderer", "Torus geometry generated")

                Log.d("Ring3DGLRenderer", "OpenGL initialization complete")
            } catch (e: Exception) {
                Log.e("Ring3DGLRenderer", "Error in onSurfaceCreated: ${e.message}", e)
            }
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            Log.d("Ring3DGLRenderer", "Surface changed: $width x $height")

            try {
                GLES20.glViewport(0, 0, width, height)

                val aspect = width.toFloat() / height.toFloat()
                matrixPerspective(projectionMatrix, 45f, aspect, 0.1f, 100f)

                // Setup view matrix (camera position)
                matrixLookAt(
                    viewMatrix,
                    0f, 0f, 2.5f,  // eye position
                    0f, 0f, 0f,    // look at
                    0f, 1f, 0f     // up vector
                )
                Log.d("Ring3DGLRenderer", "Matrices initialized")
            } catch (e: Exception) {
                Log.e("Ring3DGLRenderer", "Error in onSurfaceChanged: ${e.message}", e)
            }
        }

        override fun onDrawFrame(gl: GL10?) {
            try {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

                // Update ring transformation matrix
                matrixIdentity(modelMatrix)
                matrixTranslate(modelMatrix, ringX, ringY, 0f)
                matrixRotateZ(modelMatrix, ringRotationZ)
                matrixScale(modelMatrix, ringScale, ringScale, ringScale)

                // Calculate MVP matrix: projection * view * model
                matrixMultiply(mvMatrix, viewMatrix, modelMatrix)
                matrixMultiply(mvpMatrix, projectionMatrix, mvMatrix)

                // Draw ring
                drawRing()
            } catch (e: Exception) {
                Log.e("Ring3DGLRenderer", "Error in onDrawFrame: ${e.message}", e)
            }
        }

        private fun drawRing() {
            try {
                GLES20.glUseProgram(ringShaderProgram)

                // Get ring colors
                val colors = currentRingStyle.getColors()
                val r = ((colors.primaryColor shr 16) and 0xFF) / 255f
                val g = ((colors.primaryColor shr 8) and 0xFF) / 255f
                val b = (colors.primaryColor and 0xFF) / 255f

                // Set uniform color
                val colorLocation = GLES20.glGetUniformLocation(ringShaderProgram, "color")
                GLES20.glUniform4f(colorLocation, r, g, b, 1f)

                // Set MVP matrix
                val mvpLocation = GLES20.glGetUniformLocation(ringShaderProgram, "mvpMatrix")
                GLES20.glUniformMatrix4fv(mvpLocation, 1, false, mvpMatrix, 0)

                // Enable vertex attribute array
                val positionLocation = GLES20.glGetAttribLocation(ringShaderProgram, "position")
                GLES20.glEnableVertexAttribArray(positionLocation)

                // Bind and draw
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, torusVertexBuffer)
                GLES20.glVertexAttribPointer(positionLocation, 3, GLES20.GL_FLOAT, false, 0, 0)

                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, torusIndexBuffer)
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, torusIndexCount, GLES20.GL_UNSIGNED_SHORT, 0)

                GLES20.glDisableVertexAttribArray(positionLocation)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
            } catch (e: Exception) {
                Log.e("Ring3DGLRenderer", "Error drawing ring: ${e.message}", e)
            }
        }

        private fun generateTorusGeometry() {
            try {
                val majorRadius = 0.8f
                val minorRadius = 0.3f
                val segmentsMajor = 48
                val segmentsMinor = 24

                val vertices = mutableListOf<Float>()
                val indices = mutableListOf<Short>()

                val twoPi = 2f * PI.toFloat()

                // Generate torus vertices
                for (i in 0 until segmentsMajor) {
                    val theta = (i.toFloat() / segmentsMajor) * twoPi
                    val cosTheta = cos(theta)
                    val sinTheta = sin(theta)

                    for (j in 0 until segmentsMinor) {
                        val phi = (j.toFloat() / segmentsMinor) * twoPi
                        val cosPhi = cos(phi)
                        val sinPhi = sin(phi)

                        val x = (majorRadius + minorRadius * cosPhi) * cosTheta
                        val y = minorRadius * sinPhi
                        val z = (majorRadius + minorRadius * cosPhi) * sinTheta

                        vertices.add(x)
                        vertices.add(y)
                        vertices.add(z)
                    }
                }

                // Generate indices
                for (i in 0 until segmentsMajor) {
                    val nextI = (i + 1) % segmentsMajor
                    for (j in 0 until segmentsMinor) {
                        val nextJ = (j + 1) % segmentsMinor

                        val v1 = (i * segmentsMinor + j).toShort()
                        val v2 = (nextI * segmentsMinor + j).toShort()
                        val v3 = (nextI * segmentsMinor + nextJ).toShort()
                        val v4 = (i * segmentsMinor + nextJ).toShort()

                        indices.add(v1)
                        indices.add(v2)
                        indices.add(v4)
                        indices.add(v2)
                        indices.add(v3)
                        indices.add(v4)
                    }
                }

                // Create vertex buffer
                val vertexArray = IntArray(1)
                GLES20.glGenBuffers(1, vertexArray, 0)
                torusVertexBuffer = vertexArray[0]

                val vertexData = FloatArray(vertices.size)
                vertices.forEachIndexed { index, value -> vertexData[index] = value }

                val vertexBuffer = java.nio.ByteBuffer.allocateDirect(vertexData.size * 4)
                    .order(java.nio.ByteOrder.nativeOrder())
                    .asFloatBuffer()
                vertexBuffer.put(vertexData)
                vertexBuffer.position(0)

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, torusVertexBuffer)
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.size * 4, vertexBuffer, GLES20.GL_STATIC_DRAW)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

                // Create index buffer
                val indexArray = IntArray(1)
                GLES20.glGenBuffers(1, indexArray, 0)
                torusIndexBuffer = indexArray[0]

                val indexData = ShortArray(indices.size)
                indices.forEachIndexed { index, value -> indexData[index] = value }

                val indexBuffer = java.nio.ByteBuffer.allocateDirect(indexData.size * 2)
                    .order(java.nio.ByteOrder.nativeOrder())
                    .asShortBuffer()
                indexBuffer.put(indexData)
                indexBuffer.position(0)

                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, torusIndexBuffer)
                GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexData.size * 2, indexBuffer, GLES20.GL_STATIC_DRAW)
                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

                torusIndexCount = indices.size
                Log.d("Ring3DGLRenderer", "Generated torus: vertices=${vertices.size / 3}, indices=$torusIndexCount")
            } catch (e: Exception) {
                Log.e("Ring3DGLRenderer", "Error generating torus: ${e.message}", e)
            }
        }

        private fun createShaderProgram(): Int {
            val vertexShader = """
                uniform mat4 mvpMatrix;
                attribute vec3 position;

                void main() {
                    gl_Position = mvpMatrix * vec4(position, 1.0);
                }
            """.trimIndent()

            val fragmentShader = """
                precision mediump float;
                uniform vec4 color;

                void main() {
                    gl_FragColor = color;
                }
            """.trimIndent()

            val vShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader)
            val fShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)

            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vShader)
            GLES20.glAttachShader(program, fShader)
            GLES20.glLinkProgram(program)

            GLES20.glDeleteShader(vShader)
            GLES20.glDeleteShader(fShader)

            Log.d("Ring3DGLRenderer", "Shader program linked: $program")
            return program
        }

        private fun compileShader(type: Int, shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                Log.e("Ring3DGLRenderer", "Shader compile error: ${GLES20.glGetShaderInfoLog(shader)}")
            }
            return shader
        }

        // Matrix math helper functions
        private fun matrixIdentity(matrix: FloatArray) {
            for (i in matrix.indices) matrix[i] = 0f
            matrix[0] = 1f
            matrix[5] = 1f
            matrix[10] = 1f
            matrix[15] = 1f
        }

        private fun matrixTranslate(matrix: FloatArray, x: Float, y: Float, z: Float) {
            matrix[12] = x
            matrix[13] = y
            matrix[14] = z
        }

        private fun matrixScale(matrix: FloatArray, sx: Float, sy: Float, sz: Float) {
            matrix[0] = sx
            matrix[5] = sy
            matrix[10] = sz
        }

        private fun matrixRotateZ(matrix: FloatArray, angle: Float) {
            val radians = angle * PI.toFloat() / 180f
            val cos = cos(radians)
            val sin = sin(radians)

            val temp0 = matrix[0]
            val temp1 = matrix[1]
            val temp4 = matrix[4]
            val temp5 = matrix[5]

            matrix[0] = temp0 * cos - temp4 * sin
            matrix[1] = temp1 * cos - temp5 * sin
            matrix[4] = temp0 * sin + temp4 * cos
            matrix[5] = temp1 * sin + temp5 * cos
        }

        private fun matrixPerspective(
            matrix: FloatArray,
            fov: Float,
            aspect: Float,
            near: Float,
            far: Float
        ) {
            val f = 1f / tan(fov * PI.toFloat() / 360f)
            val nf = 1f / (near - far)

            matrixIdentity(matrix)
            matrix[0] = f / aspect
            matrix[5] = f
            matrix[10] = (far + near) * nf
            matrix[11] = -1f
            matrix[14] = 2f * far * near * nf
            matrix[15] = 0f
        }

        private fun matrixLookAt(
            matrix: FloatArray,
            eyeX: Float, eyeY: Float, eyeZ: Float,
            centerX: Float, centerY: Float, centerZ: Float,
            upX: Float, upY: Float, upZ: Float
        ) {
            var fX = centerX - eyeX
            var fY = centerY - eyeY
            var fZ = centerZ - eyeZ

            var fLen = sqrt(fX * fX + fY * fY + fZ * fZ)
            fX /= fLen
            fY /= fLen
            fZ /= fLen

            var sX = fY * upZ - fZ * upY
            var sY = fZ * upX - fX * upZ
            var sZ = fX * upY - fY * upX

            var sLen = sqrt(sX * sX + sY * sY + sZ * sZ)
            sX /= sLen
            sY /= sLen
            sZ /= sLen

            val uX = sY * fZ - sZ * fY
            val uY = sZ * fX - sX * fZ
            val uZ = sX * fY - sY * fX

            matrixIdentity(matrix)
            matrix[0] = sX
            matrix[1] = uX
            matrix[2] = -fX
            matrix[4] = sY
            matrix[5] = uY
            matrix[6] = -fY
            matrix[8] = sZ
            matrix[9] = uZ
            matrix[10] = -fZ
            matrix[12] = -sX * eyeX - sY * eyeY - sZ * eyeZ
            matrix[13] = -uX * eyeX - uY * eyeY - uZ * eyeZ
            matrix[14] = fX * eyeX + fY * eyeY + fZ * eyeZ
        }

        private fun matrixMultiply(result: FloatArray, a: FloatArray, b: FloatArray) {
            val tmp = FloatArray(16)
            for (i in 0 until 4) {
                for (j in 0 until 4) {
                    tmp[i * 4 + j] = 0f
                    for (k in 0 until 4) {
                        tmp[i * 4 + j] += a[i * 4 + k] * b[k * 4 + j]
                    }
                }
            }
            System.arraycopy(tmp, 0, result, 0, 16)
        }

        fun setRingStyle(style: RingStyle) {
            currentRingStyle = style
            Log.d("Ring3DGLRenderer", "Ring style changed to: ${style.name}")
        }

        fun setRingPosition(x: Float, y: Float, scale: Float, rotationZ: Float) {
            this.ringX = (x - 0.5f) * 2  // Convert from 0-1 to -1 to 1, centered
            this.ringY = (0.5f - y) * 2  // Convert from 0-1 to -1 to 1, flip Y
            this.ringScale = max(0.1f, min(2f, scale))
            this.ringRotationZ = rotationZ
        }

        fun cleanup() {
            try {
                if (ringShaderProgram != 0) {
                    GLES20.glDeleteProgram(ringShaderProgram)
                }
                if (torusVertexBuffer != 0) {
                    GLES20.glDeleteBuffers(1, intArrayOf(torusVertexBuffer), 0)
                }
                if (torusIndexBuffer != 0) {
                    GLES20.glDeleteBuffers(1, intArrayOf(torusIndexBuffer), 0)
                }
                Log.d("Ring3DGLRenderer", "Cleaned up")
            } catch (e: Exception) {
                Log.e("Ring3DGLRenderer", "Error during cleanup: ${e.message}", e)
            }
        }
    }
}
