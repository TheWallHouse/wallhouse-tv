// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * A CleanYourTV launcher's own settings, as its theme provider gives them (`settings.json`, launcher 1204+): the
 * choices made in its Launcher settings and what Home uses now ([effective]), plus the dock, which lives in the theme
 * (`config.json` `dock_apps`). [supported] is false for a launcher too old to answer, which then only takes a theme.
 */
data class LauncherSettings(
    val pkg: String,
    val supported: Boolean,
    val version: Int? = null,
    /** What Home shows now, by settings.json key; the theme's value where nothing was chosen on the TV. */
    val effective: Map<String, Any> = emptyMap(),
    /** The choices made on the TV (or from here), by key: a theme push resets them, so they are written back after one. */
    val chosen: Map<String, Any> = emptyMap(),
    val dock: List<String> = emptyList(),
) {
    fun int(key: String) = (effective[key] as? Number)?.toInt()
    fun bool(key: String) = effective[key] as? Boolean
    fun string(key: String) = effective[key] as? String

    companion object Keys {
        const val COLUMNS = "columns"
        const val ROWS = "rows_below_dock"
        const val DOCK_OPACITY = "dock_opacity"
        const val CLOCK = "clock_format"
        const val TILES = "nothing_tiles"
        const val NAME = "nothing_greeting_name"
        const val ANIMATIONS = "animations"

        /** The values each choice may take, as the launcher's own Launcher settings offers them. */
        val COLUMN_CHOICES = listOf(4, 5, 6, 7)
        val ROW_CHOICES = listOf(0, 1, 2, 3)
        val OPACITY_CHOICES = listOf(100, 70, 50, 30)
        val CLOCK_CHOICES = listOf("system", "12", "24")
        val TILE_CHOICES = listOf("mono_red", "mono", "off")
        const val NAME_MAX = 40
        /** The dock row holds this many at most, whatever the columns. */
        const val DOCK_MAX = 7
    }
}

/** [text] from `content read` of settings.json; not supported when it isn't a settings snapshot. */
fun parseLauncherSettings(pkg: String, text: String?): LauncherSettings {
    val o = text?.trim()?.takeIf { it.startsWith("{") }?.let { runCatching { JSONObject(it) }.getOrNull() }
        ?.takeIf { it.has("keys") } ?: return LauncherSettings(pkg, supported = false)
    val now = o.optJSONObject("effective") ?: JSONObject()
    val effective = buildMap {
        for (k in now.keys()) {
            when (val v = now.opt(k)) {
                is Number, is Boolean, is String -> put(k, v)
            }
        }
    }
    val chosen = buildMap {
        for (k in o.keys()) {
            if (k == "version" || k == "keys" || k == "effective") continue
            when (val v = o.opt(k)) {
                is Number, is Boolean, is String -> put(k, v)
            }
        }
    }
    val dock = now.optJSONArray("dock_apps")?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).takeIf(PACKAGE::matches) } }.orEmpty()
    return LauncherSettings(pkg, supported = true, version = o.optInt("version").takeIf { it > 0 }, effective = effective, chosen = chosen, dock = dock)
}

/** settings.json to write: [values] by key, a null removing the choice so the theme decides again. */
fun launcherSettingsJson(values: Map<String, Any?>): String = JSONObject().apply {
    for ((k, v) in values) put(k, v ?: JSONObject.NULL)
}.toString()

/** [config] (the theme's config.json, or anything unreadable as none) with [dock] as its dock_apps. */
fun themeWithDock(config: String?, dock: List<String>): String {
    val json = try {
        config?.trim()?.takeIf { it.startsWith("{") }?.let(::JSONObject) ?: JSONObject()
    } catch (_: JSONException) {
        JSONObject()
    }
    json.put("dock_apps", JSONArray(dock.filter(PACKAGE::matches).distinct()))
    return json.toString(2)
}
