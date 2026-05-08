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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.basicnotepadv2.NoteViewModel
import com.example.basicnotepadv2.data.Note
import com.example.basicnotepadv2.data.NoteType
import com.example.basicnotepadv2.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NoteEditorScreen(
    noteId: Long,
    viewModel: NoteViewModel,
    isDarkTheme: Boolean,
    onBack: () -> Unit
) {
    var note by remember { mutableStateOf<Note?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var hasChanges by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val bgColor = if (isDarkTheme) DarkBackground else LightBackground
    val topBarBg = if (isDarkTheme) DarkTopBar else LightTopBar
    val surfaceBg = if (isDarkTheme) DarkSurface else LightSurface
    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
    val textTertiary = if (isDarkTheme) DarkTextTertiary else LightTextTertiary
    val borderColor = if (isDarkTheme) DarkBorder else LightBorder

    // Load note
    LaunchedEffect(noteId) {
        val loaded = viewModel.getNoteById(noteId)
        if (loaded != null) {
            note = loaded
            // Treat the auto-generated default title as empty so the placeholder hint shows
            title = if (loaded.title == "Untitled Note") "" else loaded.title
            content = loaded.content
        }
    }

    // Auto-save with debounce
    LaunchedEffect(title, content) {
        if (hasChanges && note != null) {
            delay(800)
            note?.let { currentNote ->
                val updatedNote = currentNote.copy(
                    title = title.ifEmpty { "Untitled Note" },
                    content = content,
                    updatedAt = System.currentTimeMillis()
                )
                viewModel.saveNote(updatedNote)
            }
        }
    }

    // Auto-scroll to bottom when content grows (keeps cursor visible)
    LaunchedEffect(content.length) {
        coroutineScope.launch {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    BackHandler {
        // Save on back
        note?.let { currentNote ->
            val updatedNote = currentNote.copy(
                title = title.ifEmpty { "Untitled Note" },
                content = content,
                updatedAt = System.currentTimeMillis()
            )
            viewModel.saveNote(updatedNote)
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
                        val noteToShare = currentNote.copy(title = title.ifEmpty { "Untitled Note" }, content = content)
                        shareNote(context, noteToShare)
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

        // Content field
        BasicTextField(
            value = content,
            onValueChange = {
                content = it
                hasChanges = true
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = textPrimary),
            cursorBrush = SolidColor(PrimaryPurple),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
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
