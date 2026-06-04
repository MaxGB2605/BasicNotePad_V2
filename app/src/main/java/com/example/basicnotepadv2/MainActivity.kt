package com.example.basicnotepadv2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.basicnotepadv2.data.NoteType
import com.example.basicnotepadv2.ui.ChecklistEditorScreen
import com.example.basicnotepadv2.ui.HomeScreen
import com.example.basicnotepadv2.ui.NoteEditorScreen
import com.example.basicnotepadv2.ui.theme.BasicNotePadV2Theme
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object NoteEditor : Screen("note_editor/{noteId}") {
        fun createRoute(noteId: Long) = "note_editor/$noteId"
    }
    object ChecklistEditor : Screen("checklist_editor/{noteId}") {
        fun createRoute(noteId: Long) = "checklist_editor/$noteId"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extract shared text if launched via an ACTION_SEND intent
        val sharedText: String? = if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else null

        val sharedSubject: String? = if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_SUBJECT)
        } else null

        setContent {
            val noteViewModel: NoteViewModel = viewModel()
            val isDarkTheme by noteViewModel.isDarkTheme.collectAsState()

            BasicNotePadV2Theme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        noteViewModel = noteViewModel,
                        isDarkTheme = isDarkTheme,
                        sharedText = sharedText,
                        sharedSubject = sharedSubject
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    noteViewModel: NoteViewModel,
    isDarkTheme: Boolean,
    sharedText: String? = null,
    sharedSubject: String? = null
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    // If the app was opened via a share intent, create a pre-filled note and
    // navigate to it once the NavHost has been composed.
    LaunchedEffect(Unit) {
        if (sharedText != null) {
            val note = com.example.basicnotepadv2.data.Note(
                title = sharedSubject?.takeIf { it.isNotBlank() } ?: "Shared Note",
                content = sharedText,
                type = NoteType.NOTE,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val noteId = noteViewModel.insertSharedNote(note)
            navController.navigate(Screen.NoteEditor.createRoute(noteId)) {
                // Remove the Home destination from the back stack so pressing
                // back exits the app rather than landing on an empty home screen.
                popUpTo(Screen.Home.route) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = noteViewModel,
                isDarkTheme = isDarkTheme,
                onNoteClick = { noteId ->
                    // Determine note type and navigate accordingly
                    coroutineScope.launch {
                        val note = noteViewModel.getNoteById(noteId)
                        if (note?.type == NoteType.CHECKLIST) {
                            navController.navigate(Screen.ChecklistEditor.createRoute(noteId))
                        } else {
                            navController.navigate(Screen.NoteEditor.createRoute(noteId))
                        }
                    }
                },
                onCreateNote = { type ->
                    coroutineScope.launch {
                        val noteId = noteViewModel.insertAndGetNote(type)
                        if (type == NoteType.CHECKLIST) {
                            navController.navigate(Screen.ChecklistEditor.createRoute(noteId))
                        } else {
                            navController.navigate(Screen.NoteEditor.createRoute(noteId))
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.NoteEditor.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
            NoteEditorScreen(
                noteId = noteId,
                viewModel = noteViewModel,
                isDarkTheme = isDarkTheme,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ChecklistEditor.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
            ChecklistEditorScreen(
                noteId = noteId,
                viewModel = noteViewModel,
                isDarkTheme = isDarkTheme,
                onBack = { navController.popBackStack() }
            )
        }
    }
}