package com.spectra.lifepilot

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

/** One day's summary. */
data class DayStat(
    val date: LocalDate,
    val steps: Long,
    val sleepMinutes: Long,
)

/**
 * Saves everything on-device in SharedPreferences as JSON.
 * records = { "2026-08-25": { "steps": 1234, "sleep": 420 }, ... }
 */
class DayStore(context: Context) {
    private val sp = context.getSharedPreferences("lifepilot", Context.MODE_PRIVATE)

    private fun records(): JSONObject = JSONObject(sp.getString("records", "{}") ?: "{}")
    private fun persist(o: JSONObject) = sp.edit().putString("records", o.toString()).apply()

    fun setSteps(date: LocalDate, steps: Long) {
        val o = records()
        val day = o.optJSONObject(date.toString()) ?: JSONObject()
        day.put("steps", steps)
        o.put(date.toString(), day)
        persist(o)
    }

    fun setSleep(date: LocalDate, minutes: Long) {
        val o = records()
        val day = o.optJSONObject(date.toString()) ?: JSONObject()
        day.put("sleep", minutes)
        o.put(date.toString(), day)
        persist(o)
    }

    fun last7Days(): List<DayStat> {
        val o = records()
        val today = LocalDate.now()
        return (6 downTo 0).map { i ->
            val d = today.minusDays(i.toLong())
            val day = o.optJSONObject(d.toString())
            DayStat(d, day?.optLong("steps") ?: 0L, day?.optLong("sleep") ?: 0L)
        }
    }

    // --- step-counter baseline (hardware counter is cumulative since boot) ---
    var baselineDate: String
        get() = sp.getString("baseDate", "") ?: ""
        set(v) { sp.edit().putString("baseDate", v).apply() }
    var baselineCount: Long
        get() = sp.getLong("baseCount", -1L)
        set(v) { sp.edit().putLong("baseCount", v).apply() }
}
