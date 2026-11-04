// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.media.ExifInterface
import java.io.ByteArrayOutputStream

/**
 * A photo from the phone as a TV wallpaper: turned upright, cropped to the screen's 16:9 around its centre, scaled to
 * 1920 × 1080 and saved as JPEG, small enough for a TV with 1 GB of RAM to decode. Null when it can't be read.
 */
object WallpaperPrep {
    const val WIDTH = 1920
    const val HEIGHT = 1080
    /** At most this many photos go to the TV in one collection. */
    const val MAX = 20

    fun jpeg(ctx: Context, uri: Uri): ByteArray? = runCatching {
        val r = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        r.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        // Decode at the smallest power of two that still covers the screen, so a 50 MP photo doesn't fill memory.
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= WIDTH && bounds.outHeight / (sample * 2) >= HEIGHT) sample *= 2
        val raw = r.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            ?: return null
        val rotation = r.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
        val upright = if (rotation == 0) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
        val out = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(upright, crop(upright.width, upright.height), Rect(0, 0, WIDTH, HEIGHT), Paint(Paint.FILTER_BITMAP_FLAG))
        ByteArrayOutputStream().use { b -> out.compress(Bitmap.CompressFormat.JPEG, 88, b); b.toByteArray() }
    }.getOrNull()

    /** The centred 16:9 part of a [w] × [h] image. */
    fun crop(w: Int, h: Int): Rect = if (w * HEIGHT > h * WIDTH) {
        val cw = h * WIDTH / HEIGHT
        Rect((w - cw) / 2, 0, (w - cw) / 2 + cw, h)
    } else {
        val ch = w * HEIGHT / WIDTH
        Rect(0, (h - ch) / 2, w, (h - ch) / 2 + ch)
    }
}
