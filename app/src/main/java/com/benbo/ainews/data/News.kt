package com.benbo.ainews.data

/** The three kinds of story the widget colour-codes. */
enum class Category(val label: String) { RESEARCH("Research"), INDUSTRY("Industry"), COMMUNITY("Community") }

data class Story(
    val title: String,
    val source: String,
    val category: Category,
    /** How many minutes before the last refresh this story was published. */
    val minutesBeforeRefresh: Int,
)

/**
 * Sample headlines, the same ones as the web preview. They are not real news.
 * Swap this list for your feed (for example the news.json your GitHub job writes).
 */
object SampleNews {
    val stories = listOf(
        Story("Small open-weights model matches much larger rivals on coding benchmarks", "Hacker News", Category.COMMUNITY, 12),
        Story("Agents that plan with a learned world model beat baselines on long tasks", "arXiv", Category.RESEARCH, 38),
        Story("An AI lab ships a faster, cheaper model tier for developers", "The Verge", Category.INDUSTRY, 54),
        Story("New phone chips run 8B-parameter models fully offline", "TechCrunch", Category.INDUSTRY, 95),
        Story("Cutting preference-training cost in half with synthetic feedback", "arXiv", Category.RESEARCH, 130),
        Story("What's the best setup for running local LLMs this year?", "r/LocalLLaMA", Category.COMMUNITY, 160),
        Story("Regulators publish updated guidance for general-purpose AI models", "Ars Technica", Category.INDUSTRY, 210),
        Story("Why long context windows are getting cheaper to serve", "Import AI", Category.COMMUNITY, 300),
    )

    /** Initials badge for each source: text and colour (our colours, not the sources' brands). */
    fun badge(source: String): Pair<String, Long> = when (source) {
        "Hacker News" -> "HN" to 0xFFE4572E
        "arXiv" -> "aX" to 0xFF9B2C2C
        "The Verge" -> "V" to 0xFF6D28D9
        "TechCrunch" -> "TC" to 0xFF15803D
        "r/LocalLLaMA" -> "r/" to 0xFFEA580C
        "Ars Technica" -> "AT" to 0xFFB45309
        "Import AI" -> "IA" to 0xFF0E7490
        else -> source.filter { it.isLetter() }.take(2) to 0xFF64748B
    }
}

/** Mascot mood from the top story's age: under 30 min happy, 2 hours or more sleepy, calm in between. */
enum class Mood { HAPPY, CALM, SLEEPY;
    companion object {
        fun fromAge(minutes: Long) = when {
            minutes < 30 -> HAPPY
            minutes >= 120 -> SLEEPY
            else -> CALM
        }
    }
}

fun ageLabel(minutes: Long, short: Boolean): String = when {
    minutes < 60 -> if (short) "${minutes}m" else "$minutes min ago"
    else -> { val h = Math.round(minutes / 60.0); if (short) "${h}h" else "$h hr ago" }
}
