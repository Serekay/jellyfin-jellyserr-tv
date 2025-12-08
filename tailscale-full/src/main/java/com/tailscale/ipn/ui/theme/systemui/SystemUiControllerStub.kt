package com.tailscale.ipn.ui.theme.systemui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Minimal stub to avoid pulling in accompanist-systemuicontroller. No-op implementation.
 */
class SystemUiController {
	fun setStatusBarColor(color: Color, darkIcons: Boolean = false) {}
	fun setNavigationBarColor(color: Color, darkIcons: Boolean = false) {}
}

@Composable
fun rememberSystemUiController(): SystemUiController = remember { SystemUiController() }
