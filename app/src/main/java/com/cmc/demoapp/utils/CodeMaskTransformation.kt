package com.cmc.demoapp.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle

// 코드 전용 마스크 트랜스포메이션 수정
class CodeMaskTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val input = text.text
        val mask = "00000000"
        val combined = input + mask.takeLast(8 - input.length)

        val out = buildAnnotatedString {
            for (i in combined.indices) {
                // 입력된 길이는 검정색, 나머지는 회색
                val color = if (i < input.length) Color(0xFF000000) else Color(0xFFC7C7C7)
                withStyle(SpanStyle(color = color)) {
                    append(combined[i])
                }
            }
        }

        // 에러 해결을 위한 핵심: 커서 위치 매핑
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                // 실제 입력 위치를 변형된 텍스트 위치로 그대로 보냄
                return offset
            }

            override fun transformedToOriginal(offset: Int): Int {
                // 변형된 텍스트(8자리) 위에서의 커서 위치가 실제 입력 길이를 넘지 못하게 제한
                return offset.coerceAtMost(input.length)
            }
        }

        return TransformedText(out, offsetMapping)
    }
}