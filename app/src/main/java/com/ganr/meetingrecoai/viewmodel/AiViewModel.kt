package com.ganr.meetingrecoai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ganr.meetingrecoai.MeetingRecoAIApp
import com.ganr.meetingrecoai.data.MeetingEntity
import com.ganr.meetingrecoai.manager.LlmManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AiUiState(
    val isGenerating: Boolean = false,
    val outputText: String = "",           // 生成的纪要或回答
    val modelReady: Boolean = false
)

class AiViewModel(application: Application) : AndroidViewModel(application) {

    private val llmManager = LlmManager(application)
    private val app = application as MeetingRecoAIApp
    private val dao = app.database.meetingDao()

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    // 模型文件路径（Gemma .litertlm 文件）
    private val modelPath: String by lazy {
        "${application.getExternalFilesDir("models")?.absolutePath}/gemma-4-e4b-it-int4.litertlm"
    }

    fun initModel() {
        viewModelScope.launch {
            try {
                llmManager.initialize(modelPath)
                _uiState.update { it.copy(modelReady = true) }
            } catch (e: Exception) {
                // 加载失败
                _uiState.update { it.copy(modelReady = false) }
            }
        }
    }

    /**
     * 基于当前会议转写文本生成纪要。
     */
    fun generateSummary(transcript: String) {
        val prompt = """
            你是一个专业的会议记录助手。请根据以下会议转录文本，生成一份简洁的会议纪要，
            包含：会议主题、讨论要点、决议、待办事项。用中文回答。
            
            会议转录：
            $transcript
        """.trimIndent()
        startGeneration(prompt)
    }

    /**
     * 自由问答，可附带会议上下文。
     */
    fun askQuestion(question: String, context: String = "") {
        val prompt = """
            根据以下会议内容回答问题，如果会议内容中没有相关信息，请如实说明。
            
            会议内容：
            $context
            
            问题：$question
        """.trimIndent()
        startGeneration(prompt)
    }

    private fun startGeneration(prompt: String) {
        _uiState.update { it.copy(isGenerating = true, outputText = "") }
        viewModelScope.launch {
            llmManager.generateStream(prompt)
            // 收集生成结果
            llmManager.generatedText.collect { text ->
                _uiState.update { it.copy(outputText = text) }
            }
            _uiState.update { it.copy(isGenerating = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        llmManager.release()
    }
}