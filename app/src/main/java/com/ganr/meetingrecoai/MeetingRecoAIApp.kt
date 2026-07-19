package com.ganr.meetingrecoai

import android.app.Application
import com.ganr.meetingrecoai.data.AppDatabase

class MeetingRecoAIApp : Application() {
    // 数据库单例
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
    }
}