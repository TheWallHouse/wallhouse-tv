// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.tellyscope.R
import app.tellyscope.data.AppInfo
import app.tellyscope.data.LauncherSettings
import app.tellyscope.data.LauncherSettings.Keys as K
import app.tellyscope.data.SharedLists

/** [DeviceUi.busy] while a launcher change is written. */
const val LAUNCHER_SAVING = "launcher"

/**
 * The launcher this app can set up on the TV: one from the WallHouse catalogue (it pins a download) that is
 * installed and on. Its settings then say whether it answers ([LauncherSettings.supported]).
 */
internal fun configurableLauncher(apps: List<AppInfo>): String? =
    SharedLists.launchers.filter { it.direct != null }.map { it.pkg }
        .firstOrNull { pkg -> apps.any { it.packageName == pkg && it.enabled } }

/**
 * The catalogue launcher's own settings, the ones its Launcher settings on the TV has, written to the TV as each is
 * changed: apps style, your name, apps per row, rows below the dock, the dock's background, the clock, animations
 * and the dock's apps. A launcher too old to answer offers its update instead.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LauncherBlock(ui: DeviceUi, actions: DeviceActions) {
    val s = ui.launcherSettings ?: return
    val name = ui.apps.firstOrNull { it.packageName == s.pkg }?.label ?: launcherName(s.pkg)
    val saving = ui.busy == LAUNCHER_SAVING
    val idle = ui.busy == null
    if (saving) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (!s.supported) {
        Muted(stringResource(R.string.launcher_too_old, name))
        FilledTonalButton(onClick = actions.installLauncher, enabled = idle) { Text(stringResource(R.string.launcher_update)) }
        return
    }
    Choices(
        stringResource(R.string.launcher_apps_style),
        K.TILE_CHOICES, s.string(K.TILES), idle,
        label = {
            stringResource(
                when (it) {
                    "mono_red" -> R.string.launcher_style_mono_red
                    "mono" -> R.string.launcher_style_mono
                    else -> R.string.launcher_style_colour
                },
            )
        },
    ) { actions.launcherChoice(K.TILES, it) }
    NameRow(s.string(K.NAME).orEmpty(), idle) { actions.launcherChoice(K.NAME, it) }
    Choices(stringResource(R.string.launcher_columns), K.COLUMN_CHOICES, s.int(K.COLUMNS), idle, label = { "$it" }) {
        actions.launcherChoice(K.COLUMNS, it)
    }
    Choices(stringResource(R.string.launcher_rows), K.ROW_CHOICES, s.int(K.ROWS), idle, label = { "$it" }) {
        actions.launcherChoice(K.ROWS, it)
    }
    Choices(stringResource(R.string.launcher_dock_opacity), K.OPACITY_CHOICES, s.int(K.DOCK_OPACITY), idle, label = { "$it %" }) {
        actions.launcherChoice(K.DOCK_OPACITY, it)
    }
    Choices(
        stringResource(R.string.launcher_clock), K.CLOCK_CHOICES, s.string(K.CLOCK), idle,
        label = { if (it == "system") stringResource(R.string.launcher_clock_system) else "$it h" },
    ) { actions.launcherChoice(K.CLOCK, it) }
    // Only launchers that know the setting (1205+) list it among their keys.
    if (K.LOCALE in s.keys) {
        val tv = stringResource(R.string.launcher_language_tv)
        Choices(
            stringResource(R.string.launcher_language), K.LOCALE_CHOICES, s.chosen[K.LOCALE] as? String ?: "", idle,
            label = { app.tellyscope.data.AppLanguage.name(it, tv) },
        ) { actions.launcherChoice(K.LOCALE, it.ifEmpty { null }) }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.launcher_animations), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = s.bool(K.ANIMATIONS) != false, onCheckedChange = { actions.launcherChoice(K.ANIMATIONS, it) }, enabled = idle)
    }
    DockEditor(s, ui.apps, idle, actions.launcherDock)
    if ("font_file" in s.effective) FontPicker(s.fontName, idle, actions.launcherFont)
    ThemeOptions(s, ui.apps, idle, actions)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Choices(title: String, values: List<T>, current: T?, enabled: Boolean, label: @Composable (T) -> String, onPick: (T) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (v in values) {
                FilterChip(selected = v == current, onClick = { if (v != current) onPick(v) }, label = { Text(label(v)) }, enabled = enabled)
            }
        }
    }
}

@Composable
private fun NameRow(current: String, enabled: Boolean, onSave: (String?) -> Unit) {
    var text by rememberSaveable(current) { mutableStateOf(current) }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.launcher_name), style = MaterialTheme.typography.bodyLarge)
        Muted(stringResource(R.string.launcher_name_help))
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= K.NAME_MAX) text = it.replace('\n', ' ') },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { onSave(text.trim()) }, enabled = enabled && text.trim() != current) { Text(stringResource(R.string.launcher_save)) }
            if (current.isNotEmpty()) TextButton(onClick = { onSave("") }, enabled = enabled) { Text(stringResource(R.string.launcher_name_remove)) }
        }
    }
}

/** The dock's apps in order: up, down and out on each, then Add from the TV's other apps, up to [K.DOCK_MAX]. */
@Composable
private fun DockEditor(s: LauncherSettings, apps: List<AppInfo>, enabled: Boolean, onDock: (List<String>) -> Unit) {
    // Only what is on the TV: the default dock names apps it may not have (Hotstar), which the launcher skips anyway.
    // A change writes the dock as shown, so those drop out of the theme too.
    val dock = if (apps.isEmpty()) s.dock else s.dock.filter { pkg -> apps.any { it.packageName == pkg && it.enabled } }
    fun label(pkg: String) = apps.firstOrNull { it.packageName == pkg }?.label ?: pkg
    var adding by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(R.string.launcher_dock), style = MaterialTheme.typography.bodyLarge)
        if (dock.isEmpty()) Muted(stringResource(R.string.launcher_dock_empty))
        dock.forEachIndexed { i, pkg ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}. ${label(pkg)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { onDock(dock.toMutableList().apply { add(i - 1, removeAt(i)) }) }, enabled = enabled && i > 0) { Text("↑") }
                TextButton(onClick = { onDock(dock.toMutableList().apply { add(i + 1, removeAt(i)) }) }, enabled = enabled && i < dock.lastIndex) { Text("↓") }
                TextButton(onClick = { onDock(dock - pkg) }, enabled = enabled) { Text(stringResource(R.string.launcher_dock_remove)) }
            }
        }
        val others = apps.filter { it.launchable && it.enabled && it.packageName !in dock && it.packageName != s.pkg }.sortedBy { it.label.lowercase() }
        Row {
            FilledTonalButton(onClick = { adding = true }, enabled = enabled && dock.size < K.DOCK_MAX && others.isNotEmpty()) {
                Text(stringResource(R.string.launcher_dock_add))
            }
            DropdownMenu(expanded = adding, onDismissRequest = { adding = false }) {
                for (a in others) DropdownMenuItem(text = { Text(a.label) }, onClick = { adding = false; onDock(dock + a.packageName) })
            }
        }
        if (dock.size >= K.DOCK_MAX) Muted(stringResource(R.string.launcher_dock_full, K.DOCK_MAX))
    }
}

/** Turn off debugging, with what it means first: this phone loses the TV until it is turned on again. */
@Composable
fun DebuggingOffRow(enabled: Boolean, onConfirm: () -> Unit) {
    var ask by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.debugging_off_title), style = MaterialTheme.typography.bodyLarge)
        Muted(stringResource(R.string.debugging_off_text))
        FilledTonalButton(onClick = { ask = true }, enabled = enabled) { Text(stringResource(R.string.debugging_off_button)) }
    }
    if (ask) {
        AlertDialog(
            onDismissRequest = { ask = false },
            title = { Text(stringResource(R.string.debugging_off_confirm_title)) },
            text = { Text(stringResource(R.string.debugging_off_confirm_text)) },
            confirmButton = { Button(onClick = { ask = false; onConfirm() }) { Text(stringResource(R.string.debugging_off_button)) } },
            dismissButton = { TextButton(onClick = { ask = false }) { Text(stringResource(R.string.screen_cancel)) } },
        )
    }
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * The launcher's theme options a phone can change too (launcher 1206+ says how Home has them): the greeting, the date,
 * app names, the dock's look, the idle clock, the app opened at start-up, the remote's colour buttons, hidden apps, and
 * a repair for remote buttons that stopped answering. Each shows only where the launcher reports it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeOptions(s: LauncherSettings, apps: List<AppInfo>, enabled: Boolean, actions: DeviceActions) {
    val launchable = apps.filter { it.launchable && it.enabled && it.packageName != s.pkg }.sortedBy { it.label.lowercase() }
    fun label(pkg: String?) = pkg?.let { p -> apps.firstOrNull { it.packageName == p }?.label ?: p }
    for ((key, title) in listOf(
        "show_greeting" to R.string.launcher_show_greeting,
        "date_below_clock" to R.string.launcher_date_below,
        "always_show_names" to R.string.launcher_always_names,
        "dock_glass" to R.string.launcher_dock_glass,
        "dock_band" to R.string.launcher_dock_band,
    )) {
        val on = s.bool(key) ?: continue
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = on, onCheckedChange = { actions.launcherTheme(key, it) }, enabled = enabled)
        }
    }
    s.int("home_idle_clock_minutes")?.let { minutes ->
        val off = stringResource(R.string.launcher_idle_off)
        Choices(stringResource(R.string.launcher_idle_clock), listOf(0, 5, 10, 30), minutes, enabled, label = { if (it == 0) off else "$it min" }) {
            actions.launcherTheme("home_idle_clock_minutes", it)
        }
    }
    if (OPEN_ON_START in s.effective) {
        AppPicker(
            stringResource(R.string.launcher_open_on_start), label(s.string(OPEN_ON_START)?.ifEmpty { null }),
            stringResource(R.string.launcher_nothing), launchable, enabled,
        ) { actions.launcherChoice(OPEN_ON_START, it) }
    }
    s.appKeys?.let { keys ->
        Text(stringResource(R.string.launcher_colour_buttons), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
        Muted(stringResource(R.string.launcher_colour_buttons_help))
        val names = listOf(R.string.launcher_red, R.string.launcher_green, R.string.launcher_yellow, R.string.launcher_blue)
        app.tellyscope.data.COLOUR_KEYS.forEachIndexed { i, button ->
            AppPicker(
                stringResource(names[i]), label(app.tellyscope.data.appKeyFor(keys, button)),
                stringResource(R.string.launcher_tv_default), launchable, enabled,
            ) { actions.launcherTheme("app_keys", app.tellyscope.data.appKeysWith(keys, button, it)) }
        }
    }
    s.hidden?.let { hidden -> HiddenApps(hidden, launchable, enabled) { actions.launcherTheme("hidden_apps", it) } }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.launcher_repair), style = MaterialTheme.typography.bodyLarge)
        Muted(stringResource(R.string.launcher_repair_help))
        FilledTonalButton(onClick = actions.repairRemote, enabled = enabled) { Text(stringResource(R.string.launcher_repair_button)) }
    }
}

private const val OPEN_ON_START = "open_on_start"

/** [title]: [current] app, or [none]; a tap lists [apps] with [none] first. */
@Composable
private fun AppPicker(title: String, current: String?, none: String, apps: List<AppInfo>, enabled: Boolean, onPick: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Muted(current ?: none)
        }
        Box {
            TextButton(onClick = { open = true }, enabled = enabled) { Text(stringResource(R.string.launcher_change)) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(text = { Text(none) }, onClick = { open = false; onPick(null) })
                for (a in apps) DropdownMenuItem(text = { Text(a.label) }, onClick = { open = false; onPick(a.packageName) })
            }
        }
    }
}

/** Hidden apps: how many, and a list to tick them in. */
@Composable
private fun HiddenApps(hidden: List<String>, apps: List<AppInfo>, enabled: Boolean, onSave: (List<String>) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.launcher_hidden), style = MaterialTheme.typography.bodyLarge)
        Muted(pluralStringResource(R.plurals.launcher_hidden_count, hidden.size, hidden.size))
        FilledTonalButton(onClick = { editing = true }, enabled = enabled && apps.isNotEmpty()) { Text(stringResource(R.string.launcher_hidden_choose)) }
    }
    if (editing) {
        val picked = remember { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(hidden) } }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(R.string.launcher_hidden)) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn {
                    items(apps.size) { i ->
                        val a = apps[i]
                        Row(
                            Modifier.fillMaxWidth().clickable { if (a.packageName in picked) picked.remove(a.packageName) else picked.add(a.packageName) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.Checkbox(checked = a.packageName in picked, onCheckedChange = null)
                            Text(a.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { editing = false; onSave(picked.toList()) }) { Text(stringResource(R.string.launcher_save)) } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(R.string.screen_cancel)) } },
        )
    }
}

/** The launcher's font: its own Doto, or one of [app.tellyscope.data.GOOGLE_FONTS], downloaded and sent to the TV. */
@Composable
private fun FontPicker(current: String?, enabled: Boolean, onPick: (app.tellyscope.data.GoogleFont?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val own = stringResource(R.string.launcher_font_own)
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.launcher_font), style = MaterialTheme.typography.bodyLarge)
            Muted(current ?: own)
        }
        Box {
            TextButton(onClick = { open = true }, enabled = enabled) { Text(stringResource(R.string.launcher_change)) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(text = { Text(own) }, onClick = { open = false; if (current != null) onPick(null) })
                for (f in app.tellyscope.data.GOOGLE_FONTS) DropdownMenuItem(text = { Text(f.name) }, onClick = { open = false; if (f.name != current) onPick(f) })
            }
        }
    }
    Muted(stringResource(R.string.launcher_font_help))
}
