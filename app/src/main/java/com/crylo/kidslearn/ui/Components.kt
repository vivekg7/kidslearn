package com.crylo.kidslearn.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crylo.kidslearn.R
import com.crylo.kidslearn.content.Picture
import com.crylo.kidslearn.content.ShapeKind
import com.crylo.kidslearn.speech.Speaker
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Text that grows to fill whatever room it is given. Cards are sized by the screen, not
 * the text, so the same layout works on a small phone, a tablet and in landscape.
 */
@Composable
fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    weight: FontWeight = FontWeight.Bold,
    maxLines: Int = 1,
    maxSize: Int = 400,
) {
    // The box centres the text vertically too; BasicText alone draws from the top.
    Box(modifier, contentAlignment = Alignment.Center) {
        BasicText(
            text = text,
            style = TextStyle(color = color, fontWeight = weight, textAlign = TextAlign.Center),
            maxLines = maxLines,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = maxSize.sp, stepSize = 1.sp),
        )
    }
}

/** A centred box that hands its content all of its space. */
@Composable
fun Slot(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center, content = content)
}

/**
 * The three-part layout every learning card uses: a big primary (the letter or number), a
 * picture, and a caption. Stacked in portrait; primary and picture side by side when wide.
 */
@Composable
fun CardLayout(
    wide: Boolean,
    primary: @Composable BoxScope.() -> Unit,
    picture: @Composable BoxScope.() -> Unit,
    caption: @Composable BoxScope.() -> Unit,
    bigPrimary: Boolean = false,
) {
    // Colour and shape cards: the swatch is the lesson, the names are labels.
    val (p, m, c) = if (bigPrimary) Triple(0.56f, 0.24f, 0.2f) else Triple(0.36f, 0.4f, 0.24f)
    if (wide) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.weight(0.72f).fillMaxWidth()) {
                Slot(Modifier.weight(1f).fillMaxSize(), primary)
                Slot(Modifier.weight(1f).fillMaxSize(), picture)
            }
            Slot(Modifier.weight(0.28f).fillMaxWidth(), caption)
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Slot(Modifier.weight(p).fillMaxWidth(), primary)
            Slot(Modifier.weight(m).fillMaxWidth(), picture)
            Slot(Modifier.weight(c).fillMaxWidth(), caption)
        }
    }
}

/** A big rounded card tinted with the section colour. Tapping it says the card aloud. */
@Composable
fun KidCard(accent: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)?, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val border = BorderStroke(4.dp, accent)
    val inner: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxSize().background(accent.copy(alpha = 0.10f)).padding(16.dp),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border) { inner() }
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, border = border) { inner() }
    }
}

@Composable
fun PictureView(picture: Picture, modifier: Modifier = Modifier, tint: Color = ShapePalette[0]) {
    when (picture) {
        is Picture.Emoji -> FitText(picture.text, modifier)
        is Picture.Drawn -> ShapeView(picture.shape, tint, modifier.fillMaxSize(0.85f))
    }
}

/** Draws a [ShapeKind] in code, so shapes are crisp at any size and need no image files. */
@Composable
fun ShapeView(kind: ShapeKind, color: Color, modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outline
    Canvas(modifier.aspectRatio(1f)) {
        val s = min(size.width, size.height)
        val c = center
        val path = shapePath(kind, c, s)
        drawPath(path, color)
        // White shapes vanish on light cards without an edge.
        drawPath(path, outline, style = androidx.compose.ui.graphics.drawscope.Stroke(width = s * 0.012f))
    }
}

private fun shapePath(kind: ShapeKind, c: Offset, s: Float): Path {
    val r = s * 0.45f
    return when (kind) {
        ShapeKind.CIRCLE -> Path().apply { addOval(rect(c, r * 2, r * 2)) }
        ShapeKind.SQUARE -> Path().apply { addRect(rect(c, r * 1.6f, r * 1.6f)) }
        ShapeKind.RECTANGLE -> Path().apply { addRect(rect(c, r * 2, r * 1.2f)) }
        ShapeKind.OVAL -> Path().apply { addOval(rect(c, r * 2, r * 1.3f)) }
        ShapeKind.TRIANGLE -> polygon(c.copy(y = c.y + r * 0.2f), r * 1.1f, 3)
        ShapeKind.DIAMOND -> Path().apply {
            moveTo(c.x, c.y - r)
            lineTo(c.x + r * 0.7f, c.y)
            lineTo(c.x, c.y + r)
            lineTo(c.x - r * 0.7f, c.y)
            close()
        }
        ShapeKind.PENTAGON -> polygon(c.copy(y = c.y + r * 0.08f), r, 5)
        ShapeKind.HEXAGON -> polygon(c, r, 6)
        ShapeKind.STAR -> Path().apply {
            for (i in 0 until 10) {
                val radius = if (i % 2 == 0) r else r * 0.45f
                val p = point(c.copy(y = c.y + r * 0.08f), radius, i * PI / 5)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        ShapeKind.HEART -> Path().apply {
            val top = c.y - r * 0.55f
            moveTo(c.x, c.y + r * 0.85f)
            cubicTo(c.x - r * 1.3f, c.y + r * 0.05f, c.x - r * 0.95f, c.y - r * 1.15f, c.x, top)
            cubicTo(c.x + r * 0.95f, c.y - r * 1.15f, c.x + r * 1.3f, c.y + r * 0.05f, c.x, c.y + r * 0.85f)
            close()
        }
        ShapeKind.CRESCENT -> {
            val outer = Path().apply { addOval(rect(c, r * 2, r * 2)) }
            val bite = Path().apply { addOval(rect(c.copy(x = c.x + r * 0.55f, y = c.y - r * 0.2f), r * 1.8f, r * 1.8f)) }
            Path().apply { op(outer, bite, PathOperation.Difference) }
        }
    }
}

private fun rect(c: Offset, w: Float, h: Float) =
    androidx.compose.ui.geometry.Rect(Offset(c.x - w / 2, c.y - h / 2), Size(w, h))

/** Point on a circle, angle 0 at the top, clockwise. */
private fun point(c: Offset, r: Float, angle: Double) =
    Offset(c.x + (r * sin(angle)).toFloat(), c.y - (r * cos(angle)).toFloat())

private fun polygon(c: Offset, r: Float, sides: Int) = Path().apply {
    for (i in 0 until sides) {
        val p = point(c, r, i * 2 * PI / sides)
        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

/** A colour swatch: a big circle, outlined so white shows up on a light card. */
@Composable
fun ColourSwatch(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(1f)
            .background(color, CircleShape)
            .border(3.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}

/**
 * The friendly note shown when the phone cannot speak Hindi (or anything). Its button
 * leads into system settings, so it sits behind the parent gate.
 */
@Composable
fun VoiceNote(speaker: Speaker, hindi: Boolean, modifier: Modifier = Modifier) {
    val missing = when (speaker.state) {
        Speaker.State.NO_ENGINE -> true
        Speaker.State.READY -> hindi && !speaker.hindiReady
        Speaker.State.STARTING -> false
    }
    if (!missing) return
    val noEngine = speaker.state == Speaker.State.NO_ENGINE
    val context = LocalContext.current
    var gate by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (noEngine) R.string.no_engine_title else R.string.hindi_missing_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    stringResource(if (noEngine) R.string.no_engine_body else R.string.hindi_missing_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            FilledTonalButton(onClick = { gate = true }, modifier = Modifier.padding(start = 12.dp)) {
                Text(stringResource(R.string.install_voice))
            }
        }
    }
    if (gate) {
        ParentGateDialog(
            onPass = {
                gate = false
                speaker.openVoiceInstall(context)
            },
            onDismiss = { gate = false },
        )
    }
}

/**
 * "Press and hold for 3 seconds". Toddlers tap, they rarely hold still that long, and an
 * adult reading the dialog knows what to do. Letting go early resets the ring.
 */
@Composable
fun ParentGateDialog(onPass: () -> Unit, onDismiss: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gate_title)) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(stringResource(R.string.gate_body))
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier.size(132.dp),
                        strokeWidth = 8.dp,
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(104.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(onPress = {
                                    val hold = scope.launch {
                                        progress.animateTo(1f, tween(GATE_HOLD_MS, easing = LinearEasing))
                                        onPass()
                                    }
                                    tryAwaitRelease()
                                    if (progress.value < 1f) {
                                        hold.cancel()
                                        scope.launch { progress.animateTo(0f, tween(200)) }
                                    }
                                })
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.gate_hold),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private const val GATE_HOLD_MS = 3000
