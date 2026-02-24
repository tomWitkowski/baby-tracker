package com.babytracker.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babytracker.data.db.entity.BabyEvent
import com.babytracker.data.db.entity.DiaperSubType
import com.babytracker.data.db.entity.FeedingSubType
import com.babytracker.data.preferences.AppPreferences
import com.babytracker.data.repository.EventRepository
import com.babytracker.data.sync.SyncManager
import com.babytracker.data.sync.SyncState
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
    private val appPreferences: AppPreferences
) : ViewModel() {

    val recentEvents: StateFlow<List<BabyEvent>> = repository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncState: StateFlow<SyncState> = syncManager.syncState

    val babyName: StateFlow<String> = appPreferences.babyName

    init {
        syncManager.start()
    }

    fun logBottleFeeding(milliliters: Int?) {
        viewModelScope.launch {
            repository.logFeeding(FeedingSubType.BOTTLE, milliliters)
        }
    }

    fun logBreastFeeding(subType: FeedingSubType) {
        viewModelScope.launch {
            repository.logFeeding(subType)
        }
    }

    fun logPump(subType: FeedingSubType, milliliters: Int? = null) {
        viewModelScope.launch {
            repository.logFeeding(subType, milliliters)
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
}
