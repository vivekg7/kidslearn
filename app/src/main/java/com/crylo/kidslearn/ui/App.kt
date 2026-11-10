package com.crylo.kidslearn.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.crylo.kidslearn.Prefs
import com.crylo.kidslearn.R
import com.crylo.kidslearn.content.Content
import com.crylo.kidslearn.speech.Speaker

/**
 * Navigation is a single nullable [Section]: home, or one section, or settings. With no
 * deep links and one level of depth, a navigation library would only add weight.
 */
@Composable
fun KidsLearnApp(content: Content, prefs: Prefs, speaker: Speaker) {
    var section by rememberSaveable { mutableStateOf<Section?>(null) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var gate by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        // A parent may be coming back from installing the Hindi voice.
        speaker.refresh()
        onPauseOrDispose { speaker.stop() }
    }

    val current = section
    when {
        settings -> {
            BackHandler { settings = false }
            SettingsScreen(prefs, speaker, onBack = { settings = false })
        }
        current == null -> HomeScreen(onOpen = { section = it }, onSettings = { gate = true })
        else -> {
            val back = { section = null }
            BackHandler(onBack = back)
            SectionScreen(current, content, prefs, speaker, back)
        }
    }

    if (gate) {
        ParentGateDialog(
            onPass = {
                gate = false
                settings = true
            },
            onDismiss = { gate = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(onOpen: (Section) -> Unit, onSettings: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title), fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.settings))
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(Section.entries) { s ->
                Card(
                    onClick = { onOpen(s) },
                    modifier = Modifier.aspectRatio(1f),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = s.accent, contentColor = Color.White),
                ) {
                    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        FitText(stringResource(s.glyph), Modifier.weight(1f).fillMaxWidth(), color = Color.White, weight = FontWeight.Black)
                        Text(
                            stringResource(s.label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** Top bar shared by every section: back, title, and an optional "3 / 26" counter. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionScaffold(
    section: Section,
    onBack: () -> Unit,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(section.label), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                    }
                },
                actions = {
                    if (trailing != null) {
                        Text(trailing, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 16.dp))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding), content = content)
    }
}
