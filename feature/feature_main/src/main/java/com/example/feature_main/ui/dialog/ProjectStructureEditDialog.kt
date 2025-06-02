package com.example.feature_main.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.feature_main.viewmodel.ProjectStructureEditUiState

/**
 * Composable for the Project Structure Edit dialog.
 * This dialog allows users to modify categories and channels within a project.
 */
@Composable
fun ProjectStructureEditDialog(
    uiState: ProjectStructureEditUiState,
    onDismissRequest: () -> Unit,
    onSaveClick: () -> Unit, // The ViewModel will grab the current uiState to save
    // TODO: Add more specific event handlers for actions within the dialog, e.g.:
    // onCategoryNameChange: (categoryId: String, newName: String) -> Unit,
    // onChannelNameChange: (categoryId: String, channelId: String, newName: String) -> Unit,
    // onAddCategory: (name: String) -> Unit,
    // onAddChannel: (categoryId: String, channelName: String /*, type: ChannelType */) -> Unit,
    // onDeleteCategory: (categoryId: String) -> Unit,
    // onDeleteChannel: (channelId: String) -> Unit,
    // onReorderCategories: (fromIndex: Int, toIndex: Int) -> Unit,
    // onReorderChannels: (categoryId: String, fromIndex: Int, toIndex: Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Edit Project Structure") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                uiState.error?.let {
                    Text(
                        text = "Error: $it",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                uiState.successMessage?.let {
                    Text(
                        text = "Success: $it",
                        color = MaterialTheme.colorScheme.primary, // Or a success color
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                // Placeholder for the actual editing UI
                Text(
                    "Project structure editing UI (categories, channels) will be implemented here.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // TODO: Implement LazyColumn for categories, each category item allowing:
                //  - TextField for name editing
                //  - Button to add a channel to this category
                //  - LazyColumn/FlowRow for channels within the category, each channel item allowing:
                //      - TextField for name editing
                //      - Button to delete channel
                //  - Button to delete category
                //  - Drag handles for reordering categories and channels

                Spacer(modifier = Modifier.height(16.dp))

                // Example: Button to add a new category (ViewModel would handle the logic)
                // Button(onClick = { /* onAddCategory("New Category") */ }) {
                //     Text("Add New Category (Example)")
                // }
            }
        },
        confirmButton = {
            Button(
                onClick = onSaveClick,
                enabled = !uiState.isLoading
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
