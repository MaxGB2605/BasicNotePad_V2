package com.example.basicnotepadv2

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
        setContent {
            val noteViewModel: NoteViewModel = viewModel()
            val isDarkTheme by noteViewModel.isDarkTheme.collectAsState()

            BasicNotePadV2Theme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(noteViewModel = noteViewModel, isDarkTheme = isDarkTheme)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(noteViewModel: NoteViewModel, isDarkTheme: Boolean) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

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