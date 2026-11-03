// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope

import app.tellyscope.data.launcherSettingsJson
import app.tellyscope.data.parseLauncherSettings
import app.tellyscope.data.themeWithDock
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** org.json is Android's, so this runs under Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LauncherSettingsTest {
    private val pkg = "io.github.thewallhouse.nothingtv"

    /** What the launcher's ThemeProvider answered on a Google TV Streamer, trimmed. */
    private val snapshot = """{"columns":5,"nothing_tiles":"mono","version":1204,"keys":["columns","nothing_tiles"],
        "effective":{"columns":5,"rows_below_dock":1,"nothing_tiles":"mono","animations":true,"dock_apps":["com.netflix.ninja","not a package"]}}"""

    @Test fun aSnapshotReadsAsChosenAndEffective() {
        val s = parseLauncherSettings(pkg, snapshot)
        assertTrue(s.supported)
        assertEquals(1204, s.version)
        assertEquals(mapOf("columns" to 5, "nothing_tiles" to "mono"), s.chosen)
        assertEquals(1, s.int("rows_below_dock"))
        assertEquals(true, s.bool("animations"))
        assertEquals(listOf("com.netflix.ninja"), s.dock) // a bad entry is dropped
    }

    @Test fun anOldLauncherOrAnErrorIsUnsupported() {
        assertFalse(parseLauncherSettings(pkg, "Error while accessing provider").supported)
        assertFalse(parseLauncherSettings(pkg, "").supported)
        assertFalse(parseLauncherSettings(pkg, """{"columns":5}""").supported) // a config.json, not a snapshot
    }

    @Test fun aNullPutsTheThemesValueBack() {
        val o = JSONObject(launcherSettingsJson(mapOf("columns" to 6, "nothing_greeting_name" to null)))
        assertEquals(6, o.getInt("columns"))
        assertTrue(o.isNull("nothing_greeting_name"))
    }

    @Test fun theDockGoesIntoTheThemeKeepingTheRest() {
        val o = JSONObject(themeWithDock("""{"stock_home_redirect":"com.google.android.apps.tv.launcherx"}""", listOf("a.b", "a.b", "bad", "c.d")))
        assertEquals("com.google.android.apps.tv.launcherx", o.getString("stock_home_redirect"))
        assertEquals(listOf("a.b", "c.d"), (0 until o.getJSONArray("dock_apps").length()).map { o.getJSONArray("dock_apps").getString(it) })
        assertEquals(1, JSONObject(themeWithDock("not json", listOf("a.b"))).getJSONArray("dock_apps").length())
    }
}
