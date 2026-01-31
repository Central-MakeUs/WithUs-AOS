package com.widthus.app.model

data class QuestionAnswer(
    val userName: String,
    val profileImageUrl: String? = null,
    val time: String,
    val imageUrl: String,
    val comment: String
)

data class MemorySet(
    val myAnswer: QuestionAnswer,
    val partnerAnswer: QuestionAnswer
)
