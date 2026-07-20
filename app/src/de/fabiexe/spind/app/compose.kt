package de.fabiexe.spind.app

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable

@Composable
fun isMobileScreen(): Boolean = currentWindowAdaptiveInfo().windowSizeClass.minWidthDp < 800