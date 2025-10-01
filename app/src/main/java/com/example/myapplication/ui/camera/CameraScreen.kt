@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.camera

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.MainViewModel

@Composable
fun CameraScreen(
    viewModel: MainViewModel,
    onConfirmPrint: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("カメラでスキャン") },
                actions = {
                    TextButton(onClick = { viewModel.clearFocus() }) {
                        Text("解除")
                    }
                }
            )
        }
    ) { pad ->
        Box(
            Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            // 上：プレビュー
            CameraPreview(
                viewModel = viewModel,
                onBarcodeSelected = { code ->
                    // タップされたバーコードをフォーカス値に反映
                    viewModel.updateFocusedValue(code)
                }
            )

            // 下：確定ボタン
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val enabled = viewModel.focusedValue != null
                Button(
                    onClick = { viewModel.focusedValue?.let(onConfirmPrint) },
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("印刷")
                }
            }
        }
    }
}
