package com.example.core_ui.components.user

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core_ui.R // Ensure this R is correct

@Composable
fun UserProfileImage(
    profileImageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop // Default to Crop, common for profile pics
) {
    val imageRequest = if (profileImageUrl == "DEFAULT_PROFILE_IMAGE_MARKER" || profileImageUrl.isNullOrEmpty()) {
        ImageRequest.Builder(LocalContext.current)
            .data(R.drawable.ic_default_profile_placeholder)
            .crossfade(true)
            .build()
    } else {
        ImageRequest.Builder(LocalContext.current)
            .data(profileImageUrl)
            .placeholder(R.drawable.ic_default_profile_placeholder)
            .error(R.drawable.ic_default_profile_placeholder)
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
