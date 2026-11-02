// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope

import app.tellyscope.data.ACCESSIBILITY_LAUNCHERS
import app.tellyscope.data.KnownLauncher
import app.tellyscope.data.SharedLists
import app.tellyscope.data.parseLabels
import app.tellyscope.data.parseLaunchers
import app.tellyscope.data.directLauncher
import app.tellyscope.data.isSignedBy
import app.tellyscope.data.mergeLaunchers
import app.tellyscope.data.parseCatalogue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import app.tellyscope.ui.RECOMMENDED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** org.json is Android's, so this runs under Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LaunchersTest {
    @Test fun theListReadsWhole() {
        val text = File("src/test/resources/fixtures/launchers.json").readText()
        val launchers = parseLaunchers(text)
        assertEquals(Regex("\"package\"").findAll(text).count(), launchers.size) // nothing dropped
        assertEquals(launchers.size, launchers.map { it.pkg }.toSet().size)
    }

    /** Named here on purpose, so a reviewer sees any launcher that gets to turn on an accessibility service. */
    @Test fun onlyTheBuiltInLaunchersMayTurnOnAnAccessibilityService() {
        assertEquals(setOf("com.spocky.projengmenu", "com.overdevs.at4k", "com.spauldhaliwal.dispatch", "io.github.thewallhouse.nothingtv"), ACCESSIBILITY_LAUNCHERS.keys)
    }

    @Test fun theRecommendedOnesAreTheLaunchersWithAnAboutLineInFileOrder() {
        assertEquals(SharedLists.launchers.filter { it.about != null }.map { it.pkg }, RECOMMENDED.map { it.pkg })
        assertTrue(RECOMMENDED.isNotEmpty()) // the picker opens on the first one
    }

    @Test fun theLabelsReadWhole() {
        val text = File("src/test/resources/fixtures/labels.json").readText()
        val labels = parseLabels(text)
        // Every "key": "text" pair but the group names is a label: none dropped, none listed twice.
        val pairs = Regex("\"[^\"]+\"\\s*:\\s*\"").findAll(text).count()
        assertEquals(pairs - Regex("\"name\"\\s*:").findAll(text).count(), labels.size)
        assertEquals("Android System", labels["android"])
        assertEquals("Netflix", SharedLists.labels["com.netflix.ninja"])
    }

    @Test fun badLabelsAreDroppedAndOnlySchemaOneCounts() {
        val text = """{"schema":1,"groups":[
            {"name":"Good","labels":{"com.good.app":"Good","android":"Android System","not a package":"Bad"}},
            {"name":"No labels"},
            "not an object"
        ]}"""
        assertEquals(mapOf("com.good.app" to "Good", "android" to "Android System"), parseLabels(text))
        assertTrue(parseLabels(text.replace("\"schema\":1", "\"schema\":2")).isEmpty())
        assertTrue(parseLabels("{").isEmpty())
    }

    /** A list from someone's fork can't grant an accessibility service: only [ACCESSIBILITY_LAUNCHERS] can. */
    @Test fun badEntriesAreDroppedAndOnlySchemaOneCounts() {
        val text = """{"schema":1,"launchers":[
            {"package":"com.good.launcher","name":"Good","accessibility":true},
            {"package":"not a package; rm -rf /","name":"Bad","accessibility":true},
            {"name":"No package"},
            "not an object"
        ]}"""
        assertEquals(listOf(KnownLauncher("com.good.launcher", "Good")), parseLaunchers(text))
        assertTrue(parseLaunchers(text).none { it.accessibility })
        assertTrue(parseLaunchers(text.replace("\"schema\":1", "\"schema\":2")).isEmpty())
        assertTrue(parseLaunchers("{").isEmpty())
    }

    /** The catalogue shipped in the app: every entry reads, each with a download pinned to a repo and signer. */
    @Test fun theBundledCatalogueReadsWholeAndPinsItsDownloads() {
        val text = File("src/main/resources/catalog/launchers.json").readText()
        val launchers = parseCatalogue(text)
        assertEquals(Regex("\"package\"").findAll(text).count(), launchers.size)
        assertTrue(launchers.all { it.direct != null && it.direct!!.signers.isNotEmpty() })
        val nothing = launchers.first { it.pkg == NOTHING }
        assertEquals("TheWallHouse/cleanyourtv", nothing.direct?.repo)
        assertFalse(nothing.direct!!.store)
        assertEquals(nothing.direct, directLauncher(NOTHING)) // seeded from the bundled copy
    }

    @Test fun theCatalogueDropsBadDownloadsAndStorelessEntriesWithoutOne() {
        fun one(fields: String) = parseCatalogue("""{"schema":1,"launchers":[{"package":"com.example.home","name":"Home",$fields}]}""")
        val signer = "ab".repeat(32)
        assertEquals("o/r", one(""""github":"o/r","signers":["$signer"]""").single().direct?.repo)
        assertNull(one(""""github":"not a repo","signers":["$signer"]""").single().direct)
        assertNull(one(""""github":"o/r","signers":["short"]""").single().direct)
        assertTrue(one(""""github":"o/r","signers":[],"store":false""").isEmpty()) // nowhere to get it
        assertTrue(one(""""store":true""").single().direct == null)
        assertTrue(parseCatalogue("{").isEmpty())
    }

    @Test fun theCatalogueComesFirstAndWinsOverTheList() {
        val cat = listOf(KnownLauncher("a.b", "Mine", "about"))
        val list = listOf(KnownLauncher("c.d", "Theirs", "x"), KnownLauncher("a.b", "Old name", "y"))
        assertEquals(listOf("Mine", "Theirs"), mergeLaunchers(cat, list).map { it.name })
    }

    @Test fun aDownloadMustBeThatPackageSignedOnlyByItsSigners() {
        val d = directLauncher(NOTHING)!!
        val signer = d.signers.single()
        assertTrue(isSignedBy(d, NOTHING, setOf(signer)))
        assertFalse(isSignedBy(d, NOTHING, setOf(signer, "0".repeat(64))))
        assertFalse(isSignedBy(d, NOTHING, emptySet()))
        assertFalse(isSignedBy(d, "com.gathin.tvhome", setOf(signer)))
    }
}

private const val NOTHING = "io.github.thewallhouse.nothingtv"
