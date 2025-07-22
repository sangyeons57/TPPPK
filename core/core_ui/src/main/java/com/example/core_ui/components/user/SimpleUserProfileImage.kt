package com.example.core_ui.components.user

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core_ui.R
import android.util.Log

/**
 * A simple composable that displays a user profile image from a given URL.
 * It does not contain any business logic or ViewModel, making it suitable for lists
 * where the parent composable already manages the state.
 */
@Composable
fun SimpleUserProfileImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Log.d("ProfileImageDebug", "SimpleUserProfileImage: loading image URL: $imageUrl")
    
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .placeholder(R.drawable.ic_default_profile_placeholder)
            .error(R.drawable.ic_default_profile_placeholder)
            .crossfade(true)
            .listener(
                onStart = { 
                    Log.d("ProfileImageDebug", "SimpleUserProfileImage: started loading $imageUrl")
                },
                onSuccess = { _, _ ->
                    Log.d("ProfileImageDebug", "SimpleUserProfileImage: successfully loaded $imageUrl")
                },
                onError = { _, error ->
                    Log.e("ProfileImageDebug", "SimpleUserProfileImage: failed to load $imageUrl", error.throwable)
                }
            )
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
