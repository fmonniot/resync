package eu.monniot.resync.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colors
//
// Transcribed from the "Calm Reader v2" design (Claude Design project
// 274c396b-cc57-4eb1-8e13-4bea2287765d), which specifies these as OKLCH; the sRGB hex below is
// the pre-computed conversion. Source (role -> OKLCH -> sRGB hex used below):
//
// Light: primary oklch(47% 0.18 276) #4547BD, primaryContainer oklch(91% 0.045 276) #D9DFFF,
// onPrimaryContainer oklch(24% 0.09 276) #161749, secondaryContainer oklch(89% 0.05 276) #D1D9FD,
// onSecondaryContainer oklch(24% 0.07 276) #171A40, surface oklch(98.5% 0.004 276) #F9FAFD,
// onSurface oklch(19% 0.01 276) #121318, surfaceContainerLow oklch(96.5% 0.006 276) #F2F3F8,
// surfaceContainer oklch(95% 0.008 276) #EDEEF4, surfaceContainerHigh oklch(92% 0.012 276) #E2E4ED,
// surfaceContainerHighest oklch(90% 0.014 276) #DBDDE8, onSurfaceVariant oklch(42% 0.02 276)
// #4A4C58, outline oklch(70% 0.015 276) #9C9EA8, outlineVariant oklch(84% 0.012 276) #C8CAD3,
// error oklch(48% 0.19 25) #B00A1D, errorContainer oklch(93% 0.04 25) #FFDEDB, onErrorContainer
// oklch(28% 0.08 25) #491513.
//
// Dark: primary oklch(82% 0.09 276) #B4C0FF, onPrimary oklch(28% 0.10 276) #1E2059,
// primaryContainer oklch(34% 0.10 276) #2B306A, onPrimaryContainer oklch(90% 0.05 276) #D4DCFF,
// secondaryContainer oklch(35% 0.06 276) #32375A, onSecondaryContainer oklch(90% 0.04 276)
// #D6DCF9, surface oklch(16% 0.006 276) #0C0D10, onSurface oklch(93% 0.004 276) #E7E8EA,
// surfaceContainerLow oklch(19% 0.008 276) #131417, surfaceContainer oklch(21% 0.010 276) #17181D,
// surfaceContainerHigh oklch(26% 0.014 276) #22242B, surfaceContainerHighest oklch(30% 0.016 276)
// #2B2D36, onSurfaceVariant oklch(74% 0.014 276) #A8AAB4, outline oklch(46% 0.016 276) #555761,
// outlineVariant oklch(33% 0.014 276) #33353D, error oklch(78% 0.13 25) #FF958D, errorContainer
// oklch(34% 0.09 25) #5E211F, onErrorContainer oklch(90% 0.045 25) #FBD3CF.
//
// Two of these sit marginally outside the sRGB gamut and were clipped on one channel during
// conversion (light errorContainer, blue channel 1.026; dark error, red channel 1.007) - the hue
// shift is well under one perceptual step, so use the hex values above rather than re-deriving.
//
// Roles the design doesn't style directly (secondary, tertiary, inverse*, background, scrim, ...)
// are filled in on the same ~276deg hue as primary, so M3 components the design never touches
// (Switch, Snackbar, AlertDialog, ...) still render coherently. `surfaceTint` is set to `primary`.

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4547BD),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9DFFF),
    onPrimaryContainer = Color(0xFF161749),
    inversePrimary = Color(0xFFB4C0FF),
    secondary = Color(0xFF51587D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1D9FD),
    onSecondaryContainer = Color(0xFF171A40),
    tertiary = Color(0xFF4A529D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3DBFF),
    onTertiaryContainer = Color(0xFF161944),
    background = Color(0xFFF9FAFD),
    onBackground = Color(0xFF121318),
    surface = Color(0xFFF9FAFD),
    onSurface = Color(0xFF121318),
    surfaceVariant = Color(0xFFDBDDE8),
    onSurfaceVariant = Color(0xFF4A4C58),
    surfaceTint = Color(0xFF4547BD),
    inverseSurface = Color(0xFF17181D),
    inverseOnSurface = Color(0xFFE7E8EA),
    surfaceContainerLow = Color(0xFFF2F3F8),
    surfaceContainer = Color(0xFFEDEEF4),
    surfaceContainerHigh = Color(0xFFE2E4ED),
    surfaceContainerHighest = Color(0xFFDBDDE8),
    outline = Color(0xFF9C9EA8),
    outlineVariant = Color(0xFFC8CAD3),
    error = Color(0xFFB00A1D),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDEDB),
    onErrorContainer = Color(0xFF491513),
    scrim = Color(0xFF000000),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB4C0FF),
    onPrimary = Color(0xFF1E2059),
    primaryContainer = Color(0xFF2B306A),
    onPrimaryContainer = Color(0xFFD4DCFF),
    inversePrimary = Color(0xFF4547BD),
    secondary = Color(0xFFBEC3D8),
    onSecondary = Color(0xFF242839),
    secondaryContainer = Color(0xFF32375A),
    onSecondaryContainer = Color(0xFFD6DCF9),
    tertiary = Color(0xFFB9C2EC),
    onTertiary = Color(0xFF202549),
    tertiaryContainer = Color(0xFF2E335E),
    onTertiaryContainer = Color(0xFFD3DCFF),
    background = Color(0xFF0C0D10),
    onBackground = Color(0xFFE7E8EA),
    surface = Color(0xFF0C0D10),
    onSurface = Color(0xFFE7E8EA),
    surfaceVariant = Color(0xFF2B2D36),
    onSurfaceVariant = Color(0xFFA8AAB4),
    surfaceTint = Color(0xFFB4C0FF),
    inverseSurface = Color(0xFFDBDDE8),
    inverseOnSurface = Color(0xFF121318),
    surfaceContainerLow = Color(0xFF131417),
    surfaceContainer = Color(0xFF17181D),
    surfaceContainerHigh = Color(0xFF22242B),
    surfaceContainerHighest = Color(0xFF2B2D36),
    outline = Color(0xFF555761),
    outlineVariant = Color(0xFF33353D),
    error = Color(0xFFFF958D),
    onError = Color(0xFF4F0A0D),
    errorContainer = Color(0xFF5E211F),
    onErrorContainer = Color(0xFFFBD3CF),
    scrim = Color(0xFF000000),
)

// Theme

@Composable
fun ReSyncTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}
