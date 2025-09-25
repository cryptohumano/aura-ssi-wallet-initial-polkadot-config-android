package com.aura.substratecryptotest.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aura.substratecryptotest.ui.screens.*

/**
 * Navegación principal de la aplicación wallet
 */
@Composable
fun WalletNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = WalletScreens.Welcome.route
    ) {
        // Pantalla de bienvenida/onboarding
        composable(WalletScreens.Welcome.route) {
            WelcomeScreen(
                onNavigateToCreateWallet = {
                    navController.navigate(WalletScreens.CreateWallet.route)
                },
                onNavigateToImportWallet = {
                    navController.navigate(WalletScreens.ImportWallet.route)
                },
                onNavigateToDashboard = {
                    navController.navigate(WalletScreens.Dashboard.route)
                }
            )
        }
        
        // Crear nueva wallet
        composable(WalletScreens.CreateWallet.route) {
            CreateWalletScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onWalletCreated = {
                    navController.navigate(WalletScreens.Dashboard.route) {
                        popUpTo(WalletScreens.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Importar wallet existente
        composable(WalletScreens.ImportWallet.route) {
            ImportWalletScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onWalletImported = {
                    navController.navigate(WalletScreens.Dashboard.route) {
                        popUpTo(WalletScreens.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Dashboard principal
        composable(WalletScreens.Dashboard.route) {
            DashboardScreen(
                onNavigateToWalletInfo = {
                    navController.navigate(WalletScreens.WalletInfo.route)
                },
                onNavigateToSettings = {
                    navController.navigate(WalletScreens.Settings.route)
                },
                onNavigateToDID = {
                    navController.navigate(WalletScreens.DIDManagement.route)
                }
            )
        }
        
        // Información de wallet
        composable(WalletScreens.WalletInfo.route) {
            WalletInfoScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Gestión de DID
        composable(WalletScreens.DIDManagement.route) {
            DIDManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDIDDashboard = {
                    navController.navigate(WalletScreens.DIDDashboard.route)
                }
            )
        }
        
        // Dashboard de Identidad Digital
        composable(WalletScreens.DIDDashboard.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            DIDDashboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                activity = context as androidx.fragment.app.FragmentActivity
            )
        }
        
        // Configuración
        composable(WalletScreens.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(WalletScreens.Welcome.route) {
                        popUpTo(WalletScreens.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

/**
 * Definición de pantallas de la aplicación
 */
sealed class WalletScreens(val route: String) {
    object Welcome : WalletScreens("welcome")
    object CreateWallet : WalletScreens("create_wallet")
    object ImportWallet : WalletScreens("import_wallet")
    object Dashboard : WalletScreens("dashboard")
    object WalletInfo : WalletScreens("wallet_info")
    object DIDManagement : WalletScreens("did_management")
    object DIDDashboard : WalletScreens("did_dashboard")
    object Settings : WalletScreens("settings")
}
