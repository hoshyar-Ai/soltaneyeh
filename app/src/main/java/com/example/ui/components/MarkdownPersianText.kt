package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Renders Persian AI markdown (headings ###, **bold**, and bullet points) cleanly in Compose.
 */
@Composable
fun MarkdownPersianText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    headingColor: Color = MaterialTheme.colorScheme.primary,
    baseStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val lines = remember(text) { text.lines() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        lines.forEach { rawLine ->
            val trimmed = rawLine.trim()
            when {
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.padding(vertical = 1.dp))
                }
                trimmed.startsWith("###") || trimmed.startsWith("##") || trimmed.startsWith("#") -> {
                    val headingText = trimmed.trimStart('#').trim().replace("**", "")
                    Text(
                        text = headingText,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = headingColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                    val bulletContent = trimmed.drop(2).trim()
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "◆",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseBoldInline(bulletContent, textColor),
                            style = baseStyle,
                            color = textColor
                        )
                    }
                }
                else -> {
                    Text(
                        text = parseBoldInline(trimmed, textColor),
                        style = baseStyle,
                        color = textColor
                    )
                }
            }
        }
    }
}

private fun parseBoldInline(line: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var idx = 0
        while (idx < line.length) {
            val start = line.indexOf("**", startIndex = idx)
            if (start == -1) {
                append(line.substring(idx))
                break
            }
            if (start > idx) {
                append(line.substring(idx, start))
            }
            val end = line.indexOf("**", startIndex = start + 2)
            if (end == -1) {
                append(line.substring(start))
                break
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                append(line.substring(start + 2, end))
            }
            idx = end + 2
        }
    }
}
