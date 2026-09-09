package com.incleanhome.mobile.legal.presentation

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.ui.components.ScreenBackground
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.components.SecondaryButton
import com.incleanhome.mobile.ui.theme.Navy

@Composable
fun LegalDocumentScreen(
    title: String,
    @RawRes documentResource: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resources = LocalContext.current.resources
    val languageNotice = stringResource(R.string.legal_spanish_only_notice)
    val blocks = remember(documentResource) {
        resources.openRawResource(documentResource).bufferedReader(Charsets.UTF_8).use { reader ->
            parseLegalMarkdown(reader.readText())
        }
    }

    ScreenBackground(modifier) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            ScreenHeader(title = title, onBack = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (languageNotice.isNotBlank()) {
                    item {
                        Text(
                            text = languageNotice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(blocks) { block -> LegalBlockContent(block) }
            }
        }
    }
}

@Composable
fun LegalDocumentLinks(
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryButton(
            text = stringResource(R.string.legal_view_terms),
            onClick = onTerms,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
        SecondaryButton(
            text = stringResource(R.string.legal_view_privacy),
            onClick = onPrivacy,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
    }
}

@Composable
private fun LegalBlockContent(block: LegalBlock) {
    when (block) {
        is LegalBlock.Heading -> Text(
            text = block.text,
            style = when (block.level) {
                1 -> MaterialTheme.typography.headlineMedium
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
            color = Navy,
            fontWeight = FontWeight.Bold
        )

        is LegalBlock.Paragraph -> Text(
            text = block.text,
            style = MaterialTheme.typography.bodyLarge
        )

        is LegalBlock.Bullet -> Row(Modifier.fillMaxWidth()) {
            Text("•", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.width(8.dp))
            Text(
                text = block.text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        LegalBlock.Divider -> HorizontalDivider()
    }
}

internal sealed interface LegalBlock {
    data class Heading(val level: Int, val text: String) : LegalBlock
    data class Paragraph(val text: String) : LegalBlock
    data class Bullet(val text: String) : LegalBlock
    data object Divider : LegalBlock
}

internal fun parseLegalMarkdown(markdown: String): List<LegalBlock> {
    val blocks = mutableListOf<LegalBlock>()
    val paragraph = mutableListOf<String>()
    var bullet: StringBuilder? = null

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += LegalBlock.Paragraph(cleanInlineMarkdown(paragraph.joinToString(" ")))
            paragraph.clear()
        }
    }

    fun flushBullet() {
        bullet?.let { blocks += LegalBlock.Bullet(cleanInlineMarkdown(it.toString())) }
        bullet = null
    }

    markdown.replace("\r\n", "\n").lineSequence().forEach { sourceLine ->
        val trimmed = sourceLine.trim()
        when {
            trimmed.isEmpty() -> {
                flushParagraph()
                flushBullet()
            }

            trimmed == "---" -> {
                flushParagraph()
                flushBullet()
                blocks += LegalBlock.Divider
            }

            trimmed.startsWith("#") -> {
                flushParagraph()
                flushBullet()
                val level = trimmed.takeWhile { it == '#' }.length
                val text = trimmed.drop(level).trim()
                blocks += LegalBlock.Heading(level, cleanInlineMarkdown(text))
            }

            trimmed.startsWith("- ") -> {
                flushParagraph()
                flushBullet()
                bullet = StringBuilder(trimmed.removePrefix("- "))
            }

            bullet != null -> bullet?.append(' ')?.append(trimmed)
            else -> paragraph += trimmed
        }
    }
    flushParagraph()
    flushBullet()
    return blocks
}

private val markdownLink = Regex("\\[([^]]+)]\\([^)]+\\)")

private fun cleanInlineMarkdown(text: String): String =
    markdownLink.replace(text) { match -> match.groupValues[1] }
        .replace("**", "")
        .replace("`", "")
