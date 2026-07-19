package com.ganr.meetingrecoai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganr.meetingrecoai.viewmodel.AiViewModel
import com.ganr.meetingrecoai.viewmodel.AsrViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    asrViewModel: AsrViewModel = viewModel(),
    aiViewModel: AiViewModel = viewModel()
) {
    val asrState by asrViewModel.uiState.collectAsState()
    val aiState by aiViewModel.uiState.collectAsState()

    var question by remember { mutableStateOf("") }

    // 初始化模型
    LaunchedEffect(Unit) {
        asrViewModel.initModel()
        aiViewModel.initModel()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MeetingRecoAI") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 录音控制区域
            Button(
                onClick = {
                    if (asrState.isRecording) asrViewModel.stopRecording()
                    else asrViewModel.startRecording()
                },
                enabled = asrState.modelReady,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (asrState.isRecording)
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (asrState.isRecording) "停止录音" else "开始录音")
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (!asrState.modelReady) {
                Text("语音模型未就绪，请检查模型文件", color = MaterialTheme.colorScheme.error)
            }
            if (asrState.isRecording) {
                Text("录音中...", color = MaterialTheme.colorScheme.error)
            }

            // 实时转写显示区
            Text(
                text = "实时转写：",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = asrState.partialText.ifEmpty { "（等待语音输入）" },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            // 完整文本显示（停止后）
            if (asrState.finalText.isNotEmpty()) {
                Text(
                    text = "最终转写文本：",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    text = asrState.finalText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // LLM 操作区：生成纪要
            Button(
                onClick = {
                    // 使用最终的完整转写文本生成纪要
                    aiViewModel.generateSummary(asrState.finalText.ifEmpty { asrState.partialText })
                },
                enabled = aiState.modelReady && (asrState.finalText.isNotEmpty() || asrState.partialText.isNotEmpty())
            ) {
                Text("生成会议纪要")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 纪要结果显示
            if (aiState.outputText.isNotEmpty()) {
                Text(
                    text = "会议纪要：",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    text = aiState.outputText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }

            if (aiState.isGenerating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // 自由问答区
            Text(
                text = "提问（基于会议内容）",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("输入问题") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    aiViewModel.askQuestion(
                        question,
                        asrState.finalText.ifEmpty { asrState.partialText }
                    )
                },
                enabled = aiState.modelReady && question.isNotBlank()
            ) {
                Text("提问")
            }

            if (aiState.outputText.isNotEmpty() && question.isNotEmpty()) {
                Text(
                    text = "回答：",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    text = aiState.outputText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}