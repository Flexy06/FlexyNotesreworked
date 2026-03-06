package com.flexynotes.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.flexynotes.data.AppLanguage
import com.flexynotes.data.ThemeMode
import com.flexynotes.data.UserPreferences
import com.flexynotes.ui.screens.*
import com.flexynotes.ui.theme.FlexyNotesTheme
import com.flexynotes.viewmodel.MainViewModel
import com.flexynotes.viewmodel.NotesViewModel
import com.flexynotes.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Splash Screen - requires 'androidx.core:core-splashscreen' in gradle
        val splashScreen = installSplashScreen()

        // Ensure AppCompat theme for locale switching
        setTheme(androidx.appcompat.R.style.Theme_AppCompat_DayNight_NoActionBar)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val preferences by mainViewModel.preferences.collectAsState()

            var isUnlocked by rememberSaveable { mutableStateOf(false) }

            // Locale logic using AppCompatDelegate
            LaunchedEffect(preferences.language) {
                val localeTag = when (preferences.language) {
                    AppLanguage.ENGLISH -> "en"
                    AppLanguage.GERMAN -> "de"
                    AppLanguage.FRENCH -> "fr"
                    AppLanguage.SYSTEM -> ""
                }
                val appLocale = if (localeTag.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(localeTag)
                }
                if (AppCompatDelegate.getApplicationLocales() != appLocale) {
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            }

            val isDarkTheme = when (preferences.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LaunchedEffect(preferences.isAppLockEnabled, isUnlocked) {
                if (preferences.isAppLockEnabled && !isUnlocked) {
                    showBiometricPrompt { success -> isUnlocked = success }
                } else {
                    isUnlocked = true
                }
            }

            FlexyNotesTheme(
                darkTheme = isDarkTheme,
                dynamicColor = preferences.useDynamicColor,
                isOledMode = preferences.isOledMode
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isUnlocked) {
                        FlexyNotesNavigation(
                            preferences = preferences,
                            onUpdatePreferences = { update -> mainViewModel.updatePreferences(update) }
                        )
                    } else {
                        Box(Modifier.fillMaxSize())
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
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.lock_title))
            .setSubtitle(getString(R.string.lock_subtitle))
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
    val currentFullRoute = navBackStackEntry?.destination?.route ?: "notes_list"
    val currentRoute = currentFullRoute.substringBefore("/")

    var lastBackEdge by remember { mutableIntStateOf(0) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !currentFullRoute.startsWith("note_editor"),
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(32.dp))
                Text(stringResource(R.string.app_name), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineMedium)

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Edit, null) },
                    label = { Text(stringResource(R.string.nav_notes)) },
                    selected = currentRoute == "notes_list",
                    onClick = {
                        navController.navigate("notes_list") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Archive, null) },
                    label = { Text(stringResource(R.string.nav_archive)) },
                    selected = currentRoute == "archive",
                    onClick = {
                        navController.navigate("archive") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Delete, null) },
                    label = { Text(stringResource(R.string.nav_trash)) },
                    selected = currentRoute == "trash",
                    onClick = {
                        navController.navigate("trash") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(Modifier.weight(1f))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text(stringResource(R.string.nav_settings)) },
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        },
        modifier = Modifier.systemBarsPadding().pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(false)
                if (down.position.x < size.width * 0.15f) lastBackEdge = 0 else if (down.position.x > size.width * 0.85f) lastBackEdge = 1
            }
        }
    ) {
        NavHost(navController = navController, startDestination = "notes_list") {
            composable("notes_list") {
                NotesListScreen(
                    viewModel = hiltViewModel(),
                    isGridView = true,
                    useHaptics = preferences.useHaptics,
                    onGridViewToggle = {},
                    onNavigateToEditor = { id, isCheck -> navController.navigate("note_editor/$id?isChecklist=$isCheck") },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable("archive") {
                ArchiveScreen(
                    viewModel = hiltViewModel(),
                    isGridView = true,
                    useHaptics = preferences.useHaptics,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigateToEditor = { id -> navController.navigate("note_editor/$id?isChecklist=false") }
                )
            }
            composable("trash") {
                TrashScreen(
                    viewModel = hiltViewModel(),
                    isGridView = true,
                    useHaptics = preferences.useHaptics,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable("settings") {
                SettingsScreen(
                    preferences = preferences,
                    onUpdatePreferences = onUpdatePreferences,
                    useHaptics = preferences.useHaptics,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable(
                route = "note_editor/{noteId}?isChecklist={isChecklist}",
                arguments = listOf(
                    navArgument("noteId") { type = NavType.StringType; nullable = true },
                    navArgument("isChecklist") { type = NavType.BoolType; defaultValue = false }
                ),
                popExitTransition = {
                    val slideDirection = if (lastBackEdge == 0) {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    }

                    slideOutOfContainer(
                        towards = slideDirection,
                        animationSpec = tween(durationMillis = 350)
                    ) + scaleOut(
                        targetScale = 0.65f,
                        animationSpec = tween(durationMillis = 350)
                    )
                }
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId")?.toLongOrNull()?.takeIf { it != -1L }
                NoteEditorScreen(
                    viewModel = hiltViewModel(),
                    noteId = noteId,
                    isChecklist = backStackEntry.arguments?.getBoolean("isChecklist") ?: false,
                    showTimestamp = preferences.showTimestamp,
                    useHaptics = preferences.useHaptics
                ) { navController.popBackStack() }
            }
        }
    }
}