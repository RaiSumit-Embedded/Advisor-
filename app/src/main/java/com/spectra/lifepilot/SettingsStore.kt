package com.spectra.lifepilot

import android.content.Context

class SettingsStore(context: Context) {
    private val sp = context.getSharedPreferences("lifepilot_settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = sp.getString("apiKey", "") ?: ""
        set(v) { sp.edit().putString("apiKey", v.trim()).apply() }

    // Free Google AI Studio (Gemini) model by default
    var model: String
        get() = sp.getString("model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        set(v) { sp.edit().putString("model", v.trim()).apply() }
}
