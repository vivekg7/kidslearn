package com.crylo.kidslearn.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crylo.kidslearn.Prefs
import com.crylo.kidslearn.R
import com.crylo.kidslearn.content.Content
import com.crylo.kidslearn.content.LetterCard
import com.crylo.kidslearn.content.NumberPicture
import com.crylo.kidslearn.content.Tables
import com.crylo.kidslearn.speech.Language
import com.crylo.kidslearn.speech.Speaker
import kotlinx.coroutines.launch

@Composable
fun SectionScreen(section: Section, content: Content, prefs: Prefs, speaker: Speaker, onBack: () -> Unit) {
    val res = LocalResources.current
    fun englishPhrase(c: LetterCard) = res.getString(R.string.english_phrase, c.letter, c.word)
    fun hindiPhrase(c: LetterCard) =
        res.getString(if (c.startsWithLetter) R.string.hindi_phrase else R.string.hindi_phrase_like, c.letter, c.word)

    when (section) {
        Section.ENGLISH -> CardPager(
            section, content.english, prefs, speaker, onBack,
            say = { speaker.speak(englishPhrase(it), Language.ENGLISH) },
        ) { card, _, wide ->
            CardLayout(
                wide,
                primary = { FitText("${card.letter} ${card.letter.lowercase()}", weight = FontWeight.Black, color = section.accent) },
                picture = { PictureView(card.picture) },
                caption = { FitText(englishPhrase(card)) },
            )
        }

        Section.SWAR, Section.VYANJAN -> CardPager(
            section, if (section == Section.SWAR) content.swar else content.vyanjan, prefs, speaker, onBack,
            hindi = true,
            say = { speaker.speak(hindiPhrase(it), Language.HINDI) },
        ) { card, index, wide ->
            CardLayout(
                wide,
                primary = { FitText(card.letter, weight = FontWeight.Black, color = section.accent) },
                picture = { PictureView(card.picture, tint = ShapePalette[index % ShapePalette.size]) },
                caption = { FitText(hindiPhrase(card)) },
            )
        }

        Section.NUMBERS -> CardPager(
            section, content.numbers, prefs, speaker, onBack,
            hindi = true,
            say = {
                speaker.speak(it.english, Language.ENGLISH)
                speaker.speak(it.hindi, Language.HINDI, queue = true)
            },
        ) { n, _, wide ->
            CardLayout(
                wide,
                primary = { FitText(n.value.toString(), weight = FontWeight.Black, color = section.accent) },
                picture = { FitText(NumberPicture.rows(n.value).joinToString("\n"), Modifier.fillMaxSize(0.9f), maxLines = 4) },
                caption = { FitText("${n.english}\n${n.devanagari} · ${n.hindi}", maxLines = 2) },
            )
        }

        Section.COLOURS -> CardPager(
            section, content.colours, prefs, speaker, onBack,
            hindi = true,
            say = {
                speaker.speak(it.english, Language.ENGLISH)
                speaker.speak(it.hindi, Language.HINDI, queue = true)
            },
        ) { colour, _, wide ->
            CardLayout(
                wide,
                primary = { ColourSwatch(Color(colour.argb), Modifier.fillMaxHeight(0.9f)) },
                picture = { FitText(colour.english, Modifier.fillMaxSize(0.8f)) },
                caption = { FitText(colour.hindi) },
                bigPrimary = true,
            )
        }

        Section.SHAPES -> CardPager(
            section, content.shapes, prefs, speaker, onBack,
            hindi = true,
            say = {
                speaker.speak(it.english, Language.ENGLISH)
                speaker.speak(it.hindi, Language.HINDI, queue = true)
            },
        ) { shape, index, wide ->
            CardLayout(
                wide,
                primary = { ShapeView(shape.kind, ShapePalette[index % ShapePalette.size], Modifier.fillMaxHeight(0.95f)) },
                picture = { FitText(shape.english, Modifier.fillMaxSize(0.8f)) },
                caption = { FitText(shape.hindi) },
                bigPrimary = true,
            )
        }

        Section.TABLES -> {
            fun rowSpoken(r: Tables.Row) = res.getString(R.string.table_row_spoken, r.table, r.times, r.product)
            CardPager(
                section, Tables.range.toList(), prefs, speaker, onBack,
                clickableCard = false,
                // Swiping says only the title; ten lines on every swipe would be a lecture.
                autoSay = { speaker.speak(res.getString(R.string.table_title, it), Language.ENGLISH) },
                say = { t -> Tables.of(t).forEachIndexed { i, r -> speaker.speak(rowSpoken(r), Language.ENGLISH, queue = i > 0) } },
            ) { table, _, _ ->
                TableCard(table, section.accent) { speaker.speak(rowSpoken(it), Language.ENGLISH) }
            }
        }

        Section.QUIZ -> QuizScreen(content, speaker, onBack)
    }
}

/**
 * The swipeable deck every learning section uses: a pager of big cards, plus previous /
 * say again / next buttons for children who have not discovered swiping yet.
 */
@Composable
private fun <T> CardPager(
    section: Section,
    items: List<T>,
    prefs: Prefs,
    speaker: Speaker,
    onBack: () -> Unit,
    hindi: Boolean = false,
    clickableCard: Boolean = true,
    say: (T) -> Unit,
    autoSay: (T) -> Unit = say,
    card: @Composable (item: T, index: Int, wide: Boolean) -> Unit,
) {
    val pager = rememberPagerState { items.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { if (prefs.autoSpeak) autoSay(items[it]) }
    }
    DisposableEffect(Unit) { onDispose { speaker.stop() } }

    SectionScaffold(section, onBack, trailing = stringResource(R.string.page_of, pager.currentPage + 1, items.size)) {
        VoiceNote(speaker, hindi, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            pageSpacing = 16.dp,
        ) { page ->
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth > maxHeight * 1.1f
                KidCard(
                    section.accent,
                    Modifier.fillMaxSize(),
                    onClick = if (clickableCard) ({ say(items[page]) }) else null,
                ) { card(items[page], page, wide) }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val big = Modifier.size(72.dp)
            FilledTonalIconButton(
                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                enabled = pager.currentPage > 0,
                modifier = big,
            ) { Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.previous), Modifier.size(40.dp)) }
            FilledIconButton(
                onClick = { say(items[pager.currentPage]) },
                modifier = Modifier.size(88.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = section.accent, contentColor = Color.White),
            ) { Icon(painterResource(R.drawable.ic_volume_up), stringResource(R.string.say_again), Modifier.size(44.dp)) }
            FilledTonalIconButton(
                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                enabled = pager.currentPage < items.size - 1,
                modifier = big,
            ) { Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.next), Modifier.size(40.dp)) }
        }
    }
}

/** One multiplication table; each row is its own big tap target that reads itself out. */
@Composable
private fun TableCard(table: Int, accent: Color, onRow: (Tables.Row) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(0.14f), contentAlignment = Alignment.Center) {
            FitText(stringResource(R.string.table_title, table), color = accent, weight = FontWeight.Black)
        }
        LazyColumn(
            Modifier.fillMaxWidth().weight(0.86f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(Tables.of(table)) { row ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().clickable { onRow(row) },
                ) {
                    Text(
                        stringResource(R.string.table_row, row.table, row.times, row.product),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
