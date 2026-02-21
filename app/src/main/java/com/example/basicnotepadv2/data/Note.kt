package com.example.basicnotepadv2.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.json.JSONArray
import org.json.JSONObject

enum class NoteType {
    NOTE, CHECKLIST
}

@Entity(tableName = "notes")
@TypeConverters(ChecklistItemConverter::class)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val type: NoteType = NoteType.NOTE,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ChecklistItem(
    val id: Long = System.currentTimeMillis(),
    val text: String = "",
    val isChecked: Boolean = false
)

class ChecklistItemConverter {

    @TypeConverter
    fun fromChecklistItems(items: List<ChecklistItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("text", item.text)
            obj.put("isChecked", item.isChecked)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toChecklistItems(json: String): List<ChecklistItem> {
        return try {
            val array = JSONArray(json)
            val items = mutableListOf<ChecklistItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                items.add(
                    ChecklistItem(
                        id = obj.getLong("id"),
                        text = obj.getString("text"),
                        isChecked = obj.getBoolean("isChecked")
                    )
                )
            }
            items
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromNoteType(type: NoteType): String = type.name

    @TypeConverter
    fun toNoteType(value: String): NoteType = NoteType.valueOf(value)
}
