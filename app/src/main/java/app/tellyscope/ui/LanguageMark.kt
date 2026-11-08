// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.tellyscope.data.AppLanguage

/** The flag beside a language choice; the device's own language ("" tag) shows [device] (the phone or the TV) instead. */
@Composable
fun LanguageMark(tag: String, @DrawableRes device: Int) {
    val flag = AppLanguage.flag(tag)
    // Decorative: the language's name is always written next to it.
    if (flag != null) {
        Image(painterResource(flag), contentDescription = null, modifier = Modifier.size(24.dp, 16.dp))
    } else {
        Icon(painterResource(device), contentDescription = null, modifier = Modifier.size(24.dp, 16.dp))
    }
}
