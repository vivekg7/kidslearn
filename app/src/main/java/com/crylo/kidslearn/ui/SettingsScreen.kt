package com.crylo.kidslearn.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.crylo.kidslearn.Prefs
import com.crylo.kidslearn.R
import com.crylo.kidslearn.speech.Language
import com.crylo.kidslearn.speech.Speaker

/** Parent-only settings, reached through the hold-to-open gate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(prefs: Prefs, speaker: Speaker, onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    val sample = stringResource(R.string.english_phrase, "A", "Apple")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_speed), style = MaterialTheme.typography.titleMedium)
                val speeds = listOf(true to R.string.speed_slow, false to R.string.speed_normal)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    speeds.forEachIndexed { i, (slow, label) ->
                        SegmentedButton(
                            selected = prefs.slowSpeech == slow,
                            onClick = {
                                prefs.updateSlowSpeech(slow)
                                speaker.speak(sample, Language.ENGLISH)
                            },
                            shape = SegmentedButtonDefaults.itemShape(i, speeds.size),
                        ) { Text(stringResource(label)) }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_auto_speak),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = prefs.autoSpeak, onCheckedChange = prefs::updateAutoSpeak)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_voice), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.settings_voice_body), style = MaterialTheme.typography.bodyMedium)
                VoiceNote(speaker, hindi = true)
                FilledTonalButton(onClick = { speaker.openVoiceInstall(context) }) {
                    Text(stringResource(R.string.open_voice_settings))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.about_body, version), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
