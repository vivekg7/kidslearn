package com.crylo.kidslearn.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuizTest {
    private val content = shippedContent()

    @Test
    fun answerNeverAppearsAsADistractor() {
        val quiz = QuizGenerator(content, Random(42))
        repeat(20_000) {
            val q = quiz.next()
            assertEquals(QuizGenerator.OPTIONS, q.options.size)
            assertTrue(q.answer in q.options.indices)
            assertEquals("answer must appear exactly once", 1, q.options.count { it == q.answerItem })
            assertEquals("options must be distinct", q.options.size, q.options.toSet().size)
            val labels = q.options.map { quiz.label(q.kind, it) }
            assertEquals("options must look different: $labels", labels.size, labels.toSet().size)
            q.options.forEach { assertTrue(it in 0 until quiz.poolSize(q.kind)) }
        }
    }

    @Test
    fun everyItemOfEveryKindCanBeAskedWithDistinctOptions() {
        val quiz = QuizGenerator(content, Random(7))
        for (kind in QuizKind.entries) {
            for (item in 0 until quiz.poolSize(kind)) {
                val q = quiz.build(kind, item)
                assertEquals(item, q.answerItem)
                assertEquals(q.options.size, q.options.toSet().size)
            }
        }
    }

    @Test
    fun sameAnswerIsNeverAskedTwiceInARow() {
        val quiz = QuizGenerator(content, Random(1))
        var previous = quiz.next(listOf(QuizKind.COLOUR))
        repeat(2_000) {
            val q = quiz.next(listOf(QuizKind.COLOUR))
            assertNotEquals(previous.answerItem, q.answerItem)
            previous = q
        }
    }

    @Test
    fun answerPositionIsSpreadAcrossOptions() {
        val quiz = QuizGenerator(content, Random(3))
        val counts = IntArray(QuizGenerator.OPTIONS)
        repeat(4_000) { counts[quiz.next().answer]++ }
        // Not always the first card, or a child learns the position, not the letter.
        counts.forEach { assertTrue(counts.toList().toString(), it in 800..1200) }
    }

    @Test
    fun onlyRequestedKindsAreAsked() {
        val quiz = QuizGenerator(content, Random(9))
        val allowed = setOf(QuizKind.NUMBER, QuizKind.SHAPE)
        repeat(1_000) { assertTrue(quiz.next(allowed).kind in allowed) }
    }

    @Test
    fun numberQuizStaysWithinTwenty() {
        val quiz = QuizGenerator(content, Random(5))
        repeat(1_000) {
            val q = quiz.next(listOf(QuizKind.NUMBER))
            q.options.forEach { assertTrue(content.numbers[it].value <= 20) }
        }
    }
}
