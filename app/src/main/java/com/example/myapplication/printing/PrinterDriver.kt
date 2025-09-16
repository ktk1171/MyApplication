package com.example.myapplication.printing

import android.graphics.Bitmap

interface PrinterDriver {
    suspend fun print(labelBitmap: Bitmap): Result<Unit>
}