package com.techv1.xplay.ui.theme

import androidx.compose.ui.graphics.Color

// ── Background Scale ─────────────────────────────────────────────────────────
val BackgroundDeep    = Color(0xFF0B0B0F)   // primary background, near-black with depth
val BackgroundSurface = Color(0xFF13131A)   // elevated surfaces (cards, bottom sheet)
val BackgroundCard    = Color(0xFF1C1C26)   // card backgrounds

// ── Brand Accent ─────────────────────────────────────────────────────────────
// Single saturated accent — used ONLY on: progress bars, active tab, primary CTA, focus rings
val AccentPrimary     = Color(0xFF7B5CF0)   // electric violet
val AccentLight       = Color(0xFFAB8FF7)   // lighter tint for text on dark bg
val AccentContainer   = Color(0xFF2A1F55)   // accent used as container (pressed states)

// ── Content Scale ────────────────────────────────────────────────────────────
val ContentPrimary    = Color(0xFFEEEEF5)   // primary text / icons
val ContentSecondary  = Color(0xFF9898A8)   // secondary text, metadata
val ContentTertiary   = Color(0xFF555568)   // hints, disabled, placeholder

// ── Semantic ─────────────────────────────────────────────────────────────────
val Success           = Color(0xFF4CAF7D)
val Error             = Color(0xFFEF5350)
val Warning           = Color(0xFFFFB347)

// ── Gradient helpers (scrim over thumbnails) ─────────────────────────────────
val ScrimStart        = Color(0x00000000)   // transparent
val ScrimEnd          = Color(0xCC000000)   // 80% black
