package com.metrolist.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Cocoa950 = Color(0xFF140E0D)
val Cocoa900 = Color(0xFF1B1311)
val Cocoa850 = Color(0xFF241916)
val Cocoa800 = Color(0xFF2D211D)
val Cocoa700 = Color(0xFF4B3831)
val Ivory = Color(0xFFF7EFE3)
val WarmWhite = Color(0xFFFFFAF2)
val Sand = Color(0xFFCDBEAA)
val Burgundy = Color(0xFF9D3F4F)
val BurgundyLight = Color(0xFFE6A3AC)
val Olive = Color(0xFF87905F)

private val HikalistDarkColors = darkColorScheme(
    primary = BurgundyLight,
    onPrimary = Cocoa950,
    primaryContainer = Color(0xFF5E2630),
    onPrimaryContainer = WarmWhite,
    secondary = Olive,
    onSecondary = Cocoa950,
    secondaryContainer = Color(0xFF353A27),
    onSecondaryContainer = Ivory,
    tertiary = Color(0xFFD6AE79),
    background = Cocoa950,
    onBackground = Ivory,
    surface = Cocoa900,
    onSurface = Ivory,
    surfaceVariant = Cocoa800,
    onSurfaceVariant = Sand,
    outline = Color(0xFF6A554A),
    outlineVariant = Color(0xFF3A2B26),
    error = Color(0xFFFFB4AB),
)

private val HikalistLightColors = lightColorScheme(
    primary = Burgundy,
    onPrimary = WarmWhite,
    primaryContainer = Color(0xFFF6D8DC),
    onPrimaryContainer = Color(0xFF3D1119),
    secondary = Color(0xFF626B3F),
    onSecondary = WarmWhite,
    secondaryContainer = Color(0xFFE4E9C4),
    onSecondaryContainer = Color(0xFF20270D),
    tertiary = Color(0xFF7D5C2D),
    background = Color(0xFFFFF8F2),
    onBackground = Color(0xFF251916),
    surface = Color(0xFFFFFBF7),
    onSurface = Color(0xFF251916),
    surfaceVariant = Color(0xFFF1E4DB),
    onSurfaceVariant = Color(0xFF66534C),
    outline = Color(0xFF88736A),
    outlineVariant = Color(0xFFD9C4BA),
    error = Color(0xFFBA1A1A),
)

private val HikalistTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 54.sp, lineHeight = 58.sp, letterSpacing = (-1.5).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.7.sp),
)

@Composable
fun MetrolistDesktopTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) HikalistDarkColors else HikalistLightColors,
        typography = HikalistTypography,
        content = content,
    )
}
