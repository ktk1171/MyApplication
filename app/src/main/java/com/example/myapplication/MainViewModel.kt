package com.example.myapplication

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class DetectedBarcode(val value: String, val x: Int, val y: Int)

class MainViewModel : ViewModel() {
    private val _barcodes = mutableStateListOf<DetectedBarcode>()
    val barcodes: List<DetectedBarcode> get() = _barcodes

    var selectedBarcode: String? by mutableStateOf(null)
        private set

    fun updateBarcodes(newBarcodes: List<DetectedBarcode>) {
        _barcodes.clear()
        _barcodes.addAll(newBarcodes)
    }

    fun onBarcodeSelected(value: String) {
        selectedBarcode = value
        Log.d("MainViewModel", "選択されたバーコード: $value")
    }
}