package com.example.vocabgame.model

/**
 * Historical game record for a completed round of "Rearrange".
 */
data class RearrangeRecord(
    val id: Long,
    val baseWord: String,
    val wordsFoundCount: Int,
    val wordsFoundList: List<String>,
    val possibleWordsCount: Int,
    val points: Int,
    val timestamp: Long
)
