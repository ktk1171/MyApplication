package com.example.myapplication

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import java.util.ArrayDeque

data class DetectedBarcode(
    val value: String,   // 安定化済みの文字列（Analyzer側で多数決してから渡す）
    val x: Float,        // 中心X（0..1, PreviewView基準）
    val y: Float,        // 中心Y（0..1, PreviewView基準）
    val w: Float,        // 幅（0..1, PreviewView基準）
    val h: Float,        // 高さ（0..1, PreviewView基準）
    val distanceToCenter: Float
)


class MainViewModel : ViewModel() {
    // 現在の候補リスト
    private val _barcodes = mutableStateListOf<DetectedBarcode>()
    val barcodes: List<DetectedBarcode> = _barcodes

    // 下部ラベル用（ユーザーがタップして選択した値）
    private val _selectedBarcode = mutableStateOf<String?>(null)
    val selectedBarcode: State<String?> = _selectedBarcode

    // CameraScreen から呼ばれるやつ
    var focusedValue by mutableStateOf<String?>(null)
        private set

    // 桁落ち防止用：直近の値をバッファに保持
    private val recentValues = ArrayDeque<String>()

    fun updateFocusedValue(value: String) {
        focusedValue = value
    }

    fun clearFocus() {
        focusedValue = null
    }

    fun clearSelected() {
        _selectedBarcode.value = null
    }

    fun selectBarcode(value: String) {
        _selectedBarcode.value = value
        focusedValue = value
    }


    fun updateBarcodes(new: List<DetectedBarcode>) {
        _barcodes.clear()
        _barcodes.addAll(new)
    }
}
