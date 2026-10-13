package com.quizlock.app.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Question(
    @SerializedName("id")
    val id: Long = System.currentTimeMillis(),
    
    @SerializedName("question")
    val question: String,
    
    @SerializedName("options")
    val options: List<String>,
    
    @SerializedName("correctIndex")
    val correctIndex: Int,
    
    @SerializedName("category")
    val category: String = "General"
) : Serializable
