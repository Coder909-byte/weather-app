package com.aeris.weather.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aeris.weather.R
import com.aeris.weather.domain.model.Appearance

val Manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Light), Font(R.font.manrope, FontWeight.Normal),
    Font(R.font.manrope, FontWeight.Medium), Font(R.font.manrope, FontWeight.SemiBold),
    Font(R.font.manrope, FontWeight.Bold),
)
val Sunlight = Color(0xFFE4CC9B)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8C9D8), onPrimary = Color(0xFF101820),
    background = Color(0xFF101820), onBackground = Color(0xFFF5F7F8),
    surface = Color(0xFF19252F), onSurface = Color(0xFFF5F7F8),
    surfaceVariant = Color(0xFF23313B), onSurfaceVariant = Color(0xFFA9B6BF),
    outline = Color(0xFF34434D), outlineVariant = Color(0xFF2A3943),
    error = Color(0xFFFFB4AB),
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF577F96), onPrimary = Color.White,
    background = Color(0xFFF5F7F8), onBackground = Color(0xFF17232B),
    surface = Color.White, onSurface = Color(0xFF17232B),
    surfaceVariant = Color(0xFFE9EFF2), onSurfaceVariant = Color(0xFF566772),
    outline = Color(0xFFADBCC5), outlineVariant = Color(0xFFDDE5E9),
    error = Color(0xFFA83232),
)
@Composable
fun useDarkTheme(appearance: Appearance) = when (appearance) {
    Appearance.SYSTEM -> isSystemInDarkTheme()
    Appearance.DARK -> true
    Appearance.LIGHT -> false
}

@Composable
fun AerisTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val typography = Typography(
        displayLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Light, fontSize = 100.sp, letterSpacing = (-5).sp),
        headlineLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 28.sp),
        headlineMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 26.sp),
        titleLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
        bodyLarge = TextStyle(fontFamily = Manrope, fontSize = 16.sp, lineHeight = 25.sp),
        bodyMedium = TextStyle(fontFamily = Manrope, fontSize = 14.sp, lineHeight = 22.sp),
        bodySmall = TextStyle(fontFamily = Manrope, fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        labelMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.sp),
        labelSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 1.sp),
    )
    MaterialTheme(colorScheme = if (useDarkTheme(appearance)) DarkColors else LightColors, typography = typography, content = content)
}
