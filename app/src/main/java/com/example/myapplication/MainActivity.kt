package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.myapplication.ui.home.HomeScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.camera.CameraScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MainViewModel = viewModel()
            val nav = rememberNavController()

            MaterialTheme {
                Surface {
                    NavHost(navController = nav, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onClickCamera = { nav.navigate("camera") },
                                onClickTest   = { /* 後で実装 */ }
                            )
                        }
                        composable("camera") {
                            CameraScreen(
                                viewModel = vm,
                                onConfirmPrint = { code ->
                                    android.util.Log.i("Camera", "Selected: $code")  // ← ここで今はログ出力だけ
                                    // TODO: プリンタ購入後はここから印刷に繋ぐ
                                    // nav.popBackStack()  // 印刷後にホームへ戻したいなら有効化
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

    private fun startPrintJob(content: String, testMode: Boolean = true) {
        if (testMode) {
            android.util.Log.i("PrintJob", "印刷テスト: $content")
            return
        }
    }
/*
        val printer = Printer()
        val settings = printer.printerInfo.apply {

            printerModel = PrinterInfo.Model.RJ_2150
            port = PrinterInfo.Port.NET
            ipAddress = "192.168.0.50"
            paperSize = PrinterInfo.PaperSize.CUSTOM
            customPaperWidth = 58
            customPaperLength = 80

        }
        printer.printerInfo = settings

        if (printer.startCommunication()) {
            val result = printer.printText(content)
            printer.endCommunication()
            android.util.Log.i("PrintJob", "印刷結果: ${result.errorCode}")
        }
    }
}
*/