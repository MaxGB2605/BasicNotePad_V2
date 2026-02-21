package com.example.basicnotepadv2

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basicnotepadv2.data.ChecklistItem
import com.example.basicnotepadv2.data.Note
import com.example.basicnotepadv2.data.NoteDatabase
import com.example.basicnotepadv2.data.NoteRepository
import com.example.basicnotepadv2.data.NoteType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")

enum class SortOrder { DATE_DESC, DATE_ASC, TITLE_ASC, TITLE_DESC }
enum class ViewMode { LIST, GRID, STAGGERED }
enum class FilterType { ALL, NOTES, CHECKLISTS }

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    private val dataStore = application.dataStore

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    val notes: StateFlow<List<Note>>

    init {
        val db = NoteDatabase.getDatabase(application)
        repository = NoteRepository(db.noteDao())

        // Load theme preference
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            _isDarkTheme.value = prefs[DARK_THEME_KEY] ?: true
        }

        notes = combine(
            filterType.flatMapLatest { filter ->
                when (filter) {
                    FilterType.ALL -> repository.getAllNotes()
                    FilterType.NOTES -> repository.getNotesByType(NoteType.NOTE)
                    FilterType.CHECKLISTS -> repository.getNotesByType(NoteType.CHECKLIST)
                }
            },
            sortOrder,
            searchQuery
        ) { noteList, sort, query ->
            var filtered = noteList
            if (query.isNotEmpty()) {
                filtered = filtered.filter { note ->
                    note.title.contains(query, ignoreCase = true) ||
                            note.content.contains(query, ignoreCase = true) ||
                            note.checklistItems.any { it.text.contains(query, ignoreCase = true) }
                }
            }
            when (sort) {
                SortOrder.DATE_DESC -> filtered.sortedByDescending { it.updatedAt }
                SortOrder.DATE_ASC -> filtered.sortedBy { it.updatedAt }
                SortOrder.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
                SortOrder.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun setFilter(filter: FilterType) { _filterType.value = filter }
    fun setSortOrder(sort: SortOrder) { _sortOrder.value = sort }
    fun setViewMode(mode: ViewMode) { _viewMode.value = mode }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun toggleSearch() { _isSearching.value = !_isSearching.value; if (!_isSearching.value) _searchQuery.value = "" }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[DARK_THEME_KEY] = _isDarkTheme.value
            }
        }
    }

    fun createNote(title: String = "", content: String = ""): Long {
        var newId = 0L
        viewModelScope.launch {
            val note = Note(
                title = title.ifEmpty { "Untitled Note" },
                content = content,
                type = NoteType.NOTE,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            newId = repository.insertNote(note)
        }
        return newId
    }

    fun createChecklist(title: String = ""): Long {
        var newId = 0L
        viewModelScope.launch {
            val note = Note(
                title = title.ifEmpty { "Untitled Checklist" },
                type = NoteType.CHECKLIST,
                checklistItems = emptyList(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            newId = repository.insertNote(note)
        }
        return newId
    }

    suspend fun insertAndGetNote(type: NoteType): Long {
        val note = Note(
            title = if (type == NoteType.NOTE) "Untitled Note" else "Untitled Checklist",
            type = type,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return repository.insertNote(note)
    }

    suspend fun getNoteById(id: Long): Note? = repository.getNoteById(id)

    fun saveNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { repository.deleteNote(note) }
    }

    fun deleteNoteById(id: Long) {
        viewModelScope.launch { repository.deleteNoteById(id) }
    }
}
