package org.withus.app.model

import com.google.gson.annotations.SerializedName

// 1. 커플 키워드 조회용 모델 (/api/me/couple/keywords)
data class CoupleKeywordsData(
    @SerializedName("coupleKeywords") val coupleKeywords: List<CoupleKeyword>
)

data class CoupleKeyword(
    @SerializedName("keywordId") val keywordId: Int,
    @SerializedName("coupleKeywordId") val coupleKeywordId: Int,
    @SerializedName("content") val content: String
)

// 2. 전체 키워드 조회용 모델 (/api/keywords)
data class KeywordsData(
    @SerializedName("keywordInfoList") val keywordInfoList: List<KeywordInfo>
)

data class KeywordInfo(
    @SerializedName("keywordId") val keywordId: Int,
    @SerializedName("content") val content: String,
    @SerializedName("displayOrder") val displayOrder: Int
)

data class KeywordListResponse(
    @SerializedName("keywordInfoList")
    val keywordInfoList: List<KeywordInfo>
)

// --- 2. 키워드 업로드 (PUT) 요청 모델 ---
data class KeywordUpdateRequest(
    @SerializedName("defaultKeywordIds")
    val defaultKeywordIds: List<Long>,

    // 서버가 문자열 형태의 리스트("['A', 'B']")를 원하므로 String으로 선언
    @SerializedName("customKeywords")
    val customKeywords: String
)
