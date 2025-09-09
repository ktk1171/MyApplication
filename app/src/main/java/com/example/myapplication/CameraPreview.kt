package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import androidx.activity.ComponentActivity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
@Composable
fun CameraPreview(viewModel: MainViewModel, onBarcodeSelected: (String) -> Unit) {
    val context = LocalContext.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val snackbarHostState = remember { SnackbarHostState() }
    val barcodes = viewModel.barcodes
    LaunchedEffect(barcodes) {
        if (barcodes.isNotEmpty()) {
            val values = barcodes.joinToString(", ") { it.value }
            snackbarHostState.showSnackbar("読み取り: $values")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    startCamera(ctx, previewView, viewModel, cameraExecutor)
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            BarcodeOverlay(viewModel = viewModel, onBarcodeSelected = onBarcodeSelected)
        }

        Text(
            text = "読み取ったコード: ${barcodes.joinToString(", ") { it.value }}",
            modifier = Modifier.padding(16.dp)
        )
    }
}
@SuppressLint("UnsafeOptInUsageError")
private fun startCamera(
    context: Context,
    previewView: PreviewView,
    viewModel: MainViewModel,
    executor: java.util.concurrent.Executor
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val analyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(executor, BarcodeAnalyzer(viewModel))
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                context as ComponentActivity,
                cameraSelector,
                preview,
                analyzer
            )
        } catch (e: Exception) {
            Log.e("CameraPreview", "Camera binding failed", e)
        }
    }, ContextCompat.getMainExecutor(context))
}

class BarcodeAnalyzer(private val viewModel: MainViewModel) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                Log.d("BarcodeAnalyzer", "検出数: ${barcodes.size}")
                val detected = barcodes.mapNotNull { barcode ->
                    val box = barcode.boundingBox ?: return@mapNotNull null
                    val value = barcode.rawValue ?: return@mapNotNull null
                    Log.d("BarcodeAnalyzer", "検出: $value")
                    DetectedBarcode(value, box.centerX() / 3, box.centerY() / 3)
                }
                viewModel.updateBarcodes(detected)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
@Composable
fun BarcodeOverlay(viewModel: MainViewModel, onBarcodeSelected: (String) -> Unit) {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Transparent)) {
        viewModel.barcodes.forEach { barcode ->
            Text(
                text = barcode.value,
                modifier = Modifier
                    .offset(x = barcode.x.dp, y = barcode.y.dp)
                    .background(Color.White.copy(alpha = 0.8f))
                    .clickable {
                        viewModel.onBarcodeSelected(barcode.value)
                        onBarcodeSelected(barcode.value) // ← これが重要！
                    }
                    .padding(8.dp)
            )
        }
    }
}
