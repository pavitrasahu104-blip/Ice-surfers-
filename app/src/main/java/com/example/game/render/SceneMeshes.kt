package com.example.game.render

import com.example.game.engine.FrostRushEngine
import com.example.game.model.CharacterId
import com.example.game.model.SledId

/**
 * Generates all procedural 3D meshes in code (terrain, rivers, mountains, trees, ice blocks,
 * snowballs, wooden barriers, high gates, ramps, coins, power-ups, 3 original characters, and 3 original sleds).
 */
class SceneMeshes {

    val trackSegment: GlMesh = buildTrackSegment()
    val frozenRiverPatch: GlMesh = buildFrozenRiver()
    val woodenSideFence: GlMesh = buildSideFence()
    val distantMountains: GlMesh = buildMountainRange()

    val pineTree: GlMesh = buildPineTree()
    val iceSpire: GlMesh = buildIceSpire()
    val boulder: GlMesh = buildBoulder()
    val iceBlock: GlMesh = buildIceBlock()
    val snowball: GlMesh = buildSnowball()
    val lowBarrier: GlMesh = buildLowBarrier()
    val highGate: GlMesh = buildHighGate()
    val narrowPillar: GlMesh = buildNarrowPillar()
    val snowRamp: GlMesh = buildSnowRamp()

    val coin: GlMesh = MeshBuilder().apply {
        addOctagonCoin(radius = 0.42f, thickness = 0.13f, r = 1.0f, g = 0.78f, b = 0.16f)
    }.toGlMesh()

    val magnetPickup: GlMesh = buildMagnetPickup()
    val boostPickup: GlMesh = buildBoostPickup()
    val shieldPickup: GlMesh = buildShieldPickup()
    val shieldBubble: GlMesh = MeshBuilder().apply {
        addSphere(0f, 0.78f, 0f, 1.18f, stacks = 8, slices = 12, r = 0.12f, g = 0.96f, b = 0.62f, a = 0.34f)
    }.toGlMesh()
    val magnetAuraRing: GlMesh = buildMagnetRing()
    val boostFlameCone: GlMesh = MeshBuilder().apply {
        addConeFrustum(0f, 0f, 0f, 0.24f, 0.02f, 1.25f, 8, 0.0f, 0.94f, 1.0f, 0.85f)
    }.toGlMesh()

    val shadowDisc: GlMesh = MeshBuilder().apply {
        addDiscXZ(0f, 0.02f, 0f, 0.75f, 0.75f, 12, 0.07f, 0.16f, 0.29f, 0.42f)
    }.toGlMesh()

    val snowParticleCube: GlMesh = MeshBuilder().apply {
        addBox(0f, 0f, 0f, 1f, 1f, 1f, 0.96f, 0.99f, 1.0f, 0.92f)
    }.toGlMesh()

    val skyBackdrop: GlMesh = buildSkyBackdrop()

    private val characterStanding = CharacterId.entries.associateWith { buildCharacter(it, crouched = false) }
    private val characterSliding = CharacterId.entries.associateWith { buildCharacter(it, crouched = true) }
    private val sledMeshes = SledId.entries.associateWith { buildSled(it) }

    fun getCharacterMesh(id: CharacterId, isSliding: Boolean): GlMesh {
        return (if (isSliding) characterSliding[id] else characterStanding[id])
            ?: characterStanding.getValue(CharacterId.EXPLORER)
    }

    fun getSledMesh(id: SledId): GlMesh {
        return sledMeshes[id] ?: sledMeshes.getValue(SledId.ALPINE_GLIDER)
    }

    private fun buildTrackSegment(): GlMesh = MeshBuilder().apply {
        val len = FrostRushEngine.SEGMENT_LENGTH
        val halfTrack = 4.35f
        // Main groomed snow track (-len to 0)
        addQuad(
            -halfTrack, 0f, 0f,
            halfTrack, 0f, 0f,
            halfTrack, 0f, -len,
            -halfTrack, 0f, -len,
            0.93f, 0.97f, 1.0f
        )
        // Subtle icy lane guide grooves
        val groove1 = -FrostRushEngine.LANE_WIDTH * 0.5f
        val groove2 = FrostRushEngine.LANE_WIDTH * 0.5f
        addQuad(groove1 - 0.06f, 0.012f, 0f, groove1 + 0.06f, 0.012f, 0f, groove1 + 0.06f, 0.012f, -len, groove1 - 0.06f, 0.012f, -len, 0.68f, 0.87f, 0.98f)
        addQuad(groove2 - 0.06f, 0.012f, 0f, groove2 + 0.06f, 0.012f, 0f, groove2 + 0.06f, 0.012f, -len, groove2 - 0.06f, 0.012f, -len, 0.68f, 0.87f, 0.98f)

        // Left & right raised snowy embankments & wide mountain slopes
        addQuad(-halfTrack - 1.6f, 0.85f, 0f, -halfTrack, 0f, 0f, -halfTrack, 0f, -len, -halfTrack - 1.6f, 0.85f, -len, 0.84f, 0.92f, 0.99f)
        addQuad(-26f, 4.2f, 0f, -halfTrack - 1.6f, 0.85f, 0f, -halfTrack - 1.6f, 0.85f, -len, -26f, 4.2f, -len, 0.89f, 0.95f, 1.0f)

        addQuad(halfTrack, 0f, 0f, halfTrack + 1.6f, 0.85f, 0f, halfTrack + 1.6f, 0.85f, -len, halfTrack, 0f, -len, 0.84f, 0.92f, 0.99f)
        addQuad(halfTrack + 1.6f, 0.85f, 0f, 26f, 4.2f, 0f, 26f, 4.2f, -len, halfTrack + 1.6f, 0.85f, -len, 0.89f, 0.95f, 1.0f)
    }.toGlMesh()

    private fun buildFrozenRiver(): GlMesh = MeshBuilder().apply {
        val len = FrostRushEngine.SEGMENT_LENGTH * 0.55f
        addQuad(-22f, 0.025f, -2.5f, 22f, 0.025f, -2.5f, 22f, 0.025f, -2.5f - len, -22f, 0.025f, -2.5f - len, 0.34f, 0.82f, 0.96f, 0.88f)
        // Bright specular ice streaks
        addQuad(-8f, 0.032f, -3.5f, 8f, 0.032f, -4.1f, 8f, 0.032f, -4.5f, -8f, 0.032f, -3.9f, 0.82f, 0.97f, 1.0f, 0.9f)
        addQuad(-6f, 0.032f, -6.2f, 9f, 0.032f, -5.6f, 9f, 0.032f, -6.0f, -6f, 0.032f, -6.6f, 0.82f, 0.97f, 1.0f, 0.9f)
    }.toGlMesh()

    private fun buildSideFence(): GlMesh = MeshBuilder().apply {
        // Posts + rails along segment length
        val zPositions = floatArrayOf(-1.5f, -5.5f, -9.5f)
        for (zp in zPositions) {
            addBox(0f, 0.55f, zp, 0.18f, 1.1f, 0.18f, 0.45f, 0.28f, 0.15f)
            addBox(0f, 1.13f, zp, 0.22f, 0.09f, 0.22f, 0.95f, 0.98f, 1.0f)
        }
        addBox(0f, 0.78f, -5.5f, 0.10f, 0.14f, 9.2f, 0.56f, 0.35f, 0.18f)
        addBox(0f, 0.42f, -5.5f, 0.10f, 0.14f, 9.2f, 0.56f, 0.35f, 0.18f)
    }.toGlMesh()

    private fun buildMountainRange(): GlMesh = MeshBuilder().apply {
        val peaks = arrayOf(
            floatArrayOf(-58f, -155f, 34f, 44f),
            floatArrayOf(-26f, -172f, 38f, 54f),
            floatArrayOf(8f, -178f, 42f, 58f),
            floatArrayOf(40f, -168f, 36f, 48f),
            floatArrayOf(68f, -150f, 32f, 42f)
        )
        for (p in peaks) {
            val cx = p[0]; val cz = p[1]; val rad = p[2]; val h = p[3]
            // Slate blue lower massif
            addConeFrustum(cx, -2f, cz, rad, rad * 0.36f, h * 0.64f, 5, 0.42f, 0.58f, 0.78f)
            // Bright snow-capped summit
            addConeFrustum(cx, -2f + h * 0.62f, cz, rad * 0.38f, 0.0f, h * 0.40f, 5, 0.95f, 0.99f, 1.0f)
        }
    }.toGlMesh()

    private fun buildSkyBackdrop(): GlMesh = MeshBuilder().apply {
        // Large horizon-to-zenith gradient wall behind mountains
        val z = -195f
        val x = 220f
        // Lower mist band
        addQuad(-x, -30f, z, x, -30f, z, x, 28f, z, -x, 28f, z, 0.78f, 0.93f, 1.0f)
        // Mid winter sky
        addQuad(-x, 28f, z, x, 28f, z, x, 75f, z, -x, 75f, z, 0.36f, 0.74f, 0.98f)
        // Upper crisp azure
        addQuad(-x, 75f, z, x, 75f, z, x, 140f, z, -x, 140f, z, 0.14f, 0.52f, 0.92f)
    }.toGlMesh()

    private fun buildPineTree(): GlMesh = MeshBuilder().apply {
        // Trunk
        addConeFrustum(0f, 0f, 0f, 0.24f, 0.16f, 0.85f, 6, 0.38f, 0.24f, 0.14f)
        // Three evergreen foliage tiers + white snow caps
        addConeFrustum(0f, 0.65f, 0f, 1.02f, 0.25f, 1.05f, 7, 0.10f, 0.38f, 0.32f)
        addConeFrustum(0f, 1.15f, 0f, 0.78f, 0.18f, 0.58f, 7, 0.92f, 0.97f, 1.0f)

        addConeFrustum(0f, 1.40f, 0f, 0.82f, 0.18f, 0.95f, 7, 0.12f, 0.44f, 0.36f)
        addConeFrustum(0f, 1.85f, 0f, 0.60f, 0.12f, 0.52f, 7, 0.94f, 0.98f, 1.0f)

        addConeFrustum(0f, 2.15f, 0f, 0.56f, 0.0f, 1.02f, 7, 0.15f, 0.48f, 0.39f)
        addConeFrustum(0f, 2.58f, 0f, 0.36f, 0.0f, 0.62f, 7, 0.97f, 0.99f, 1.0f)
    }.toGlMesh()

    private fun buildIceSpire(): GlMesh = MeshBuilder().apply {
        addConeFrustum(0f, 0f, 0f, 0.55f, 0.05f, 2.3f, 6, 0.22f, 0.84f, 0.98f, 0.92f)
        addConeFrustum(-0.38f, 0f, 0.18f, 0.35f, 0.03f, 1.45f, 5, 0.45f, 0.92f, 1.0f, 0.92f)
        addConeFrustum(0.36f, 0f, -0.15f, 0.32f, 0.03f, 1.25f, 5, 0.16f, 0.74f, 0.95f, 0.92f)
    }.toGlMesh()

    private fun buildBoulder(): GlMesh = MeshBuilder().apply {
        addSphere(0f, 0.38f, 0f, 0.76f, 5, 7, 0.42f, 0.48f, 0.56f)
        // Snow cap on boulder
        addSphere(0f, 0.58f, 0f, 0.58f, 4, 7, 0.95f, 0.98f, 1.0f)
    }.toGlMesh()

    private fun buildIceBlock(): GlMesh = MeshBuilder().apply {
        addBox(0f, 0.45f, 0f, 1.46f, 0.90f, 1.15f, 0.15f, 0.82f, 0.98f, 0.88f)
        addBox(0f, 0.86f, 0f, 1.22f, 0.14f, 0.92f, 0.78f, 0.96f, 1.0f, 0.95f)
        addConeFrustum(-0.35f, 0.88f, 0f, 0.22f, 0.0f, 0.22f, 5, 0.55f, 0.95f, 1.0f)
        addConeFrustum(0.32f, 0.88f, 0.1f, 0.18f, 0.0f, 0.18f, 5, 0.55f, 0.95f, 1.0f)
    }.toGlMesh()

    private fun buildSnowball(): GlMesh = MeshBuilder().apply {
        // Centered at origin so it can spin around X cleanly, rendered at y = 0.54f
        addSphere(0f, 0f, 0f, 0.54f, 7, 9, 0.95f, 0.98f, 1.0f)
        // Subtle blue ice chunks embedded so spin rotation is visibly dynamic!
        addBox(0.34f, 0.18f, 0.15f, 0.22f, 0.22f, 0.22f, 0.52f, 0.84f, 0.98f)
        addBox(-0.28f, -0.25f, -0.22f, 0.24f, 0.20f, 0.20f, 0.52f, 0.84f, 0.98f)
    }.toGlMesh()

    private fun buildLowBarrier(): GlMesh = MeshBuilder().apply {
        // Left & right wooden A-frame legs
        addBox(-0.78f, 0.44f, 0f, 0.16f, 0.88f, 0.36f, 0.48f, 0.29f, 0.14f)
        addBox(0.78f, 0.44f, 0f, 0.16f, 0.88f, 0.36f, 0.48f, 0.29f, 0.14f)
        // High-contrast red & white ski hazard plank
        addBox(0f, 0.66f, 0f, 1.80f, 0.28f, 0.18f, 0.92f, 0.22f, 0.20f)
        addBox(-0.45f, 0.66f, 0.01f, 0.32f, 0.29f, 0.20f, 0.97f, 0.98f, 1.0f)
        addBox(0.45f, 0.66f, 0.01f, 0.32f, 0.29f, 0.20f, 0.97f, 0.98f, 1.0f)
        // Snow dusting on top
        addBox(0f, 0.83f, 0f, 1.82f, 0.07f, 0.20f, 0.96f, 0.99f, 1.0f)
    }.toGlMesh()

    private fun buildHighGate(): GlMesh = MeshBuilder().apply {
        // Tall side timber posts outside player's narrow center body, supporting overhead beam (y = 0.92..2.55)
        addBox(-0.92f, 1.30f, 0f, 0.18f, 2.60f, 0.26f, 0.46f, 0.28f, 0.14f)
        addBox(0.92f, 1.30f, 0f, 0.18f, 2.60f, 0.26f, 0.46f, 0.28f, 0.14f)
        // Overhead wooden beam + bright orange/gold "SLIDE UNDER" hazard banner at y = 0.92..1.65
        addBox(0f, 2.35f, 0f, 2.08f, 0.32f, 0.32f, 0.52f, 0.32f, 0.16f)
        addBox(0f, 1.38f, 0f, 1.72f, 0.92f, 0.16f, 0.96f, 0.46f, 0.10f)
        addBox(0f, 1.02f, 0.02f, 1.74f, 0.18f, 0.18f, 1.0f, 0.84f, 0.18f)
        // Snow roof cap
        addBox(0f, 2.56f, 0f, 2.14f, 0.12f, 0.36f, 0.96f, 0.99f, 1.0f)
    }.toGlMesh()

    private fun buildNarrowPillar(): GlMesh = MeshBuilder().apply {
        addBox(0f, 1.45f, 0f, 1.56f, 2.90f, 1.18f, 0.24f, 0.38f, 0.56f)
        addBox(0f, 1.45f, 0.60f, 1.24f, 2.50f, 0.08f, 0.12f, 0.78f, 0.96f)
        addBox(0f, 2.96f, 0f, 1.64f, 0.16f, 1.26f, 0.96f, 0.99f, 1.0f)
    }.toGlMesh()

    private fun buildSnowRamp(): GlMesh = MeshBuilder().apply {
        addRampWedge(width = 1.82f, height = 0.84f, depth = 2.65f, r = 0.85f, g = 0.95f, b = 1.0f)
        // Glowing cyan launch lip strip
        addBox(0f, 0.82f, -1.26f, 1.78f, 0.09f, 0.16f, 0.0f, 0.92f, 1.0f)
        addBox(-0.88f, 0.42f, 0f, 0.09f, 0.16f, 2.60f, 0.0f, 0.78f, 0.98f)
        addBox(0.88f, 0.42f, 0f, 0.09f, 0.16f, 2.60f, 0.0f, 0.78f, 0.98f)
    }.toGlMesh()

    private fun buildMagnetPickup(): GlMesh = MeshBuilder().apply {
        // Classic horseshoe U-magnet with crimson arch and silver-white pole tips
        addBox(-0.28f, 0.05f, 0f, 0.18f, 0.52f, 0.18f, 0.95f, 0.18f, 0.26f)
        addBox(0.28f, 0.05f, 0f, 0.18f, 0.52f, 0.18f, 0.95f, 0.18f, 0.26f)
        addBox(0f, -0.22f, 0f, 0.74f, 0.18f, 0.18f, 0.95f, 0.18f, 0.26f)
        addBox(-0.28f, 0.36f, 0f, 0.19f, 0.16f, 0.19f, 0.92f, 0.96f, 1.0f)
        addBox(0.28f, 0.36f, 0f, 0.19f, 0.16f, 0.19f, 0.92f, 0.96f, 1.0f)
    }.toGlMesh()

    private fun buildBoostPickup(): GlMesh = MeshBuilder().apply {
        // Futuristic cyan/gold lightning bolt crystal
        addConeFrustum(0f, 0.0f, 0f, 0.38f, 0.0f, 0.55f, 6, 0.0f, 0.94f, 1.0f)
        addConeFrustum(0f, -0.55f, 0f, 0.0f, 0.38f, 0.55f, 6, 0.0f, 0.78f, 0.98f)
        addBox(0f, 0f, 0f, 0.62f, 0.14f, 0.62f, 1.0f, 0.82f, 0.18f)
    }.toGlMesh()

    private fun buildShieldPickup(): GlMesh = MeshBuilder().apply {
        // Emerald hexagonal shield crest with gold border
        addOctagonCoin(radius = 0.45f, thickness = 0.15f, r = 0.06f, g = 0.88f, b = 0.52f)
        addBox(0f, 0f, 0f, 0.52f, 0.52f, 0.19f, 0.62f, 1.0f, 0.82f)
    }.toGlMesh()

    private fun buildMagnetRing(): GlMesh = MeshBuilder().apply {
        val segs = 12
        val rad = 1.05f
        for (i in 0 until segs) {
            val a = (2.0 * Math.PI * i / segs).toFloat()
            val x = kotlin.math.cos(a) * rad
            val z = kotlin.math.sin(a) * rad
            addBox(x, 0.45f, z, 0.14f, 0.08f, 0.14f, 1.0f, 0.32f, 0.55f, 0.85f)
        }
    }.toGlMesh()

    private fun buildCharacter(id: CharacterId, crouched: Boolean): GlMesh = MeshBuilder().apply {
        val suit = id.suitRgb
        val acc = id.accentRgb
        val visor = id.visorRgb

        val torsoY = if (crouched) 0.42f else 0.68f
        val torsoH = if (crouched) 0.34f else 0.56f
        val headY = if (crouched) 0.66f else 1.14f
        val headZ = if (crouched) -0.18f else -0.04f

        // Seated legs & winter boots on sled deck
        addBox(-0.18f, 0.30f, -0.18f, 0.16f, 0.16f, 0.48f, 0.16f, 0.20f, 0.28f)
        addBox(0.18f, 0.30f, -0.18f, 0.16f, 0.16f, 0.48f, 0.16f, 0.20f, 0.28f)
        addBox(-0.18f, 0.34f, -0.44f, 0.18f, 0.20f, 0.16f, acc[0] * 0.85f, acc[1] * 0.85f, acc[2] * 0.85f)
        addBox(0.18f, 0.34f, -0.44f, 0.18f, 0.20f, 0.16f, acc[0] * 0.85f, acc[1] * 0.85f, acc[2] * 0.85f)

        // Insulated winter jacket torso
        addBox(0f, torsoY, 0.04f, 0.48f, torsoH, 0.32f, suit[0], suit[1], suit[2])
        // Accent chest stripe / collar
        addBox(0f, torsoY + torsoH * 0.35f, 0.03f, 0.52f, 0.12f, 0.35f, acc[0], acc[1], acc[2])

        // Arms reaching forward toward sled handlebars
        val armY = if (crouched) 0.44f else 0.66f
        addBox(-0.29f, armY, -0.16f, 0.14f, 0.14f, 0.46f, suit[0] * 0.92f, suit[1] * 0.92f, suit[2] * 0.92f)
        addBox(0.29f, armY, -0.16f, 0.14f, 0.14f, 0.46f, suit[0] * 0.92f, suit[1] * 0.92f, suit[2] * 0.92f)

        // Helmet & glowing goggles/visor
        addSphere(0f, headY, headZ, 0.24f, 6, 8, suit[0], suit[1], suit[2])
        addBox(0f, headY + 0.02f, headZ - 0.16f, 0.34f, 0.13f, 0.16f, visor[0], visor[1], visor[2])

        // Character-unique 3D equipment details!
        when (id) {
            CharacterId.EXPLORER -> {
                // Cozy trailing winter scarf & backpack pack
                addBox(0f, torsoY + 0.02f, 0.24f, 0.34f, torsoH * 0.72f, 0.14f, 0.22f, 0.34f, 0.52f)
                addBox(0.10f, headY - 0.18f, headZ + 0.28f, 0.14f, 0.08f, 0.42f, 0.96f, 0.98f, 1.0f)
            }
            CharacterId.RACER -> {
                // Futuristic twin purple/cyan dorsal aero fins!
                addBox(-0.16f, torsoY + 0.08f, 0.26f, 0.06f, torsoH * 0.85f, 0.26f, visor[0], visor[1], visor[2])
                addBox(0.16f, torsoY + 0.08f, 0.26f, 0.06f, torsoH * 0.85f, 0.26f, visor[0], visor[1], visor[2])
                addBox(0f, headY + 0.22f, headZ + 0.04f, 0.08f, 0.12f, 0.34f, acc[0], acc[1], acc[2])
            }
            CharacterId.ADVENTURER -> {
                // Gold expedition shoulder pauldrons + summit helmet crest!
                addBox(-0.32f, torsoY + torsoH * 0.32f, 0.02f, 0.18f, 0.16f, 0.28f, acc[0], acc[1], acc[2])
                addBox(0.32f, torsoY + torsoH * 0.32f, 0.02f, 0.18f, 0.16f, 0.28f, acc[0], acc[1], acc[2])
                addConeFrustum(0f, headY + 0.18f, headZ, 0.12f, 0.02f, 0.22f, 5, acc[0], acc[1], acc[2])
            }
        }
    }.toGlMesh()

    private fun buildSled(id: SledId): GlMesh = MeshBuilder().apply {
        val body = id.bodyRgb
        val runner = id.runnerRgb
        val trim = id.trimRgb

        // Twin curved runners (z from -0.95 front to +0.85 rear)
        addBox(-0.46f, 0.05f, -0.02f, 0.09f, 0.07f, 1.78f, runner[0], runner[1], runner[2])
        addBox(0.46f, 0.05f, -0.02f, 0.09f, 0.07f, 1.78f, runner[0], runner[1], runner[2])
        // Upturned front runner tips
        addBox(-0.46f, 0.14f, -0.94f, 0.09f, 0.16f, 0.18f, runner[0], runner[1], runner[2])
        addBox(0.46f, 0.14f, -0.94f, 0.09f, 0.16f, 0.18f, runner[0], runner[1], runner[2])

        // Strut risers
        addBox(-0.42f, 0.14f, -0.42f, 0.08f, 0.14f, 0.12f, 0.25f, 0.30f, 0.38f)
        addBox(0.42f, 0.14f, -0.42f, 0.08f, 0.14f, 0.12f, 0.25f, 0.30f, 0.38f)
        addBox(-0.42f, 0.14f, 0.42f, 0.08f, 0.14f, 0.12f, 0.25f, 0.30f, 0.38f)
        addBox(0.42f, 0.14f, 0.42f, 0.08f, 0.14f, 0.12f, 0.25f, 0.30f, 0.38f)

        // Main sled chassis deck
        addBox(0f, 0.22f, 0.02f, 0.88f, 0.14f, 1.48f, body[0], body[1], body[2])
        // Dark padded seat
        addBox(0f, 0.30f, 0.14f, 0.58f, 0.06f, 0.76f, 0.12f, 0.16f, 0.24f)
        // Sculpted front nose cowl + windshield + handlebars
        addBox(0f, 0.32f, -0.62f, 0.72f, 0.18f, 0.46f, body[0], body[1], body[2])
        addBox(0f, 0.44f, -0.50f, 0.54f, 0.14f, 0.24f, trim[0], trim[1], trim[2])
        addBox(0f, 0.48f, -0.38f, 0.64f, 0.06f, 0.07f, 0.20f, 0.24f, 0.32f)

        when (id) {
            SledId.ALPINE_GLIDER -> {
                addBox(-0.46f, 0.24f, 0.02f, 0.08f, 0.08f, 1.38f, trim[0], trim[1], trim[2])
                addBox(0.46f, 0.24f, 0.02f, 0.08f, 0.08f, 1.38f, trim[0], trim[1], trim[2])
            }
            SledId.PLASMA_WING -> {
                // Swept side delta wings!
                addBox(-0.62f, 0.23f, 0.15f, 0.36f, 0.06f, 0.92f, trim[0], trim[1], trim[2])
                addBox(0.62f, 0.23f, 0.15f, 0.36f, 0.06f, 0.92f, trim[0], trim[1], trim[2])
                addBox(0f, 0.44f, 0.68f, 0.10f, 0.34f, 0.28f, runner[0], runner[1], runner[2])
            }
            SledId.BLAZE_TURBINE -> {
                // Dual rear cylindrical rocket boosters + gold armored nose bumper!
                addBox(0f, 0.25f, -0.88f, 0.84f, 0.14f, 0.16f, runner[0], runner[1], runner[2])
                addBox(-0.26f, 0.36f, 0.68f, 0.24f, 0.24f, 0.42f, 0.25f, 0.28f, 0.34f)
                addBox(0.26f, 0.36f, 0.68f, 0.24f, 0.24f, 0.42f, 0.25f, 0.28f, 0.34f)
                addBox(-0.26f, 0.36f, 0.90f, 0.16f, 0.16f, 0.08f, trim[0], trim[1], trim[2])
                addBox(0.26f, 0.36f, 0.90f, 0.16f, 0.16f, 0.08f, trim[0], trim[1], trim[2])
            }
        }
    }.toGlMesh()
}
