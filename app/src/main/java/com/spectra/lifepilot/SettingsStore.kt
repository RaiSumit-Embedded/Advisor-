package com.spectra.lifepilot

import android.content.Context

class SettingsStore(context: Context) {
    private val sp = context.getSharedPreferences("lifepilot_settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = sp.getString("apiKey", "") ?: ""
        set(v) { sp.edit().putString("apiKey", v.trim()).apply() }

    var model: String
        get() = sp.getString("model", "claude-sonnet-5") ?: "claude-sonnet-5"
        set(v) { sp.edit().putString("model", v.trim()).apply() }
}
