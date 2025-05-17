package com.example.core_ui.components.loading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A full-screen loading indicator with an optional message.
 *
 * @param modifier Modifier to be applied to the loading indicator container
 * @param color The color of the loading indicator
 * @param message Optional text to display below the loading indicator
 */
@Composable
fun FullScreenLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    message: String? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = color,
                strokeWidth = 3.dp
            )
            
            message?.let { 
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * A small circular loading indicator.
 *
 * @param modifier Modifier to be applied to the loading indicator
 * @param size The size of the loading indicator
 * @param color The color of the loading indicator
 * @param strokeWidth The width of the indicator stroke
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Int = 24,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Int = 2
) {
    CircularProgressIndicator(
        modifier = modifier.size(size.dp),
        color = color,
        strokeWidth = strokeWidth.dp
    )
}

/**
 * A loading indicator that can be used as a content of a button.
 *
 * @param color The color of the loading indicator
 * @param modifier Modifier to be applied to the loading indicator
 */
@Composable
fun ButtonLoadingIndicator(
    color: Color = MaterialTheme.colorScheme.onPrimary,
    modifier: Modifier = Modifier
) {
    LoadingIndicator(
        modifier = modifier.size(24.dp),
        color = color,
        strokeWidth = 2
    )
}

/**
 * A loading indicator that fills its container with a semi-transparent overlay.
 *
 * @param modifier Modifier to be applied to the loading indicator container
 * @param backgroundColor The background color of the overlay
 * @param indicatorColor The color of the loading indicator
 * @param message Optional text to display below the loading indicator
 */
@Composable
fun OverlayLoadingIndicator(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    message: String? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Semi-transparent overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(backgroundColor)
        )
        
        // Loading content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LoadingIndicator(
                size = 48,
                color = indicatorColor,
                strokeWidth = 3
            )
            
            message?.let { 
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
