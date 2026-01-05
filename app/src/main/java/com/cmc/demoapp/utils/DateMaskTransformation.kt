package com.cmc.demoapp.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle

class DateMaskTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val input = text.text // 실제 사용자가 입력한 숫자
        val mask = "00000000"
        val combined = input + mask.takeLast(8 - input.length)

        // AnnotatedString을 사용하여 스타일이 적용된 텍스트 생성
        val out = buildAnnotatedString {
            // 날짜 포맷팅 로직 (2024년 01월 01일)
            for (i in combined.indices) {
                // 입력된 숫자는 검정색(000000), 나머지는 회색(C7C7C7)
                val targetColor = if (i < input.length) Color(0xFF000000) else Color(0xFFC7C7C7)

                withStyle(style = SpanStyle(color = targetColor)) {
                    append(combined[i])
                }

                // 구분자 추가 (년, 월, 일)
                // 구분자 색상도 입력 진행도에 따라 맞추거나 고정 회색으로 설정
                val separatorColor = if (i < input.length) Color(0xFF000000) else Color(0xFFC7C7C7)
                withStyle(style = SpanStyle(color = separatorColor)) {
                    if (i == 3) append("년 ")
                    if (i == 5) append("월 ")
                    if (i == 7) append("일")
                }
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 5) return offset + 2
                if (offset <= 7) return offset + 4
                return offset + 5
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset.coerceAtMost(input.length)
                if (offset <= 8) return (offset - 2).coerceAtMost(input.length)
                if (offset <= 12) return (offset - 4).coerceAtMost(input.length)
                return (offset - 5).coerceAtMost(input.length)
            }
        }

        return TransformedText(out, offsetMapping)
    }
}