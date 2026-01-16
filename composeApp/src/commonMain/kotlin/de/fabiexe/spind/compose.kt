package de.fabiexe.spind

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable

@Composable
fun isMobileScreen(): Boolean = currentWindowAdaptiveInfo().windowSizeClass.minWidthDp < 800