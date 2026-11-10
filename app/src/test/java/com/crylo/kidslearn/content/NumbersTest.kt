package com.crylo.kidslearn.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NumbersTest {
    private val content = shippedContent()

    @Test
    fun englishWords() {
        val expected = mapOf(
            1 to "one", 7 to "seven", 10 to "ten", 11 to "eleven", 13 to "thirteen",
            15 to "fifteen", 19 to "nineteen", 20 to "twenty", 21 to "twenty-one",
            40 to "forty", 45 to "forty-five", 58 to "fifty-eight", 80 to "eighty",
            99 to "ninety-nine", 100 to "one hundred",
        )
        expected.forEach { (n, word) -> assertEquals(word, EnglishNumbers.words(n)) }
    }

    @Test
    fun englishWordsForOneToHundredAreDistinct() {
        val words = (1..100).map(EnglishNumbers::words)
        assertEquals(100, words.toSet().size)
        words.forEach { assertTrue(it, it.matches(Regex("[a-z]+(-[a-z]+)?( hundred)?"))) }
    }

    @Test
    fun hindiWords() {
        val expected = mapOf(
            1 to "एक", 5 to "पाँच", 10 to "दस", 19 to "उन्नीस", 29 to "उनतीस", 35 to "पैंतीस",
            49 to "उनचास", 50 to "पचास", 67 to "सड़सठ", 79 to "उनासी", 89 to "नवासी",
            99 to "निन्यानवे", 100 to "सौ",
        )
        expected.forEach { (n, word) -> assertEquals(word, content.numbers[n - 1].hindi) }
    }

    @Test
    fun numbersCoverOneToHundredWithDistinctWords() {
        assertEquals((1..100).toList(), content.numbers.map { it.value })
        assertEquals(100, content.numbers.map { it.hindi }.toSet().size)
        assertEquals(100, content.numbers.map { it.english }.toSet().size)
        content.numbers.forEach { n -> assertTrue(n.hindi, n.hindi.all { it in 'ऀ'..'ॿ' }) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun hindiFileWithAGapIsRejected() {
        Content.parseHindiNumbers((1..99).joinToString("\n") { "$it\tx" })
    }

    @Test
    fun devanagariDigits() {
        assertEquals("१", toDevanagariDigits(1))
        assertEquals("४२", toDevanagariDigits(42))
        assertEquals("१००", toDevanagariDigits(100))
    }

    @Test
    fun numberPictureShowsPlaceValue() {
        assertEquals(listOf("⭐⭐⭐"), NumberPicture.rows(3))
        assertEquals(listOf("⭐⭐⭐⭐⭐", "⭐⭐"), NumberPicture.rows(7))
        assertEquals(listOf("🔟🔟", "⭐⭐⭐"), NumberPicture.rows(23))
        assertEquals(listOf("🔟🔟🔟🔟🔟", "🔟🔟🔟🔟🔟"), NumberPicture.rows(100))
        for (n in 1..100) {
            val all = NumberPicture.rows(n).joinToString("")
            val tens = all.windowed(2, 2).count { it == NumberPicture.TEN }
            val ones = all.count { it == '⭐' }
            assertEquals(n, tens * 10 + ones)
        }
    }

    @Test
    fun tablesOneToTwenty() {
        for (t in Tables.range) {
            val rows = Tables.of(t)
            assertEquals((1..10).toList(), rows.map { it.times })
            rows.forEach { assertEquals(t * it.times, it.product) }
        }
        assertEquals(133, Tables.of(19)[6].product)
    }

    @Test(expected = IllegalArgumentException::class)
    fun tableOutsideRangeIsRejected() {
        Tables.of(21)
    }
}
