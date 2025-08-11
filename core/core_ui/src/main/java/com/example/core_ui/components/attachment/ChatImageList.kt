package com.example.core_ui.components.attachment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatImageList(
    imageUrls: List<String>,
    onImageClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (imageUrls.isEmpty()) return
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(imageUrls.indices.toList()) { index ->
            ChatImage(
                model = imageUrls[index],
                modifier = Modifier,
                onClick = { onImageClick(index) }
            )
        }
    }
}


