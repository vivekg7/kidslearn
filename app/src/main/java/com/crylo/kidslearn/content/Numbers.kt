package com.crylo.kidslearn.content

/** English number words. Regular enough below 1000 to generate rather than list. */
object EnglishNumbers {
    private val units = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
        "seventeen", "eighteen", "nineteen",
    )
    private val tens = listOf(
        "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety",
    )

    fun words(n: Int): String {
        require(n in 0..100) { "Only 0 to 100 are supported: $n" }
        return when {
            n < 20 -> units[n]
            n == 100 -> "one hundred"
            n % 10 == 0 -> tens[n / 10]
            else -> "${tens[n / 10]}-${units[n % 10]}"
        }
    }
}

/**
 * Rows of emoji that show a number as place value: one 🔟 per ten, one ⭐ per unit, five
 * to a row so a child can count them. 23 -> ["🔟🔟", "⭐⭐⭐"].
 */
object NumberPicture {
    const val TEN = "🔟"
    const val ONE = "⭐"
    private const val PER_ROW = 5

    fun rows(n: Int): List<String> {
        require(n in 1..100) { "Only 1 to 100 are supported: $n" }
        val tenRows = List(n / 10) { TEN }.chunked(PER_ROW).map { it.joinToString("") }
        val oneRows = List(n % 10) { ONE }.chunked(PER_ROW).map { it.joinToString("") }
        return tenRows + oneRows
    }
}

/** Multiplication tables, 1 to 20, each up to ×10 as taught in Indian schools. */
object Tables {
    val range = 1..20
    const val ROWS = 10

    data class Row(val table: Int, val times: Int) {
        val product: Int get() = table * times
    }

    fun of(table: Int): List<Row> {
        require(table in range) { "Tables run from 1 to 20: $table" }
        return (1..ROWS).map { Row(table, it) }
    }
}
