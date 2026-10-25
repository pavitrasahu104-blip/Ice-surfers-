package com.example.game.render

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Holds an interleaved vertex buffer:
 * [x, y, z, nx, ny, nz, r, g, b, a] = 10 floats per vertex (40 bytes).
 */
class GlMesh(vertices: FloatArray) {
    val vertexCount: Int = vertices.size / FLOATS_PER_VERTEX
    val buffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertices)
            position(0)
        }

    fun draw(posLoc: Int, normLoc: Int, colorLoc: Int) {
        if (vertexCount <= 0) return
        val stride = FLOATS_PER_VERTEX * 4
        buffer.position(0)
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, stride, buffer)
        GLES20.glEnableVertexAttribArray(posLoc)

        buffer.position(3)
        GLES20.glVertexAttribPointer(normLoc, 3, GLES20.GL_FLOAT, false, stride, buffer)
        GLES20.glEnableVertexAttribArray(normLoc)

        buffer.position(6)
        GLES20.glVertexAttribPointer(colorLoc, 4, GLES20.GL_FLOAT, false, stride, buffer)
        GLES20.glEnableVertexAttribArray(colorLoc)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
    }

    companion object {
        const val FLOATS_PER_VERTEX = 10
    }
}

class MeshBuilder {
    private val data = ArrayList<Float>(1024)

    fun toGlMesh(): GlMesh {
        val arr = FloatArray(data.size)
        for (i in data.indices) arr[i] = data[i]
        return GlMesh(arr)
    }

    private fun pushVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float, a: Float
    ) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(r); data.add(g); data.add(b); data.add(a)
    }

    fun addTriangleAutoNormal(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        val ux = x2 - x1; val uy = y2 - y1; val uz = z2 - z1
        val vx = x3 - x1; val vy = y3 - y1; val vz = z3 - z1
        var nx = uy * vz - uz * vy
        var ny = uz * vx - ux * vz
        var nz = ux * vy - uy * vx
        val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
        nx /= len; ny /= len; nz /= len
        pushVertex(x1, y1, z1, nx, ny, nz, r, g, b, a)
        pushVertex(x2, y2, z2, nx, ny, nz, r, g, b, a)
        pushVertex(x3, y3, z3, nx, ny, nz, r, g, b, a)
    }

    fun addQuad(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        addTriangleAutoNormal(x1, y1, z1, x2, y2, z2, x3, y3, z3, r, g, b, a)
        addTriangleAutoNormal(x1, y1, z1, x3, y3, z3, x4, y4, z4, r, g, b, a)
    }

    fun addBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f
        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz

        // Front (+Z)
        addQuad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a)
        // Back (-Z)
        addQuad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, r, g, b, a)
        // Top (+Y)
        addQuad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, r, g, b, a)
        // Bottom (-Y)
        addQuad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a)
        // Right (+X)
        addQuad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, r, g, b, a)
        // Left (-X)
        addQuad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a)
    }

    fun addConeFrustum(
        cx: Float, bottomY: Float, cz: Float,
        bottomRadius: Float, topRadius: Float, height: Float,
        sides: Int,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        val topY = bottomY + height
        for (i in 0 until sides) {
            val a0 = (2.0 * PI * i / sides).toFloat()
            val a1 = (2.0 * PI * (i + 1) / sides).toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)

            val bx0 = cx + c0 * bottomRadius
            val bz0 = cz + s0 * bottomRadius
            val bx1 = cx + c1 * bottomRadius
            val bz1 = cz + s1 * bottomRadius

            val tx0 = cx + c0 * topRadius
            val tz0 = cz + s0 * topRadius
            val tx1 = cx + c1 * topRadius
            val tz1 = cz + s1 * topRadius

            if (topRadius <= 0.001f) {
                addTriangleAutoNormal(bx0, bottomY, bz0, bx1, bottomY, bz1, cx, topY, cz, r, g, b, a)
            } else {
                addQuad(bx0, bottomY, bz0, bx1, bottomY, bz1, tx1, topY, tz1, tx0, topY, tz0, r, g, b, a)
                addTriangleAutoNormal(cx, topY, cz, tx0, topY, tz0, tx1, topY, tz1, r, g, b, a)
            }
            addTriangleAutoNormal(cx, bottomY, cz, bx1, bottomY, bz1, bx0, bottomY, bz0, r, g, b, a)
        }
    }

    fun addSphere(
        cx: Float, cy: Float, cz: Float,
        radius: Float,
        stacks: Int, slices: Int,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        for (i in 0 until stacks) {
            val phi0 = (PI * i / stacks).toFloat()
            val phi1 = (PI * (i + 1) / stacks).toFloat()
            val y0 = cy + radius * cos(phi0)
            val r0 = radius * sin(phi0)
            val y1 = cy + radius * cos(phi1)
            val r1 = radius * sin(phi1)

            for (j in 0 until slices) {
                val th0 = (2.0 * PI * j / slices).toFloat()
                val th1 = (2.0 * PI * (j + 1) / slices).toFloat()

                val x00 = cx + r0 * cos(th0); val z00 = cz + r0 * sin(th0)
                val x01 = cx + r0 * cos(th1); val z01 = cz + r0 * sin(th1)
                val x10 = cx + r1 * cos(th0); val z10 = cz + r1 * sin(th0)
                val x11 = cx + r1 * cos(th1); val z11 = cz + r1 * sin(th1)

                if (i != 0) {
                    addTriangleAutoNormal(x00, y0, z00, x10, y1, z10, x11, y1, z11, r, g, b, a)
                }
                if (i != stacks - 1) {
                    addTriangleAutoNormal(x00, y0, z00, x11, y1, z11, x01, y0, z01, r, g, b, a)
                }
            }
        }
    }

    fun addDiscXZ(
        cx: Float, y: Float, cz: Float,
        rx: Float, rz: Float, segments: Int,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        for (i in 0 until segments) {
            val a0 = (2.0 * PI * i / segments).toFloat()
            val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
            val x0 = cx + cos(a0) * rx
            val z0 = cz + sin(a0) * rz
            val x1 = cx + cos(a1) * rx
            val z1 = cz + sin(a1) * rz
            pushVertex(cx, y, cz, 0f, 1f, 0f, r, g, b, a)
            pushVertex(x0, y, z0, 0f, 1f, 0f, r, g, b, a)
            pushVertex(x1, y, z1, 0f, 1f, 0f, r, g, b, a)
        }
    }

    /**
     * Sloped snow ramp rising from +halfDepth (low front facing player) to -halfDepth (high launch lip).
     */
    fun addRampWedge(
        width: Float, height: Float, depth: Float,
        r: Float, g: Float, b: Float
    ) {
        val hw = width * 0.5f
        val hd = depth * 0.5f
        // Slope top: front (+hd, y=0.04) up to back (-hd, y=height)
        addQuad(-hw, 0.04f, hd, hw, 0.04f, hd, hw, height, -hd, -hw, height, -hd, r, g, b)
        // Back wall (-Z)
        addQuad(hw, 0f, -hd, -hw, 0f, -hd, -hw, height, -hd, hw, height, -hd, r * 0.85f, g * 0.9f, b)
        // Left & right triangles
        addTriangleAutoNormal(-hw, 0f, -hd, -hw, 0.04f, hd, -hw, height, -hd, r * 0.9f, g * 0.95f, b)
        addTriangleAutoNormal(hw, 0.04f, hd, hw, 0f, -hd, hw, height, -hd, r * 0.9f, g * 0.95f, b)
    }

    /**
     * Extruded octagonal coin facing along Z (spins around Y).
     */
    fun addOctagonCoin(radius: Float, thickness: Float, r: Float, g: Float, b: Float) {
        val hz = thickness * 0.5f
        val sides = 10
        for (i in 0 until sides) {
            val a0 = (2.0 * PI * i / sides).toFloat()
            val a1 = (2.0 * PI * (i + 1) / sides).toFloat()
            val x0 = cos(a0) * radius; val y0 = sin(a0) * radius
            val x1 = cos(a1) * radius; val y1 = sin(a1) * radius

            // Front face (+Z)
            addTriangleAutoNormal(0f, 0f, hz, x0, y0, hz, x1, y1, hz, r, g, b)
            // Back face (-Z)
            addTriangleAutoNormal(0f, 0f, -hz, x1, y1, -hz, x0, y0, -hz, r, g, b)
            // Rim
            addQuad(
                x0, y0, hz, x0, y0, -hz, x1, y1, -hz, x1, y1, hz,
                (r * 0.88f).coerceAtMost(1f), (g * 0.72f).coerceAtMost(1f), b * 0.5f
            )
        }
        // Bright gold inner star core
        addBox(0f, 0f, 0f, radius * 0.35f, radius * 0.95f, thickness * 1.25f, 1.0f, 0.93f, 0.55f)
    }
}
