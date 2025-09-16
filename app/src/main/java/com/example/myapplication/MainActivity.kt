package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.myapplication.ui.home.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    HomeScreen(
                        onClickCamera = {
                            // TODO: ここでカメラ画面へ遷移（NavHostを後で入れる）
                        },
                        onClickTest = {
                            // TODO: ここでテスト印刷画面へ遷移
                        }
                    )
                }
            }
        }
    }
}
