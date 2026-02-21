package com.example.basicnotepadv2.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.basicnotepadv2.FilterType
import com.example.basicnotepadv2.NoteViewModel
import com.example.basicnotepadv2.SortOrder
import com.example.basicnotepadv2.ViewMode
import com.example.basicnotepadv2.data.Note
import com.example.basicnotepadv2.data.NoteType
import com.example.basicnotepadv2.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Builds a plain-text representation of a note and fires the system share sheet.
 */
fun shareNote(context: Context, note: Note) {
    val shareText = buildString {
        append(note.title)
        append("\n\n")
        if (note.type == NoteType.NOTE) {
            append(note.content.ifEmpty { "(no content)" })
        } else {
            val total = note.checklistItems.size
            val done = note.checklistItems.count { it.isChecked }
            append("$done of $total completed\n\n")
            note.checklistItems.forEach { item ->
                val mark = if (item.isChecked) "\u2611" else "\u2610" // ☑ / ☐
                append("$mark ${item.text}\n")
            }
        }
    }.trim()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, note.title)
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NoteViewModel,
    onNoteClick: (Long) -> Unit,
    onCreateNote: (NoteType) -> Unit,
    isDarkTheme: Boolean
) {
    val notes by viewModel.notes.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val bgColor = if (isDarkTheme) DarkBackground else LightBackground
    val topBarBg = if (isDarkTheme) DarkTopBar else LightTopBar
    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
    val borderColor = if (isDarkTheme) DarkBorder else LightBorder
    val surfaceColor = if (isDarkTheme) DarkSurface else LightSurface

    Scaffold(
        containerColor = bgColor,
        floatingActionButton = {
            GradientFab(onClick = { showCreateDialog = true })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(bgColor)
        ) {
            // Top App Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topBarBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // App Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(GradientStart, GradientEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Notepad",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Outlined.WbSunny else Icons.Outlined.DarkMode,
                            contentDescription = "Toggle theme",
                            tint = textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Search bar (animated)
            AnimatedVisibility(visible = isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(topBarBg)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDarkTheme) DarkSurfaceVariant else LightSurfaceVariant)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = textPrimary),
                                cursorBrush = SolidColor(PrimaryPurple),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search notes...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = textSecondary
                                        )
                                    }
                                    innerField()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.setSearchQuery("") },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = textSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Filter / View Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topBarBg)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter chips
                FilterChipRow(
                    currentFilter = filterType,
                    onFilterChange = { viewModel.setFilter(it) },
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier.weight(1f)
                )

                // View mode toggles
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    ViewModeButton(
                        icon = Icons.Default.ViewStream,
                        isSelected = viewMode == ViewMode.LIST,
                        onClick = { viewModel.setViewMode(ViewMode.LIST) },
                        isDarkTheme = isDarkTheme
                    )
                    ViewModeButton(
                        icon = Icons.Default.GridView,
                        isSelected = viewMode == ViewMode.GRID,
                        onClick = { viewModel.setViewMode(ViewMode.GRID) },
                        isDarkTheme = isDarkTheme
                    )
                    ViewModeButton(
                        icon = Icons.Default.Dashboard,
                        isSelected = viewMode == ViewMode.STAGGERED,
                        onClick = { viewModel.setViewMode(ViewMode.STAGGERED) },
                        isDarkTheme = isDarkTheme
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Sort button
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Sort",
                                tint = if (isDarkTheme) DarkTextSecondary else LightTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(if (isDarkTheme) DarkSurface else LightSurface)
                        ) {
                            SortOrder.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = when (sort) {
                                                SortOrder.DATE_DESC -> "Newest First"
                                                SortOrder.DATE_ASC -> "Oldest First"
                                                SortOrder.TITLE_ASC -> "Title A-Z"
                                                SortOrder.TITLE_DESC -> "Title Z-A"
                                            },
                                            color = if (sortOrder == sort) PrimaryPurple else (if (isDarkTheme) DarkTextPrimary else LightTextPrimary)
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSortOrder(sort)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Search button
                    IconButton(
                        onClick = { viewModel.toggleSearch() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearching) PrimaryPurple else (if (isDarkTheme) DarkTextSecondary else LightTextSecondary),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Divider
            HorizontalDivider(color = borderColor, thickness = 0.5.dp)

            // Content
            if (notes.isEmpty()) {
                EmptyState(isDarkTheme = isDarkTheme)
            } else {
                when (viewMode) {
                    ViewMode.LIST -> NoteListView(
                        notes = notes,
                        onNoteClick = onNoteClick,
                        onDeleteNote = { viewModel.deleteNote(it) },
                        isDarkTheme = isDarkTheme
                    )
                    ViewMode.GRID -> NoteGridView(
                        notes = notes,
                        onNoteClick = onNoteClick,
                        onDeleteNote = { viewModel.deleteNote(it) },
                        isDarkTheme = isDarkTheme
                    )
                    ViewMode.STAGGERED -> NoteStaggeredView(
                        notes = notes,
                        onNoteClick = onNoteClick,
                        onDeleteNote = { viewModel.deleteNote(it) },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }

    // Create Note Dialog
    if (showCreateDialog) {
        CreateNoteDialog(
            isDarkTheme = isDarkTheme,
            onDismiss = { showCreateDialog = false },
            onCreateNote = { type ->
                showCreateDialog = false
                onCreateNote(type)
            }
        )
    }
}

@Composable
fun FilterChipRow(
    currentFilter: FilterType,
    onFilterChange: (FilterType) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterType.entries.forEach { filter ->
            val isSelected = currentFilter == filter
            val label = when (filter) {
                FilterType.ALL -> "All"
                FilterType.NOTES -> "Notes"
                FilterType.CHECKLISTS -> "Checklists"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) {
                            if (isDarkTheme) ChipActiveDark else ChipActiveLightBg
                        } else Color.Transparent
                    )
                    .clickable { onFilterChange(filter) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) {
                        if (isDarkTheme) PrimaryPurple else ChipActiveLightText
                    } else {
                        if (isDarkTheme) DarkTextSecondary else LightTextSecondary
                    }
                )
            }
        }
    }
}

@Composable
fun ViewModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) PrimaryPurple else (if (isDarkTheme) DarkTextSecondary else LightTextSecondary),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun NoteListView(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    onDeleteNote: (Note) -> Unit,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                onClick = { onNoteClick(note.id) },
                onDelete = { onDeleteNote(note) },
                onShare = { shareNote(context, note) },
                isDarkTheme = isDarkTheme
            )
        }
    }
}

@Composable
fun NoteGridView(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    onDeleteNote: (Note) -> Unit,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                onClick = { onNoteClick(note.id) },
                onDelete = { onDeleteNote(note) },
                onShare = { shareNote(context, note) },
                isDarkTheme = isDarkTheme
            )
        }
    }
}

@Composable
fun NoteStaggeredView(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    onDeleteNote: (Note) -> Unit,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        items(notes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                onClick = { onNoteClick(note.id) },
                onDelete = { onDeleteNote(note) },
                onShare = { shareNote(context, note) },
                isDarkTheme = isDarkTheme
            )
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    isDarkTheme: Boolean
) {
    val cardBg = if (isDarkTheme) DarkSurface else LightSurface
    val borderColor = if (isDarkTheme) DarkBorder else LightBorder
    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
    val textTertiary = if (isDarkTheme) DarkTextTertiary else LightTextTertiary
    val actionIconColor = if (isDarkTheme) ActionIconDark else ActionIconLight

    val isNote = note.type == NoteType.NOTE
    val iconBg = if (isDarkTheme) {
        if (isNote) NoteIconBgDark else ChecklistIconBgDark
    } else {
        if (isNote) NoteIconBgLight else ChecklistIconBgLight
    }
    val iconColor = if (isDarkTheme) {
        if (isNote) NoteIconColorDark else ChecklistIconColorDark
    } else {
        if (isNote) NoteIconColorLight else ChecklistIconColorLight
    }

    val dateFormat = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(note.updatedAt))

    val completedCount = if (note.type == NoteType.CHECKLIST) note.checklistItems.count { it.isChecked } else 0
    val totalCount = note.checklistItems.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkTheme) 0.dp else 2.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Type icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isNote) Icons.Default.Article else Icons.Default.GridOn,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Action buttons
                    Row {
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share",
                                tint = actionIconColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = actionIconColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isNote) {
                    // Note content preview
                    Text(
                        text = note.content.ifEmpty { "No content" },
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    // Checklist status
                    Text(
                        text = "$completedCount of $totalCount completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Progress bar
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

                Spacer(modifier = Modifier.height(8.dp))

                // Date
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = textTertiary
                )
            }
        }
    }
}

@Composable
fun EmptyState(isDarkTheme: Boolean) {
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
    val textTertiary = if (isDarkTheme) DarkTextTertiary else LightTextTertiary
    val iconBg = if (isDarkTheme) DarkSurfaceVariant else LightSurfaceVariant

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = null,
                tint = textTertiary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No items yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = textSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tap the + button to create your first item",
            style = MaterialTheme.typography.bodySmall,
            color = textTertiary
        )
    }
}

@Composable
fun GradientFab(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fabScale"
    )

    Box(
        modifier = Modifier
            .size(60.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Create new",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun CreateNoteDialog(
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onCreateNote: (NoteType) -> Unit
) {
    val dialogBg = if (isDarkTheme) DialogBgDark else DialogBgLight
    val optionBg = if (isDarkTheme) DialogOptionBgDark else DialogOptionBgLight
    val borderColor = if (isDarkTheme) DialogOptionBorderDark else DialogOptionBorderLight
    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Create New",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Choose what you want to create",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Note Option
                DialogOption(
                    icon = Icons.Default.Article,
                    iconBg = if (isDarkTheme) NoteIconBgDark else NoteIconBgLight,
                    iconColor = if (isDarkTheme) NoteIconColorDark else NoteIconColorLight,
                    title = "Note",
                    subtitle = "Create a new text note",
                    bgColor = optionBg,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { onCreateNote(NoteType.NOTE) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Checklist Option
                DialogOption(
                    icon = Icons.Default.CheckBox,
                    iconBg = if (isDarkTheme) ChecklistIconBgDark else ChecklistIconBgLight,
                    iconColor = if (isDarkTheme) ChecklistIconColorDark else ChecklistIconColorLight,
                    title = "Checklist",
                    subtitle = "Create a new checklist",
                    bgColor = optionBg,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { onCreateNote(NoteType.CHECKLIST) }
                )
            }
        }
    }
}

@Composable
fun DialogOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconColor: Color,
    title: String,
    subtitle: String,
    bgColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )
            }
        }
    }
}
