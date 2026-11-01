package com.example.uare.ui.screens

import androidx.compose.runtime.Composable
import com.example.uare.ui.viewmodel.ExplorerUiState
import com.example.uare.ui.viewmodel.ExplorerViewModel

/**
 * Reflection is embedded directly into the Runtime Runner and Unified Explorer.
 * This screen delegates to the integrated RunnerScreen.
 */
@Composable
fun ReflectionStudioScreen(
    state: ExplorerUiState,
    viewModel: ExplorerViewModel
) {
    RunnerScreen(state = state, viewModel = viewModel)
}
