package com.crylo.kidslearn.content

import kotlin.random.Random

enum class QuizKind { ENGLISH_LETTER, HINDI_LETTER, NUMBER, COLOUR, SHAPE }

/**
 * One question. [options] are indices into the pool for [kind] (see [QuizGenerator.pool]);
 * [answer] is the position within [options] of the right one.
 */
data class Question(val kind: QuizKind, val options: List<Int>, val answer: Int) {
    val answerItem: Int get() = options[answer]
}

/**
 * Builds multiple-choice questions: one right answer plus distinct distractors from the
 * same pool, shuffled. Plain Kotlin with an injectable [random] so the tests can run
 * thousands of seeded questions.
 */
class QuizGenerator(
    private val content: Content,
    private val random: Random = Random.Default,
) {
    private var last: Pair<QuizKind, Int>? = null

    /** Numbers in the quiz stop at 20: beyond that it is a reading test, not counting. */
    fun poolSize(kind: QuizKind): Int = when (kind) {
        QuizKind.ENGLISH_LETTER -> content.english.size
        QuizKind.HINDI_LETTER -> content.hindiLetters.size
        QuizKind.NUMBER -> QUIZ_NUMBER_MAX
        QuizKind.COLOUR -> content.colours.size
        QuizKind.SHAPE -> content.shapes.size
    }

    /** What a pool item looks like, used to prove no two options look the same. */
    fun label(kind: QuizKind, item: Int): String = when (kind) {
        QuizKind.ENGLISH_LETTER -> content.english[item].letter
        QuizKind.HINDI_LETTER -> content.hindiLetters[item].letter
        QuizKind.NUMBER -> content.numbers[item].value.toString()
        QuizKind.COLOUR -> content.colours[item].english
        QuizKind.SHAPE -> content.shapes[item].english
    }

    /** A new question of one of [kinds], never the same right answer twice in a row. */
    fun next(kinds: Collection<QuizKind> = QuizKind.entries): Question {
        require(kinds.isNotEmpty()) { "Need at least one kind of question" }
        val kindList = kinds.toList()
        var kind: QuizKind
        var answerItem: Int
        do {
            kind = kindList[random.nextInt(kindList.size)]
            answerItem = random.nextInt(poolSize(kind))
        } while (kind to answerItem == last)
        last = kind to answerItem
        return build(kind, answerItem)
    }

    fun build(kind: QuizKind, answerItem: Int): Question {
        val size = poolSize(kind)
        require(answerItem in 0 until size) { "Answer $answerItem outside pool of $size" }
        val count = minOf(OPTIONS, size)
        val distractors = (0 until size).filter { it != answerItem }.shuffled(random).take(count - 1)
        val options = (distractors + answerItem).shuffled(random)
        return Question(kind, options, options.indexOf(answerItem))
    }

    companion object {
        const val OPTIONS = 4
        const val QUIZ_NUMBER_MAX = 20
    }
}
