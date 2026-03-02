package com.example.FlexyNotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.FlexyNotes.ui.screens.ArchiveScreen
import com.example.FlexyNotes.ui.screens.NoteEditorScreen
import com.example.FlexyNotes.ui.screens.NotesListScreen
import com.example.FlexyNotes.ui.screens.SettingsScreen
import com.example.FlexyNotes.ui.screens.TrashScreen
import com.example.FlexyNotes.ui.theme.FlexyNotesreworkedTheme
import com.example.FlexyNotes.viewmodel.NotesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Transient state for UI testing, persistence comes next
            var isOledMode by remember { mutableStateOf(true) }

            FlexyNotesreworkedTheme(isOledMode = isOledMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FlexyNotesNavigation(
                        isOledMode = isOledMode,
                        onOledModeChange = { isOledMode = it }
                    )
                }
            }
        }
    }
}

@Composable
fun FlexyNotesNavigation(
    isOledMode: Boolean,
    onOledModeChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "notes_list"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "FlexyNotes",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp)
                )
                Spacer(Modifier.height(16.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    label = { Text("Notizen") },
                    selected = currentRoute == "notes_list",
                    onClick = {
                        navController.navigate("notes_list") {
                            popUpTo("notes_list") { inclusive = true }
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                    label = { Text("Archiv") },
                    selected = currentRoute == "archive",
                    onClick = {
                        navController.navigate("archive")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    label = { Text("Papierkorb") },
                    selected = currentRoute == "trash",
                    onClick = {
                        navController.navigate("trash")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.weight(1f))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Einstellungen") },
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(Modifier.height(16.dp))
            }
        },
        modifier = Modifier.systemBarsPadding()
    ) {
        NavHost(
            navController = navController,
            startDestination = "notes_list"
        ) {
            composable("notes_list") {
                val viewModel: NotesViewModel = hiltViewModel()
                NotesListScreen(
                    viewModel = viewModel,
                    onNavigateToEditor = { noteId ->
                        navController.navigate("note_editor/${noteId ?: -1L}")
                    },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }

            composable("archive") {
                val viewModel: NotesViewModel = hiltViewModel()
                ArchiveScreen(
                    viewModel = viewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigateToEditor = { noteId ->
                        navController.navigate("note_editor/${noteId}")
                    }
                )
            }

            composable("trash") {
                val viewModel: NotesViewModel = hiltViewModel()
                TrashScreen(
                    viewModel = viewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }

            composable("settings") {
                SettingsScreen(
                    isOledMode = isOledMode,
                    onOledModeChange = onOledModeChange,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }

            composable("note_editor/{noteId}") { backStackEntry ->
                val viewModel: NotesViewModel = hiltViewModel()
                val noteIdStr = backStackEntry.arguments?.getString("noteId")
                val noteId = noteIdStr?.toLongOrNull()?.takeIf { it != -1L }

                NoteEditorScreen(
                    viewModel = viewModel,
                    noteId = noteId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}