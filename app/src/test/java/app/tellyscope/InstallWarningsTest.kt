// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope

import app.tellyscope.data.AppInfo
import app.tellyscope.data.Device
import app.tellyscope.data.DeviceState
import app.tellyscope.data.FIRE_TV_LAUNCHER
import app.tellyscope.data.InstallWarning
import app.tellyscope.data.LastKnown
import app.tellyscope.data.ScanHit
import app.tellyscope.data.ScanKind
import app.tellyscope.data.androidMajor
import app.tellyscope.data.installWarnings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Makers and versions as CleanYourTV's TV reports give them.
private fun tv(manufacturer: String?, model: String?, android: String?, brand: String? = null) =
    Device("tv", ScanHit("tv", 5555, ScanKind.Classic, null, null), DeviceState.Online, model, manufacturer, android, 0L, brand = brand)

private fun app(pkg: String, home: Boolean = false) = AppInfo(pkg, pkg, null, true, true, null, "1.0", home = home)

private val store = app("com.android.vending")
private val tvHome = app("com.google.android.tvlauncher", home = true)
private val googleTv = app("com.google.android.apps.tv.launcherx", home = true)
private val wizard = app("com.google.android.tungsten.setupwraith")

class InstallWarningsTest {
    @Test
    fun readsTheMajorVersion() {
        assertEquals(11, androidMajor("11"))
        assertEquals(8, androidMajor("8.1.0"))
        assertEquals(7, androidMajor(" 7.1.2"))
        assertNull(androidMajor(null))
        assertNull(androidMajor("unknown"))
    }

    @Test
    fun anOrdinaryAndroidTvGetsNone() {
        assertEquals(emptyList<InstallWarning>(), installWarnings(tv("Vestel", "MB130", "11"), listOf(store, tvHome)))
    }

    @Test
    fun belowAndroid8IsTooOld() {
        assertEquals(listOf(InstallWarning.OldAndroid), installWarnings(tv("Philips", "TPM171E", "7.1.2"), listOf(store, tvHome)))
        assertEquals(emptyList<InstallWarning>(), installWarnings(tv("Philips", "TPM171E", "8.0.0"), listOf(store, tvHome)))
    }

    @Test
    fun theLastKnownVersionCountsWhileOffline() {
        val offline = tv("Sony", "BRAVIA", null).copy(lastKnown = LastKnown(android = "6.0.1"))
        assertEquals(listOf(InstallWarning.OldAndroid), installWarnings(offline, listOf(store)))
    }

    @Test
    fun aTvThatSaysNothingGetsNone() {
        assertEquals(emptyList<InstallWarning>(), installWarnings(tv(null, null, null), emptyList()))
    }

    @Test
    fun fireOsByNameOrByItsHome() {
        assertEquals(listOf(InstallWarning.FireOs), installWarnings(tv("Amazon", "AFTKRT", "9"), emptyList()))
        val edition = app(FIRE_TV_LAUNCHER, home = true)
        assertEquals(listOf(InstallWarning.FireOs), installWarnings(tv("Insignia", "NS-43F301NA22", "9"), listOf(edition)))
    }

    @Test
    fun chineseTvsWithoutTheStoreMayKeepTheirHome() {
        val patchwall = app("com.mitv.tvhome", home = true)
        assertEquals(listOf(InstallWarning.LockedHome), installWarnings(tv("Xiaomi", "MiTV-MSSP2", "9"), listOf(patchwall)))
        assertEquals(listOf(InstallWarning.LockedHome), installWarnings(tv("Skyworth", "Q71", "9", brand = "Coocaa"), listOf(patchwall)))
        // The same makers' global TVs have the Play Store, and take another Home.
        assertEquals(emptyList<InstallWarning>(), installWarnings(tv("Xiaomi", "MiTV-AXFR0", "11"), listOf(store, tvHome)))
        assertEquals(emptyList<InstallWarning>(), installWarnings(tv("TCL", "Smart TV Pro", "11"), listOf(store, tvHome)))
    }

    @Test
    fun googleTvWithItsSetupOnNeedsTheService() {
        assertEquals(listOf(InstallWarning.GoogleTv), installWarnings(tv("TCL", "C735", "11"), listOf(store, googleTv, wizard)))
        assertEquals(emptyList<InstallWarning>(), installWarnings(tv("TCL", "C735", "11"), listOf(store, googleTv)))
    }

    @Test
    fun tooOldComesFirst() {
        assertEquals(listOf(InstallWarning.OldAndroid, InstallWarning.LockedHome),
            installWarnings(tv("Xiaomi", "MiTV-MSSP0", "6.0"), listOf(app("com.mitv.tvhome", home = true))))
    }
}
