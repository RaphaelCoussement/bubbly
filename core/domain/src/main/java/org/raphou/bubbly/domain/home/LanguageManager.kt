package org.raphou.bubbly.domain.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LanguageManager {
    private const val PREFS_NAME = "app_prefs"
    private const val LANGUAGE_KEY = "language"

    fun detectDefaultLanguage(): String {
        val locale = Locale.getDefault()
        return if (locale.language == "fr") "fr" else "en"
    }

    // langue sauvegardée
    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(LANGUAGE_KEY, "auto") ?: "auto"
    }

    fun saveLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(LANGUAGE_KEY, language).apply()
    }

    fun applyLanguage(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        }

        return context.createConfigurationContext(config)
    }

    // on redémarre l'appli pour appliquer le changement de langue
    fun restartActivity(activity: Activity) {
        val intent = Intent(activity, activity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.startActivity(intent)
        activity.finish()
    }
}
