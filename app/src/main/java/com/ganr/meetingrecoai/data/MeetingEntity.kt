package com.ganr.meetingrecoai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "会议记录",        // 可自定义标题
    val transcript: String = "",           // 原始转写全文
    val summary: String = "",              // LLM 生成的纪要
    val createdAt: Long = System.currentTimeMillis()
)