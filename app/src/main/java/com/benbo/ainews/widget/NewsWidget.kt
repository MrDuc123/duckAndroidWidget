package com.benbo.ainews.widget

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.benbo.ainews.MainActivity
import com.benbo.ainews.R
import com.benbo.ainews.data.Category
import com.benbo.ainews.data.Mood
import com.benbo.ainews.data.Prefs
import com.benbo.ainews.data.SampleNews
import com.benbo.ainews.data.Story
import com.benbo.ainews.data.WidgetSettings
import com.benbo.ainews.data.ageLabel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.glance.color.ColorProvider as dayNight

// Widget sizes, matching the Galaxy picker: 2x2, 4x2, 4x4, and 8x4 (open Z Fold 8 inner screen).
private val SIZE_2x2 = DpSize(110.dp, 110.dp)
private val SIZE_4x2 = DpSize(250.dp, 110.dp)
private val SIZE_4x4 = DpSize(250.dp, 250.dp)
private val SIZE_8x4 = DpSize(460.dp, 250.dp)

/** Per-widget state: which filter chip is selected, plus a tick that forces a redraw. */
val FilterKey = stringPreferencesKey("filter")
val TickKey = longPreferencesKey("tick")
val FilterParam = ActionParameters.Key<String>("filter")

class NewsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SIZE_2x2, SIZE_4x2, SIZE_4x4, SIZE_8x4))
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = currentState<Preferences>()
            state[TickKey] // read so a tick change redraws
            val settings = Prefs.read(LocalContext.current)
            WidgetContent(settings, state[FilterKey] ?: "all")
        }
    }
}

class NewsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NewsWidget()
}

// The same widget offered at other starting sizes in the Widgets list (2x2 and 4x4).
class NewsWidgetSmallReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NewsWidget()
}

class NewsWidgetLargeReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NewsWidget()
}

/** Redraw every AI News widget, e.g. after the mascot or colour changes. */
suspend fun refreshAllWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    manager.getGlanceIds(NewsWidget::class.java).forEach { id ->
        updateAppWidgetState(context, id) { it[TickKey] = System.currentTimeMillis() }
        NewsWidget().update(context, id)
    }
}

class SetFilterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { it[FilterKey] = parameters[FilterParam] ?: "all" }
        NewsWidget().update(context, glanceId)
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Prefs.markRefreshed(context) // with a real feed, fetch news.json here
        refreshAllWidgets(context)
    }
}

// ---------- colours ----------

private fun mix(a: Color, b: Color, t: Float) = Color(
    red = b.red + (a.red - b.red) * t,
    green = b.green + (a.green - b.green) * t,
    blue = b.blue + (a.blue - b.blue) * t,
)

private val LightBase = Color(0xFFF7F8FB)
private const val GlassAlpha = 0.86f
private val DarkBase = Color(0xFF1C1E24)
private val Ink = dayNight(day = Color(0xFF111318), night = Color(0xFFF2F3F5))
private val Muted = dayNight(day = Color(0xFF6B7280), night = Color(0xFF9CA3AF))

private fun categoryColor(c: Category): Pair<Color, Color> = when (c) {
    Category.RESEARCH -> Color(0xFFC2410C) to Color(0xFFFB923C)
    Category.INDUSTRY -> Color(0xFF2563EB) to Color(0xFF7CA6FF)
    Category.COMMUNITY -> Color(0xFF0F9D76) to Color(0xFF34D399)
}

private fun categoryProvider(c: Category): ColorProvider =
    categoryColor(c).let { (d, n) -> dayNight(day = d, night = n) }

private fun categoryIcon(c: Category) = when (c) {
    Category.RESEARCH -> R.drawable.ic_research
    Category.INDUSTRY -> R.drawable.ic_industry
    Category.COMMUNITY -> R.drawable.ic_community
}

/** The same icons, faded (13%) with thinner lines, for the "Icon" mascot choice. */
private fun categoryMark(c: Category) = when (c) {
    Category.RESEARCH -> R.drawable.ic_research_mark
    Category.INDUSTRY -> R.drawable.ic_industry_mark
    Category.COMMUNITY -> R.drawable.ic_community_mark
}

/** The label above the lead story: "TOP STORY", or "TOP IN RESEARCH" etc. while a filter chip is on. */
private fun topLabel(filter: String, lead: Story) =
    if (filter == "all") "TOP STORY" else "TOP IN " + lead.category.label.uppercase()

/** The widget colour: your pick, or the top story's category colour. */
private class Palette(settings: WidgetSettings, top: Category, wash: Float) {
    private val day = settings.color?.let { Color(it) } ?: categoryColor(top).first
    private val night = settings.color?.let { Color(it) } ?: categoryColor(top).second
    val tone = dayNight(day = day, night = night)
    /**
     * The colour wash in the top-left corner; wash_overlay.xml fades it out towards the bottom-right.
     * Slightly see-through (like the preview's 86% glass) so the wallpaper's colours show through.
     * Android widgets can't blur what's behind them, so there's no frosted blur.
     */
    val background = dayNight(
        day = mix(day, LightBase, wash).copy(alpha = GlassAlpha),
        night = mix(night, DarkBase, wash).copy(alpha = GlassAlpha),
    )
    val card = dayNight(day = mix(day, Color.White, 0.18f), night = mix(night, Color(0xFF262A33), 0.26f))
    val chip = dayNight(day = mix(day, Color.White, 0.10f), night = mix(night, Color(0xFF2A2E38), 0.18f))
}

private val Accent = dayNight(day = Color(0xFF3056E8), night = Color(0xFF7CA6FF))
private val AccentSoft = dayNight(day = Color(0xFFE3E9FF), night = Color(0xFF1E2744))

// The page's "rainbow" gradient (research orange > pink > industry blue > community green),
// applied letter by letter because widget text can't have a gradient.
private val RainbowStops = listOf(0f, 0.35f, 0.68f, 1f)
private val RainbowDay = listOf(Color(0xFFC2410C), Color(0xFFE0457B), Color(0xFF2563EB), Color(0xFF0F9D76))
private val RainbowNight = listOf(Color(0xFFFB923C), Color(0xFFE0457B), Color(0xFF7CA6FF), Color(0xFF34D399))

private fun rainbowAt(colors: List<Color>, t: Float): Color {
    for (i in 1 until RainbowStops.size) {
        if (t <= RainbowStops[i]) {
            val local = (t - RainbowStops[i - 1]) / (RainbowStops[i] - RainbowStops[i - 1])
            return mix(colors[i], colors[i - 1], local)
        }
    }
    return colors.last()
}

/** Big, faded, slightly tilted copies used behind the text. */
private fun fadedRes(res: Int): Int = when (res) {
    R.drawable.duck_happy -> R.drawable.duck_happy_faded
    R.drawable.duck_calm -> R.drawable.duck_calm_faded
    R.drawable.duck_sleepy -> R.drawable.duck_sleepy_faded
    R.drawable.cat_box_happy -> R.drawable.cat_box_happy_faded
    R.drawable.cat_box_calm -> R.drawable.cat_box_calm_faded
    else -> R.drawable.cat_curl_sleepy_faded
}

private fun mascotRes(mascot: String, mood: Mood): Int? = when (mascot) {
    "duck" -> when (mood) {
        Mood.HAPPY -> R.drawable.duck_happy
        Mood.CALM -> R.drawable.duck_calm
        Mood.SLEEPY -> R.drawable.duck_sleepy
    }
    // The box cat climbs out of its box and curls up when it gets sleepy.
    "catbox" -> when (mood) {
        Mood.HAPPY -> R.drawable.cat_box_happy
        Mood.CALM -> R.drawable.cat_box_calm
        Mood.SLEEPY -> R.drawable.cat_curl_sleepy
    }
    else -> null
}

// ---------- layout ----------

@Composable
private fun WidgetContent(settings: WidgetSettings, filter: String) {
    val size = LocalSize.current
    val all = SampleNews.stories
    val stories = if (filter == "all") all else all.filter { it.category.name.lowercase() == filter }
    val lead = stories.firstOrNull() ?: all.first()
    val mood = Mood.fromAge(Prefs.ageOf(all.first(), settings))
    val small = size.width < 200.dp
    // 2 x 2 is the iPhone-style card with a stronger wash; bigger sizes are the Galaxy card.
    val palette = if (small) Palette(settings, all.first().category, 0.34f) else Palette(settings, lead.category, 0.24f)
    val mascotImage = mascotRes(settings.mascot, mood)
    val faded = mascotImage != null && settings.mascotStyle == "faded"
    // Peeking: the layouts draw the small mascot. Faded: it's drawn once, big, behind everything.
    val mascot = if (faded) null else mascotImage

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(palette.background)
            .cornerRadius(24.dp),
    ) {
        Box(modifier = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.wash_overlay))) {}
        Box(modifier = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_outline))) {}
        if (faded && mascotImage != null) {
            val big = when {
                small -> 104.dp
                size.height < 200.dp -> 120.dp
                else -> 180.dp
            }
            // The faded pictures are drawn off-centre, so the mascot hangs over the corner.
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                FlipBook(MascotFrames.fadedFor(mascotImage), fadedRes(mascotImage), big)
            }
        }
        if (settings.mascot == "icon") {
            // "Icon": the lead story's category icon, big and faded in the corner, instead of a mascot.
            val category = if (small) all.first().category else lead.category
            val big = when {
                small -> 78.dp
                size.height < 200.dp -> 100.dp
                else -> 130.dp
            }
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                Image(
                    ImageProvider(categoryMark(category)), null, GlanceModifier.size(big),
                    colorFilter = ColorFilter.tint(palette.tone),
                )
            }
        }
        Box(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
            when {
                small -> SmallLayout(all.first(), settings, palette, mascot)
                size.height < 200.dp -> MediumLayout(lead, settings, palette, mascot, topLabel(filter, lead))
                else -> LargeLayout(stories, lead, settings, palette, mascot, filter, twoColumns = size.width >= 400.dp)
            }
        }
    }
}

/** 2 x 2: just the top story, with the mascot in the corner. */
@Composable
private fun SmallLayout(top: Story, settings: WidgetSettings, palette: Palette, mascot: Int?) {
    Column(modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>())) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SmallBrand()
            Spacer(GlanceModifier.defaultWeight())
            CategoryBadge(top.category, 20.dp)
        }
        Spacer(GlanceModifier.height(6.dp))
        Kicker("TOP STORY", palette)
        Text(
            text = top.title,
            maxLines = 3,
            style = TextStyle(color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(GlanceModifier.defaultWeight())
        // Source badge and name in the widget colour, then the age (same as the web preview).
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SourceBadge(top.source, 16.dp)
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = top.source,
                maxLines = 1,
                style = TextStyle(color = palette.tone, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = (if (mascot != null) " · " else " ") + ageLabel(Prefs.ageOf(top, settings), short = true),
                style = TextStyle(color = Muted, fontSize = 11.sp),
            )
            if (mascot != null) AnimatedMascot(mascot, 46.dp)
        }
    }
}

/** 4 x 2: header and the top story card. */
@Composable
private fun MediumLayout(lead: Story, settings: WidgetSettings, palette: Palette, mascot: Int?, kicker: String) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Header(palette, mascot)
        Spacer(GlanceModifier.height(8.dp))
        TopStoryCard(lead, settings, palette, kicker)
    }
}

/** 4 x 4 and 8 x 4: filter chips, top story, the list, and the footer with the mascot. */
@Composable
private fun LargeLayout(
    stories: List<Story>, lead: Story, settings: WidgetSettings, palette: Palette,
    mascot: Int?, filter: String, twoColumns: Boolean,
) {
    val rest = stories.drop(1)
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Header(palette, null)
        Spacer(GlanceModifier.height(8.dp))
        FilterChips(filter)
        Spacer(GlanceModifier.height(8.dp))
        TopStoryCard(lead, settings, palette, topLabel(filter, lead))
        Spacer(GlanceModifier.height(4.dp))
        LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            if (twoColumns) {
                // Two columns filled row by row (1 | 2, 3 | 4 ...) like the web preview; each row is as tall
                // as its taller story. (LazyVerticalGrid would overlap rows whose stories differ in height.)
                items(rest.chunked(2)) { pair ->
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        StoryRow(pair[0], settings, maxLines = 3, modifier = GlanceModifier.defaultWeight())
                        Spacer(GlanceModifier.width(12.dp))
                        if (pair.size > 1) {
                            StoryRow(pair[1], settings, maxLines = 3, modifier = GlanceModifier.defaultWeight())
                        } else {
                            Spacer(GlanceModifier.defaultWeight())
                        }
                    }
                }
            } else {
                items(rest) { StoryRow(it, settings, maxLines = 2) }
            }
        }
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Updated " + clock(settings.refreshedAt) + " · every 30 min",
                style = TextStyle(color = Muted, fontSize = 11.sp),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (mascot != null) AnimatedMascot(mascot, 48.dp)
        }
    }
}

// ---------- pieces ----------

/** The peeking mascot, stepping through its idle animation (hop, sway, peekaboo, breathing...). */
@Composable
private fun AnimatedMascot(image: Int, size: Dp) = FlipBook(MascotFrames.forImage(image), image, size)

/** Flips through [frames] every 250 ms; shows the still [fallback] picture if there are no frames. */
@Composable
private fun FlipBook(frames: IntArray?, fallback: Int, size: Dp) {
    val context = LocalContext.current
    if (frames == null) {
        Image(ImageProvider(fallback), null, GlanceModifier.size(size))
        return
    }
    val flipper = RemoteViews(context.packageName, R.layout.mascot_flipper).apply {
        removeAllViews(R.id.mascot_flipper)
        frames.forEach { frame ->
            addView(
                R.id.mascot_flipper,
                RemoteViews(context.packageName, R.layout.mascot_frame).apply {
                    setImageViewResource(R.id.mascot_frame, frame)
                },
            )
        }
    }
    AndroidRemoteViews(flipper, GlanceModifier.size(size))
}

@Composable
private fun RainbowText(text: String, fontSize: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { i, ch ->
            val t = if (text.length > 1) i / (text.length - 1f) else 0f
            Text(
                ch.toString(),
                style = TextStyle(
                    color = dayNight(day = rainbowAt(RainbowDay, t), night = rainbowAt(RainbowNight, t)),
                    fontSize = fontSize.sp, fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

/** 2 x 2 brand: an orange sparkle and "AI NEWS" in the rainbow colours. */
@Composable
private fun SmallBrand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            ImageProvider(R.drawable.ic_spark), null, GlanceModifier.size(14.dp),
            colorFilter = ColorFilter.tint(categoryProvider(Category.RESEARCH)),
        )
        Spacer(GlanceModifier.width(4.dp))
        RainbowText("AI NEWS", 11)
    }
}

/** Header for 4 x 2 and up: gradient badge with a white sparkle, rainbow "AI News", refresh. */
@Composable
private fun Header(palette: Palette, mascot: Int?) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = GlanceModifier.size(28.dp).background(ImageProvider(R.drawable.brand_badge_bg)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                ImageProvider(R.drawable.ic_spark), null, GlanceModifier.size(16.dp),
                colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        RainbowText("AI News", 16)
        Spacer(GlanceModifier.defaultWeight())
        if (mascot != null) {
            AnimatedMascot(mascot, 32.dp)
            Spacer(GlanceModifier.width(6.dp))
        }
        Box(
            modifier = GlanceModifier.size(30.dp).background(AccentSoft).cornerRadius(15.dp)
                .clickable(actionRunCallback<RefreshAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                ImageProvider(R.drawable.ic_refresh), "Refresh", GlanceModifier.size(16.dp),
                colorFilter = ColorFilter.tint(Accent),
            )
        }
    }
}

/** Filter chips with their icons, coloured by category like the web preview. "All" is dark navy when selected. */
@Composable
private fun FilterChips(selected: String) {
    val chips = listOf("all" to "All") + Category.values().map { it.name.lowercase() to it.label }
    run {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            chips.forEach { (key, label) ->
                val on = key == selected
                val category = Category.values().firstOrNull { it.name.lowercase() == key }
                val (day, night) = category?.let { categoryColor(it) } ?: (Color(0xFF1F2A44) to Color(0xFFC9D5FF))
                val bg = when {
                    on -> dayNight(day = day, night = night)
                    category == null -> dayNight(day = Color(0xFFEEF0F5), night = Color(0xFF2A2E38))
                    else -> dayNight(day = mix(day, LightBase, 0.14f), night = mix(night, DarkBase, 0.18f))
                }
                val fg = when {
                    on && category == null -> dayNight(day = Color.White, night = Color(0xFF10152A))
                    on -> dayNight(day = Color.White, night = Color(0xFF10131A))
                    category == null -> Ink
                    else -> dayNight(day = day, night = night)
                }
                Row(
                    modifier = GlanceModifier
                        .background(bg)
                        .cornerRadius(12.dp)
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                        .clickable(actionRunCallback<SetFilterAction>(actionParametersOf(FilterParam to key))),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        ImageProvider(category?.let { categoryIcon(it) } ?: R.drawable.ic_all), null,
                        GlanceModifier.size(12.dp), colorFilter = ColorFilter.tint(fg),
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = label,
                        maxLines = 1,
                        style = TextStyle(color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    )
                }
                Spacer(GlanceModifier.width(5.dp))
            }
        }
    }
}

@Composable
private fun Kicker(text: String, palette: Palette) {
    Text(text, style = TextStyle(color = palette.tone, fontSize = 9.sp, fontWeight = FontWeight.Bold))
}

@Composable
private fun TopStoryCard(story: Story, settings: WidgetSettings, palette: Palette, kicker: String) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().background(palette.card).cornerRadius(14.dp).padding(10.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Top,
    ) {
        CategoryBadge(story.category, 32.dp)
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Kicker(kicker, palette)
            Text(
                story.title, maxLines = 2,
                style = TextStyle(color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            )
            Meta(story, settings)
        }
    }
}

@Composable
private fun StoryRow(
    story: Story, settings: WidgetSettings, maxLines: Int,
    modifier: GlanceModifier = GlanceModifier.fillMaxWidth(),
) {
    Row(
        modifier = modifier.padding(vertical = 5.dp).clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Top,
    ) {
        CategoryBadge(story.category, 22.dp)
        Spacer(GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(story.title, maxLines = maxLines, style = TextStyle(color = Ink, fontSize = 13.sp))
            Meta(story, settings)
        }
    }
}

@Composable
private fun Meta(story: Story, settings: WidgetSettings) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SourceBadge(story.source, 14.dp)
        Spacer(GlanceModifier.width(4.dp))
        Text(
            story.source,
            maxLines = 1,
            style = TextStyle(color = categoryProvider(story.category), fontSize = 11.sp, fontWeight = FontWeight.Bold),
        )
        Text(
            " · " + ageLabel(Prefs.ageOf(story, settings), short = false),
            maxLines = 1,
            style = TextStyle(color = Muted, fontSize = 11.sp),
        )
    }
}

@Composable
private fun CategoryBadge(category: Category, size: Dp) {
    val (day, night) = categoryColor(category)
    Box(
        modifier = GlanceModifier.size(size)
            .background(dayNight(day = mix(day, Color.White, 0.18f), night = mix(night, DarkBase, 0.25f)))
            .cornerRadius(size / 4),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            ImageProvider(categoryIcon(category)), category.label, GlanceModifier.size(size * 0.6f),
            colorFilter = ColorFilter.tint(categoryProvider(category)),
        )
    }
}

@Composable
private fun SourceBadge(source: String, size: Dp) {
    val (initials, color) = SampleNews.badge(source)
    Box(
        modifier = GlanceModifier.size(size).background(Color(color)).cornerRadius(size / 2),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, style = TextStyle(color = ColorProvider(Color.White), fontSize = (size.value * 0.5f).sp, fontWeight = FontWeight.Bold))
    }
}

private fun clock(millis: Long): String =
    DateTimeFormatter.ofPattern("H:mm").format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
