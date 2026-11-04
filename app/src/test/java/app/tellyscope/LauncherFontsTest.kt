// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope

import app.tellyscope.data.FONTS_BASE
import app.tellyscope.data.GOOGLE_FONTS
import app.tellyscope.data.isFont
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherFontsTest {
    @Test fun everyFontIsPinnedToOneCommitAndAHash() {
        assertTrue(FONTS_BASE.matches(Regex("https://raw\\.githubusercontent\\.com/google/fonts/[0-9a-f]{40}/ofl/")))
        assertEquals(GOOGLE_FONTS.size, GOOGLE_FONTS.map { it.name }.toSet().size)
        for (f in GOOGLE_FONTS) {
            assertTrue(f.name, f.sha256.matches(Regex("[0-9a-f]{64}")))
            assertTrue(f.name, f.size in 1..(2L shl 20))
            assertTrue(f.name, f.url.startsWith(FONTS_BASE) && '[' !in f.url && f.url.endsWith(".ttf"))
        }
    }

    @Test fun aFileThatIsntTheFontIsRefused() {
        val f = GOOGLE_FONTS.first()
        assertFalse(isFont(f, ByteArray(f.size.toInt())))
        assertFalse(isFont(f, ByteArray(10)))
    }
}
