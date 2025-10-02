package com.aura.substratecryptotest.ui.lifecycle

import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.collectAsState

/**
 * Composable que proporciona información sobre la activity actualmente activa
 */
@Composable
fun rememberCurrentActivity(): FragmentActivity? {
    val activityLifecycleManager = remember { ActivityLifecycleManager.getInstance() }
    val currentActivity by activityLifecycleManager.currentActivity.collectAsState()
    
    return currentActivity
}

/**
 * Composable que proporciona información sobre si la app está en primer plano
 */
@Composable
fun rememberAppForegroundState(): Boolean {
    val activityLifecycleManager = remember { ActivityLifecycleManager.getInstance() }
    val isInForeground by activityLifecycleManager.isAppInForeground.collectAsState()
    
    return isInForeground
}

/**
 * Composable que verifica si una activity específica está activa
 */
@Composable
fun rememberIsActivityActive(activityClass: Class<out FragmentActivity>): Boolean {
    val activityLifecycleManager = remember { ActivityLifecycleManager.getInstance() }
    val currentActivity by activityLifecycleManager.currentActivity.collectAsState()
    
    return currentActivity?.javaClass == activityClass
}

/**
 * Composable que ejecuta una acción cuando la activity cambia
 */
@Composable
fun OnActivityChange(
    onActivityChanged: (FragmentActivity?) -> Unit
) {
    val activityLifecycleManager = remember { ActivityLifecycleManager.getInstance() }
    val currentActivity by activityLifecycleManager.currentActivity.collectAsState()
    
    LaunchedEffect(currentActivity) {
        onActivityChanged(currentActivity)
    }
}
