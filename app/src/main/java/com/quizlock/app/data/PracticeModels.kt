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


data class LevelMockTestMeta(
    val level: Int,
    val mockNumber: Int,
    val title: String,
    val levelBadge: String,
    val levelName: String,
    val questionCount: Int = 100,
    val durationMinutes: Int = 60,
    val maxMarks: Double = 200.0,
    val isCompleted: Boolean = false,
    val score: Double = 0.0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val unattemptedCount: Int = 0,
    val timeTakenSeconds: Int = 0,
    val lastAttemptTimestamp: Long = 0L
) {
    val accuracyPercent: Int
        get() {
            val attempted = correctCount + wrongCount
            return if (attempted > 0) ((correctCount.toFloat() / attempted) * 100).toInt().coerceIn(0, 100) else 0
        }

    val formattedScore: String
        get() = String.format("%.2f", score)
}

data class SectionalScore(
    val sectionKey: String,
    val sectionName: String,
    val iconEmoji: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val unattemptedCount: Int,
    val netScore: Double
) {
    val formattedScore: String
        get() = String.format("%.2f", netScore)
}
