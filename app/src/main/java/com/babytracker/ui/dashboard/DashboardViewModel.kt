package com.babytracker.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babytracker.data.db.entity.BabyEvent
import com.babytracker.data.db.entity.DiaperSubType
import com.babytracker.data.db.entity.EventType
import com.babytracker.data.db.entity.FeedingSubType
import com.babytracker.data.repository.DayStats
import com.babytracker.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(todayMidnight())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    val dayEvents: StateFlow<List<BabyEvent>> = _selectedDate
        .flatMapLatest { date -> repository.getEventsForDay(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dayStats = MutableStateFlow<DayStats?>(null)
    val dayStats: StateFlow<DayStats?> = _dayStats.asStateFlow()

    init {
        // Recompute stats whenever day events change (which also triggers on date change)
        viewModelScope.launch {
            dayEvents.collect {
                _dayStats.value = repository.getDayStats(_selectedDate.value)
            }
        }
    }

    fun selectDate(timestamp: Long) {
        _selectedDate.value = timestamp
    }

    fun goToPreviousDay() {
        _selectedDate.value = _selectedDate.value - 86_400_000L
    }

    fun goToNextDay() {
        val next = _selectedDate.value + 86_400_000L
        if (next <= todayMidnight()) {
            _selectedDate.value = next
        }
    }

    fun isToday(): Boolean = _selectedDate.value == todayMidnight()

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

    suspend fun exportToCsv(context: Context): String {
        val events = repository.getAllEventsForExport()
        val sb = StringBuilder()
        sb.appendLine("Data i godzina,Typ,Podtyp,Mililitry,Notatka")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        events.forEach { event ->
            val time = sdf.format(Date(event.timestamp))
            val type = when (event.eventType) {
                EventType.FEEDING.name -> "Karmienie"
                EventType.DIAPER.name -> "Pieluszka"
                else -> event.eventType
            }
            val subType = when {
                event.subType == FeedingSubType.BOTTLE.name -> "Butelka"
                event.subType == FeedingSubType.NATURAL.name -> "Naturalne"
                event.subType == DiaperSubType.PEE.name -> "Siku"
                event.subType == DiaperSubType.POOP.name -> "Kupka"
                event.subType == DiaperSubType.MIXED.name -> "Mieszane"
                else -> event.subType
            }
            sb.appendLine("$time,$type,$subType,${event.milliliters ?: ""},${event.note ?: ""}")
        }
        return sb.toString()
    }

    private fun todayMidnight(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
