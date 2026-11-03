// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.ui

import androidx.compose.foundation.layout.Arrangement
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
    val dock = s.dock
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
