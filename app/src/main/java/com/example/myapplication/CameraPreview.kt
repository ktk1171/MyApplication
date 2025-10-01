@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package com.example.myapplication.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.os.Vibrator
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.myapplication.MainViewModel
import com.example.myapplication.DetectedBarcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.max
import java.util.ArrayDeque


@Composable
fun CameraPreview(
    viewModel: MainViewModel,
    onBarcodeSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    // 👇 プレビューの拡大方法を固定
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                startCamera(ctx, previewView, viewModel, cameraExecutor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )


        // バーコードのオーバーレイ
        BarcodeOverlay(
            viewModel = viewModel,
            onBarcodeSelected = onBarcodeSelected
        )
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun startCamera(
    context: android.content.Context,
    previewView: PreviewView,
    viewModel: MainViewModel,
    executor: java.util.concurrent.Executor
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        // プレビューの拡大方法は FILL_CENTER に固定
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val analyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(executor, BarcodeAnalyzer(viewModel, previewView))
            }

        val selector = CameraSelector.DEFAULT_BACK_CAMERA
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                context as androidx.activity.ComponentActivity,
                selector,
                preview,
                analyzer
            )
        } catch (e: Exception) {
            android.util.Log.e("CameraPreview", "Camera binding failed", e)
        }
    }, androidx.core.content.ContextCompat.getMainExecutor(context))
}



@OptIn(ExperimentalGetImage::class)
class BarcodeAnalyzer(
    private val viewModel: MainViewModel,
    private val previewView: PreviewView
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    // 位置ごとの履歴（直近5フレーム）
    private data class Key(val cxBin: Int, val cyBin: Int, val wBin: Int, val hBin: Int)
    private val history = mutableMapOf<Key, ArrayDeque<String>>()

    // 近いキーにマージするためのヘルパ
    private fun bin(v: Float, buckets: Int = 20) = (v * buckets).toInt().coerceIn(0, buckets)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)

        val viewW = previewView.width.toFloat()
        val viewH = previewView.height.toFloat()
        if (viewW <= 0f || viewH <= 0f) { imageProxy.close(); return }

        // センサー画像サイズ（回転考慮）
        val srcW: Float
        val srcH: Float
        when ((rotation % 360 + 360) % 360) {
            0, 180 -> { srcW = mediaImage.width.toFloat();  srcH = mediaImage.height.toFloat() }
            90, 270 -> { srcW = mediaImage.height.toFloat(); srcH = mediaImage.width.toFloat() }
            else -> { srcW = mediaImage.width.toFloat();     srcH = mediaImage.height.toFloat() }
        }

        // ScaleType.FILL_CENTER 相当（cover + 中央トリム）
        val scale = max(viewW / srcW, viewH / srcH)
        val scaledW = srcW * scale
        val scaledH = srcH * scale
        val offsetX = (viewW - scaledW) / 2f
        val offsetY = (viewH - scaledH) / 2f

        scanner.process(image)
            .addOnSuccessListener { results ->
                val out = results.mapNotNull { b ->
                    val raw = b.rawValue?.trim() ?: return@mapNotNull null
                    val box = b.boundingBox ?: return@mapNotNull null

                    // 中心座標をUI正規化（0..1）
                    val cx = ((box.centerX() * scale + offsetX) / viewW).coerceIn(0f, 1f)
                    val cy = ((box.centerY() * scale + offsetY) / viewH).coerceIn(0f, 1f)
                    // サイズをUI正規化（0..1）
                    val wNorm = (box.width().toFloat()  * scale / viewW).coerceIn(0f, 1f)
                    val hNorm = (box.height().toFloat() * scale / viewH).coerceIn(0f, 1f)
                    if (wNorm <= 0f || hNorm <= 0f) return@mapNotNull null

                    // 近傍を同一とみなすキー（±1binは同一扱い）
                    val key = Key(bin(cx), bin(cy), bin(wNorm), bin(hNorm))


                    val deque = history.getOrPut(key) { ArrayDeque() }

// 履歴を9件まで保持
                    deque.addLast(raw)
                    if (deque.size > 9) deque.removeFirst()

// 多数決
                    val counts = deque.groupingBy { it }.eachCount()
                    val candidate = counts.maxByOrNull { it.value }?.key

// 9件中6回以上出たものだけを安定値として採用
                    val stable = if (candidate != null && counts[candidate]!! >= 6) {
                        candidate
                    } else {
                        null // 判定がまだ不十分なら値なし
                    }


                    val dx = cx - 0.5f
                    val dy = cy - 0.5f
                    val dist = dx*dx + dy*dy

                    if (stable == null) return@mapNotNull null

                    DetectedBarcode(
                        value = stable,
                        x = cx, y = cy,
                        w = wNorm, h = hNorm,
                        distanceToCenter = dist
                    )

                }
                viewModel.updateBarcodes(out)
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}


@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun BarcodeOverlay(
    viewModel: MainViewModel,
    onBarcodeSelected: (String) -> Unit
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }

        viewModel.barcodes.forEach { bc ->
            val xDp = with(density) { (bc.x * wPx).toDp() }
            val yDp = with(density) { (bc.y * hPx).toDp() }
            val wDp = with(density) { (bc.w * wPx).toDp() }
            val hDp = with(density) { (bc.h * hPx).toDp() }

            Canvas(
                modifier = Modifier
                    .offset(x = xDp - wDp / 2, y = yDp - hDp / 2)
                    .size(width = wDp, height = hDp)
                    .clickable {
                        viewModel.selectBarcode(bc.value) // すでに安定化済み
                        onBarcodeSelected(bc.value)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
            ) {
                val col = Color.White
                val sw = with(density) { 3.dp.toPx() }
                val c = with(density) { 20.dp.toPx() }
                val L = 0f; val T = 0f; val R = size.width; val B = size.height

                // 四隅 bracket
                drawLine(col, Offset(L, T), Offset(L + c, T), sw)
                drawLine(col, Offset(L, T), Offset(L, T + c), sw)
                drawLine(col, Offset(R, T), Offset(R - c, T), sw)
                drawLine(col, Offset(R, T), Offset(R, T + c), sw)
                drawLine(col, Offset(L, B), Offset(L + c, B), sw)
                drawLine(col, Offset(L, B), Offset(L, B - c), sw)
                drawLine(col, Offset(R, B), Offset(R - c, B), sw)
                drawLine(col, Offset(R, B), Offset(R, B - c), sw)
            }
        }

        val selected by viewModel.selectedBarcode

        selected?.let { code ->
            Text(
                text = code,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
                    .background(Color.Yellow.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                    .border(2.dp, Color.Red, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

