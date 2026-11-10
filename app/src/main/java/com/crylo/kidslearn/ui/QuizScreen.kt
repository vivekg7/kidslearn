package com.crylo.kidslearn.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crylo.kidslearn.R
import com.crylo.kidslearn.content.Content
import com.crylo.kidslearn.content.Question
import com.crylo.kidslearn.content.QuizGenerator
import com.crylo.kidslearn.content.QuizKind
import com.crylo.kidslearn.speech.Language
import com.crylo.kidslearn.speech.Speaker
import kotlinx.coroutines.delay

private val Correct = Color(0xFF2E7D32)

/**
 * Endless, scoreless-in-the-bad-way quiz: a right answer earns a star and a cheer, a wrong
 * one just fades that card and says "try again". Nothing is ever taken away, because the
 * point at this age is to keep a child wanting to play.
 */
@Composable
fun QuizScreen(content: Content, speaker: Speaker, onBack: () -> Unit) {
    val generator = remember { QuizGenerator(content) }
    // A Hindi prompt nobody can hear is a reading test for a child who cannot read yet.
    val kinds = if (speaker.hindiReady) QuizKind.entries else QuizKind.entries - QuizKind.HINDI_LETTER
    var question by remember { mutableStateOf(generator.next(kinds)) }
    var wrong by remember(question) { mutableStateOf(emptySet<Int>()) }
    var solved by remember(question) { mutableStateOf(false) }
    var feedback by remember(question) { mutableStateOf<String?>(null) }
    var stars by rememberSaveable { mutableIntStateOf(0) }
    val cheers = stringArrayResource(R.array.cheers)
    val tryAgain = stringArrayResource(R.array.try_again)

    val prompt = prompt(question, content)
    val promptLanguage = if (question.kind == QuizKind.HINDI_LETTER) Language.HINDI else Language.ENGLISH

    LaunchedEffect(question) { speaker.speak(prompt, promptLanguage) }
    LaunchedEffect(question, solved) {
        if (solved) {
            delay(NEXT_QUESTION_MS)
            question = generator.next(kinds)
        }
    }
    DisposableEffect(Unit) { onDispose { speaker.stop() } }

    SectionScaffold(Section.QUIZ, onBack, trailing = stringResource(R.string.quiz_stars, stars)) {
        VoiceNote(speaker, hindi = false, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        Row(
            Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FitText(
                feedback ?: prompt,
                Modifier.weight(1f).fillMaxHeight().padding(vertical = 8.dp),
                color = if (solved) Correct else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                maxSize = 40,
            )
            FilledIconButton(
                onClick = { speaker.speak(prompt, promptLanguage) },
                modifier = Modifier.padding(start = 12.dp).size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Section.QUIZ.accent, contentColor = Color.White),
            ) { Icon(painterResource(R.drawable.ic_volume_up), stringResource(R.string.say_again), Modifier.size(40.dp)) }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
            val wide = maxWidth > maxHeight * 1.6f
            val rows = if (wide) listOf(question.options.indices.toList()) else question.options.indices.chunked(2)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                rows.forEach { row ->
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { i ->
                            val isAnswer = i == question.answer
                            KidCard(
                                accent = if (solved && isAnswer) Correct else Section.QUIZ.accent,
                                modifier = Modifier.weight(1f).fillMaxHeight().alpha(if (i in wrong) 0.3f else 1f),
                                onClick = {
                                    if (!solved && i !in wrong) {
                                        if (isAnswer) {
                                            solved = true
                                            stars++
                                            val cheer = cheers.random()
                                            feedback = "🎉 $cheer"
                                            speaker.speak(cheer, Language.ENGLISH)
                                        } else {
                                            wrong = wrong + i
                                            val again = tryAgain.random()
                                            feedback = again
                                            speaker.speak(again, Language.ENGLISH)
                                        }
                                    }
                                },
                            ) { Option(question.kind, question.options[i], content) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun prompt(q: Question, content: Content): String {
    val item = q.answerItem
    return when (q.kind) {
        QuizKind.ENGLISH_LETTER -> stringResource(R.string.quiz_english_letter, content.english[item].letter)
        QuizKind.HINDI_LETTER -> stringResource(R.string.quiz_hindi_letter, content.hindiLetters[item].letter)
        QuizKind.NUMBER -> stringResource(R.string.quiz_number, content.numbers[item].value)
        QuizKind.COLOUR -> stringResource(R.string.quiz_colour, content.colours[item].english.lowercase())
        QuizKind.SHAPE -> stringResource(R.string.quiz_shape, content.shapes[item].english.lowercase())
    }
}

@Composable
private fun Option(kind: QuizKind, item: Int, content: Content) {
    val accent = Section.QUIZ.accent
    when (kind) {
        QuizKind.ENGLISH_LETTER -> FitText(content.english[item].letter, color = accent, weight = FontWeight.Black)
        QuizKind.HINDI_LETTER -> FitText(content.hindiLetters[item].letter, color = accent, weight = FontWeight.Black)
        QuizKind.NUMBER -> FitText(content.numbers[item].value.toString(), color = accent, weight = FontWeight.Black)
        QuizKind.COLOUR -> ColourSwatch(Color(content.colours[item].argb), Modifier.fillMaxHeight(0.85f))
        // Every shape gets the same colour, so colour is never the clue.
        QuizKind.SHAPE -> ShapeView(content.shapes[item].kind, ShapePalette[1], Modifier.fillMaxHeight(0.85f))
    }
}

private const val NEXT_QUESTION_MS = 1800L
