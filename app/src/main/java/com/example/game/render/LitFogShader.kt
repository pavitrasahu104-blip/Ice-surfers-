package com.example.game.render

import android.opengl.GLES20

/**
 * Custom GLSL directional + hemisphere lit shader with atmospheric winter distance fog and emissive glow.
 */
class LitFogShader {
    val programId: Int

    val aPositionLoc: Int
    val aNormalLoc: Int
    val aColorLoc: Int

    val uMvpMatrixLoc: Int
    val uModelMatrixLoc: Int
    val uFogColorLoc: Int
    val uFogDensityLoc: Int
    val uEmissiveLoc: Int
    val uAlphaMultLoc: Int

    init {
        val vsId = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fsId = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        programId = GLES20.glCreateProgram().also { prog ->
            GLES20.glAttachShader(prog, vsId)
            GLES20.glAttachShader(prog, fsId)
            GLES20.glLinkProgram(prog)
        }

        aPositionLoc = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormalLoc = GLES20.glGetAttribLocation(programId, "aNormal")
        aColorLoc = GLES20.glGetAttribLocation(programId, "aColor")

        uMvpMatrixLoc = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        uModelMatrixLoc = GLES20.glGetUniformLocation(programId, "uModelMatrix")
        uFogColorLoc = GLES20.glGetUniformLocation(programId, "uFogColor")
        uFogDensityLoc = GLES20.glGetUniformLocation(programId, "uFogDensity")
        uEmissiveLoc = GLES20.glGetUniformLocation(programId, "uEmissive")
        uAlphaMultLoc = GLES20.glGetUniformLocation(programId, "uAlphaMult")
    }

    fun use() {
        GLES20.glUseProgram(programId)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            uniform float uFogDensity;

            attribute vec3 aPosition;
            attribute vec3 aNormal;
            attribute vec4 aColor;

            varying vec4 vColor;
            varying vec3 vWorldNormal;
            varying float vFogFactor;

            void main() {
                vec4 clipPos = uMVPMatrix * vec4(aPosition, 1.0);
                gl_Position = clipPos;
                vColor = aColor;
                vWorldNormal = normalize(mat3(uModelMatrix[0].xyz, uModelMatrix[1].xyz, uModelMatrix[2].xyz) * aNormal);
                float dist = abs(clipPos.w);
                vFogFactor = clamp(exp(-pow(dist * uFogDensity, 1.65)), 0.0, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;

            uniform vec3 uFogColor;
            uniform float uEmissive;
            uniform float uAlphaMult;

            varying vec4 vColor;
            varying vec3 vWorldNormal;
            varying float vFogFactor;

            void main() {
                vec3 n = normalize(vWorldNormal);
                vec3 sunDir = normalize(vec3(0.42, 0.84, 0.36));
                float ndotl = max(dot(n, sunDir), 0.0);

                // Crisp alpine sky-to-snow hemisphere ambient + warm winter sunlight
                vec3 skyAmbient = mix(vec3(0.46, 0.58, 0.76), vec3(0.84, 0.93, 1.0), n.y * 0.5 + 0.5);
                vec3 sunLight = vec3(1.0, 0.98, 0.94) * (ndotl * 0.68);
                vec3 litRgb = vColor.rgb * (skyAmbient * 0.66 + sunLight) + vColor.rgb * uEmissive;

                // Subtle icy rim highlight
                float rim = pow(1.0 - max(abs(n.z), 0.0), 2.5) * 0.14;
                litRgb += vec3(0.75, 0.95, 1.0) * rim;

                vec3 finalRgb = mix(uFogColor, clamp(litRgb, 0.0, 1.0), vFogFactor);
                gl_FragColor = vec4(finalRgb, vColor.a * uAlphaMult);
            }
        """
    }
}
