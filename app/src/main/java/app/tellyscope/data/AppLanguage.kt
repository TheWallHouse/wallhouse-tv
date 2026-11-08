// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.data

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * The app's own language, picked in Settings: the phone's, or one of [CHOICES]. Android 13+ keeps it as the per-app
 * language (LocaleManager, also shown in the phone's settings); older phones keep it here and [wrap] applies it.
 */
object AppLanguage {
    /** BCP 47 tags, in the order Settings lists them; an empty tag is the phone's language. */
    val CHOICES = listOf("", "es-ES", "en-US", "gl")

    private const val PREFS = "app_language"
    private const val KEY = "tag"

    fun current(ctx: Context): String = if (Build.VERSION.SDK_INT >= 33) {
        ctx.getSystemService(LocaleManager::class.java)?.applicationLocales?.takeUnless { it.isEmpty }?.get(0)?.toLanguageTag().orEmpty()
    } else {
        stored(ctx)
    }.let { tag -> CHOICES.firstOrNull { it.isNotEmpty() && Locale.forLanguageTag(it).language == Locale.forLanguageTag(tag).language } ?: "" }

    /** Sets [tag] (empty for the phone's). Below Android 13 the caller recreates the activity to show it. */
    fun set(ctx: Context, tag: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            ctx.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
        }
    }

    /** [base] in the stored language, for attachBaseContext below Android 13; [base] itself otherwise. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val tag = stored(base).ifEmpty { return base }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocales(LocaleList(locale)) }
        return base.createConfigurationContext(config)
    }

    private fun stored(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()

    /** The flag shown beside each choice, or null for the device's own language (shown with the device instead). */
    fun flag(tag: String): Int? = when (tag) {
        "es-ES" -> app.tellyscope.R.drawable.flag_es
        "en-US" -> app.tellyscope.R.drawable.flag_us
        "gl" -> app.tellyscope.R.drawable.flag_gl
        else -> null
    }

    /** How each choice is named, in its own language, as language pickers do. */
    fun name(tag: String, phone: String): String = when (tag) {
        "es-ES" -> "Castellano (España)"
        "en-US" -> "English (US)"
        "gl" -> "Galego"
        else -> phone
    }
}
