package com.mohan.news.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mohan.news.ReaderUiState
import com.mohan.news.data.ReaderArticle
import com.mohan.news.data.ReaderBlock
import com.mohan.news.tts.TtsState

@Composable
fun ReaderScreen(
    readerState: ReaderUiState,
    ttsState: TtsState,
    ttsCurrentIndex: Int,
    onBack: () -> Unit,
    onOpenOriginal: (String) -> Unit,
    onRetry: () -> Unit,
    onToggleReadAloud: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            if (readerState is ReaderUiState.Loaded) {
                ExtendedFloatingActionButton(onClick = onToggleReadAloud) {
                    val icon = when (ttsState) {
                        TtsState.SPEAKING -> Icons.Filled.Pause
                        TtsState.PAUSED -> Icons.Filled.PlayArrow
                        TtsState.IDLE -> Icons.Filled.RecordVoiceOver
                    }
                    Icon(icon, contentDescription = "Read article aloud")
                    Spacer(Modifier.padding(4.dp))
                    Text(
                        when (ttsState) {
                            TtsState.SPEAKING -> "Pause"
                            TtsState.PAUSED -> "Resume"
                            TtsState.IDLE -> "Listen"
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (readerState) {
                is ReaderUiState.Idle, is ReaderUiState.Loading -> ReaderLoading()
                is ReaderUiState.Error -> ReaderError(
                    message = readerState.message,
                    onOpenOriginal = { onOpenOriginal(readerState.originalUrl) },
                    onRetry = onRetry
                )
                is ReaderUiState.Loaded -> ReaderContent(
                    article = readerState.article,
                    ttsState = ttsState,
                    ttsCurrentIndex = ttsCurrentIndex,
                    bottomPadding = padding.calculateBottomPadding(),
                    onOpenOriginal = { onOpenOriginal(readerState.article.canonicalUrl) }
                )
            }

            // Floating back button, always available, overlaid on top of the content.
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .padding(top = 12.dp, start = 12.dp)
                    .size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        }
    }
}

@Composable
private fun ReaderLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(16.dp))
        Text(
            "Fetching a clean, ad-free version…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReaderError(message: String, onOpenOriginal: () -> Unit, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.size(16.dp))
        Text("Couldn't load a clean view", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.size(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.size(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.OutlinedButton(onClick = onRetry) {
                Text("Try again")
            }
            androidx.compose.material3.Button(onClick = onOpenOriginal) {
                Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Open in browser")
            }
        }
    }
}

@Composable
private fun ReaderContent(
    article: ReaderArticle,
    ttsState: TtsState,
    ttsCurrentIndex: Int,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onOpenOriginal: () -> Unit
) {
    // Maps the flat "spoken paragraph" index (from TTS) back to its position
    // within article.blocks, so we can gently highlight what's being read.
    val speakableIndices = article.speakableBlockIndices
    val highlightedBlockIndex = if (ttsState != TtsState.IDLE && ttsCurrentIndex in speakableIndices.indices) {
        speakableIndices[ttsCurrentIndex]
    } else {
        -1
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + 96.dp)
    ) {
        item {
            if (!article.leadImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = article.leadImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                )
            } else {
                Spacer(Modifier.height(56.dp))
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = article.sourceName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = "${article.readingTimeMinutes} min read",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        lineHeight = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!article.byline.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "By ${article.byline}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Original: ${article.canonicalUrl}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                )
            }
        }

        itemsIndexed(article.blocks) { index, block ->
            val isHighlighted = index == highlightedBlockIndex
            ReaderBlockView(block = block, isHighlighted = isHighlighted)
        }

        item {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                androidx.compose.material3.TextButton(onClick = onOpenOriginal) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("View original page")
                }
            }
        }
    }
}

@Composable
private fun ReaderBlockView(block: ReaderBlock, isHighlighted: Boolean) {
    val highlightColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            Color.Transparent
        },
        label = "readerHighlight"
    )

    when (block) {
        is ReaderBlock.Heading -> {
            Text(
                text = block.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(highlightColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
        is ReaderBlock.Paragraph -> {
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontSize = 17.sp,
                    lineHeight = 28.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(highlightColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        is ReaderBlock.Quote -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(highlightColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.size(14.dp))
                Text(
                    text = block.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 17.sp,
                        lineHeight = 26.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        is ReaderBlock.Image -> {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                AsyncImage(
                    model = block.url,
                    contentDescription = block.caption,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                )
                if (!block.caption.isNullOrBlank()) {
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = block.caption,
                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}


