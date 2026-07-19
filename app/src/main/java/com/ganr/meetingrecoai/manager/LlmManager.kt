package com.ganr.meetingrecoai.manager

import android.content.Context
import com.google.ai.edge.litertlm.LlmInference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * 封装 LiteRT-LM 本地大模型推理。
 * 使用 Gemma 4 E4B-it INT4 量化模型 (.litertlm)。
 */
class LlmManager(private val context: Context) {

    // 流式输出文本
    private val _generatedText = MutableStateFlow("")
    val generatedText: StateFlow<String> = _generatedText

    private var inference: LlmInference? = null
    private var isInitialized = false

    /**
     * 加载模型文件，需传入 .litertlm 文件的绝对路径。
     */
    fun initialize(modelPath: String) {
        inference = LlmInference.createFromFile(context, modelPath)
        isInitialized = true
    }

    /**
     * 执行流式推理。
     * @param prompt 完整的提示词
     */
    fun generateStream(prompt: String) {
        if (!isInitialized) return
        _generatedText.value = ""
        // LiteRT-LM 流式生成示例，具体 API 需参考文档
        // 假设提供 generateStream 方法并传入回调
        inference?.generateStream(prompt) { partialResponse ->
            // partialResponse 为生成的一小段文本
            _generatedText.value += partialResponse
        }
    }

    fun release() {
        inference?.close()
        inference = null
        isInitialized = false
    }
}