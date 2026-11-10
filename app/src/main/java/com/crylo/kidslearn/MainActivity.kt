package com.crylo.kidslearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.crylo.kidslearn.content.Content
import com.crylo.kidslearn.speech.Speaker
import com.crylo.kidslearn.ui.KidsLearnApp
import com.crylo.kidslearn.ui.KidsLearnTheme

class MainActivity : ComponentActivity() {
    private lateinit var speaker: Speaker

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A few kilobytes of text: parsing it up front is faster than any async dance.
        val content = Content.load { name -> assets.open("content/$name").bufferedReader().use { it.readText() } }
        val prefs = Prefs(this)
        speaker = Speaker(this) { if (prefs.slowSpeech) Speaker.RATE_SLOW else Speaker.RATE_NORMAL }
        setContent {
            KidsLearnTheme {
                KidsLearnApp(content, prefs, speaker)
            }
        }
    }

    override fun onDestroy() {
        speaker.shutdown()
        super.onDestroy()
    }
}
