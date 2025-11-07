package cn.nepuko.sklhelper.model

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val question: String,
    val answer: String
)

