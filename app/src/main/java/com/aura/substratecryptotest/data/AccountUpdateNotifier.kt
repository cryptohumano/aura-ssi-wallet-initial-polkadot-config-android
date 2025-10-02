package com.aura.substratecryptotest.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Singleton para notificar cambios en las cuentas
 * Permite que diferentes partes de la aplicación se notifiquen mutuamente
 * cuando se crean, modifican o eliminan cuentas
 */
object AccountUpdateNotifier {
    
    private val _accountUpdates = MutableSharedFlow<AccountUpdateEvent>()
    val accountUpdates: SharedFlow<AccountUpdateEvent> = _accountUpdates.asSharedFlow()
    
    /**
     * Notifica que se ha creado una nueva cuenta
     */
    suspend fun notifyAccountCreated(accountName: String) {
        _accountUpdates.emit(AccountUpdateEvent.AccountCreated(accountName))
    }
    
    /**
     * Notifica que se ha eliminado una cuenta
     */
    suspend fun notifyAccountDeleted(accountName: String) {
        _accountUpdates.emit(AccountUpdateEvent.AccountDeleted(accountName))
    }
    
    /**
     * Notifica que se ha modificado una cuenta
     */
    suspend fun notifyAccountModified(accountName: String) {
        _accountUpdates.emit(AccountUpdateEvent.AccountModified(accountName))
    }
    
    /**
     * Notifica que se ha cambiado de cuenta
     */
    suspend fun notifyAccountSwitched(accountName: String) {
        _accountUpdates.emit(AccountUpdateEvent.AccountSwitched(accountName))
    }
}

/**
 * Eventos de actualización de cuentas
 */
sealed class AccountUpdateEvent {
    data class AccountCreated(val accountName: String) : AccountUpdateEvent()
    data class AccountDeleted(val accountName: String) : AccountUpdateEvent()
    data class AccountModified(val accountName: String) : AccountUpdateEvent()
    data class AccountSwitched(val accountName: String) : AccountUpdateEvent()
}
