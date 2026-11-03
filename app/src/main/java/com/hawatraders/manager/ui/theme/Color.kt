package com.hawatraders.manager.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.hawatraders.manager.data.db.Category

val LightColors = lightColorScheme(
    primary = Color(0xFF166534),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFEBD6),
    onPrimaryContainer = Color(0xFF0B3A1D),
    secondary = Color(0xFF4D6356),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E8D8),
    onSecondaryContainer = Color(0xFF0A1F13),
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDA8),
    onTertiaryContainer = Color(0xFF2B1800),
    background = Color(0xFFFAF8F2),
    onBackground = Color(0xFF1C1C17),
    surface = Color(0xFFFFFEFB),
    onSurface = Color(0xFF1C1C17),
    surfaceVariant = Color(0xFFEDE9DC),
    onSurfaceVariant = Color(0xFF4A4D45),
    outline = Color(0xFF7A7D72),
    outlineVariant = Color(0xFFD4D0C2),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F2E9),
    surfaceContainer = Color(0xFFF1EEE4),
    surfaceContainerHigh = Color(0xFFECE9DE),
    surfaceContainerHighest = Color(0xFFE6E3D8),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD9A8),
    onPrimary = Color(0xFF003919),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFBDF0CC),
    secondary = Color(0xFFB3CCBB),
    onSecondary = Color(0xFF1F352A),
    secondaryContainer = Color(0xFF354B3F),
    onSecondaryContainer = Color(0xFFCFE8D7),
    tertiary = Color(0xFFF2BE5C),
    onTertiary = Color(0xFF452B00),
    tertiaryContainer = Color(0xFF633F00),
    onTertiaryContainer = Color(0xFFFFDDA8),
    background = Color(0xFF0E1410),
    onBackground = Color(0xFFE2E3DC),
    surface = Color(0xFF131A15),
    onSurface = Color(0xFFE2E3DC),
    surfaceVariant = Color(0xFF3F4941),
    onSurfaceVariant = Color(0xFFBFC9C0),
    outline = Color(0xFF89938A),
    outlineVariant = Color(0xFF3F4941),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF0A0F0C),
    surfaceContainerLow = Color(0xFF171E19),
    surfaceContainer = Color(0xFF1B231D),
    surfaceContainerHigh = Color(0xFF252D27),
    surfaceContainerHighest = Color(0xFF303831),
)

/** Colors outside the Material scheme: the price accent and per-category badge colors. */
class ExtraColors(
    val price: Color,
    val priceContainer: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    private val badges: Map<Category, Pair<Color, Color>>,
) {
    /** (background, foreground) for a category badge. */
    fun badge(category: Category): Pair<Color, Color> = badges.getValue(category)
}

val LightExtraColors = ExtraColors(
    price = Color(0xFF0F3D22),
    priceContainer = Color(0xFFE3F0E6),
    heroStart = Color(0xFF14532D),
    heroEnd = Color(0xFF1F7A43),
    onHero = Color(0xFFFFFFFF),
    badges = mapOf(
        Category.PESTICIDE to (Color(0xFFFDE2D3) to Color(0xFF8A3B12)),
        Category.FUNGICIDE to (Color(0xFFE3DDF7) to Color(0xFF4A3A8C)),
        Category.HERBICIDE to (Color(0xFFFBE9A8) to Color(0xFF6B5300)),
        Category.FERTILIZER to (Color(0xFFD6E8F7) to Color(0xFF1D4E7A)),
        Category.SEED to (Color(0xFFE7F2C9) to Color(0xFF3F5A0E)),
        Category.OTHER to (Color(0xFFE5E2D9) to Color(0xFF4B4A42)),
    ),
)

val DarkExtraColors = ExtraColors(
    price = Color(0xFFF2BE5C),
    priceContainer = Color(0xFF2A2614),
    heroStart = Color(0xFF0F3D22),
    heroEnd = Color(0xFF1A5A33),
    onHero = Color(0xFFE8F5EC),
    badges = mapOf(
        Category.PESTICIDE to (Color(0xFF5A2A10) to Color(0xFFFFD2B8)),
        Category.FUNGICIDE to (Color(0xFF3F356B) to Color(0xFFDCD2FF)),
        Category.HERBICIDE to (Color(0xFF5A4800) to Color(0xFFFFE58A)),
        Category.FERTILIZER to (Color(0xFF1C3F5E) to Color(0xFFBFE0FA)),
        Category.SEED to (Color(0xFF36500F) to Color(0xFFD6EFA0)),
        Category.OTHER to (Color(0xFF3B3C36) to Color(0xFFD7D8CF)),
    ),
)
