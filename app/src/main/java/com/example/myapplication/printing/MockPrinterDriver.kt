package com.example.myapplication.printing

import android.graphics.Bitmap

class MockPrinterDriver : PrinterDriver {
    override suspend fun print(labelBitmap: Bitmap): Result<Unit> {
        // TODO: 画像をローカル保存して擬似印刷
        return Result.success(Unit)
    }
}