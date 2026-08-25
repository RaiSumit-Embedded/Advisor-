package com.spectra.lifepilot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val store = DayStore(app)

    private val _days = MutableStateFlow(store.last7Days())
    val days: StateFlow<List<DayStat>> = _days.asStateFlow()

    /** Called with cumulative-since-boot steps from the sensor. */
    fun onCumulativeSteps(total: Long) {
        val today = LocalDate.now()
        val todayKey = today.toString()
        // New day, first-ever read, or reboot (counter reset) -> reset baseline
        if (store.baselineDate != todayKey || store.baselineCount < 0 || total < store.baselineCount) {
            store.baselineDate = todayKey
            store.baselineCount = total
        }
        val todaySteps = (total - store.baselineCount).coerceAtLeast(0)
        store.setSteps(today, todaySteps)
        _days.value = store.last7Days()
    }

    fun logSleep(minutes: Long) {
        store.setSleep(LocalDate.now(), minutes)
        _days.value = store.last7Days()
    }
}
