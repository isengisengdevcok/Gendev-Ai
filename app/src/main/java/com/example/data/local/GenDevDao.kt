package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GenDevDao {
    // Sessions
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun observeSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): ChatSessionEntity?

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    fun observeSessionById(sessionId: Long): Flow<ChatSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    // Messages
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC, id ASC")
    fun observeMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC, id ASC")
    suspend fun getMessagesForSession(sessionId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId AND id >= :fromMessageId")
    suspend fun deleteMessagesFrom(sessionId: Long, fromMessageId: Long)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: Long)

    // Usage Analytics
    @Query("SELECT * FROM usage_records ORDER BY timestamp DESC LIMIT 200")
    fun observeUsageRecords(): Flow<List<UsageRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageRecord(record: UsageRecordEntity): Long

    @Query("DELETE FROM usage_records")
    suspend fun clearUsageRecords()

    // Prompt Presets
    @Query("SELECT * FROM prompt_presets ORDER BY updatedAt DESC")
    fun observePromptPresets(): Flow<List<PromptPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromptPreset(preset: PromptPresetEntity): Long

    @Query("DELETE FROM prompt_presets WHERE id = :presetId")
    suspend fun deletePromptPreset(presetId: Long)

    // Generated Android Project Studio Files
    @Query("SELECT * FROM project_files ORDER BY path ASC")
    fun observeProjectFiles(): Flow<List<ProjectFileEntity>>

    @Query("SELECT * FROM project_files ORDER BY path ASC")
    suspend fun getProjectFiles(): List<ProjectFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProjectFile(file: ProjectFileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProjectFiles(files: List<ProjectFileEntity>)

    @Query("DELETE FROM project_files WHERE id = :fileId")
    suspend fun deleteProjectFile(fileId: Long)

    @Query("DELETE FROM project_files")
    suspend fun clearProjectFiles()

    // Dynamic Gemini Model Catalog
    @Query("SELECT * FROM gemini_models ORDER BY sortOrder ASC, displayName ASC")
    fun observeGeminiModels(): Flow<List<GeminiModelEntity>>

    @Query("SELECT * FROM gemini_models ORDER BY sortOrder ASC, displayName ASC")
    suspend fun getGeminiModels(): List<GeminiModelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGeminiModels(models: List<GeminiModelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGeminiModel(model: GeminiModelEntity)

    @Query("DELETE FROM gemini_models WHERE id = :modelId AND isDynamic = 1")
    suspend fun deleteDynamicModel(modelId: String)
}
