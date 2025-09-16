@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.CorporateLogoB
import com.example.myapplication.ui.theme.TanugoBold

private data class UiOption(
    val id: String,
    val title: String,
    val details: List<String>
)

@Composable
fun HomeScreen(
    onClickCamera: () -> Unit,
    onClickTest: () -> Unit
) {
    // タイトルのフォント/サイズ（ランタイム切替）
    var titleFont by remember { mutableStateOf(CorporateLogoB) }
    var titleSize by remember { mutableStateOf(22.sp) }

    // ▼DDLサンプル
    val options = remember {
        listOf(
            UiOption(
                id = "GENPIN_ADVAN",
                title = "現品票：アドバン",
                details = listOf(
                    "プリンタ：RJ-2150　ラベル：55mmx80mm　動作：剥離　備考：検印無し"
                )
            ),
            UiOption(
                id = "GENPIN_JB",
                title = "現品票：ジェイビル",
                details = listOf(
                    "プリンタ：RJ-2150　ラベル：55mmx80mm　動作：剥離　備考：検印有り"
                )
            ),
            UiOption(
                id = "GENPIN_CHUO",
                title = "現品票：中央電材",
                details = listOf(
                    "プリンタ：TD-4750TNWB-LP　ラベル：55mmx80mm　動作：剥離　備考：QRコード"
                )
            ),
            UiOption(
                id = "BOM_GENERAL",
                title = "部品表：一般",
                details = listOf(
                    "プリンタ：TD-4750TNWB-CU　ラベル：55mmx10mm　動作：オートカット　備考：QRコード出力"
                )
            ),
            UiOption(
                id = "BOM_SPECIAL",
                title = "部品表：特殊",
                details = listOf(
                    "プリンタ：TD-4750TNWB-CU　ラベル：55mmx10mm　動作：オートカット　備考：CODE39出力"
                )
            )
        )
    }
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(options.first()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "アイデア募集中",
                        fontFamily = titleFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = titleSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1976D2)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ▼フォーマット（DDL）
            Text("フォーマット", style = MaterialTheme.typography.titleMedium)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selected.title,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("選択してください") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.title) },
                            onClick = {
                                selected = opt
                                expanded = false
                            }
                        )
                    }
                }
            }

            // ▼説明を“表カード”で
            Text("内容", style = MaterialTheme.typography.titleMedium)
            SpecCard(selected.details)

            // 余白で下に押す（Columnの子なので weight が使える）
            Spacer(Modifier.weight(1f))

            // 開発用：フォント切替/サイズ調整（不要になったら消してOK）
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { titleFont = CorporateLogoB }) { Text("ロゴB") }
                OutlinedButton(onClick = { titleFont = TanugoBold })    { Text("たぬゴ") }
                Slider(
                    value = titleSize.value,
                    onValueChange = { titleSize = it.sp },
                    valueRange = 18f..30f,
                    modifier = Modifier.weight(1f)
                )
            }

            // ▼アクション（左：CAMERA 右：TEST）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onClickCamera,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text("CAMERA") }

                OutlinedButton(
                    onClick = onClickTest,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text("TEST") }
            }
        }
    }
}

/* ---- 表表示用の小コンポーネント（トップレベルに置く） ---- */

@Composable
private fun SpecCard(lines: List<String>) {
    Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            lines.forEach { line ->
                val pairs = line.split('　') // 全角スペースで区切る
                    .mapNotNull { seg ->
                        val i = seg.indexOf('：') // 全角コロン
                        if (i > 0) seg.substring(0, i) to seg.substring(i + 1) else null
                    }
                SpecRowGrid(pairs)
                Divider()
            }
        }
    }
}

@Composable
private fun SpecRowGrid(pairs: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        pairs.forEach { (k, v) ->
            Row(Modifier.fillMaxWidth()) {
                Text(k, modifier = Modifier.width(76.dp), color = MaterialTheme.colorScheme.primary)
                Text(v, modifier = Modifier.weight(1f))
            }
        }
    }
}
