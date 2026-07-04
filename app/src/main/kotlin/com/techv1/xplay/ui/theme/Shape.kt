package com.techv1.xplay.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Consistent shape scale — defined once, referenced everywhere
val XPlayShapes = Shapes(
    // Hero cards (featured content)
    extraLarge = RoundedCornerShape(20.dp),
    // Standard content cards
    large = RoundedCornerShape(12.dp),
    // Chips, pills, small elements
    medium = RoundedCornerShape(8.dp),
    // Buttons, small interactive elements
    small = RoundedCornerShape(6.dp),
    // Minimal rounding (e.g. progress bars)
    extraSmall = RoundedCornerShape(4.dp)
)

// Direct references for one-off usage
val ShapeHeroCard    = RoundedCornerShape(20.dp)
val ShapeCard        = RoundedCornerShape(12.dp)
val ShapeChip        = RoundedCornerShape(8.dp)
val ShapeButton      = RoundedCornerShape(50) // fully rounded pill CTA
