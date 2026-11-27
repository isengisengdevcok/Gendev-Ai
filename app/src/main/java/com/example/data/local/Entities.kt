package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val modeId: String,
    val modelId: String,
    val systemPrompt: String,
    val compressedContextSummary: String = "",
    val totalInputTokens: Int = 0,
    val totalOutputTokens: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val role: String, // "user" or "model"
    val content: String,
    val originalPrompt: String? = null,
    val rephrasedPrompt: String? = null,
    val attachmentSummary: String? = null,
    val modelId: String = "",
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val latencyMs: Long = 0L,
    val aceNote: String? = null,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "usage_records")
data class UsageRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val modelId: String,
    val engineMode: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val estimatedCostUsd: Double,
    val latencyMs: Long,
    val success: Boolean,
    val errorDetail: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "prompt_presets")
data class PromptPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val modeId: String,
    val systemPrompt: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_files", indices = [Index(value = ["path"], unique = true)])
data class ProjectFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val path: String,
    val content: String,
    val language: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "gemini_models")
data class GeminiModelEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val tierName: String,
    val tagline: String,
    val contextWindowTokens: Int,
    val maxOutputTokens: Int,
    val inputPricePerMillionUsd: Double,
    val outputPricePerMillionUsd: Double,
    val recommendedFor: String,
    val isDynamic: Boolean = false,
    val supportsLegacySamplingParams: Boolean = true,
    val sortOrder: Int = 10,
    val updatedAt: Long = System.currentTimeMillis()
)
