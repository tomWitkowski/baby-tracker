package com.babytracker.ui.dashboard

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

enum class DashboardViewMode { DAY, WEEK, TIMELINE }

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    // ── Day view ─────────────────────────────────────────────────────────────

    private val _selectedDate = MutableStateFlow(todayMidnight())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    val dayEvents: StateFlow<List<BabyEvent>> = _selectedDate
        .flatMapLatest { date -> repository.getEventsForDay(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dayStats = MutableStateFlow<DayStats?>(null)
    val dayStats: StateFlow<DayStats?> = _dayStats.asStateFlow()

    // ── Week view ─────────────────────────────────────────────────────────────

    private val _viewMode = MutableStateFlow(DashboardViewMode.DAY)
    val viewMode: StateFlow<DashboardViewMode> = _viewMode.asStateFlow()

    private val _selectedWeekStart = MutableStateFlow(currentWeekStart())
    val selectedWeekStart: StateFlow<Long> = _selectedWeekStart.asStateFlow()

    val weeklyStats: StateFlow<List<DayStats>?> = combine(
        _selectedWeekStart,
        repository.getAllEvents()
    ) { weekStart, _ -> weekStart }
        .flatMapLatest { weekStart ->
            flow { emit(repository.getWeekStats(weekStart)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            dayEvents.collect {
                _dayStats.value = repository.getDayStats(_selectedDate.value)
            }
        }
    }

    // ── Day navigation ────────────────────────────────────────────────────────

    fun selectDate(timestamp: Long) { _selectedDate.value = timestamp }

    fun goToPreviousDay() { _selectedDate.value = _selectedDate.value - 86_400_000L }

    fun goToNextDay() {
        val next = _selectedDate.value + 86_400_000L
        if (next <= todayMidnight()) _selectedDate.value = next
    }

    fun isToday(): Boolean = _selectedDate.value == todayMidnight()

    // ── Week navigation ───────────────────────────────────────────────────────

    fun setViewMode(mode: DashboardViewMode) { _viewMode.value = mode }

    fun goToPreviousWeek() { _selectedWeekStart.value -= 7 * 86_400_000L }

    fun goToNextWeek() {
        val next = _selectedWeekStart.value + 7 * 86_400_000L
        if (next <= currentWeekStart()) _selectedWeekStart.value = next
    }

    fun isCurrentWeek(): Boolean = _selectedWeekStart.value == currentWeekStart()

    // ── Event operations ──────────────────────────────────────────────────────

    fun deleteEvent(event: BabyEvent) { viewModelScope.launch { repository.deleteEvent(event) } }
    fun updateEvent(event: BabyEvent) { viewModelScope.launch { repository.updateEvent(event) } }

    suspend fun exportToCsv(): String {
        val events = repository.getAllEventsForExport()
        val sb = StringBuilder()
        sb.appendLine("Data i godzina,Typ,Podtyp,Mililitry,Notatka")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        events.forEach { event ->
            val time = sdf.format(Date(event.timestamp))
            val type = when (event.eventType) {
                EventType.FEEDING.name -> "Karmienie"
                EventType.DIAPER.name -> "Pieluszka"
                EventType.SPIT_UP.name -> "Zwrócenie"
                else -> event.eventType
            }
            val subType = when (event.subType) {
                FeedingSubType.BOTTLE.name -> "Butelka"
                FeedingSubType.BOTTLE_FORMULA.name -> "Mleko modyfikowane"
                FeedingSubType.BOTTLE_EXPRESSED.name -> "Odciągnięte mleko"
                FeedingSubType.BREAST_LEFT.name -> "Lewa pierś"
                FeedingSubType.BREAST_RIGHT.name -> "Prawa pierś"
                FeedingSubType.BREAST_BOTH_LR.name -> "Lewa+Prawa"
                FeedingSubType.BREAST_BOTH_RL.name -> "Prawa+Lewa"
                FeedingSubType.PUMP.name -> "Laktator"
                FeedingSubType.PUMP_LEFT.name -> "Laktator lewa"
                FeedingSubType.PUMP_RIGHT.name -> "Laktator prawa"
                FeedingSubType.PUMP_BOTH_LR.name -> "Laktator L+P"
                FeedingSubType.PUMP_BOTH_RL.name -> "Laktator P+L"
                FeedingSubType.NATURAL.name -> "Karmienie piersią"
                DiaperSubType.PEE.name -> "Siku"
                DiaperSubType.POOP.name -> "Kupka"
                DiaperSubType.MIXED.name -> "Mieszane"
                EventType.SPIT_UP.name -> "Zwrócenie"
                else -> event.subType
            }
            sb.appendLine("$time,$type,$subType,${event.milliliters ?: ""},${event.note ?: ""}")
        }
        return sb.toString()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun todayMidnight(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun currentWeekStart(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysToMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
            add(Calendar.DAY_OF_MONTH, -daysToMonday)
        }
        return cal.timeInMillis
    }
}
