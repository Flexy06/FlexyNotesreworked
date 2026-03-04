package com.example.FlexyNotes

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.FlexyNotes.data.UserPreferences
import com.example.FlexyNotes.ui.screens.ArchiveScreen
import com.example.FlexyNotes.ui.screens.NoteEditorScreen
import com.example.FlexyNotes.ui.screens.NotesListScreen
import com.example.FlexyNotes.ui.screens.SettingsScreen
import com.example.FlexyNotes.ui.screens.TrashScreen
import com.example.FlexyNotes.ui.theme.FlexyNotesreworkedTheme
import com.example.FlexyNotes.viewmodel.MainViewModel
import com.example.FlexyNotes.viewmodel.NotesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() { // Using FragmentActivity avoids Theme.AppCompat crashes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val preferences by mainViewModel.preferences.collectAsState()

            var isUnlocked by rememberSaveable { mutableStateOf(false) }
            var showPromptTrigger by remember { mutableStateOf(true) }
            var isDataStoreLoaded by rememberSaveable { mutableStateOf(false) }

            val lifecycleOwner = LocalLifecycleOwner.current

            // Wait briefly on cold start for DataStore to load actual preferences
            LaunchedEffect(Unit) {
                if (!isDataStoreLoaded) {
                    delay(100)
                    isDataStoreLoaded = true
                }
            }

            // Lock the app automatically when sent to background
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        isUnlocked = false
                        showPromptTrigger = true // Ensure prompt shows when returning
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Secure Mode Logic
            LaunchedEffect(preferences.isSecureMode) {
                if (preferences.isSecureMode) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            // Trigger biometric prompt based on state
            LaunchedEffect(preferences.isAppLockEnabled, isUnlocked, showPromptTrigger, isDataStoreLoaded) {
                if (isDataStoreLoaded && preferences.isAppLockEnabled && !isUnlocked && showPromptTrigger) {
                    showPromptTrigger = false // Consume trigger to avoid infinite loops on cancel
                    showBiometricPrompt { success ->
                        isUnlocked = success
                    }
                }
            }

            FlexyNotesreworkedTheme(
                themeMode = preferences.themeMode,
                dynamicColor = preferences.useDynamicColor,
                isOledMode = preferences.isOledMode
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isDataStoreLoaded) {
                        // Empty screen while waiting for initial preferences
                        Box(Modifier.fillMaxSize())
                    } else if (!preferences.isAppLockEnabled || isUnlocked) {
                        FlexyNotesNavigation(
                            preferences = preferences,
                            onUpdatePreferences = { update -> mainViewModel.updatePreferences(update) }
                        )
                    } else {
                        // Locked Screen UI (Shown if user cancels prompt or just opened app)
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "FlexyNotes is Locked",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(Modifier.height(24.dp))
                                Button(onClick = { showPromptTrigger = true }) {
                                    Text("Unlock")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt(onResult: (Boolean) -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // If user cancels, they see the fallback "Unlock" button screen
                onResult(false)
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("FlexyNotes Locked")
            .setSubtitle("Use biometrics to access your notes")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

@Composable
fun FlexyNotesNavigation(
    preferences: UserPreferences,
    onUpdatePreferences: ((UserPreferences) -> UserPreferences) -> Unit
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "notes_list"

    var isGridView by rememberSaveable { mutableStateOf(true) }
    val gesturesEnabled = currentRoute.startsWith("note_editor") == false

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
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
                    label = { Text("Notes") },
                    selected = currentRoute == "notes_list",
                    onClick = {
                        navController.navigate("notes_list") { popUpTo("notes_list") { inclusive = true } }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                    label = { Text("Archive") },
                    selected = currentRoute == "archive",
                    onClick = {
                        navController.navigate("archive")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    label = { Text("Trash") },
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
                    label = { Text("Settings") },
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
        NavHost(navController = navController, startDestination = "notes_list") {
            composable("notes_list") {
                val viewModel: NotesViewModel = hiltViewModel()
                NotesListScreen(
                    viewModel = viewModel,
                    isGridView = isGridView,
                    useHaptics = preferences.useHaptics,
                    onGridViewToggle = { isGridView = !isGridView },
                    onNavigateToEditor = { noteId -> navController.navigate("note_editor/${noteId ?: -1L}") },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable("archive") {
                val viewModel: NotesViewModel = hiltViewModel()
                ArchiveScreen(
                    viewModel = viewModel,
                    isGridView = isGridView,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigateToEditor = { noteId -> navController.navigate("note_editor/${noteId}") }
                )
            }
            composable("trash") {
                val viewModel: NotesViewModel = hiltViewModel()
                TrashScreen(
                    viewModel = viewModel,
                    isGridView = isGridView,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable("settings") {
                SettingsScreen(
                    preferences = preferences,
                    onUpdatePreferences = onUpdatePreferences,
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
                    showTimestamp = preferences.showTimestamp,
                    useHaptics = preferences.useHaptics,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}