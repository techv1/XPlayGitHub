package com.techv1.xplay.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

// ── Durations (ms) ────────────────────────────────────────────────────────────
object MotionDuration {
    const val SHORT   = 150
    const val MEDIUM  = 250
    const val LONG    = 400
    const val PLAYER  = 200 // control fade in/out
}

// ── Easings ───────────────────────────────────────────────────────────────────
object MotionEasing {
    val Standard: Easing = FastOutSlowInEasing
}

// ── Spec helpers ─────────────────────────────────────────────────────────────
fun <T> standardTween() = tween<T>(
    durationMillis = MotionDuration.MEDIUM,
    easing = MotionEasing.Standard
)

fun <T> shortTween() = tween<T>(
    durationMillis = MotionDuration.SHORT,
    easing = MotionEasing.Standard
)

// Press scale spring — tactile card tap feeling
fun <T> pressSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessHigh
)

// Shared-element spring
fun <T> sharedElementSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)
