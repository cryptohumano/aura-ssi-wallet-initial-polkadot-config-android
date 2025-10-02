package com.aura.substratecryptotest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.aura.substratecryptotest.R
import com.aura.substratecryptotest.utils.LanguageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.runtime.collectAsState
import com.aura.substratecryptotest.ui.context.LanguageAware

/**
 * Pantalla de configuración con herramientas de debug
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {},
    onLanguageChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    var developerMode by remember { mutableStateOf(false) }
    var debugInfo by remember { mutableStateOf("Cargando información...") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Language management
    val languageManager = remember { LanguageManager.getInstance(context) }
    val currentLanguage by languageManager.currentLanguage.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    // Inicializar servicios
    val legacyDiagnosticService = remember { com.aura.substratecryptotest.data.services.LegacyDataDiagnosticService(context) }
    val legacyMigrationService = remember { com.aura.substratecryptotest.data.services.LegacyUserMigrationService(context) }
    
    // Cargar información del usuario al iniciar
    LaunchedEffect(Unit) {
        try {
            val userInfo = buildString {
                appendLine("=== INFORMACIÓN DEL USUARIO ===")
                appendLine()
                appendLine("👤 USUARIO ACTUAL:")
                appendLine("  Verificando estado del usuario...")
                appendLine()
                appendLine("📚 BITÁCORAS:")
                appendLine("  Verificando bitácoras...")
                appendLine()
                appendLine("🔍 Usa las herramientas de debug para más información")
            }
            
            debugInfo = userInfo
        } catch (e: Exception) {
            debugInfo = "Error cargando información: ${e.message}"
        }
    }

    LanguageAware {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                        }
                    }
                )
            }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Language Settings
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLanguageDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(languageManager.getLanguageDisplayName(currentLanguage))
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
            }
            
            // Información del Usuario
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.user_info_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = debugInfo,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Modo Desarrollador
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_developer_mode),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.settings_enable_debug))
                        Switch(
                            checked = developerMode,
                            onCheckedChange = { developerMode = it }
                        )
                    }
                    
                    Text(
                        text = stringResource(R.string.settings_debug_description),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Herramientas de Debug (solo si está habilitado)
            if (developerMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_developer_tools),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Botón 1: Diagnosticar Datos Anteriores
                        Button(
                            onClick = {
                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val diagnosticResult = legacyDiagnosticService.diagnoseAllPreferences()
                                        val diagnosticInfo = buildString {
                                            appendLine("=== DIAGNÓSTICO DE DATOS ANTERIORES ===")
                                            appendLine("Total archivos: ${diagnosticResult.totalFiles}")
                                            appendLine("Con datos: ${diagnosticResult.filesWithData}")
                                            if (diagnosticResult.filesWithData > 0) {
                                                appendLine("✅ DATOS ENCONTRADOS")
                                            } else {
                                                appendLine("❌ NO HAY DATOS")
                                            }
                                        }
                                        debugInfo = diagnosticInfo
                                    } catch (e: Exception) {
                                        debugInfo = "❌ Error: ${e.message}"
                                    }
                                    isLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Text(stringResource(R.string.debug_diagnose_data))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Botón 2: Migrar Usuario Anterior
                        Button(
                            onClick = {
                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val migrationResult = legacyMigrationService.migrateLegacyUserData()
                                        val migrationInfo = buildString {
                                            appendLine("=== MIGRACIÓN DE USUARIO ANTERIOR ===")
                                            when (migrationResult) {
                                                is com.aura.substratecryptotest.data.services.LegacyUserMigrationService.MigrationResult.Success -> {
                                                    appendLine("✅ MIGRACIÓN EXITOSA")
                                                    appendLine("Usuario: ${migrationResult.migratedUser.name}")
                                                }
                                                is com.aura.substratecryptotest.data.services.LegacyUserMigrationService.MigrationResult.Error -> {
                                                    appendLine("❌ ERROR: ${migrationResult.message}")
                                                }
                                                is com.aura.substratecryptotest.data.services.LegacyUserMigrationService.MigrationResult.NoDataFound -> {
                                                    appendLine("⚠️ NO HAY DATOS")
                                                }
                                            }
                                        }
                                        debugInfo = migrationInfo
                                    } catch (e: Exception) {
                                        debugInfo = "❌ Error: ${e.message}"
                                    }
                                    isLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Text(stringResource(R.string.debug_migrate_user))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Botón 3: Test de API Client
                        Button(
                            onClick = {
                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val apiTest = com.aura.substratecryptotest.api.DidApiTest(context)
                                        val success = apiTest.runBasicTests()
                                        apiTest.cleanup()
                                        
                                        val apiTestInfo = buildString {
                                            appendLine("=== TEST DE API CLIENT ===")
                                            if (success) {
                                                appendLine("✅ TODOS LOS TESTS PASARON")
                                                appendLine("🎉 API Client funcionando correctamente")
                                            } else {
                                                appendLine("❌ ALGUNOS TESTS FALLARON")
                                                appendLine("Ver logs con: adb logcat -s DidApiTest")
                                            }
                                        }
                                        debugInfo = apiTestInfo
                                    } catch (e: Exception) {
                                        debugInfo = "❌ Error: ${e.message}"
                                    }
                                    isLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Text(stringResource(R.string.debug_test_api))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Botón 4: Ver Logs
                        Button(
                            onClick = {
                                val logsInfo = buildString {
                                    appendLine("=== LOGS DE MIGRACIÓN ===")
                                    appendLine("📱 PARA VER LOGS COMPLETOS:")
                                    appendLine("1. Android Studio → Logcat")
                                    appendLine("2. Filtrar por tags:")
                                    appendLine("   - LegacyUserMigrationService")
                                    appendLine("   - LegacyDataDiagnosticService")
                                    appendLine("   - DidApiTest")
                                    appendLine()
                                    appendLine("3. Terminal (ADB):")
                                    appendLine("   adb logcat -s LegacyUserMigrationService")
                                    appendLine("   adb logcat -s DidApiTest")
                                }
                                debugInfo = logsInfo
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Text(stringResource(R.string.debug_view_logs))
                        }
                    }
                }
            }
        }
        }
    }
    
    // Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column {
                    languageManager.getAvailableLanguages().forEach { language ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        languageManager.setLanguage(language)
                                        showLanguageDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentLanguage == language,
                                    onClick = {
                                        languageManager.setLanguage(language)
                                        showLanguageDialog = false
                                        onLanguageChanged()
                                    }
                                )
                            Text(
                                text = languageManager.getLanguageDisplayName(language),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.action_confirm))
                }
            }
        )
    }
}