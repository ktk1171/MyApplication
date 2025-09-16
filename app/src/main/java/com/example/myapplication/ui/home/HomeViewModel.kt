package com.example.myapplication.ui.home

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.model.LabelFormat

data class HomeUiState(
    val formats: List<LabelFormat> = emptyList(),
    val selectedFormatId: String? = null
)

class HomeViewModel : ViewModel() {
    private val _state = mutableStateOf(HomeUiState(
        formats = listOf(
            LabelFormat("GENPIN_STD_58","現品票 標準(58mm)","RJ-2150","Wi-Fi",58,1),
            LabelFormat("TEST_58","テスト印刷(58mm)","RJ-2150","Wi-Fi",58,99)
        ),
        selectedFormatId = "GENPIN_STD_58"
    ))
    val state get() = _state.value

    fun selectFormat(id: String) {
        _state.value = _state.value.copy(selectedFormatId = id)
        // TODO: DataStoreへ保存
    }
}