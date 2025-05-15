package com.example.feature_chat.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.core_ui.theme.AppTheme
import com.example.feature_chat.viewmodel.ChannelListViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 채널 기능 테스트를 위한 테스트용 액티비티
 */
@AndroidEntryPoint
class TestChannelActivity : ComponentActivity() {
    
    // View Model
    val viewModel: ChannelListViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChannelListScreen(
                        viewModel = viewModel,
                        onChannelClick = { channelId ->
                            // In tests, this will be measured by UI interactions
                        },
                        onAddChannelClick = {
                            // In tests, this will be measured by UI interactions
                        }
                    )
                }
            }
        }
    }
} 