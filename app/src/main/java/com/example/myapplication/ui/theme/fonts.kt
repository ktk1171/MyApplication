package com.example.myapplication.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.myapplication.R

// タイトル：コーポレート・ロゴB（Bold）
val CorporateLogoB = FontFamily(
    Font(R.font.logotypejp_mp_b_1_1, weight = FontWeight.Bold)
)
// 予備：たぬゴ丸 Bold
val TanugoBold = FontFamily(
    Font(R.font.tanugo_round_bold, weight = FontWeight.Bold)
)
// 汎用（必要なら）
val NotoSansJP = FontFamily(Font(R.font.notosansjp_vf))
val NotoSerifJP = FontFamily(Font(R.font.notoserifjp_vf))
// CODE39（フォントで出す場合のFamily）
val Code39 = FontFamily(Font(R.font.code39))

