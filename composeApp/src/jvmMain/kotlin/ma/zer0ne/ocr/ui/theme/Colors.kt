package ma.zer0ne.ocr.ui.theme

import androidx.compose.ui.graphics.Color

// JetBrains-style Color Palette
object JBColors {
    val black = Color(0xFF000000)
    val gray = Color(0xFF7D7D7D)
    val lightGray = Color(0xFFCDCDCD)
    val yellow = Color(0xFFFCF84A)
    val orange = Color(0xFFFDB60D)
    val deepOrange = Color(0xFFFC801D)
    val red = Color(0xFFFE2857)
    val crimson = Color(0xFFDD1265)
    val pink = Color(0xFFFF318C)
    val magenta = Color(0xFFFF45ED)
    val purple = Color(0xFFAF1DF5)
    val violet = Color(0xFF6B57FF)
    val blue = Color(0xFF087CFA)
    val cyan = Color(0xFF07C3F2)
    val green = Color(0xFF21D789)
    val lime = Color(0xFF3DEA62)
}

// Common interface for color schemes
interface AppColorScheme {
    val bgDark: Color
    val bgMedium: Color
    val bgLight: Color
    val primary: Color
    val primaryDim: Color
    val accent: Color
    val accentDim: Color
    val success: Color
    val error: Color
    val text: Color
    val textSecondary: Color
}

// Modern AI Agent Color Scheme (Dark)
object AIColors : AppColorScheme {
    override val bgDark = Color(0xFF0F1419)
    override val bgMedium = Color(0xFF1A202C)
    override val bgLight = Color(0xFF2D3748)
    override val primary = Color(0xFF00D9FF)  // Cyan
    override val primaryDim = Color(0xFF0099BB)
    override val accent = Color(0xFF7C3AED)   // Purple
    override val accentDim = Color(0xFF5B21B6)
    override val success = Color(0xFF10B981)
    override val error = Color(0xFFEF4444)
    override val text = Color(0xFFE5E7EB)
    override val textSecondary = Color(0xFF9CA3AF)
}

// Light Color Scheme
object AILightColors : AppColorScheme {
    override val bgDark = Color(0xFFF7FAFC)
    override val bgMedium = Color(0xFFE2E8F0)
    override val bgLight = Color(0xFFCBD5E1)
    override val primary = Color(0xFF0066CC)  // Blue
    override val primaryDim = Color(0xFF00509E)
    override val accent = Color(0xFF7C3AED)   // Purple (same as dark)
    override val accentDim = Color(0xFF5B21B6)
    override val success = Color(0xFF059669)
    override val error = Color(0xFFB91C1C)
    override val text = Color(0xFF1A202C)
    override val textSecondary = Color(0xFF64748B)
}
