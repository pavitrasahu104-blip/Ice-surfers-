package com.example.game.render

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.game.engine.FrostRushEngine
import com.example.game.model.CharacterId
import com.example.game.model.SledId

/**
 * Hosts the hardware-accelerated OpenGL ES 2.0 3D viewport inside Jetpack Compose.
 * Automatically manages GLSurfaceView lifecycle (onResume / onPause) to prevent leaked render loops,
 * and safely degrades in headless JVM Robolectric tests where hardware EGL is unavailable.
 */
@Composable
fun FrostRush3DViewport(
    engine: FrostRushEngine,
    selectedCharacter: CharacterId,
    selectedSled: SledId,
    onHudTick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRobolectric = remember { Build.FINGERPRINT.equals("robolectric", ignoreCase = true) }

    if (isRobolectric) {
        Box(
            modifier = modifier
                .testTag("gl_viewport_surface")
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2884E8), Color(0xFFBFE8FF), Color(0xFFEAF7FF))
                    )
                )
        )
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val holder = remember(context, engine) {
        GlSurfaceHolder(context, engine, onHudTick)
    }

    LaunchedEffect(selectedCharacter, selectedSled) {
        holder.renderer.selectedCharacter = selectedCharacter
        holder.renderer.selectedSled = selectedSled
    }

    DisposableEffect(lifecycleOwner, holder) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> holder.glView.onResume()
                Lifecycle.Event.ON_PAUSE -> holder.glView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            holder.glView.onPause()
        }
    }

    AndroidView(
        factory = { holder.glView },
        modifier = modifier.testTag("gl_viewport_surface")
    )
}

private class GlSurfaceHolder(
    context: Context,
    engine: FrostRushEngine,
    onHudTick: () -> Unit
) {
    val renderer = FrostRushGlRenderer(engine = engine, onHudTick = onHudTick)
    val glView = GLSurfaceView(context).apply {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
}
