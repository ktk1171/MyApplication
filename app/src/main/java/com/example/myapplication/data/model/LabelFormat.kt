package com.example.myapplication.data.model

data class LabelFormat(
    val id: String,
    val title: String,
    val printerModel: String,
    val connection: String,
    val mediaWidthMm: Int,
    val templateKey: Int
)