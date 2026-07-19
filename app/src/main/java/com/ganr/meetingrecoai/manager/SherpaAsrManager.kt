package com.ganr.meetingrecoai.manager

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * 封装 sherpa-onnx 的流式语音识别。
 * 模型文件需事先通过 adb push 放置到应用外部私有目录下的 models 文件夹。
 */
class SherpaAsrManager(private val context: Context) {

    // 识别结果片段（实时更新）
    private val _partialResult = MutableStateFlow("")
    val partialResult: StateFlow<String> = _partialResult

    // 完整识别文本（停止时拼接）
    private val _finalResult = MutableStateFlow("")
    val finalResult: StateFlow<String> = _finalResult

    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    // 音频参数：16kHz，单通道，16位 PCM
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    /**
     * 初始化识别器，需要模型目录路径。
     * @param modelDir 模型文件所在的外部文件夹绝对路径
     */
    fun initialize(modelDir: String) {
        val config = OnlineRecognizerConfig(
            modelDir = modelDir,
            // SenseVoice 模型及 token、VAD 等由 sherpa-onnx 自动加载
            decodingMethod = "greedy_search",
            enableEndpoint = true,
            rule1MinTrailingSilence = 1.2f,
            rule2MinTrailingSilence = 0.5f,
            rule3MinUtteranceLength = 20.0f
        )
        recognizer = OnlineRecognizer(config)
    }

    /**
     * 开始录音识别。需要在权限允许后调用。
     */
    fun startRecognition() {
        if (isRecording) return
        isRecording = true

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        audioRecord?.startRecording()

        // 创建识别流
        stream = recognizer?.createStream()

        // 启动线程持续读取音频数据送入识别器
        Thread {
            val buffer = ShortArray(bufferSize)
            while (isRecording) {
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readSize > 0) {
                    val samples = buffer.copyOf(readSize)
                    // 将音频数据送入流
                    stream?.acceptWaveform(samples, sampleRate)
                    // 解码当前流，获取最新识别结果
                    recognizer?.let { rec ->
                        rec.decodeStream(stream)
                        val text = rec.getText(stream)
                        _partialResult.value = text
                    }
                }
            }
        }.start()
    }

    /**
     * 停止录音，拼接最终结果并释放资源。
     */
    fun stopRecognition() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        // 获取最终文本
        recognizer?.let { rec ->
            val finalText = rec.getText(stream)
            _finalResult.value = finalText
            _partialResult.value = ""
        }

        stream?.let {
            recognizer?.reset(it)
        }
        stream = null
    }

    fun release() {
        stopRecognition()
        recognizer?.release()
        recognizer = null
    }
}