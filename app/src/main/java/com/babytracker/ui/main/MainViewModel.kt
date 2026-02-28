package com.babytracker.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babytracker.billing.BillingManager
import com.babytracker.data.db.entity.BabyEvent
import com.babytracker.data.db.entity.DiaperSubType
import com.babytracker.data.db.entity.FeedingSubType
import com.babytracker.data.notifications.FeedingReminderScheduler
import com.babytracker.data.preferences.AppPreferences
import com.babytracker.data.repository.EventRepository
import com.babytracker.data.sync.SyncManager
import com.babytracker.data.sync.SyncState
import com.babytracker.data.sync.TrustRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: EventRepository,
    private val syncManager: SyncManager,
    private val appPreferences: AppPreferences,
    private val reminderScheduler: FeedingReminderScheduler,
    val billingManager: BillingManager
) : ViewModel() {

    val recentEvents: StateFlow<List<BabyEvent>> = repository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncState: StateFlow<SyncState> = syncManager.syncState
    val pendingTrustRequest: StateFlow<TrustRequest?> = syncManager.pendingTrustRequest

    val babyName: StateFlow<String> = appPreferences.babyName

    val showBottle get() = appPreferences.showBottle
    val showBottleFormula get() = appPreferences.showBottleFormula
    val showBottleExpressed get() = appPreferences.showBottleExpressed
    val showBreast get() = appPreferences.showBreast
    val showPump get() = appPreferences.showPump
    val showSpitUp get() = appPreferences.showSpitUp

    fun logBottleFeeding(subType: FeedingSubType = FeedingSubType.BOTTLE, milliliters: Int?) {
        viewModelScope.launch {
            repository.logFeeding(subType, milliliters)
            reminderScheduler.scheduleIfEnabled()
        }
    }

    fun logBreastFeeding(subType: FeedingSubType) {
        viewModelScope.launch {
            repository.logFeeding(subType)
            reminderScheduler.scheduleIfEnabled()
        }
    }

    fun logPump(subType: FeedingSubType, milliliters: Int? = null) {
        viewModelScope.launch {
            repository.logFeeding(subType, milliliters)
            reminderScheduler.scheduleIfEnabled()
        }
    }

    fun logDiaper(subType: DiaperSubType) {
        viewModelScope.launch {
            repository.logDiaper(subType)
        }
    }

    fun logSpitUp() {
        viewModelScope.launch {
            repository.logSpitUp()
        }
    }

    fun deleteEvent(event: BabyEvent) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }

    fun updateEvent(event: BabyEvent) {
        viewModelScope.launch {
            repository.updateEvent(event)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncManager.syncNow()
        }
    }

    fun approveTrust(permanent: Boolean) {
        viewModelScope.launch { syncManager.approveTrust(permanent) }
    }

    fun denyTrust() = syncManager.denyTrust()

    /** True when user has an active Play Store subscription OR is within trial period. */
    val isProSubscription: StateFlow<Boolean> = billingManager.isPro
        .stateIn(viewModelScope, SharingStarted.Eagerly, appPreferences.isPro)

    fun isProOrTrial(): Boolean = billingManager.isPro.value || appPreferences.isProOrTrial()
    fun hasTrialStarted() = appPreferences.hasTrialStarted()
    fun startTrial() { appPreferences.startTrial() }

    fun refreshPurchases() {
        viewModelScope.launch { billingManager.refreshPurchases() }
    }
}
