package com.crylo.kidslearn.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Loads the exact files the app ships, from the source tree. */
fun shippedContent(): Content = Content.load { File("src/main/assets/content/$it").readText() }

class ContentTest {
    private val content = shippedContent()

    private fun isDevanagari(c: Char) = c in 'ऀ'..'ॿ'

    @Test
    fun englishAlphabetIsAToZInOrder() {
        assertEquals(('A'..'Z').map { it.toString() }, content.english.map { it.letter })
    }

    @Test
    fun everyEnglishLetterHasAWordStartingWithItAndAPicture() {
        for (card in content.english) {
            assertTrue("${card.letter} must start its word", card.startsWithLetter)
            assertTrue("${card.word} must start with ${card.letter}", card.word.startsWith(card.letter, ignoreCase = true))
            assertPicture(card)
        }
    }

    @Test
    fun hindiSwarAndVyanjanAreComplete() {
        assertEquals(
            listOf("अ", "आ", "इ", "ई", "उ", "ऊ", "ऋ", "ए", "ऐ", "ओ", "औ", "अं", "अः"),
            content.swar.map { it.letter },
        )
        val vyanjan = "क ख ग घ ङ च छ ज झ ञ ट ठ ड ढ ण त थ द ध न प फ ब भ म य र ल व श ष स ह क्ष त्र ज्ञ"
        assertEquals(vyanjan.split(' '), content.vyanjan.map { it.letter })
    }

    @Test
    fun everyHindiLetterHasADevanagariWordAndAPicture() {
        for (card in content.hindiLetters) {
            assertTrue("${card.letter}: word must be Devanagari", card.word.all { isDevanagari(it) })
            if (card.startsWithLetter) {
                assertTrue("${card.word} must start with ${card.letter}", card.word.startsWith(card.letter))
            } else {
                // A "like" word must at least contain the sound it illustrates.
                assertTrue("${card.word} must contain ${card.letter}", card.word.contains(card.letter.last()))
            }
            assertPicture(card)
        }
    }

    @Test
    fun lettersAreUnique() {
        assertEquals(content.english.size, content.english.map { it.letter }.toSet().size)
        assertEquals(content.hindiLetters.size, content.hindiLetters.map { it.letter }.toSet().size)
    }

    @Test
    fun drawnPicturesUseKnownShapes() {
        val drawn = content.hindiLetters.map { it.picture }.filterIsInstance<Picture.Drawn>()
        assertTrue(drawn.isNotEmpty())
        drawn.forEach { assertTrue(it.shape in ShapeKind.entries) }
    }

    @Test
    fun coloursHaveNamesInBothLanguagesAndDistinctValues() {
        assertTrue(content.colours.size >= 8)
        content.colours.forEach {
            assertTrue(it.english.isNotBlank())
            assertTrue(it.hindi.all { c -> isDevanagari(c) })
            assertEquals(0xFF000000, it.argb and 0xFF000000)
        }
        assertEquals(content.colours.size, content.colours.map { it.english }.toSet().size)
        assertEquals(content.colours.size, content.colours.map { it.argb }.toSet().size)
    }

    @Test
    fun everyShapeKindIsTaughtExactlyOnce() {
        assertEquals(ShapeKind.entries.toList(), content.shapes.map { it.kind })
        content.shapes.forEach {
            assertTrue(it.english.isNotBlank())
            assertTrue(it.hindi.all { c -> isDevanagari(c) })
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownShapeIsRejected() {
        Picture.parse("shape:BLOB")
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingPictureIsRejected() {
        Content.parseLetters("A\tApple\t ")
    }

    private fun assertPicture(card: LetterCard) {
        when (val p = card.picture) {
            is Picture.Emoji -> {
                assertTrue("${card.letter} has an empty picture", p.text.isNotBlank())
                // An emoji, not a stray word typed into the picture column.
                assertTrue("${card.letter} picture '${p.text}' is not an emoji", p.text.none { it.isLetter() })
            }
            is Picture.Drawn -> Unit
        }
    }
}
