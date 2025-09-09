package com.example.myapplication
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Response
import com.brother.sdk.lmprinter.*
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.brother.sdk.lmprinter.setting.RJTemplatePrintSettings
import com.brother.sdk.lmprinter.TemplateObjectReplacer
import com.brother.sdk.lmprinter.PrinterModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import android.os.Build
import androidx.activity.viewModels
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
/**
 * 名前で差し替えるテンプレ印刷（IOスレッドで実行）
 * 成功: Result.success(Unit) / 失敗: Result.failure(Throwable)
 */
suspend fun printTemplateLabelIO(
    templateKey: Int,
    fields: List<Pair<String, String>>,
    printerIp: String = "192.168.0.241",
    copies: Int = 1
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val channel = Channel.newWifiChannel(printerIp)
        val result = PrinterDriverGenerator.openChannel(channel)
        if (result.error.code != OpenChannelError.ErrorCode.NoError) {
            error("OpenChannel失敗: ${result.error.code}")
        }

        val driver = result.driver
        try {
            val settings = com.brother.sdk.lmprinter.setting.RJTemplatePrintSettings(
                PrinterModel.RJ_2150
            ).apply {
                numCopies = copies
            }

            val replacers = ArrayList<TemplateObjectReplacer>(fields.size).apply {
                fields.forEach { (name, value) ->
                    add(
                        TemplateObjectReplacer(
                            name,
                            value,
                            TemplateObjectReplacer.Encode.SHIFT_JIS
                        )
                    )
                }
            }

            val printError = driver.printTemplate(templateKey, settings, replacers)
            if (printError.code != PrintError.ErrorCode.NoError) {
                error("printTemplate失敗: ${printError.code}")
            }
        } finally {
            try { driver.closeChannel() } catch (_: Throwable) {}
        }
    }
}

data class TantoData(
    val 担当者コード: String,
    val 担当者正式名: String,
    val 担当者略称: String
)

interface TantoApi {
    @GET("api/tanto")
    suspend fun getTanto(@Query("code") code: String): Response<TantoData>
}

fun printTemplateLabel(
    templateKey: Int,
    index: Int,
    newText: String,
    encode: TemplateObjectReplacer.Encode = TemplateObjectReplacer.Encode.UTF_8,
    printerIp: String = "192.168.0.241",
    copies: Int = 1
) {
    val channel = Channel.newWifiChannel(printerIp)
    val result = PrinterDriverGenerator.openChannel(channel)

    if (result.error.code != OpenChannelError.ErrorCode.NoError) {
        Log.e("BrotherPrint", "接続失敗: ${result.error.code}")
        return
    }

    val driver = result.driver
    val settings = RJTemplatePrintSettings(PrinterModel.RJ_2150).apply {
        numCopies = copies
    }

    val replacers = arrayListOf(
        TemplateObjectReplacer(index, newText, encode)
    )

    val printError = driver.printTemplate(templateKey, settings, replacers)
    if (printError.code != PrintError.ErrorCode.NoError) {
        Log.e("BrotherPrint", "印刷失敗: ${printError.code}")
    } else {
        Log.d("BrotherPrint", "印刷成功")
    }

    driver.closeChannel()
}

/**
 * ② オブジェクト「名前」で差し替える版（新規）
 *    呼び出し側が listOf("code" to "...", "name" to "...") などを渡せるようにする。
 */
fun printTemplateLabel(
    templateKey: Int,
    fields: List<Pair<String, String>>,
    printerIp: String = "192.168.0.241",
    copies: Int = 1
) {
    val channel = Channel.newWifiChannel(printerIp)
    val result = PrinterDriverGenerator.openChannel(channel)

    if (result.error.code != OpenChannelError.ErrorCode.NoError) {
        Log.e("BrotherPrint", "接続失敗: ${result.error.code}")
        return
    }

    val driver = result.driver
    val settings = RJTemplatePrintSettings(PrinterModel.RJ_2150).apply {
        numCopies = copies
    }
    // Pair<String,String> → TemplateObjectReplacer に変換（名前指定）
    val replacers = ArrayList<TemplateObjectReplacer>(fields.size).apply {
        fields.forEach { (name, value) ->
            add(TemplateObjectReplacer(name, value, TemplateObjectReplacer.Encode.SHIFT_JIS))
        }
    }
    val printError = driver.printTemplate(templateKey, settings, replacers)
    if (printError.code != PrintError.ErrorCode.NoError) {
        Log.e("BrotherPrint", "印刷失敗: ${printError.code}")
    } else {
        Log.d("BrotherPrint", "印刷成功")
    }
    driver.closeChannel()
}



class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "greeting"
                ) {
                    composable("greeting") {
                        GreetingScreen(
                            scannedCode = viewModel.selectedBarcode ?: "",
                            onScanClick = {
                                navController.navigate("camera")
                            }
                        )
                    }

                    composable("camera") {
                        CameraPreview(
                            viewModel = viewModel,
                            onBarcodeSelected = { code ->
                                viewModel.onBarcodeSelected(code)
                                navController.navigate("greeting")
                            }
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun GreetingScreen(
    scannedCode: String,
    onScanClick: () -> Unit
) {
    var name by remember { mutableStateOf("Android") }
    var inputText by remember { mutableStateOf(scannedCode) }
    var isLoading by remember { mutableStateOf(false) }
    var triggerApiCall by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusColor by remember { mutableStateOf(Color.DarkGray) }
    val context = LocalContext.current
    val retrofit: Retrofit = remember {
        Retrofit.Builder()
            .baseUrl("http://192.168.0.84:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val api: TantoApi = remember { retrofit.create(TantoApi::class.java) }

    val scope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }
    var printMessage by remember { mutableStateOf<String?>(null) }

    fun vibrate(isSuccess: Boolean) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val duration = if (isSuccess) 100L else 500L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
    LaunchedEffect(triggerApiCall) {
        if (triggerApiCall) {
            statusMessage = "担当者名を取得中..."
            try {
                val response = api.getTanto(inputText)
                Log.d("GreetingScreen", "API呼び出しコード: $inputText")
                if (response.isSuccessful) {
                    val data = response.body()
                    name = data?.担当者略称 ?: "不明"
                    statusMessage = "担当者名を取得しました: $name"
                    vibrate(true) // 成功
                } else {
                    name = "取得失敗"
                    statusMessage = "API応答が不正です (${response.code()})"
                    vibrate(false) // 失敗
                    Log.e("GreetingScreen", "API失敗: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                name = "エラー: ${e.message}"
                statusMessage = "API接続エラー: ${e.message}"
                vibrate(false) // 失敗
            } finally {
                isLoading = false
                triggerApiCall = false
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Hello $name!")
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("担当者コードを入力") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onScanClick, modifier = Modifier.fillMaxWidth()) {
            Text("カメラ起動")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                isLoading = true
                triggerApiCall = true
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "取得中..." else "担当者名を取得")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    isPrinting = true
                    statusMessage = "印刷処理を開始しました"
                    val result = printTemplateLabelIO(
                        templateKey = 1,
                        fields = listOf(
                            "code" to inputText,
                            "name" to name
                        )
                    )
                    isPrinting = false

                    result.onSuccess {
                        statusMessage = "印刷成功"
                        vibrate(true)
                    }.onFailure { e ->
                        statusMessage = "印刷失敗: ${e.message ?: e::class.java.simpleName}"
                        vibrate(false)
                        Log.e("BrotherPrint", "print error", e)
                    }

                }
            },
            enabled = !isPrinting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isPrinting) "印刷中..." else "印刷する")
        }

        if (statusMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusMessage!!,
                color = Color.DarkGray,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEFEFEF))
                    .padding(8.dp)
            )
        }
    }
}
@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Composable
fun AppNavHost(
    scannedCode: String,
    onScanClick: () -> Unit
) {
    Column {
        Button(onClick = onScanClick) {
            Text("スキャン開始")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "読み取ったコード: $scannedCode")
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        GreetingScreen(
            scannedCode = "プレビュー用コード",
            onScanClick = {}
        )
    }
}
