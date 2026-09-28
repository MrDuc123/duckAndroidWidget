package com.benbo.ainews

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benbo.ainews.data.Mood
import com.benbo.ainews.data.Prefs
import com.benbo.ainews.data.SampleNews
import com.benbo.ainews.widget.NewsWidgetLargeReceiver
import com.benbo.ainews.widget.NewsWidgetReceiver
import com.benbo.ainews.widget.NewsWidgetSmallReceiver
import com.benbo.ainews.widget.refreshAllWidgets
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) { SettingsScreen() }
            }
        }
    }
}

/**
 * Opens from the widget itself: touch and hold the widget, then tap its settings (pencil) button.
 * Also opens when the widget is first added. Changes apply right away; Done closes it.
 */
class ConfigureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Keep the widget even if you leave with the back button.
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) { SettingsScreen(onDone = { finish() }) }
            }
        }
    }
}

private val COLORS: List<Pair<String, Long?>> = listOf(
    "Category" to null,
    "Pink" to 0xFFEC4899, "Violet" to 0xFF8B5CF6, "Blue" to 0xFF3B82F6,
    "Amber" to 0xFFF59E0B, "Green" to 0xFF10B981, "Red" to 0xFFEF4444,
)

/** The "+" circle for picking any colour: a colour wheel until you've picked one. */
private val CustomSwatch = Brush.sweepGradient(
    listOf(0xFFEF4444, 0xFFF59E0B, 0xFF10B981, 0xFF3B82F6, 0xFF8B5CF6, 0xFFEC4899, 0xFFEF4444).map { Color(it) },
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsScreen(onDone: (() -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(Prefs.read(context)) }
    var addSize by remember { mutableStateOf("4x2") }

    fun changed() {
        settings = Prefs.read(context)
        scope.launch { refreshAllWidgets(context) }
    }

    val mood = Mood.fromAge(Prefs.ageOf(SampleNews.stories.first(), settings))
    // "icon" draws a faded category icon instead of a mascot, so there's no mascot or style to show.
    val hasMascot = settings.mascot == "duck" || settings.mascot == "catbox"

    Column(
        // safeDrawingPadding: keep clear of the status and navigation bars (apps draw behind them on Android 15+).
        modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("AI News", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(
            "Pick your widget's mascot and colour. Changes show on the home screen widget right away. " +
                "The headlines are samples, not real news.",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (hasMascot) {
            MascotStage(
                mascot = settings.mascot,
                mood = mood,
                modifier = Modifier.fillMaxWidth().height(220.dp),
            )
            Text(
                "Tap the mascot. Change the story age below to watch it fall asleep or wake up.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Section("Mascot") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("duck" to "Duck", "catbox" to "Box cat", "icon" to "Icon", "none" to "None").forEach { (key, label) ->
                    FilterChip(
                        selected = settings.mascot == key,
                        onClick = { Prefs.setMascot(context, key); changed() },
                        label = { Text(label) },
                    )
                }
            }
        }

        if (hasMascot) {
            Section("Mascot style") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("peek" to "Peeking", "faded" to "Faded, behind text").forEach { (key, label) ->
                        FilterChip(
                            selected = settings.mascotStyle == key,
                            onClick = { Prefs.setMascotStyle(context, key); changed() },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        Section("Widget colour") {
            // A colour that isn't one of the circles was picked with the "+" circle.
            val custom = settings.color?.takeIf { color -> COLORS.none { it.second == color } }
            var picking by remember { mutableStateOf(false) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                COLORS.forEach { (name, value) ->
                    val selected = settings.color == value
                    val fill = if (value == null) {
                        Brush.sweepGradient(listOf(Color(0xFFC2410C), Color(0xFF2563EB), Color(0xFF0F9D76), Color(0xFFC2410C)))
                    } else Brush.linearGradient(listOf(Color(value), Color(value)))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            .padding(if (selected) 5.dp else 0.dp)
                            .background(fill, CircleShape)
                            .clickable { Prefs.setColor(context, value); changed() },
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(if (custom != null) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        .padding(if (custom != null) 5.dp else 0.dp)
                        .background(custom?.let { SolidColor(Color(it)) } ?: CustomSwatch, CircleShape)
                        .clickable { picking = true },
                    contentAlignment = Alignment.Center,
                ) { PlusMark() }
            }
            Text(
                "First circle: use the top story's category colour. Last circle: pick any colour.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (picking) {
                ColorPickerDialog(
                    initial = custom ?: 0xFFEC4899,
                    onDismiss = { picking = false },
                    onPick = { Prefs.setColor(context, it); changed(); picking = false },
                )
            }
        }

        Section("Try the moods") {
            Text(
                "The mascot is happy when the top story is under 30 minutes old, calm until 2 hours, then sleepy " +
                    "(the box cat climbs out and curls up). Make the top story look older to test it:",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(12 to "12 min", 45 to "45 min", 180 to "3 hr").forEach { (min, label) ->
                    OutlinedButton(onClick = { Prefs.setTopStoryAge(context, min); changed() }) { Text(label) }
                }
            }
        }

        Section("Widget size") {
            if (onDone != null) {
                Text(
                    "Android doesn't let apps resize a widget that's already on the home screen. " +
                        "Touch and hold the widget, then drag its edges: 2×2 shows the top story, 4×2 adds the header, " +
                        "4×4 adds the chips and list, and wider than 4 (on the open Fold) shows two columns.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("2x2" to "2 × 2  Top story", "4x2" to "4 × 2  With header", "4x4" to "4 × 4  Full list")
                        .forEach { (key, label) ->
                            FilterChip(selected = addSize == key, onClick = { addSize = key }, label = { Text(label) })
                        }
                }
                Button(onClick = {
                    val receiver = when (addSize) {
                        "2x2" -> NewsWidgetSmallReceiver::class.java
                        "4x4" -> NewsWidgetLargeReceiver::class.java
                        else -> NewsWidgetReceiver::class.java
                    }
                    val manager = AppWidgetManager.getInstance(context)
                    if (manager.isRequestPinAppWidgetSupported) {
                        manager.requestPinAppWidget(ComponentName(context, receiver), null, null)
                    }
                }) { Text("Add " + addSize.replace("x", " × ") + " widget to home screen") }
                Text(
                    "Already added one? Touch and hold it and drag its edges to change its size.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (onDone != null) {
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        } else {
            Text(
                "You can also long-press the home screen, tap Widgets, and find AI News. " +
                    "To change the mascot later, open this app again, or touch and hold the widget and tap its settings button.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** White "+" with a soft shadow, so it shows on any colour. */
@Composable
private fun PlusMark() {
    Canvas(Modifier.size(14.dp)) {
        listOf(Color.Black.copy(alpha = 0.3f) to 4.dp.toPx(), Color.White to 2.5.dp.toPx()).forEach { (color, width) ->
            drawLine(color, Offset(center.x, 0f), Offset(center.x, size.height), width, StrokeCap.Round)
            drawLine(color, Offset(0f, center.y), Offset(size.width, center.y), width, StrokeCap.Round)
        }
    }
}

/** Pick any widget colour with hue, saturation and brightness bars. Returns a colour like 0xFFEC4899. */
@Composable
private fun ColorPickerDialog(initial: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val start = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toInt(), it) } }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var saturation by remember { mutableFloatStateOf(start[1]) }
    var brightness by remember { mutableFloatStateOf(start[2]) }
    val color = Color.hsv(hue, saturation, brightness)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick any colour") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.fillMaxWidth().height(48.dp).background(color, RoundedCornerShape(12.dp)))
                GradientBar("Hue", hue / 360f, Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f, 1f, 1f) })) {
                    hue = it * 360f
                }
                GradientBar(
                    "Saturation", saturation,
                    Brush.horizontalGradient(listOf(Color.hsv(hue, 0f, brightness), Color.hsv(hue, 1f, brightness))),
                ) { saturation = it }
                GradientBar(
                    "Brightness", brightness,
                    Brush.horizontalGradient(listOf(Color.Black, Color.hsv(hue, saturation, 1f))),
                ) { brightness = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(color.toArgb().toLong() and 0xFFFFFFFFL) }) { Text("Use colour") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** A colour bar you tap or drag along. [value] and the value passed to [onChange] run from 0 to 1. */
@Composable
private fun GradientBar(label: String, value: Float, brush: Brush, onChange: (Float) -> Unit) {
    val change by rememberUpdatedState(onChange)
    // The thumb's centre travels from half a thumb in from one end to half a thumb short of the other.
    fun PointerInputScope.valueAt(x: Float) = ((x - 14.dp.toPx()) / (size.width - 28.dp.toPx())).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(brush)
                .pointerInput(Unit) { detectTapGestures(onPress = { change(valueAt(it.x)) }) }
                .pointerInput(Unit) { detectHorizontalDragGestures { pointer, _ -> change(valueAt(pointer.position.x)) } },
        ) {
            Box(
                Modifier
                    .offset(x = (maxWidth - 28.dp) * value)
                    .size(28.dp)
                    .border(3.dp, Color.White, CircleShape),
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}
