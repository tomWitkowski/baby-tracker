package com.babytracker.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babytracker.data.db.entity.BabyEvent
import com.babytracker.data.db.entity.DiaperSubType
import com.babytracker.data.db.entity.FeedingSubType
import com.babytracker.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    val recentEvents: StateFlow<List<BabyEvent>> = repository.getRecentEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logBottleFeeding(milliliters: Int?) {
        viewModelScope.launch {
            repository.logFeeding(FeedingSubType.BOTTLE, milliliters)
        }
    }

    fun logNaturalFeeding() {
        viewModelScope.launch {
            repository.logFeeding(FeedingSubType.NATURAL)
        }
    }

    fun logDiaper(subType: DiaperSubType) {
        viewModelScope.launch {
            repository.logDiaper(subType)
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
}
