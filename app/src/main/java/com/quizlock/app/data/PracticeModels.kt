package com.quizlock.app.data

data class PracticeSubject(
    val name: String,
    val iconEmoji: String,
    val totalQuestions: Int,
    val topics: List<PracticeTopic>
)

data class PracticeTopic(
    val subjectName: String,
    val categoryName: String,
    val displayName: String,
    val totalQuestions: Int,
    val sets: List<PracticeSet>
) {
    val completedSetsCount: Int
        get() = sets.count { it.isCompleted }

    val masteredSetsCount: Int
        get() = sets.count { it.isCompleted && it.accuracyPercent >= 80 }

    val progressPercent: Int
        get() = if (sets.isNotEmpty()) ((completedSetsCount.toFloat() / sets.size) * 100).toInt() else 0
}

data class PracticeSet(
    val setKey: String,
    val subjectName: String,
    val topicDisplayName: String,
    val categoryName: String,
    val setNumber: Int,
    val questionIds: List<Long>,
    val startRange: Int,
    val endRange: Int,
    var score: Int = 0,
    var isCompleted: Boolean = false,
    var lastAttemptTimestamp: Long = 0L
) {
    val totalQuestions: Int
        get() = questionIds.size

    val accuracyPercent: Int
        get() = if (totalQuestions > 0) ((score.toFloat() / totalQuestions) * 100).toInt().coerceIn(0, 100) else 0

    val statusBadge: String
        get() = when {
            !isCompleted -> "Unattempted"
            accuracyPercent >= 80 -> "Mastered"
            accuracyPercent >= 50 -> "Good"
            else -> "Needs Revision"
        }
}

data class LastPracticedInfo(
    val setKey: String,
    val title: String,
    val subtitle: String,
    val score: Int,
    val total: Int,
    val timestamp: Long,
    val questionIds: List<Long>
) {
    val accuracyPercent: Int
        get() = if (total > 0) ((score.toFloat() / total) * 100).toInt().coerceIn(0, 100) else 0
}
