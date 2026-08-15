package com.mohan.news.data

/**
 * A single piece of extracted article content, in original reading order.
 * This is the output of the in-app "readability" style extraction performed
 * by ArticleReaderRepository — plain, ad-free, tracker-free content.
 */
sealed class ReaderBlock {
    data class Heading(val text: String) : ReaderBlock()
    data class Paragraph(val text: String) : ReaderBlock()
    data class Quote(val text: String) : ReaderBlock()
    data class Image(val url: String, val caption: String? = null) : ReaderBlock()
}

/**
 * A fully extracted, cleaned-up article ready for the distraction-free reader view.
 */
data class ReaderArticle(
    val title: String,
    val sourceName: String,
    val byline: String? = null,
    val publishedDate: String? = null,
    val leadImageUrl: String? = null,
    val canonicalUrl: String,
    val blocks: List<ReaderBlock>,
    /** True when we couldn't extract the real article body and are showing a best-effort stand-in. */
    val isFallback: Boolean = false
) {
    /** Roughly-estimated reading time at ~200 words per minute. */
    val readingTimeMinutes: Int
        get() {
            val words = blocks.sumOf { block ->
                when (block) {
                    is ReaderBlock.Heading -> block.text.split(Regex("\\s+")).size
                    is ReaderBlock.Paragraph -> block.text.split(Regex("\\s+")).size
                    is ReaderBlock.Quote -> block.text.split(Regex("\\s+")).size
                    is ReaderBlock.Image -> 0
                }
            }
            return maxOf(1, Math.round(words / 200f))
        }

    /** Text-only blocks in order, used both for read-aloud and for progress highlighting. */
    val spokenParagraphs: List<String>
        get() = blocks.mapNotNull { block ->
            when (block) {
                is ReaderBlock.Heading -> block.text
                is ReaderBlock.Paragraph -> block.text
                is ReaderBlock.Quote -> block.text
                is ReaderBlock.Image -> null
            }
        }

    /** Index (into [blocks]) of every block that is spoken aloud, in the same order as [spokenParagraphs]. */
    val speakableBlockIndices: List<Int>
        get() = blocks.indices.filter { blocks[it] !is ReaderBlock.Image }
}

/**
 * Progress through the multi-step article fetch pipeline, surfaced to the UI
 * as a real (not simulated) progress bar with a human-readable stage label.
 */
data class ReaderProgress(val fraction: Float, val label: String)
