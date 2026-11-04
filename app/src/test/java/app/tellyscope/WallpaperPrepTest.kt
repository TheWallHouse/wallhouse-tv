// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope

import android.graphics.Rect
import app.tellyscope.data.WallpaperPrep
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** android.graphics.Rect is Android's, so this runs under Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WallpaperPrepTest {
    @Test fun aPortraitPhotoKeepsItsMiddleBand() = assertEquals(Rect(0, 1167, 3000, 2854), WallpaperPrep.crop(3000, 4021))

    @Test fun aPanoramaKeepsItsMiddle() = assertEquals(Rect(1500, 0, 5340, 2160), WallpaperPrep.crop(6840, 2160))

    @Test fun aScreenShapedPhotoIsKeptWhole() = assertEquals(Rect(0, 0, 3840, 2160), WallpaperPrep.crop(3840, 2160))
}
