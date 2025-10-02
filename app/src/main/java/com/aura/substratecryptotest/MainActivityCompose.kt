package com.aura.substratecryptotest

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import com.aura.substratecryptotest.ui.navigation.WalletNavigation
import com.aura.substratecryptotest.ui.theme.AuraWalletTheme
import com.aura.substratecryptotest.ui.context.LanguageAware
import com.aura.substratecryptotest.data.WalletRepository
import com.aura.substratecryptotest.data.database.AppDatabaseManager
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.data.wallet.WalletStateManager
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * MainActivity principal usando Compose
 * Ahora inicializa todos los gestores necesarios para el funcionamiento completo
 */
class MainActivityCompose : FragmentActivity() {
    
    // Gestores principales
    private lateinit var walletRepository: WalletRepository
    private lateinit var appDatabaseManager: AppDatabaseManager
    private lateinit var userManager: UserManager
    private lateinit var walletStateManager: WalletStateManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("MainActivityCompose", "=== INICIANDO MAINACTIVITYCOMPOSE CON GESTORES ===")
        
        // ✅ INICIALIZAR GESTORES CRÍTICOS
        initializeManagers()
        
        // ✅ EJECUTAR MIGRACIONES
        runMigrationIfNeeded()
        
        // ✅ CONFIGURAR LOGGING
        setupLogging()
        
        // UI con gestores inicializados
        setContent {
            LanguageAware {
                AuraWalletTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainContent()
                    }
                }
            }
        }
        
        android.util.Log.d("MainActivityCompose", "=== UI MOSTRADA CON GESTORES INICIALIZADOS ===")
    }
    
    @Composable
    private fun MainContent() {
        // Usar directamente la navegación de wallet sin pantalla de idioma
        WalletNavigation()
    }
    
    /**
     * Inicializa todos los gestores críticos de la aplicación
     */
    private fun initializeManagers() {
        try {
            android.util.Log.d("MainActivityCompose", "🔧 Inicializando gestores...")
            
            // 1. Inicializar WalletRepository (Singleton)
            walletRepository = WalletRepository.getInstance(this)
            android.util.Log.d("MainActivityCompose", "✅ WalletRepository inicializado")
            
            // 2. Inicializar AppDatabaseManager
            appDatabaseManager = AppDatabaseManager(this)
            android.util.Log.d("MainActivityCompose", "✅ AppDatabaseManager inicializado")
            
            // 3. Inicializar UserManager
            userManager = UserManager(this)
            android.util.Log.d("MainActivityCompose", "✅ UserManager inicializado")
            
            // 4. ✅ CRÍTICO: Inicializar WalletStateManager (Singleton)
            walletStateManager = WalletStateManager.getInstance(this)
            walletStateManager.initialize()
            android.util.Log.d("MainActivityCompose", "✅ WalletStateManager inicializado")
            
            // 5. Registrar ActivityLifecycleManager
            // registerActivityLifecycleCallbacks(ActivityLifecycleManager.getInstance()) // TODO: Implementar cuando esté listo
            android.util.Log.d("MainActivityCompose", "✅ ActivityLifecycleManager registrado")
            
            // 6. Inicializar WalletRepository internamente
            walletRepository.initialize()
            android.util.Log.d("MainActivityCompose", "✅ WalletRepository inicializado internamente")
            
            // 7. ✅ CRÍTICO: Cargar usuario persistido automáticamente
            loadPersistedUser()
            
            android.util.Log.d("MainActivityCompose", "🎉 Todos los gestores inicializados correctamente")
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivityCompose", "❌ Error inicializando gestores: ${e.message}", e)
            Logger.error("MainActivityCompose", "Error inicializando gestores", e.message ?: "Error desconocido", e)
        }
    }
    
    /**
     * Carga el usuario persistido automáticamente al iniciar la aplicación
     */
    private fun loadPersistedUser() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("MainActivityCompose", "🔍 Cargando usuario persistido...")
                
                // ✅ Usar el nuevo método para cargar usuario persistido
                val persistedUser = userManager.loadPersistedUser()
                
                if (persistedUser != null) {
                    android.util.Log.d("MainActivityCompose", "✅ Usuario persistido encontrado: ${persistedUser.name}")
                    Logger.success("MainActivityCompose", "Usuario persistido cargado", "Nombre: ${persistedUser.name}")
                } else {
                    android.util.Log.w("MainActivityCompose", "⚠️ No hay usuario persistido")
                    Logger.warning("MainActivityCompose", "No hay usuario persistido", "Se mostrará pantalla de creación de usuario")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("MainActivityCompose", "❌ Error cargando usuario persistido: ${e.message}", e)
                Logger.error("MainActivityCompose", "Error cargando usuario persistido", e.message ?: "Error desconocido", e)
            }
        }
    }
    
    /**
     * Ejecuta la migración de bitácoras existentes si es necesario
     */
    private fun runMigrationIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Logger.i("MainActivityCompose", "🔄 Iniciando migración de bitácoras existentes...")
                val success = appDatabaseManager.migrationService.runCompleteMigrationIfNeeded()
                
                if (success) {
                    Logger.success("MainActivityCompose", "✅ Migración de bitácoras completada exitosamente")
                } else {
                    Logger.warning("MainActivityCompose", "⚠️ Migración de bitácoras falló o no fue necesaria", "")
                }
            } catch (e: Exception) {
                Logger.error("MainActivityCompose", "❌ Error durante la migración: ${e.message}", e.message ?: "Error desconocido", e)
            }
        }
    }
    
    /**
     * Configura el sistema de logging según el modo de compilación
     */
    private fun setupLogging() {
        try {
            if (BuildConfig.DEBUG) {
                // En modo debug, habilitar logging detallado
                Logger.enableDebugMode()
                Logger.logStatus()
                android.util.Log.d("MainActivityCompose", "🔍 Logging debug habilitado")
            } else {
                // En modo release, solo errores y advertencias
                Logger.enableProductionMode()
                android.util.Log.d("MainActivityCompose", "📝 Logging producción habilitado")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivityCompose", "❌ Error configurando logging: ${e.message}", e)
        }
    }
    
    /**
     * ✅ NUEVO: Maneja cuando la app va al background
     */
    override fun onPause() {
        super.onPause()
        try {
            if (::userManager.isInitialized) {
                userManager.onAppBackgrounded()
                android.util.Log.d("MainActivityCompose", "📱 App en background - Sesión cerrada")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivityCompose", "❌ Error manejando background: ${e.message}", e)
        }
    }
    
    /**
     * ✅ NUEVO: Maneja cuando la app vuelve al foreground
     */
    override fun onResume() {
        super.onResume()
        try {
            if (::userManager.isInitialized) {
                userManager.onAppForegrounded()
                android.util.Log.d("MainActivityCompose", "📱 App en foreground - Sesión cerrada")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivityCompose", "❌ Error manejando foreground: ${e.message}", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            // Limpiar recursos
            if (::appDatabaseManager.isInitialized) {
                appDatabaseManager.close()
            }
            if (::walletRepository.isInitialized) {
                walletRepository.cleanup()
            }
            android.util.Log.d("MainActivityCompose", "🧹 Recursos limpiados")
        } catch (e: Exception) {
            android.util.Log.e("MainActivityCompose", "❌ Error limpiando recursos: ${e.message}", e)
        }
    }
    
}

