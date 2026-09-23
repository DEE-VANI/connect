package com.example.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.entities.UserRole
import com.example.data.repository.CampusConnectRepository
import com.example.ui.components.PersonaSwitchDialog
import com.example.ui.components.ResilienceBanner
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.discovery.EventDetailScreen
import com.example.ui.screens.discovery.EventDiscoveryScreen
import com.example.ui.screens.organizer.EventAttendeesScreen
import com.example.ui.screens.organizer.EventCreateEditScreen
import com.example.ui.screens.organizer.OrganizerDashboardScreen
import com.example.ui.screens.organizer.QrScannerScreen
import com.example.ui.screens.profile.UserProfileScreen
import com.example.ui.screens.resilience.FailureSimulationLabScreen
import com.example.ui.screens.tickets.MyTicketsScreen
import com.example.ui.screens.tickets.TicketDetailScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    data object Discovery : Screen("discovery", "Events", Icons.Filled.Explore, Icons.Outlined.Explore)
    data object MyTickets : Screen("my_tickets", "Tickets", Icons.Filled.ConfirmationNumber, Icons.Outlined.ConfirmationNumber)
    data object Organizer : Screen("organizer", "Organizer", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner)
    data object Admin : Screen("admin", "Admin", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings)
    data object Profile : Screen("profile", "Campus ID", Icons.Filled.Badge, Icons.Outlined.Badge)
}

val bottomNavItems = listOf(
    Screen.Discovery,
    Screen.MyTickets,
    Screen.Organizer,
    Screen.Admin,
    Screen.Profile
)

@Composable
fun CampusNavigation(repository: CampusConnectRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val coroutineScope = rememberCoroutineScope()

    val currentUser by repository.currentUser.collectAsState()
    val isAuthenticated by repository.isAuthenticated.collectAsState()
    val allUsers by repository.allUsers.collectAsState(initial = emptyList())
    var showPersonaDialog by remember { mutableStateOf(false) }

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (showBottomBar && isAuthenticated) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Global Persistent Resilience Banner when fault simulation is active
            ResilienceBanner(
                simulationManager = repository.failureSimulationManager,
                onOpenLab = { navController.navigate("simulation_lab") }
            )

            NavHost(
                navController = navController,
                startDestination = Screen.Discovery.route,
                modifier = Modifier.fillMaxSize()
            ) {
                // Auth Routes
                composable("login") {
                    LoginScreen(
                        repository = repository,
                        onNavigateToRegister = { navController.navigate("register") },
                        onNavigateToForgotPassword = { navController.navigate("forgot_password") },
                        onNavigateToSimulationLab = { navController.navigate("simulation_lab") },
                        onLoginSuccess = {
                            navController.navigate(Screen.Discovery.route) {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                composable("register") {
                    RegisterScreen(
                        repository = repository,
                        onNavigateBackToLogin = { navController.popBackStack() },
                        onRegistrationSuccess = {
                            navController.navigate(Screen.Discovery.route) {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                composable("forgot_password") {
                    ForgotPasswordScreen(
                        repository = repository,
                        onNavigateBackToLogin = { navController.popBackStack() }
                    )
                }

                // Resilience Simulation Lab Route
                composable("simulation_lab") {
                    FailureSimulationLabScreen(
                        repository = repository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Discovery.route) {
                    EventDiscoveryScreen(
                        repository = repository,
                        onNavigateToDetail = { eventId ->
                            navController.navigate("event_detail/$eventId")
                        },
                        onOpenPersonaSwitcher = { showPersonaDialog = true },
                        onNavigateToCreateEvent = { navController.navigate("create_event") }
                    )
                }

                composable(Screen.MyTickets.route) {
                    MyTicketsScreen(
                        repository = repository,
                        onNavigateToDiscovery = {
                            navController.navigate(Screen.Discovery.route)
                        }
                    )
                }

                composable(Screen.Organizer.route) {
                    OrganizerDashboardScreen(
                        repository = repository,
                        onNavigateToCreateEvent = {
                            navController.navigate("create_event")
                        },
                        onNavigateToScanner = { eventId ->
                            navController.navigate("scanner/$eventId")
                        },
                        onNavigateToAttendees = { eventId ->
                            navController.navigate("attendees/$eventId")
                        },
                        onOpenPersonaSwitcher = { showPersonaDialog = true }
                    )
                }

                composable(Screen.Admin.route) {
                    AdminDashboardScreen(
                        repository = repository,
                        onOpenPersonaSwitcher = { showPersonaDialog = true }
                    )
                }

                composable(Screen.Profile.route) {
                    UserProfileScreen(
                        repository = repository,
                        onOpenPersonaSwitcher = { showPersonaDialog = true },
                        onOpenSimulationLab = { navController.navigate("simulation_lab") },
                        onLogout = {
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

            composable(
                route = "event_detail/{eventId}",
                arguments = listOf(navArgument("eventId") { type = NavType.LongType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getLong("eventId") ?: 0L
                EventDetailScreen(
                    eventId = eventId,
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTicket = { ticketId ->
                        navController.navigate("ticket_detail/$ticketId")
                    },
                    onNavigateToScanner = { id ->
                        navController.navigate("scanner/$id")
                    },
                    onNavigateToAttendees = { id ->
                        navController.navigate("attendees/$id")
                    }
                )
            }

            composable(
                route = "ticket_detail/{ticketId}",
                arguments = listOf(navArgument("ticketId") { type = NavType.LongType })
            ) { backStackEntry ->
                val ticketId = backStackEntry.arguments?.getLong("ticketId") ?: 0L
                TicketDetailScreen(
                    ticketId = ticketId,
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("create_event") {
                EventCreateEditScreen(
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "scanner/{eventId}",
                arguments = listOf(navArgument("eventId") { type = NavType.LongType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getLong("eventId") ?: 0L
                QrScannerScreen(
                    eventId = eventId,
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAttendees = { id ->
                        navController.navigate("attendees/$id")
                    }
                )
            }

            composable(
                route = "attendees/{eventId}",
                arguments = listOf(navArgument("eventId") { type = NavType.LongType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getLong("eventId") ?: 0L
                EventAttendeesScreen(
                    eventId = eventId,
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToScanner = { id ->
                        navController.navigate("scanner/$id")
                    }
                )
            }
        }
        }

        if (showPersonaDialog) {
            PersonaSwitchDialog(
                currentUser = currentUser,
                allUsers = allUsers,
                onSelectUser = { userId ->
                    coroutineScope.launch {
                        repository.switchUser(userId)
                    }
                },
                onCreateUser = { name, email, stuId, dept, year, role ->
                    coroutineScope.launch {
                        repository.createUser(name, email, stuId, dept, year, role)
                    }
                },
                onDismiss = { showPersonaDialog = false }
            )
        }
    }
}
