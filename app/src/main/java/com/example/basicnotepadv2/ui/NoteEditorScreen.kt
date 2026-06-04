package com.example.basicnotepadv2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.basicnotepadv2.NoteViewModel
import com.example.basicnotepadv2.data.Note
import com.example.basicnotepadv2.data.NoteType
import com.example.basicnotepadv2.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun NoteEditorScreen(
    noteId: Long,
    viewModel: NoteViewModel,
    isDarkTheme: Boolean,
    onBack: () -> Unit
) {
    var note by remember { mutableStateOf<Note?>(null) }
    var title by remember { mutableStateOf("") }
    // TextFieldValue gives us cursor selection offset, needed for cursor-tracking scroll
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }
    var hasChanges by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Captured from onTextLayout — gives us the cursor's pixel bounding rect
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    // Height of the visible scroll viewport, measured via onSizeChanged on the content Box
    var viewportHeight by remember { mutableStateOf(0) }

    val bgColor = if (isDarkTheme) DarkBackground else LightBackground
    val topBarBg = if (isDarkTheme) DarkTopBar else LightTopBar
    val surfaceBg = if (isDarkTheme) DarkSurface else LightSurface
    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
    val textTertiary = if (isDarkTheme) DarkTextTertiary else LightTextTertiary
    val borderColor = if (isDarkTheme) DarkBorder else LightBorder

    // Keyboard height — changes frame-by-frame as the keyboard animates in/out.
    // Used as a LaunchedEffect key so the cursor scroll re-fires when the keyboard
    // appears and shrinks the visible viewport.
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

    // Convenience alias — the raw string used for saving, word count, etc.
    val content = contentValue.text

    // Load note
    LaunchedEffect(noteId) {
        val loaded = viewModel.getNoteById(noteId)
        if (loaded != null) {
            note = loaded
            // Treat the auto-generated default title as empty so the placeholder hint shows
            title = if (loaded.title == "Untitled Note") "" else loaded.title
            contentValue = TextFieldValue(loaded.content)
        }
    }

    // Auto-save with debounce
    LaunchedEffect(title, content) {
        if (hasChanges && note != null) {
            delay(800)
            note?.let { currentNote ->
                viewModel.saveNote(
                    currentNote.copy(
                        title = title.ifEmpty { "Untitled Note" },
                        content = content,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Scroll to keep the cursor in view whenever the cursor moves OR the keyboard
    // animates in/out (imeBottom changes every frame during keyboard animation,
    // causing the effect to restart; only the last run — when the keyboard is fully
    // open and viewportHeight has settled — actually completes the scroll).
    LaunchedEffect(contentValue.selection, imeBottom) {
        val layout = textLayoutResult ?: return@LaunchedEffect
        if (viewportHeight == 0) return@LaunchedEffect

        try {
            val offset = contentValue.selection.start.coerceIn(0, content.length)
            val cursorRect = layout.getCursorRect(offset)
            val cursorTop    = cursorRect.top.toInt()
            val cursorBottom = cursorRect.bottom.toInt()
            val visibleTop    = scrollState.value
            val visibleBottom = scrollState.value + viewportHeight
            val padding = 48 // px buffer so the cursor isn't flush against the edge

            when {
                // Cursor is above the visible area — scroll up
                cursorTop < visibleTop + padding ->
                    scrollState.animateScrollTo((cursorTop - padding).coerceAtLeast(0))
                // Cursor is below the visible area — scroll down
                cursorBottom > visibleBottom - padding ->
                    scrollState.animateScrollTo(
                        (cursorBottom - viewportHeight + padding).coerceAtMost(scrollState.maxValue)
                    )
                // Cursor already visible — do nothing
            }
        } catch (_: Exception) {
            // Layout not ready yet; next recomposition will retry
        }
    }

    BackHandler {
        note?.let { currentNote ->
            viewModel.saveNote(
                currentNote.copy(
                    title = title.ifEmpty { "Untitled Note" },
                    content = content,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Single compact top bar: back | logo | title | share
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBarBg)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(
                onClick = {
                    note?.let { currentNote ->
                        viewModel.saveNote(
                            currentNote.copy(
                                title = title.ifEmpty { "Untitled Note" },
                                content = content,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    onBack()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            // App logo
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Brush.linearGradient(colors = listOf(GradientStart, GradientEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Notepad",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            // Share button
            IconButton(
                onClick = {
                    note?.let { currentNote ->
                        shareNote(context, currentNote.copy(title = title.ifEmpty { "Untitled Note" }, content = content))
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Share",
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(color = borderColor, thickness = 0.5.dp)

        // Title field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceBg)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = title,
                onValueChange = {
                    title = it
                    hasChanges = true
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = textPrimary,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = SolidColor(PrimaryPurple),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerField ->
                    if (title.isEmpty()) {
                        Text(
                            text = "Untitled Note",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = textTertiary
                        )
                    }
                    innerField()
                }
            )
        }

        HorizontalDivider(color = borderColor, thickness = 0.5.dp)

        // Content field — wrapped in a Box so we can measure the viewport height
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { viewportHeight = it.height }
        ) {
            BasicTextField(
                value = contentValue,
                onValueChange = { newValue ->
                    contentValue = newValue
                    hasChanges = true
                },
                onTextLayout = { textLayoutResult = it },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = textPrimary),
                cursorBrush = SolidColor(PrimaryPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                decorationBox = { innerField ->
                    Column {
                        if (content.isEmpty()) {
                            Text(
                                text = "Start writing...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textTertiary
                            )
                        }
                        innerField()
                    }
                }
            )
        }

        // Word / character count bar
        val wordCount = if (content.isBlank()) 0
            else content.trim().split(Regex("\\s+")).size
        val charCount = content.length
        HorizontalDivider(color = borderColor, thickness = 0.5.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceBg)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "$charCount characters · $wordCount words",
                style = MaterialTheme.typography.labelSmall,
                color = textTertiary
            )
        }
    }
}
