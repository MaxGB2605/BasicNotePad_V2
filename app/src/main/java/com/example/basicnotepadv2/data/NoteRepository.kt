package com.example.basicnotepadv2.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {

    fun getAllNotes(): Flow<List<Note>> = dao.getAllNotes()

    fun getNotesByType(type: NoteType): Flow<List<Note>> = dao.getNotesByType(type)

    fun searchNotes(query: String): Flow<List<Note>> = dao.searchNotes(query)

    suspend fun getNoteById(id: Long): Note? = dao.getNoteById(id)

    suspend fun insertNote(note: Note): Long = dao.insertNote(note)

    suspend fun updateNote(note: Note) = dao.updateNote(note)

    suspend fun deleteNote(note: Note) = dao.deleteNote(note)

    suspend fun deleteNoteById(id: Long) = dao.deleteNoteById(id)
}
