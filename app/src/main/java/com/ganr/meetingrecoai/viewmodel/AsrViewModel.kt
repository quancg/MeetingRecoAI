package com.ganr.meetingrecoai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ganr.meetingrecoai.MeetingRecoAIApp
import com.ganr.meetingrecoai.data.MeetingEntity
import com.ganr.meetingrecoai.manager.SherpaAsrManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// 录音状态
data class AsrUiState(
    val isRecording: Boolean = false,
    val partialText: String = "",    // 实时片段
    val finalText: String = "",      // 停止后的完整文本
    val modelReady: Boolean = false
)

class AsrViewModel(application: Application) : AndroidViewModel(application) {

    private val asrManager = SherpaAsrManager(application)
    private val app = application as MeetingRecoAIApp
    private val dao = app.database.meetingDao()

    private val _uiState = MutableStateFlow(AsrUiState())
    val uiState: StateFlow<AsrUiState> = _uiState.asStateFlow()

    // 模型所在外部目录（需 adb push 模型到此）
    private val modelDir: String by lazy {
        application.getExternalFilesDir("models")?.absolutePath ?: "/sdcard/Android/data/com.ganr.meetingrecoai/files/models"
    }

    /**
     * 初始化 sherpa-onnx 模型，应在应用启动时调用。
     */
    fun initModel() {
        viewModelScope.launch {
            try {
                asrManager.initialize(modelDir)
                _uiState.update { it.copy(modelReady = true) }
            } catch (e: Exception) {
                // 模型加载失败处理
                _uiState.update { it.copy(modelReady = false) }
            }
        }
    }

    fun startRecording() {
        asrManager.startRecognition()
        _uiState.update { it.copy(isRecording = true) }
        // 收集实时结果
        viewModelScope.launch {
            asrManager.partialResult.collect { partial ->
                _uiState.update { it.copy(partialText = partial) }
            }
        }
    }

    fun stopRecording() {
        asrManager.stopRecognition()
        _uiState.update { it.copy(isRecording = false) }
        // 将最终文本保存到数据库
        viewModelScope.launch {
            val finalText = asrManager.finalResult.value
            if (finalText.isNotBlank()) {
                val meeting = MeetingEntity(transcript = finalText)
                dao.insert(meeting)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        asrManager.release()
    }
}