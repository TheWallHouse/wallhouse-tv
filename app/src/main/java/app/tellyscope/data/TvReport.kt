// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * What CleanYourTV's report for this TV set in its launcher's config.json for the TV's own hardware: the Live TV app,
 * how its Inputs button opens the inputs, which remote button is Settings, the stock Home to send back and how the key
 * services treat the remote. Taste (dock, hidden apps, columns, wallpaper…) is the tester's and never comes over.
 * [name] is the report's TV, as the debloat list calls it.
 */
data class TvReport(val id: String, val name: String, val values: Map<String, Any>)

/** What a [TV_KEYS] entry may hold. Anything else in a report is dropped, so a typo can't reach the launcher. */
enum class TvKind {
    /** A package that must be on the TV, or the setting would point at nothing. */
    PACKAGE,
    /** `package/.Class` (or full name), its package on the TV. */
    COMPONENT,
    /** An intent action, or a URI scheme. */
    NAME,
    /** One `name=value` int extra. */
    EXTRA,
    BOOL,
    /** Android keycodes. */
    KEYCODES,
    /** `HWn=number` vendor source numbers. */
    SOURCES,
}

/** The launcher's hardware keys (WallHouse Launcher's Config.java reads each from the theme), with what each holds. */
val TV_KEYS: Map<String, TvKind> = linkedMapOf(
    "live_tv_package" to TvKind.PACKAGE,
    "live_tv_action" to TvKind.NAME,
    "input_action" to TvKind.NAME,
    "input_uri_scheme" to TvKind.NAME,
    "input_sources" to TvKind.SOURCES,
    "handle_view_inputs" to TvKind.BOOL,
    "coocaa_sources" to TvKind.BOOL,
    "source_panel_service" to TvKind.COMPONENT,
    "source_panel_action" to TvKind.NAME,
    "source_panel_extra" to TvKind.EXTRA,
    "settings_key_keycodes" to TvKind.KEYCODES,
    "settings_key_clear_task" to TvKind.BOOL,
    "stock_home_redirect" to TvKind.PACKAGE,
    "key_filter" to TvKind.BOOL,
    "consume_volume_keys" to TvKind.BOOL,
)

/** What each part of the TV a report's settings are for, in the order they're listed. */
enum class TvPart { LIVE_TV, INPUTS, SETTINGS_BUTTON, HOME, REMOTE }

fun tvPart(key: String): TvPart = when (key) {
    "live_tv_package", "live_tv_action" -> TvPart.LIVE_TV
    "settings_key_keycodes", "settings_key_clear_task" -> TvPart.SETTINGS_BUTTON
    "stock_home_redirect" -> TvPart.HOME
    "key_filter", "consume_volume_keys" -> TvPart.REMOTE
    else -> TvPart.INPUTS
}

private val NAME = Regex("[A-Za-z][A-Za-z0-9_.+-]{0,127}")
private val EXTRA = Regex("[A-Za-z][A-Za-z0-9_]{0,63}=-?[0-9]{1,9}")
private val SOURCE = Regex("[A-Za-z0-9_]{1,16}=[0-9]{1,4}")
private val COMPONENT = Regex("([A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+)/\\.?[A-Za-z][A-Za-z0-9_$]*(\\.[A-Za-z0-9_$]+)*")

/** [v] as [kind] holds it, or null when it doesn't fit: a list of ints or strings, a Boolean or a String. */
private fun valid(kind: TvKind, v: Any?): Any? = when (kind) {
    TvKind.PACKAGE -> (v as? String)?.takeIf(PACKAGE::matches)
    TvKind.COMPONENT -> (v as? String)?.takeIf(COMPONENT::matches)
    TvKind.NAME -> (v as? String)?.takeIf(NAME::matches)
    TvKind.EXTRA -> (v as? String)?.takeIf(EXTRA::matches)
    TvKind.BOOL -> v as? Boolean
    TvKind.KEYCODES -> (v as? JSONArray)?.let { a ->
        (0 until a.length()).map { a.opt(it) }.takeIf { l -> l.isNotEmpty() && l.all { it is Int && it in 1..999 } }?.map { it as Int }
    }
    TvKind.SOURCES -> (v as? JSONArray)?.let { a ->
        (0 until a.length()).map { a.opt(it) }.takeIf { l -> l.isNotEmpty() && l.all { it is String && SOURCE.matches(it) } }?.map { it as String }
    }
}

/** The [TV_KEYS] in a config.json, each only with a value of its kind; nothing for text that isn't a JSON object. */
fun tvValues(config: String?): Map<String, Any> {
    val o = try {
        config?.trim()?.takeIf { it.startsWith("{") }?.let(::JSONObject)
    } catch (_: JSONException) {
        null
    } ?: return emptyMap()
    return buildMap { for ((k, kind) in TV_KEYS) valid(kind, o.opt(k))?.let { put(k, it) } }
}

/** The package a value names, for the kinds that must be on the TV; null for the others. */
fun tvPackage(key: String, value: Any): String? = when (TV_KEYS[key]) {
    TvKind.PACKAGE -> value as? String
    TvKind.COMPONENT -> (value as? String)?.substringBefore('/')
    else -> null
}

/**
 * What applying [report] would change on this TV: its values that [theme] (the launcher's config.json now) doesn't
 * already hold, leaving out those naming an app [installed] lacks, since they would point at nothing here. Empty when
 * the TV already has them all.
 */
fun tvChanges(report: Map<String, Any>, theme: Map<String, Any>, installed: Set<String>): Map<String, Any> =
    report.filter { (k, v) -> theme[k] != v && tvPackage(k, v)?.let { it in installed } != false }
