package com.benbo.ainews

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.benbo.ainews.data.Mood
import org.json.JSONObject

/**
 * The animated mascot, using the same animations as the web preview:
 * idle loops (hop, sway, peekaboo, breathing), mood-change stories (Bedtime, Good morning,
 * Dozing off, Bubble pop), and tap reactions (boop, quack).
 *
 * It draws the SVG files from assets/mascots/ (the source of truth) inside a small web page,
 * app/src/main/assets/stage/mascot.html, because their labelled parts (data-part="...") can
 * be animated there. Home screen widgets can't animate, so this only runs inside the app.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MascotStage(mascot: String, mood: Mood, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val html = remember { stageHtml(context) }
    val call by rememberUpdatedState("setMascot('$mascot', '${mood.name.lowercase()}');")

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                // Without this the WebView gets WRAP_CONTENT, which gives the page a height of 0
                // and the mascot is drawn at 0 x 0.
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        view.tag = "ready"
                        view.evaluateJavascript(call, null)
                    }
                }
                loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
        update = { view ->
            // Read call every time, even before the page is ready: Compose only re-runs this block when state it
            // read last time changes, so a new mascot or mood would otherwise never reach the page.
            val js = call
            if (view.tag == "ready") view.evaluateJavascript(js, null)
        },
    )
}

/** The stage page with every mascot SVG from assets/mascots/ filled in. */
private fun stageHtml(context: Context): String {
    val assets = context.assets
    val svgs = JSONObject()
    assets.list("mascots").orEmpty().filter { it.endsWith(".svg") }.forEach { name ->
        svgs.put(name.removeSuffix(".svg"), assets.open("mascots/$name").bufferedReader().use { it.readText() })
    }
    val page = assets.open("stage/mascot.html").bufferedReader().use { it.readText() }
    return page.replace("/*SVG_JSON*/{}", svgs.toString())
}
