// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.data

import java.security.MessageDigest

/**
 * Fonts the launcher can show in place of its Doto dot-matrix one, from Google Fonts (all SIL Open Font License),
 * pinned to one commit of github.com/google/fonts and to each file's SHA-256: the phone downloads one, checks it, and
 * sends it to the TV as the theme's font.ttf. The launcher itself never goes online.
 */
data class GoogleFont(val name: String, val path: String, val sha256: String, val size: Long) {
    val url get() = "$FONTS_BASE" + path.replace("[", "%5B").replace("]", "%5D")
}

/** google/fonts at the commit these were checked against. */
const val FONTS_BASE = "https://raw.githubusercontent.com/google/fonts/2fe1e8c6b137fb13041a7cfca6fdcd8a910ea4dc/ofl/"

/** In the order the picker lists them: wide and technical first, then pixel, then plain. */
val GOOGLE_FONTS = listOf(
    GoogleFont("Space Mono", "spacemono/SpaceMono-Regular.ttf", "95837e182baeeada83368f7748db28357f0a1b75c6b84ff7065b5edf933c8e18", 99356),
    GoogleFont("Michroma", "michroma/Michroma-Regular.ttf", "b62301163788bc5b7f8fcac0b74b184e34e1827e577b499ecb724da065098f87", 64344),
    GoogleFont("Orbitron", "orbitron/Orbitron[wght].ttf", "f42db2dd16e642258e35782916eceb1dcdbea06fb958d77ad71dc5963587e8fd", 38576),
    GoogleFont("Barlow Condensed", "barlowcondensed/BarlowCondensed-SemiBold.ttf", "7b619d14bc2327509a9ef32b0890f709626f7ecc9ff61191c2a4314c5499d2d9", 109428),
    GoogleFont("Major Mono Display", "majormonodisplay/MajorMonoDisplay-Regular.ttf", "3f2ee71c2b8464f065bf960a148be81d68fcc13385df87737928f948208dffc6", 126788),
    GoogleFont("VT323", "vt323/VT323-Regular.ttf", "cf4de751ada78ceac033dbe16a687742939995b77bc2a052ae17a4957958594d", 153116),
    GoogleFont("Silkscreen", "silkscreen/Silkscreen-Regular.ttf", "c845473330b94c2079ce9af01c51ac8ba2d99c24f4d14c039843bbb8e642ebd8", 32220),
    GoogleFont("Bebas Neue", "bebasneue/BebasNeue-Regular.ttf", "08e4623805102d819f58601e46e345648846075e363b2ceb23313c2d1c83ec73", 61400),
    GoogleFont("JetBrains Mono", "jetbrainsmono/JetBrainsMono[wght].ttf", "48715a42ec242c21e9f02692891e147d022299a52e48d5e413e1a942193ffeda", 187208),
    GoogleFont("Share Tech Mono", "sharetechmono/ShareTechMono-Regular.ttf", "9ceab1f87414829af259c0f537573ae03ef7dd3147c0b27a36a1a0beb6732677", 43272),
    GoogleFont("Audiowide", "audiowide/Audiowide-Regular.ttf", "c7c0f2b0f6fad8c623e31772ce79f94a4edb9321ffce9fce978ea892d20ae730", 69916),
    GoogleFont("Outfit", "outfit/Outfit[wght].ttf", "fc7287273e66929776e2ba54f144fe699080bec29f61bf649d70d871468aeade", 110884),
)

/** [bytes] are [font]'s file: the size and SHA-256 it was pinned with. */
fun isFont(font: GoogleFont, bytes: ByteArray): Boolean =
    bytes.size.toLong() == font.size && MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } == font.sha256
