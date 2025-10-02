package com.aura.substratecryptotest.ui.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager para monitorear el ciclo de vida de las activities
 * Permite saber qué activity está activa y manejar el estado de la UI
 */
class ActivityLifecycleManager private constructor() : Application.ActivityLifecycleCallbacks {
    
    companion object {
        @Volatile
        private var INSTANCE: ActivityLifecycleManager? = null
        
        fun getInstance(): ActivityLifecycleManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ActivityLifecycleManager().also { INSTANCE = it }
            }
        }
    }
    
    private val _currentActivity = MutableStateFlow<FragmentActivity?>(null)
    val currentActivity: StateFlow<FragmentActivity?> = _currentActivity.asStateFlow()
    
    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()
    
    private var activityCount = 0
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Logger.debug("ActivityLifecycleManager", "Activity creada", "Clase: ${activity.javaClass.simpleName}")
        
        if (activity is FragmentActivity) {
            _currentActivity.value = activity
            Logger.debug("ActivityLifecycleManager", "Activity actual establecida", "Clase: ${activity.javaClass.simpleName}")
        }
    }
    
    override fun onActivityStarted(activity: Activity) {
        activityCount++
        Logger.debug("ActivityLifecycleManager", "Activity iniciada", "Clase: ${activity.javaClass.simpleName}, Count: $activityCount")
        
        if (activityCount == 1) {
            _isAppInForeground.value = true
            Logger.debug("ActivityLifecycleManager", "App en primer plano", "")
        }
        
        if (activity is FragmentActivity) {
            _currentActivity.value = activity
        }
    }
    
    override fun onActivityResumed(activity: Activity) {
        Logger.debug("ActivityLifecycleManager", "Activity resumida", "Clase: ${activity.javaClass.simpleName}")
        
        if (activity is FragmentActivity) {
            _currentActivity.value = activity
            Logger.debug("ActivityLifecycleManager", "Activity activa actualizada", "Clase: ${activity.javaClass.simpleName}")
        }
    }
    
    override fun onActivityPaused(activity: Activity) {
        Logger.debug("ActivityLifecycleManager", "Activity pausada", "Clase: ${activity.javaClass.simpleName}")
    }
    
    override fun onActivityStopped(activity: Activity) {
        activityCount--
        Logger.debug("ActivityLifecycleManager", "Activity detenida", "Clase: ${activity.javaClass.simpleName}, Count: $activityCount")
        
        if (activityCount == 0) {
            _isAppInForeground.value = false
            Logger.debug("ActivityLifecycleManager", "App en segundo plano", "")
        }
    }
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        Logger.debug("ActivityLifecycleManager", "Activity guardando estado", "Clase: ${activity.javaClass.simpleName}")
    }
    
    override fun onActivityDestroyed(activity: Activity) {
        Logger.debug("ActivityLifecycleManager", "Activity destruida", "Clase: ${activity.javaClass.simpleName}")
        
        if (activity is FragmentActivity && _currentActivity.value == activity) {
            _currentActivity.value = null
            Logger.debug("ActivityLifecycleManager", "Activity actual limpiada", "")
        }
    }
    
    /**
     * Obtiene la activity actualmente activa
     */
    fun getCurrentActivity(): FragmentActivity? {
        return _currentActivity.value
    }
    
    /**
     * Verifica si la app está en primer plano
     */
    fun isAppInForeground(): Boolean {
        return _isAppInForeground.value
    }
    
    /**
     * Verifica si una activity específica está activa
     */
    fun isActivityActive(activityClass: Class<out FragmentActivity>): Boolean {
        val current = _currentActivity.value
        return current != null && current.javaClass == activityClass
    }
}
