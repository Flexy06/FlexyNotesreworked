package com.example.FlexyNotes

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
class MainActivity : FragmentActivity() {
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

            LaunchedEffect(Unit) {
                if (!isDataStoreLoaded) {
                    delay(100)
                    isDataStoreLoaded = true
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        isUnlocked = false
                        showPromptTrigger = true
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            LaunchedEffect(preferences.isSecureMode) {
                if (preferences.isSecureMode) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            LaunchedEffect(preferences.isAppLockEnabled, isUnlocked, showPromptTrigger, isDataStoreLoaded) {
                if (isDataStoreLoaded && preferences.isAppLockEnabled && !isUnlocked && showPromptTrigger) {
                    showPromptTrigger = false
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
                        Box(Modifier.fillMaxSize())
                    } else if (!preferences.isAppLockEnabled || isUnlocked) {
                        FlexyNotesNavigation(
                            preferences = preferences,
                            onUpdatePreferences = { update -> mainViewModel.updatePreferences(update) }
                        )
                    } else {
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
                        if (currentRoute != "notes_list") {
                            navController.navigate("notes_list") { popUpTo("notes_list") { inclusive = true } }
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                    label = { Text("Archive") },
                    selected = currentRoute == "archive",
                    onClick = {
                        if (currentRoute != "archive") {
                            navController.navigate("archive")
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    label = { Text("Trash") },
                    selected = currentRoute == "trash",
                    onClick = {
                        if (currentRoute != "trash") {
                            navController.navigate("trash")
                        }
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
                        if (currentRoute != "settings") {
                            navController.navigate("settings")
                        }
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
            startDestination = "notes_list",
            enterTransition = {
                fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        scaleIn(initialScale = 0.95f, animationSpec = tween(220, delayMillis = 90))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(150))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        scaleIn(initialScale = 0.95f, animationSpec = tween(220, delayMillis = 90))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(150))
            }
        ) {
            composable("notes_list") {
                val viewModel: NotesViewModel = hiltViewModel()
                NotesListScreen(
                    viewModel = viewModel,
                    isGridView = isGridView,
                    useHaptics = preferences.useHaptics,
                    onGridViewToggle = { isGridView = !isGridView },
                    onNavigateToEditor = { noteId, isChecklist ->
                        navController.navigate("note_editor/${noteId ?: -1L}?isChecklist=$isChecklist")
                    },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable("archive") {
                val viewModel: NotesViewModel = hiltViewModel()
                ArchiveScreen(
                    viewModel = viewModel,
                    isGridView = isGridView,
                    useHaptics = preferences.useHaptics,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigateToEditor = { noteId -> navController.navigate("note_editor/${noteId}?isChecklist=false") }
                )
            }
            composable("trash") {
                val viewModel: NotesViewModel = hiltViewModel()
                TrashScreen(
                    viewModel = viewModel,
                    isGridView = isGridView,
                    useHaptics = preferences.useHaptics,
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

            composable(
                route = "note_editor/{noteId}?isChecklist={isChecklist}",
                arguments = listOf(
                    navArgument("noteId") { type = NavType.StringType; nullable = true },
                    navArgument("isChecklist") { type = NavType.BoolType; defaultValue = false }
                ),
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(350))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.9f, animationSpec = tween(300))
                },
                popEnterTransition = {
                    // Brings the list back smoothly from the background
                    fadeIn(animationSpec = tween(350)) + scaleIn(initialScale = 0.9f, animationSpec = tween(350))
                },
                popExitTransition = {
                    // Shrinks the editor deeply to match the native back predictive behavior on Android 14
                    scaleOut(targetScale = 0.75f, animationSpec = tween(350)) +
                            fadeOut(animationSpec = tween(350))
                }
            ) { backStackEntry ->
                val viewModel: NotesViewModel = hiltViewModel()
                val noteIdStr = backStackEntry.arguments?.getString("noteId")
                val noteId = noteIdStr?.toLongOrNull()?.takeIf { it != -1L }
                val isChecklist = backStackEntry.arguments?.getBoolean("isChecklist") ?: false

                NoteEditorScreen(
                    viewModel = viewModel,
                    noteId = noteId,
                    isChecklist = isChecklist,
                    showTimestamp = preferences.showTimestamp,
                    useHaptics = preferences.useHaptics,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}