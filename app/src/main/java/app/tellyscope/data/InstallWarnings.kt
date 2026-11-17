// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.data

/**
 * What the TV says about itself that matters before installing WallHouse Launcher, from CleanYourTV's reports of
 * 481 TVs: too old to install it at all, or a TV that installs it but keeps Home for its own screen, or one where
 * Home needs one more step.
 */
enum class InstallWarning {
    /** Android below 8: the launcher needs 8 (API 26) and the TV refuses the install. Blocks it. */
    OldAndroid,

    /** Fire OS: Amazon keeps the Home button for Fire TV's own screen. */
    FireOs,

    /** A TV sold in China (Xiaomi, Redmi, Skyworth, Coocaa, TCL, no Play Store): only its maker's home screens take Home. */
    LockedHome,

    /** Google TV with its setup on: Home is held by the launcher's accessibility service, which the app asks for after. */
    GoogleTv,
}

/** The launcher's minimum Android, as its major version. */
const val LAUNCHER_MIN_ANDROID = 8

/** Makers whose TVs for China only accept their own home screens. */
private val LOCKED_MAKERS = setOf("xiaomi", "redmi", "skyworth", "coocaa", "tcl")

/** The major version of "11", "8.1.0" or "7.1.2"; null when unknown. */
fun androidMajor(version: String?): Int? = version?.trim()?.takeWhile { it.isDigit() }?.toIntOrNull()

/**
 * The warnings for installing the launcher on [d], most serious first. Its Android comes from the live reading or
 * the last one kept. A TV that reports nothing gets none: the warnings never guess.
 */
fun installWarnings(d: Device, apps: List<AppInfo>): List<InstallWarning> {
    val out = mutableListOf<InstallWarning>()
    val major = androidMajor(d.android ?: d.lastKnown?.android)
    if (major != null && major < LAUNCHER_MIN_ANDROID) out += InstallWarning.OldAndroid
    if (isFireOs(d, apps)) {
        out += InstallWarning.FireOs
    } else {
        val makers = listOfNotNull(d.manufacturer, d.brand, d.lastKnown?.manufacturer, d.lastKnown?.brand)
            .flatMap { it.lowercase().split(Regex("[^a-z]+")) }
        val store = apps.any { it.packageName == "com.android.vending" && it.enabled }
        if (apps.isNotEmpty() && !store && makers.any { it in LOCKED_MAKERS }) out += InstallWarning.LockedHome
    }
    if (isGoogleTvGuarded(apps)) out += InstallWarning.GoogleTv
    return out
}
