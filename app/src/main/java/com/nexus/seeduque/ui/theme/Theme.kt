package com.nexus.seeduque.ui.theme

import androidx.compose.ui.graphics.Color

object CyberCores {
    val BgBase = Color(0xFF0A0B10)
    val BgSurface = Color(0xFF12141D)
    val BgCard = Color(0xFF161924)
    val BgCardAlt = Color(0xFF1A1E2B)

    val NeonCyan = Color(0xFF00F0FF)
    val NeonMagenta = Color(0xFFFF0055)
    val CyberYellow = Color(0xFFFFAA00)
    val NeonGreen = Color(0xFF00FF66)
    val NeonPurple = Color(0xFFBD00FF)

    val TextPrimary = Color(0xFFF0F4FC)
    val TextDim = Color(0xFF7A889B)

    fun parseHex(hex: String?): Color {
        if (hex.isNullOrBlank()) return NeonCyan
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            NeonCyan
        }
    }
}