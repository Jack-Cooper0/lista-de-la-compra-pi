package org.insbaixcamp.utils

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Utilitat per a gestionar el canvi d'idioma de l'aplicació.
 */
fun cambiarIdioma(context: Context, languageCode: String) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            localeManager?.applicationLocales = LocaleList.forLanguageTags(languageCode)
        } else {
            val locale = Locale.forLanguageTag(languageCode)
            Locale.setDefault(locale)
            val resources = context.resources
            val config = resources.configuration
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
            (context as? Activity)?.recreate()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Canvia l'idioma de l'aplicació a l'espanyol ("es").
 */
fun cambiarIdiomaEspanol(context: Context) {
    cambiarIdioma(context, "es")
}

/**
 * Canvia l'idioma de l'aplicació a l'anglès ("en").
 */
fun cambiarIdiomaIngles(context: Context) {
    cambiarIdioma(context, "en")
}

/**
 * Canvia l'idioma de l'aplicació al català ("ca").
 */
fun cambiarIdiomaCatalan(context: Context) {
    cambiarIdioma(context, "ca")
}
