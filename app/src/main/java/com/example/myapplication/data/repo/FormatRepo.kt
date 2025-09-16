package com.example.myapplication.data.repo

import com.example.myapplication.data.model.LabelFormat

object FormatRepo {
    fun load(): List<LabelFormat> = listOf(
        LabelFormat("GENPIN_STD_58", "現品票 標準(58mm)", "RJ-2150", "Wi-Fi", 58, 1),
        LabelFormat("TEST_58",      "テスト印刷(58mm)",   "RJ-2150", "Wi-Fi", 58, 99)
    )
}
