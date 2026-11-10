package com.crylo.kidslearn.content

/**
 * Everything the app teaches, loaded from the tab-separated files in `assets/content/`.
 *
 * Content lives in data files rather than in UI code so that fixing a word or swapping a
 * picture is a one-line data change, and so the unit tests can check the exact files the
 * app ships. This class is plain Kotlin: the loader is handed a function that reads a
 * file by name, which is the asset manager on a device and the source tree in tests.
 */
class Content(
    val english: List<LetterCard>,
    val swar: List<LetterCard>,
    val vyanjan: List<LetterCard>,
    val numbers: List<NumberItem>,
    val colours: List<ColourItem>,
    val shapes: List<ShapeItem>,
) {
    /** Swar then vyanjan, the pool the quiz draws Hindi letters from. */
    val hindiLetters: List<LetterCard> = swar + vyanjan

    companion object {
        const val ENGLISH_FILE = "english_alphabet.tsv"
        const val SWAR_FILE = "hindi_swar.tsv"
        const val VYANJAN_FILE = "hindi_vyanjan.tsv"
        const val HINDI_NUMBERS_FILE = "hindi_numbers.tsv"
        const val COLOURS_FILE = "colours.tsv"
        const val SHAPES_FILE = "shapes.tsv"

        fun load(read: (fileName: String) -> String): Content {
            val hindiNumbers = parseHindiNumbers(read(HINDI_NUMBERS_FILE))
            return Content(
                english = parseLetters(read(ENGLISH_FILE)),
                swar = parseLetters(read(SWAR_FILE)),
                vyanjan = parseLetters(read(VYANJAN_FILE)),
                numbers = (1..100).map { NumberItem(it, EnglishNumbers.words(it), hindiNumbers.getValue(it)) },
                colours = parseColours(read(COLOURS_FILE)),
                shapes = parseShapes(read(SHAPES_FILE)),
            )
        }

        /** Non-blank, non-comment lines split on tabs. */
        internal fun rows(text: String): List<List<String>> = text.lines()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line -> line.split('\t').map(String::trim) }

        internal fun parseLetters(text: String): List<LetterCard> = rows(text).map { cols ->
            require(cols.size in 3..4) { "Letter row needs 3 or 4 columns: $cols" }
            val like = cols.getOrNull(3)
            require(like == null || like == "like") { "Unknown flag '$like' in $cols" }
            LetterCard(
                letter = cols[0],
                word = cols[1],
                picture = Picture.parse(cols[2]),
                startsWithLetter = like == null,
            )
        }

        internal fun parseHindiNumbers(text: String): Map<Int, String> {
            val map = rows(text).associate { cols ->
                require(cols.size == 2) { "Number row needs 2 columns: $cols" }
                cols[0].toInt() to cols[1]
            }
            require(map.keys == (1..100).toSet()) { "Hindi numbers must cover exactly 1 to 100" }
            return map
        }

        internal fun parseColours(text: String): List<ColourItem> = rows(text).map { cols ->
            require(cols.size == 3) { "Colour row needs 3 columns: $cols" }
            val hex = cols[2]
            require(hex.length == 7 && hex[0] == '#') { "Colour must be #RRGGBB: $hex" }
            ColourItem(cols[0], cols[1], 0xFF000000 or hex.substring(1).toLong(16))
        }

        internal fun parseShapes(text: String): List<ShapeItem> = rows(text).map { cols ->
            require(cols.size == 3) { "Shape row needs 3 columns: $cols" }
            ShapeItem(ShapeKind.valueOf(cols[0]), cols[1], cols[2])
        }
    }
}

/** What a card shows as its picture: an emoji from the system font, or a drawn shape. */
sealed interface Picture {
    data class Emoji(val text: String) : Picture
    data class Drawn(val shape: ShapeKind) : Picture

    companion object {
        private const val SHAPE_PREFIX = "shape:"

        fun parse(raw: String): Picture {
            require(raw.isNotBlank()) { "Picture is empty" }
            return if (raw.startsWith(SHAPE_PREFIX)) {
                Drawn(ShapeKind.valueOf(raw.removePrefix(SHAPE_PREFIX)))
            } else {
                Emoji(raw)
            }
        }
    }
}

/**
 * One alphabet card. [startsWithLetter] is false for letters that never begin a word
 * (ङ, ञ, ण, अः, क्ष); the card then says "like" instead of "for"/"से".
 */
data class LetterCard(
    val letter: String,
    val word: String,
    val picture: Picture,
    val startsWithLetter: Boolean,
)

data class NumberItem(val value: Int, val english: String, val hindi: String) {
    val devanagari: String get() = toDevanagariDigits(value)
}

/** [argb] is a full 0xAARRGGBB value, ready for `Color(argb)`. */
data class ColourItem(val english: String, val hindi: String, val argb: Long)

data class ShapeItem(val kind: ShapeKind, val english: String, val hindi: String)

/** Every shape the app can draw. The UI's renderer is an exhaustive `when` over this. */
enum class ShapeKind {
    CIRCLE,
    SQUARE,
    TRIANGLE,
    RECTANGLE,
    OVAL,
    STAR,
    HEART,
    DIAMOND,
    PENTAGON,
    HEXAGON,
    CRESCENT,
}

/** 42 -> "४२". Devanagari digits are U+0966..U+096F, in order. */
fun toDevanagariDigits(value: Int): String =
    value.toString().map { if (it.isDigit()) '०' + (it - '0') else it }.joinToString("")
