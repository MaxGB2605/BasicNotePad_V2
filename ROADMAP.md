# BasicNotePadV2 — Feature Roadmap

> Last updated: 2026-02-21
> Current version: 1.0 (Figma design implemented ✅)

---

## ✅ Already Done (v1.0)

- [x] Dark / Light theme toggle (persisted to DataStore)
- [x] Notes and Checklists with Room database storage
- [x] Filter by type: All / Notes / Checklists
- [x] 3 view modes: List, Grid, Staggered
- [x] Sort: Newest, Oldest, Title A-Z, Title Z-A
- [x] Search notes (live filter)
- [x] Auto-save with debounce (800ms)
- [x] Share note via system share sheet
- [x] Title field shows placeholder hint on new note
- [x] Checked checklist items auto-sink to bottom of list

---

## 🚀 Phase 1 — Quick Wins

> Small effort, big UX improvement. Priority: HIGH

- [ ] **Swipe to delete** — swipe note card left/right to delete on home screen
- [ ] **Undo delete (Snackbar)** — "Undo" snackbar appears for 4s after a note is deleted
- [ ] **"Clear completed" button** — remove all ✅ checked items in the checklist editor at once
- [ ] **Haptic feedback** — short vibration when checking/unchecking a checklist item
- [ ] **Word / character count** — live count displayed at the bottom of the note editor

---

## 🎨 Phase 2 — Visual Polish

> Makes the app feel more premium and alive. Priority: MEDIUM-HIGH

- [ ] **Note color tags** — let users pick a background accent color for individual notes (like Google Keep)
- [ ] **Pin notes to top** — ⭐ pin important notes so they always appear first regardless of sort order
- [ ] **Entrance animations** — note cards fade/slide in when the home list loads
- [ ] **Smooth navigation transitions** — animated enter/exit when opening/closing a note
- [ ] **Splash screen** — branded launch screen with the purple gradient logo

---

## ✏️ Phase 3 — Editor Improvements

> Deeper editing capabilities. Priority: MEDIUM

- [ ] **Drag & drop reorder** — long-press a checklist item to drag it to a new position
- [ ] **Text formatting toolbar** — Bold, Italic, Underline buttons above keyboard in note editor
- [ ] **Font size setting** — user-selectable text size (Small / Medium / Large)

---

## 🗂️ Phase 4 — Organization

> For power users with many notes. Priority: MEDIUM

- [ ] **Reminders / notifications** — set an alarm on a note, get a push notification at that time
- [ ] **Archive** — archive notes instead of permanently deleting them, with a separate archive view
- [ ] **Export to file** — export a note as a `.txt` or `.pdf` file via the share sheet

---

## 🔒 Phase 5 — Advanced / Security

> Nice-to-have, lower priority. Priority: LOW

- [ ] **Lock a note** — protect individual notes with fingerprint / device PIN
- [ ] **Home screen widget** — glanceable Glance widget showing a pinned note or active checklist
- [ ] **Backup & restore** — export / import the entire note database as a JSON file

---

## Implementation Notes

### Tech stack

- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Database: Room
- Navigation: Navigation Compose
- State: ViewModel + StateFlow
- Persistence: DataStore Preferences
- Min SDK: 28

### Key files

| File                          | Purpose                           |
| ----------------------------- | --------------------------------- |
| `MainActivity.kt`             | Navigation host                   |
| `NoteViewModel.kt`            | All app state + DataStore theme   |
| `data/Note.kt`                | Room entity + ChecklistItem model |
| `data/NoteDao.kt`             | Database queries                  |
| `ui/HomeScreen.kt`            | List / Grid / Staggered + dialogs |
| `ui/NoteEditorScreen.kt`      | Text note editor                  |
| `ui/ChecklistEditorScreen.kt` | Checklist editor                  |
| `ui/theme/Color.kt`           | Full dark + light color palette   |
