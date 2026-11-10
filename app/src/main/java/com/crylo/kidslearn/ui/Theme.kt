package com.crylo.kidslearn.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.crylo.kidslearn.R

/**
 * Material You for the chrome (bars, dialogs, settings) so the app fits the phone; fixed
 * bright colours per section for the parts a child looks at, because a child finds
 * "the red one" faster than a label they cannot read yet.
 */
@Composable
fun KidsLearnTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Every top-level section, with its home-tile glyph and colour. Deep enough for white text. */
enum class Section(val label: Int, val glyph: Int, val accent: Color) {
    ENGLISH(R.string.section_english, R.string.tile_english, Color(0xFFE53935)),
    SWAR(R.string.section_swar, R.string.tile_swar, Color(0xFFEF6C00)),
    VYANJAN(R.string.section_vyanjan, R.string.tile_vyanjan, Color(0xFF8E24AA)),
    NUMBERS(R.string.section_numbers, R.string.tile_numbers, Color(0xFF1E88E5)),
    TABLES(R.string.section_tables, R.string.tile_tables, Color(0xFF00897B)),
    COLOURS(R.string.section_colours, R.string.tile_colours, Color(0xFFD81B60)),
    SHAPES(R.string.section_shapes, R.string.tile_shapes, Color(0xFF43A047)),
    QUIZ(R.string.section_quiz, R.string.tile_quiz, Color(0xFF3949AB)),
}

/** Colours shapes are painted in, cycled so neighbouring cards differ. */
val ShapePalette = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFFFDD835), Color(0xFF43A047),
    Color(0xFF8E24AA), Color(0xFFFB8C00), Color(0xFFEC407A), Color(0xFF00ACC1),
)
