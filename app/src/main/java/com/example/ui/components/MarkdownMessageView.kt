package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightViolet
import com.example.ui.theme.CodeComment
import com.example.ui.theme.CodeKeyword
import com.example.ui.theme.CodeNumber
import com.example.ui.theme.CodeString
import com.example.ui.theme.CodeSurface
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private sealed interface MarkdownBlock {
    data class TextBlock(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
}

private val CODE_KEYWORDS = setOf(
    "package", "import", "class", "interface", "object", "data", "sealed", "enum",
    "fun", "val", "var", "private", "public", "internal", "override", "suspend",
    "return", "if", "else", "when", "for", "while", "try", "catch", "finally",
    "true", "false", "null", "this", "super", "companion", "const", "lateinit",
    "Composable", "remember", "mutableStateOf", "Scaffold", "Column", "Row", "Box",
    "name", "on", "jobs", "steps", "uses", "with", "run", "env", "permissions"
)

@Composable
fun MarkdownMessageView(
    markdown: String,
    modifier: Modifier = Modifier,
    onSendCodeToStudio: ((code: String, language: String) -> Unit)? = null,
    onCopiedNotice: ((String) -> Unit)? = null
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.TextBlock -> {
                    val lines = block.text.lines()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        lines.forEach { rawLine ->
                            val line = rawLine.trimEnd()
                            when {
                                line.startsWith("### ") -> {
                                    Text(
                                        text = formatInlineMarkdown(line.removePrefix("### ")),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = BrightViolet
                                    )
                                }
                                line.startsWith("## ") -> {
                                    Text(
                                        text = formatInlineMarkdown(line.removePrefix("## ")),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = BrightViolet
                                    )
                                }
                                line.startsWith("# ") -> {
                                    Text(
                                        text = formatInlineMarkdown(line.removePrefix("# ")),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                }
                                line.startsWith("- ") || line.startsWith("* ") -> {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text(
                                            text = "▸ ",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = NeonCyan
                                        )
                                        Text(
                                            text = formatInlineMarkdown(line.drop(2)),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary
                                        )
                                    }
                                }
                                line.isNotBlank() -> {
                                    Text(
                                        text = formatInlineMarkdown(line),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                                else -> Spacer(modifier = Modifier.height(2.dp))
                            }
                        }
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CodeSurface,
                        border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ObsidianElevated.copy(alpha = 0.7f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Code,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = block.language.ifBlank { "code" }.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (onSendCodeToStudio != null) {
                                        TextButton(
                                            onClick = { onSendCodeToStudio(block.code, block.language.ifBlank { "kotlin" }) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.CloudUpload,
                                                contentDescription = "Send code to GitHub Studio",
                                                tint = BrightViolet,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Studio",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = BrightViolet
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(block.code))
                                            onCopiedNotice?.invoke("Code copied")
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = "Copy code",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Copy",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                            Text(
                                text = highlightCode(block.code),
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseMarkdownBlocks(raw: String): List<MarkdownBlock> {
    val result = mutableListOf<MarkdownBlock>()
    val regex = Regex("```([a-zA-Z0-9_-]*)\\n([\\s\\S]*?)```")
    var lastIndex = 0
    for (match in regex.findAll(raw)) {
        if (match.range.first > lastIndex) {
            val before = raw.substring(lastIndex, match.range.first).trim()
            if (before.isNotEmpty()) result.add(MarkdownBlock.TextBlock(before))
        }
        val lang = match.groupValues[1].trim()
        val code = match.groupValues[2].trimEnd()
        result.add(MarkdownBlock.CodeBlock(lang, code))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < raw.length) {
        val tail = raw.substring(lastIndex).trim()
        if (tail.isNotEmpty()) result.add(MarkdownBlock.TextBlock(tail))
    }
    if (result.isEmpty() && raw.isNotBlank()) {
        result.add(MarkdownBlock.TextBlock(raw))
    }
    return result
}

private fun formatInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = JetBrainsMonoFamily,
                            color = NeonCyan,
                            background = ObsidianElevated
                        )
                    ) {
                        append(" ${text.substring(i + 1, end)} ")
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}

private fun highlightCode(code: String): AnnotatedString = buildAnnotatedString {
    val lines = code.lines()
    lines.forEachIndexed { idx, line ->
        val trimmed = line.trimStart()
        if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
            withStyle(SpanStyle(color = CodeComment)) { append(line) }
        } else {
            val tokenRegex = Regex("(\"[^\"]*\")|(\\b[A-Za-z_][A-Za-z0-9_]*\\b)|(\\b\\d+(\\.\\d+)?\\b)")
            var cursor = 0
            for (m in tokenRegex.findAll(line)) {
                if (m.range.first > cursor) {
                    withStyle(SpanStyle(color = TextPrimary)) {
                        append(line.substring(cursor, m.range.first))
                    }
                }
                val token = m.value
                val color = when {
                    token.startsWith("\"") -> CodeString
                    token[0].isDigit() -> CodeNumber
                    token in CODE_KEYWORDS -> CodeKeyword
                    else -> TextPrimary
                }
                withStyle(SpanStyle(color = color)) { append(token) }
                cursor = m.range.last + 1
            }
            if (cursor < line.length) {
                withStyle(SpanStyle(color = TextPrimary)) { append(line.substring(cursor)) }
            }
        }
        if (idx < lines.lastIndex) append("\n")
    }
}
