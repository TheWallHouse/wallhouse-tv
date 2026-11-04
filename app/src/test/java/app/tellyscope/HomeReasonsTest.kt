// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope

import app.tellyscope.data.HOME_REASONS
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Every reason Actions gives for a Home switch that didn't take has its translated text, so none shows in English. */
class HomeReasonsTest {
    @Test fun everyHomeFailureIsTranslated() {
        val source = File("../adb/src/main/kotlin/app/tellyscope/adb/Actions.kt").readText()
        val said = Regex("HomeResult\\.Failed\\(\"([^\"]+)\"").findAll(source).map { it.groupValues[1] }.toSet()
        assertEquals(emptySet<String>(), said - HOME_REASONS.keys)
    }
}
