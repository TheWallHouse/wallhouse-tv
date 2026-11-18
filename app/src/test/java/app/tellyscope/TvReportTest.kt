// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope

import app.tellyscope.data.DeviceLists
import app.tellyscope.data.LISTS_BASE
import app.tellyscope.data.REPORTS_BASE
import app.tellyscope.data.TvPart
import app.tellyscope.data.tvChanges
import app.tellyscope.data.tvPart
import app.tellyscope.data.tvValues
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/** org.json is Android's, so this runs under Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TvReportTest {
    @get:Rule val tmp = TemporaryFolder()

    /** Like aqua-matrixtv-ee-50's: hardware settings mixed with the tester's own taste. */
    private val report = """
        {"dock_apps": ["com.netflix.ninja"], "columns": 6, "hidden_apps": ["com.cltv.hybrid"], "wallpaper": false,
         "live_tv_package": "com.cltv.hybrid", "settings_key_keycodes": [83], "settings_key_panel": true,
         "stock_home_redirect": "com.google.android.apps.tv.launcherx", "key_filter": false,
         "source_panel_service": "com.sdt.app.skysettings/.entry.activity.QuickSourceActivity",
         "source_panel_extra": "control_center_command=100", "input_sources": ["HW5=23", "HW1=2"]}
    """.trimIndent()

    @Test fun onlyTheHardwareSettingsComeOver() {
        assertEquals(
            mapOf(
                "live_tv_package" to "com.cltv.hybrid",
                "input_sources" to listOf("HW5=23", "HW1=2"),
                "source_panel_service" to "com.sdt.app.skysettings/.entry.activity.QuickSourceActivity",
                "source_panel_extra" to "control_center_command=100",
                "settings_key_keycodes" to listOf(83),
                "stock_home_redirect" to "com.google.android.apps.tv.launcherx",
                "key_filter" to false,
            ),
            tvValues(report),
        )
    }

    @Test fun aValueOfTheWrongKindIsDropped() {
        val odd = """
            {"live_tv_package": "com.x;reboot", "key_filter": "false", "settings_key_keycodes": [83, "84"],
             "input_uri_scheme": "live-tv", "source_panel_extra": "a=b", "input_sources": [], "live_tv_action": ""}
        """.trimIndent()
        assertEquals(mapOf("input_uri_scheme" to "live-tv"), tvValues(odd))
        assertEquals(emptyMap<String, Any>(), tvValues("not json"))
        assertEquals(emptyMap<String, Any>(), tvValues(null))
    }

    @Test fun whatTheTvAlreadyHasOrLacksTheAppForIsNotOffered() {
        val values = tvValues(report)
        val theme = mapOf("key_filter" to false, "settings_key_keycodes" to listOf(83))
        val installed = setOf("com.cltv.hybrid", "com.google.android.apps.tv.launcherx")
        // The source panel's app isn't on this TV, so it would point at nothing.
        assertEquals(
            setOf("live_tv_package", "input_sources", "source_panel_extra", "stock_home_redirect"),
            tvChanges(values, theme, installed).keys,
        )
        assertTrue(tvChanges(values, values, installed + "com.sdt.app.skysettings").isEmpty())
    }

    @Test fun eachKeyBelongsToAPartOfTheTv() {
        assertEquals(TvPart.LIVE_TV, tvPart("live_tv_package"))
        assertEquals(TvPart.INPUTS, tvPart("source_panel_service"))
        assertEquals(TvPart.SETTINGS_BUTTON, tvPart("settings_key_keycodes"))
        assertEquals(TvPart.HOME, tvPart("stock_home_redirect"))
        assertEquals(TvPart.REMOTE, tvPart("key_filter"))
    }

    private val index = """
        {"schema": 1, "source": {"repo": "Gathin23/cleanyourtv", "commit": "30626a8e4714"},
         "devices": [{"id": "acme-55", "name": "Acme 55", "match": null, "sdk": 30, "file": "devices/acme-55.json",
                      "sha256": "${"0".repeat(64)}"}]}
    """.trimIndent().toByteArray()

    @Test fun theReportIsReadAtTheIndexCommitAndCached() = runTest {
        val asked = mutableListOf<String>()
        val served = mapOf(LISTS_BASE + "index.json" to index, REPORTS_BASE + "30626a8e4714/examples/acme-55/config.json" to report.toByteArray())
        val lists = DeviceLists(tmp.root) { url -> asked += url; served[url] ?: throw IOException("404 $url") }
        assertEquals(report, lists.report("acme-55"))
        assertNull(lists.report("other"))
        assertNull(lists.report("../etc"))
        val offline = DeviceLists(tmp.root) { url -> if (url.endsWith("index.json")) index else throw IOException("offline") }
        assertEquals(report, offline.report("acme-55"))
        assertEquals(1, asked.count { it.endsWith("acme-55/config.json") })
    }

    @Test fun theUsersOwnSourceHasNoReports() = runTest {
        val lists = DeviceLists(tmp.root) { url -> if (url.endsWith("index.json")) index else report.toByteArray() }
        lists.setSource("https://example.com/lists/")
        assertNull(lists.report("acme-55"))
    }
}
