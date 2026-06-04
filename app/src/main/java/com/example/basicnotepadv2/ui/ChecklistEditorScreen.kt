package com.example.basicnotepadv2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.basicnotepadv2.NoteViewModel
import com.example.basicnotepadv2.data.ChecklistItem
import com.example.basicnotepadv2.data.Note
import com.example.basicnotepadv2.data.NoteType
import com.example.basicnotepadv2.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChecklistEditorScreen(
    noteId: Long,
    viewModel: NoteViewModel,
    isDarkTheme: Boolean,
    onBack: () -> Unit
) {
    var note by remember { mutableStateOf<Note?>(null) }
    var title by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf<ChecklistItem>()) }
    var newItemText by remember { mutableStateOf("") }
    var hasChanges by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val bgColor = if (isDarkTheme) DarkBackground else LightBackground
    val topBarBg = if (isDarkTheme) DarkTopBar else LightTopBar
    val surfaceBg = if (isDarkTheme) DarkSurface else LightSurface
    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
    val textTertiary = if (isDarkTheme) DarkTextTertiary else LightTextTertiary
    val borderColor = if (isDarkTheme) DarkBorder else LightBorder
    val inputBg = if (isDarkTheme) DarkSurfaceVariant else LightSurfaceVariant

    // Load note
    LaunchedEffect(noteId) {
        val loaded = viewModel.getNoteById(noteId)
        if (loaded != null) {
            note = loaded
            // Treat the auto-generated default title as empty so the placeholder hint shows
            title = if (loaded.title == "Untitled Checklist") "" else loaded.title
            items = loaded.checklistItems
        }
    }



    // Auto-save with debounce
    LaunchedEffect(title, items) {
        if (hasChanges && note != null) {
            delay(800)
            note?.let { currentNote ->
                viewModel.saveNote(
                    currentNote.copy(
                        title = title.ifEmpty { "Untitled Checklist" },
                        checklistItems = items,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    BackHandler {
        note?.let { currentNote ->
            viewModel.saveNote(
                currentNote.copy(
                    title = title.ifEmpty { "Untitled Checklist" },
                    checklistItems = items,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        onBack()
    }

    val completedCount = items.count { it.isChecked }
    val totalCount = items.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val progressPct = (progress * 100).toInt()

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
                                title = title.ifEmpty { "Untitled Checklist" },
                                checklistItems = items,
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
                        val noteToShare = currentNote.copy(
                            title = title.ifEmpty { "Untitled Checklist" },
                            checklistItems = items
                        )
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
                            text = "Untitled Checklist",
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

        // Progress section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceBg)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$completedCount of $totalCount completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // "Clear done" — only visible when any items are checked
                        if (completedCount > 0) {
                            Text(
                                text = "Clear done",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryPurple,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        items = items.filter { !it.isChecked }
                                        hasChanges = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "$progressPct%",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ProgressBarFill,
                    trackColor = if (isDarkTheme) ProgressBarTrackDark else ProgressBarTrackLight
                )
            }
        }

        HorizontalDivider(color = borderColor, thickness = 0.5.dp)

        // Add task input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = textPrimary),
                cursorBrush = SolidColor(PrimaryPurple),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                decorationBox = { innerField ->
                    if (newItemText.isEmpty()) {
                        Text(
                            text = "Add a task...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textTertiary
                        )
                    }
                    innerField()
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryPurple)
                    .clickable {
                        if (newItemText.isNotBlank()) {
                            val newItem = ChecklistItem(
                                id = System.currentTimeMillis(),
                                text = newItemText.trim(),
                                isChecked = false
                            )
                            // Insert at the top so the newest item is always first
                            items = items.toMutableList().also { it.add(0, newItem) }
                            newItemText = ""
                            hasChanges = true
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add task",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        HorizontalDivider(color = borderColor, thickness = 0.5.dp)

        // Checklist items
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tasks yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textTertiary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ChecklistItemRow(
                        item = item,
                        isDarkTheme = isDarkTheme,
                        onToggle = {
                            items = items
                                .map { if (it.id == item.id) it.copy(isChecked = !it.isChecked) else it }
                                .sortedBy { it.isChecked } // unchecked first, checked sink to bottom
                            hasChanges = true
                        },
                        onTextChange = { newText ->
                            items = items.map {
                                if (it.id == item.id) it.copy(text = newText) else it
                            }
                            hasChanges = true
                        },
                        onDelete = {
                            items = items.filter { it.id != item.id }
                            hasChanges = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChecklistItemRow(
    item: ChecklistItem,
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
    val borderColor = if (isDarkTheme) DarkBorder else LightBorder
    val haptic = LocalHapticFeedback.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isChecked) PrimaryPurple
                        else Color.Transparent
                    )
                    .run {
                        if (!item.isChecked) {
                            this.then(
                                Modifier.background(Color.Transparent)
                            )
                        } else this
                    }
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (item.isChecked) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(22.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isDarkTheme) DarkBorder else LightTextSecondary
                            )
                        ) {}
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text
            BasicTextField(
                value = item.text,
                onValueChange = onTextChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = if (item.isChecked) textSecondary else textPrimary,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                ),
                cursorBrush = SolidColor(PrimaryPurple),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        HorizontalDivider(
            color = borderColor.copy(alpha = 0.5f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(start = 52.dp)
        )
    }
}
