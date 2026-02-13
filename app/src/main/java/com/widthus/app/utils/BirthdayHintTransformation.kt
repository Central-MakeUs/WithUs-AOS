package com.widthus.app.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.Color

class BirthdayHintTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val mask = "YYYY-MM-DD"
        val trimmed = text.text.filter { it.isDigit() } // 숫자만 추출

        val builder = AnnotatedString.Builder()

        // 1. 입력된 숫자 채우기
        var numberIndex = 0
        for (i in mask.indices) {
            if (numberIndex < trimmed.length) {
                // 숫자가 들어갈 자리이거나 대시(-) 자리인 경우 처리
                if (mask[i] == '-') {
                    builder.append('-')
                } else {
                    builder.append(trimmed[numberIndex])
                    numberIndex++
                }
            } else {
                // 2. 남은 부분은 마스크(Y, M, D, -)로 채우기 (회색 처리)
                builder.pushStyle(SpanStyle(color = Color(0xFFC7C7C7)))
                builder.append(mask[i])
                builder.pop()
            }
        }

        val out = builder.toAnnotatedString()

        // 커서 위치 매핑 로직
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                var transformedOffset = 0
                var digitCount = 0
                for (i in out.indices) {
                    if (digitCount == offset) break
                    transformedOffset++
                    if (out[i].isDigit()) digitCount++
                }
                return transformedOffset
            }

            override fun transformedToOriginal(offset: Int): Int {
                var originalOffset = 0
                for (i in 0 until offset.coerceAtMost(out.length)) {
                    if (out[i].isDigit()) originalOffset++
                }
                return originalOffset
            }
        }

        return TransformedText(out, offsetMapping)
    }
}