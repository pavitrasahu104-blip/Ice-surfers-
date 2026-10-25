package com.example.game.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.engine.CoinInstance
import com.example.game.engine.FrostRushEngine
import com.example.game.engine.ObstacleInstance
import com.example.game.engine.PowerUpInstance
import com.example.game.engine.SidePropKind
import com.example.game.engine.SnowParticle
import com.example.game.engine.TrackSegment
import com.example.game.model.CharacterId
import com.example.game.model.GameState
import com.example.game.model.ObstacleType
import com.example.game.model.PowerUpType
import com.example.game.model.SledId
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class FrostRushGlRenderer(
    private val engine: FrostRushEngine,
    private val onHudTick: () -> Unit
) : GLSurfaceView.Renderer {

    @Volatile
    var selectedCharacter: CharacterId = CharacterId.EXPLORER

    @Volatile
    var selectedSled: SledId = SledId.ALPINE_GLIDER

    private var shader: LitFogShader? = null
    private var meshes: SceneMeshes? = null

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Pre-allocated render snapshot copies (zero per-frame heap churn!)
    private val renderSegments = Array(FrostRushEngine.SEGMENT_COUNT) { TrackSegment() }
    private val renderObstacles = Array(FrostRushEngine.MAX_OBSTACLES) { ObstacleInstance() }
    private val renderCoins = Array(FrostRushEngine.MAX_COINS) { CoinInstance() }
    private val renderPowerUps = Array(FrostRushEngine.MAX_POWERUPS) { PowerUpInstance() }
    private val renderParticles = Array(FrostRushEngine.MAX_SNOW_PARTICLES) { SnowParticle() }

    private var lastFrameNs: Long = 0L
    private var hudAccumulatorSec: Float = 0f
    private var smoothCamX: Float = 0f
    private var smoothCamY: Float = 3.6f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.76f, 0.92f, 1.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        shader = LitFogShader()
        meshes = SceneMeshes()
        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        val safeW = width.coerceAtLeast(1)
        val safeH = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, safeW, safeH)
        val aspect = safeW.toFloat() / safeH.toFloat()
        val fovY = if (aspect < 0.8f) 62f else 54f
        Matrix.perspectiveM(projMatrix, 0, fovY, aspect, 0.4f, 230f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val nowNs = System.nanoTime()
        val rawDt = if (lastFrameNs == 0L) 0.016f else ((nowNs - lastFrameNs) / 1_000_000_000.0).toFloat()
        lastFrameNs = nowNs
        val dt = rawDt.coerceIn(0.001f, 0.05f)

        engine.update(dt)

        hudAccumulatorSec += dt
        if (hudAccumulatorSec >= 0.045f) {
            hudAccumulatorSec = 0f
            onHudTick()
        }

        val s = shader ?: return
        val m = meshes ?: return

        val snap = engine.copyForRender(
            outSegments = renderSegments,
            outObstacles = renderObstacles,
            outCoins = renderCoins,
            outPowerUps = renderPowerUps,
            outParticles = renderParticles
        )

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        s.use()
        GLES20.glUniform3f(s.uFogColorLoc, 0.77f, 0.92f, 1.0f)

        // Third-person camera vs Menu showcase camera
        val isMenu = snap.gameStateOrdinal == GameState.MENU.ordinal
        if (isMenu) {
            val orbitX = sin(snap.worldTime * 0.55f) * 2.1f
            val eyeX = orbitX
            val eyeY = 2.45f + cos(snap.worldTime * 0.4f) * 0.18f
            val eyeZ = 5.1f
            Matrix.setLookAtM(
                viewMatrix, 0,
                eyeX, eyeY, eyeZ,
                0f, 0.85f, -7.5f,
                0f, 1f, 0f
            )
            smoothCamX = 0f
            smoothCamY = 3.65f
        } else {
            val targetCamX = snap.playerX * 0.64f
            val targetCamY = 3.65f + snap.playerY * 0.42f
            smoothCamX += (targetCamX - smoothCamX) * (dt * 10f).coerceAtMost(1f)
            smoothCamY += (targetCamY - smoothCamY) * (dt * 8f).coerceAtMost(1f)

            val shakeX = if (snap.crashShake > 0f) sin(snap.worldTime * 55f) * snap.crashShake * 0.28f else 0f
            val shakeY = if (snap.crashShake > 0f) cos(snap.worldTime * 48f) * snap.crashShake * 0.22f else 0f
            val boostPull = if (snap.boostActive) 0.95f else 0f

            Matrix.setLookAtM(
                viewMatrix, 0,
                smoothCamX + shakeX, smoothCamY + shakeY, 6.45f + boostPull,
                snap.playerX * 0.45f, 0.95f + snap.playerY * 0.3f, -13.5f,
                0f, 1f, 0f
            )
        }
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // 1. Sky backdrop & distant snow-capped mountain range (disable fog on sky wall)
        GLES20.glUniform1f(s.uFogDensityLoc, 0.0f)
        drawMeshIdentity(s, m.skyBackdrop, emissive = 0.25f)
        GLES20.glUniform1f(s.uFogDensityLoc, 0.0042f)
        drawMeshIdentity(s, m.distantMountains, emissive = 0.05f)

        // Enable winter atmospheric depth fog for track & gameplay objects
        GLES20.glUniform1f(s.uFogDensityLoc, 0.0095f)

        // 2. Track segments, frozen rivers, fences, and snowy slope scenery
        for (seg in renderSegments) {
            drawTranslated(s, m.trackSegment, 0f, 0f, seg.zStart, emissive = 0.04f)
            if (seg.hasFrozenRiver) {
                drawTranslated(s, m.frozenRiverPatch, 0f, 0f, seg.zStart, emissive = 0.14f)
            }
            if (seg.hasWoodenFenceLeft) {
                drawTranslated(s, m.woodenSideFence, -4.45f, 0f, seg.zStart)
            }
            if (seg.hasWoodenFenceRight) {
                drawTranslated(s, m.woodenSideFence, 4.45f, 0f, seg.zStart)
            }
            for (prop in seg.props) {
                val propMesh = when (prop.kind) {
                    SidePropKind.PINE_TREE -> m.pineTree
                    SidePropKind.ICE_SPIRE -> m.iceSpire
                    SidePropKind.BOULDER -> m.boulder
                }
                val glow = if (prop.kind == SidePropKind.ICE_SPIRE) 0.15f else 0.0f
                drawTransformed(
                    s, propMesh,
                    tx = prop.offsetX, ty = 0.15f, tz = seg.zStart + prop.offsetZ,
                    sx = prop.scale, sy = prop.scale, sz = prop.scale,
                    emissive = glow
                )
            }
        }

        // 3. Obstacles + ground contact shadows
        for (obs in renderObstacles) {
            if (!obs.active) continue
            val scale = if (obs.shattered) (1f + obs.shatterAnim * 0.6f) else 1f
            val alpha = if (obs.shattered) (1f - obs.shatterAnim).coerceAtLeast(0f) else 1f

            // Ground shadow
            if (!obs.shattered && obs.type != ObstacleType.SNOW_RAMP) {
                drawTransformed(
                    s, m.shadowDisc,
                    tx = obs.x, ty = 0.015f, tz = obs.z,
                    sx = 1.15f, sy = 1f, sz = 1.0f,
                    alphaMult = 0.85f
                )
            }

            when (obs.type) {
                ObstacleType.SNOW_TREE -> drawTransformed(s, m.pineTree, obs.x, 0f, obs.z, sx = scale, sy = scale, sz = scale, alphaMult = alpha)
                ObstacleType.ICE_BLOCK -> drawTransformed(s, m.iceBlock, obs.x, 0f, obs.z, sx = scale, sy = scale, sz = scale, emissive = 0.16f, alphaMult = alpha)
                ObstacleType.ROLLING_SNOWBALL -> drawTransformed(
                    s, m.snowball,
                    tx = obs.x, ty = 0.54f, tz = obs.z,
                    rx = obs.rollAngleDeg,
                    sx = 1.35f * scale, sy = 1.35f * scale, sz = 1.35f * scale,
                    alphaMult = alpha
                )
                ObstacleType.WOODEN_BARRIER_LOW -> drawTransformed(s, m.lowBarrier, obs.x, 0f, obs.z, sx = scale, sy = scale, sz = scale, alphaMult = alpha)
                ObstacleType.WOODEN_GATE_HIGH -> drawTransformed(s, m.highGate, obs.x, 0f, obs.z, sx = scale, sy = scale, sz = scale, alphaMult = alpha)
                ObstacleType.ROCK -> drawTransformed(s, m.boulder, obs.x, 0f, obs.z, sx = 1.05f * scale, sy = 1.05f * scale, sz = 1.05f * scale, alphaMult = alpha)
                ObstacleType.NARROW_PILLAR -> drawTransformed(s, m.narrowPillar, obs.x, 0f, obs.z, sx = scale, sy = scale, sz = scale, alphaMult = alpha)
                ObstacleType.SNOW_RAMP -> drawTransformed(s, m.snowRamp, obs.x, 0f, obs.z, emissive = 0.12f)
            }
        }

        // 4. Collectible Gold Coins (spinning + pop animation on pickup)
        for (coin in renderCoins) {
            if (!coin.active) continue
            val popScale = if (coin.collected) (1f + sin(coin.collectAnim * Math.PI.toFloat()) * 0.65f) else 1f
            val alpha = if (coin.collected) (1f - coin.collectAnim).coerceAtLeast(0f) else 1f
            if (!coin.collected) {
                drawTransformed(s, m.shadowDisc, coin.x, 0.015f, coin.z, sx = 0.52f, sy = 1f, sz = 0.52f, alphaMult = 0.65f)
            }
            drawTransformed(
                s, m.coin,
                tx = coin.x, ty = coin.y, tz = coin.z,
                ry = coin.spinPhaseDeg,
                sx = popScale, sy = popScale, sz = popScale,
                emissive = 0.34f,
                alphaMult = alpha
            )
        }

        // 5. Power-Up 3D Collectibles
        for (pu in renderPowerUps) {
            if (!pu.active || pu.collected) continue
            val bobY = pu.y + sin((snap.worldTime * 3.5f) + pu.z) * 0.14f
            drawTransformed(s, m.shadowDisc, pu.x, 0.015f, pu.z, sx = 0.65f, sy = 1f, sz = 0.65f, alphaMult = 0.75f)
            val puMesh = when (pu.type) {
                PowerUpType.COIN_MAGNET -> m.magnetPickup
                PowerUpType.SPEED_BOOST -> m.boostPickup
                PowerUpType.SHIELD -> m.shieldPickup
            }
            drawTransformed(
                s, puMesh,
                tx = pu.x, ty = bobY, tz = pu.z,
                ry = pu.spinPhaseDeg,
                sx = 1.15f, sy = 1.15f, sz = 1.15f,
                emissive = 0.38f
            )
        }

        // 6. Player Sled + Character + Active Power-Up Visual Effects!
        val idleBob = if (isMenu) sin(snap.worldTime * 2.6f) * 0.04f else 0f
        val px = snap.playerX
        val py = snap.playerY + idleBob

        // Ground shadow shrinks softly as player jumps higher
        val shadowScale = (1.15f - snap.playerY * 0.22f).coerceIn(0.45f, 1.15f)
        drawTransformed(
            s, m.shadowDisc,
            tx = px, ty = 0.018f, tz = 0f,
            sx = shadowScale * 0.95f, sy = 1f, sz = shadowScale * 1.45f,
            alphaMult = (0.9f - snap.playerY * 0.18f).coerceIn(0.25f, 0.9f)
        )

        val sledMesh = m.getSledMesh(selectedSled)
        val charMesh = m.getCharacterMesh(selectedCharacter, snap.isSliding)

        drawTransformed(
            s, sledMesh,
            tx = px, ty = py, tz = 0f,
            rx = snap.pitchAngleDeg, rz = snap.leanAngleDeg,
            emissive = if (snap.boostActive) 0.22f else 0.06f
        )
        drawTransformed(
            s, charMesh,
            tx = px, ty = py, tz = 0f,
            rx = snap.pitchAngleDeg, rz = snap.leanAngleDeg,
            emissive = 0.08f
        )

        // Speed Boost flame jet behind sled!
        if (snap.boostActive) {
            val pulse = 0.9f + sin(snap.worldTime * 28f) * 0.25f
            drawTransformed(
                s, m.boostFlameCone,
                tx = px - 0.24f, ty = py + 0.26f, tz = 0.85f,
                rx = 90f,
                sx = 1f, sy = pulse, sz = 1f,
                emissive = 0.75f
            )
            drawTransformed(
                s, m.boostFlameCone,
                tx = px + 0.24f, ty = py + 0.26f, tz = 0.85f,
                rx = 90f,
                sx = 1f, sy = pulse, sz = 1f,
                emissive = 0.75f
            )
        }

        // Coin Magnet pulsing orbit ring!
        if (snap.magnetActive) {
            drawTransformed(
                s, m.magnetAuraRing,
                tx = px, ty = py + 0.15f, tz = 0f,
                ry = (snap.worldTime * 220f) % 360f,
                emissive = 0.55f
            )
        }

        // Shield translucent frost sphere around rider!
        if (snap.shieldActive) {
            GLES20.glDepthMask(false)
            drawTransformed(
                s, m.shieldBubble,
                tx = px, ty = py, tz = 0f,
                ry = (snap.worldTime * 95f) % 360f,
                emissive = 0.45f,
                alphaMult = 0.88f
            )
            GLES20.glDepthMask(true)
        }

        // 7. Falling Snow & Sled Spray Particles
        for (p in renderParticles) {
            if (p.y < -1f) continue
            drawTransformed(
                s, m.snowParticleCube,
                tx = p.x, ty = p.y, tz = p.z,
                sx = p.size, sy = p.size, sz = p.size,
                emissive = 0.35f,
                alphaMult = if (p.isSpray) p.life.coerceIn(0f, 1f) else 0.85f
            )
        }
    }

    private fun drawMeshIdentity(shader: LitFogShader, mesh: GlMesh, emissive: Float = 0f) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uMvpMatrixLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrixLoc, 1, false, modelMatrix, 0)
        GLES20.glUniform1f(shader.uEmissiveLoc, emissive)
        GLES20.glUniform1f(shader.uAlphaMultLoc, 1f)
        mesh.draw(shader.aPositionLoc, shader.aNormalLoc, shader.aColorLoc)
    }

    private fun drawTranslated(
        shader: LitFogShader,
        mesh: GlMesh,
        tx: Float,
        ty: Float,
        tz: Float,
        emissive: Float = 0f
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, tx, ty, tz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uMvpMatrixLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrixLoc, 1, false, modelMatrix, 0)
        GLES20.glUniform1f(shader.uEmissiveLoc, emissive)
        GLES20.glUniform1f(shader.uAlphaMultLoc, 1f)
        mesh.draw(shader.aPositionLoc, shader.aNormalLoc, shader.aColorLoc)
    }

    private fun drawTransformed(
        shader: LitFogShader,
        mesh: GlMesh,
        tx: Float,
        ty: Float,
        tz: Float,
        rx: Float = 0f,
        ry: Float = 0f,
        rz: Float = 0f,
        sx: Float = 1f,
        sy: Float = 1f,
        sz: Float = 1f,
        emissive: Float = 0f,
        alphaMult: Float = 1f
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, tx, ty, tz)
        if (ry != 0f) Matrix.rotateM(modelMatrix, 0, ry, 0f, 1f, 0f)
        if (rx != 0f) Matrix.rotateM(modelMatrix, 0, rx, 1f, 0f, 0f)
        if (rz != 0f) Matrix.rotateM(modelMatrix, 0, rz, 0f, 0f, 1f)
        if (sx != 1f || sy != 1f || sz != 1f) Matrix.scaleM(modelMatrix, 0, sx, sy, sz)

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uMvpMatrixLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrixLoc, 1, false, modelMatrix, 0)
        GLES20.glUniform1f(shader.uEmissiveLoc, emissive)
        GLES20.glUniform1f(shader.uAlphaMultLoc, alphaMult)
        mesh.draw(shader.aPositionLoc, shader.aNormalLoc, shader.aColorLoc)
    }
}
