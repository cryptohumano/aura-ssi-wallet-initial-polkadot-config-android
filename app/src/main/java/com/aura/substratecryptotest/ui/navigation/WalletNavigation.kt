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
        startDestination = WalletScreens.Login.route
    ) {
        // Pantalla de inicio de sesión
        composable(WalletScreens.Login.route) {
            LoginScreen(
                onCreateNewAccount = {
                    navController.navigate(WalletScreens.CreateWallet.route)
                },
                onImportAccount = {
                    navController.navigate(WalletScreens.ImportWallet.route)
                },
                onContinueWithExisting = {
                    // TODO: Implementar lógica para continuar con cuenta existente
                    // Por ahora vamos al dashboard
                    navController.navigate(WalletScreens.Dashboard.route) {
                        popUpTo(WalletScreens.Login.route) { inclusive = true }
                    }
                }
            )
        }
        // Pantalla de bienvenida/onboarding (ya no se usa)
        composable(WalletScreens.Welcome.route) {
            WelcomeScreen(
                onLanguageSelected = {
                    navController.navigate(WalletScreens.CreateWallet.route) {
                        popUpTo(WalletScreens.Welcome.route) { inclusive = true }
                    }
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
                        popUpTo(WalletScreens.Login.route) { inclusive = true }
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
                        popUpTo(WalletScreens.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Dashboard principal (Monedero)
        composable(WalletScreens.Dashboard.route) {
            DashboardScreen(
                onNavigateToWalletInfo = {
                    navController.navigate(WalletScreens.WalletInfo.route)
                },
                onNavigateToSettings = {
                    navController.navigate(WalletScreens.Settings.route)
                },
                onNavigateToDID = {
                    navController.navigate(WalletScreens.DIDDashboard.route)
                },
                onNavigateToCredentials = {
                    navController.navigate(WalletScreens.Credentials.route)
                },
                onNavigateToDocuments = {
                    navController.navigate(WalletScreens.Documents.route)
                },
                onNavigateToRWA = {
                    navController.navigate(WalletScreens.RWA.route)
                },
                onLogout = {
                    navController.navigate(WalletScreens.Login.route) {
                        popUpTo(WalletScreens.Dashboard.route) { inclusive = true }
                    }
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
            val context = androidx.compose.ui.platform.LocalContext.current
            DIDManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDIDDashboard = {
                    navController.navigate(WalletScreens.DIDDashboard.route)
                },
                activity = context as androidx.fragment.app.FragmentActivity
            )
        }
        
        // Dashboard de Identidad Digital
        composable(WalletScreens.DIDDashboard.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            DIDDashboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCredentials = {
                    navController.navigate(WalletScreens.Credentials.route)
                },
                onNavigateToDocuments = {
                    navController.navigate(WalletScreens.Documents.route)
                },
                onNavigateToRWA = {
                    navController.navigate(WalletScreens.RWA.route)
                },
                onNavigateToWallet = {
                    navController.navigate(WalletScreens.Dashboard.route)
                },
                activity = context as androidx.fragment.app.FragmentActivity
            )
        }
        
        // Pantalla de Credenciales
        composable(WalletScreens.Credentials.route) {
            CredentialsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Pantalla de Documentos con bitácoras de montañismo
        composable(WalletScreens.Documents.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val mountaineeringViewModelFactory = com.aura.substratecryptotest.ui.viewmodels.MountaineeringViewModelFactory(context)
            val mountaineeringViewModel: com.aura.substratecryptotest.ui.viewmodels.MountaineeringViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = mountaineeringViewModelFactory)
            
            DocumentsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = null // No usar MountaineeringViewModel aquí
            )
        }
        
        // Pantalla RWA
        composable(WalletScreens.RWA.route) {
            RWAScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Configuración
        composable(WalletScreens.Settings.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(WalletScreens.Login.route) {
                        popUpTo(WalletScreens.Dashboard.route) { inclusive = true }
                    }
                },
                onLanguageChanged = {
                    // Reiniciar la actividad para aplicar el idioma
                    if (context is androidx.fragment.app.FragmentActivity) {
                        context.finish()
                        context.startActivity(context.intent)
                    }
                }
            )
        }
        
        // Pantalla de demostración del sistema seguro
        composable(WalletScreens.SecureDemo.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val viewModelFactory = com.aura.substratecryptotest.ui.viewmodels.SecureViewModelFactory(context)
            val secureWalletViewModel: com.aura.substratecryptotest.ui.viewmodels.SecureWalletViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = viewModelFactory)
            val secureDocumentViewModel: com.aura.substratecryptotest.ui.viewmodels.SecureDocumentViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = viewModelFactory)
            val userManagementViewModel: com.aura.substratecryptotest.ui.viewmodels.UserManagementViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = viewModelFactory)
            
            SecureDemoScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                secureWalletViewModel = secureWalletViewModel,
                secureDocumentViewModel = secureDocumentViewModel,
                userManagementViewModel = userManagementViewModel
            )
        }
    }
}

/**
 * Definición de pantallas de la aplicación
 */
sealed class WalletScreens(val route: String) {
    object Login : WalletScreens("login")
    object Welcome : WalletScreens("welcome")
    object CreateWallet : WalletScreens("create_wallet")
    object ImportWallet : WalletScreens("import_wallet")
    
    // Navegación principal
    object Dashboard : WalletScreens("dashboard") // Monedero
    object DIDDashboard : WalletScreens("did_dashboard") // Identidad
    object Credentials : WalletScreens("credentials") // Credenciales
    object Documents : WalletScreens("documents") // Documentos
    object RWA : WalletScreens("rwa") // RWA
    
    // Pantallas secundarias
    object WalletInfo : WalletScreens("wallet_info")
    object DIDManagement : WalletScreens("did_management")
    object Settings : WalletScreens("settings")
    object SecureDemo : WalletScreens("secure_demo")
}
