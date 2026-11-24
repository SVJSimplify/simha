package com.simha.app

import android.app.Application
import androidx.room.Room
import com.simha.app.data.GuardianRepo
import com.simha.app.data.MIGRATION_1_2
import com.simha.app.data.MIGRATION_2_3
import com.simha.app.data.Prefs
import com.simha.app.data.Remote
import com.simha.app.data.Repo
import com.simha.app.data.SimhaDb
import com.simha.app.llm.LlmClient
import com.simha.app.llm.LlmStore
import com.simha.app.monitor.Notifier
import com.simha.app.monitor.Work
import com.simha.app.voice.Recorder
import com.simha.app.voice.Voice

class ServiceLocator(app: Application) {
    val db: SimhaDb = Room.databaseBuilder(app, SimhaDb::class.java, "simha.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    val prefs = Prefs(app)
    val remote = Remote()
    val repo = Repo(app, db.dao(), prefs, remote)
    val guardian = GuardianRepo(db.guardianDao(), repo, remote).also { g -> repo.signalHook = { g.onEvent(it) } }
    val voice = Voice(app)
    val recorder = Recorder(app, voice, repo)
    val llmStore = LlmStore(app)
    val llm = LlmClient()
}

class SimhaApp : Application() {
    lateinit var sl: ServiceLocator

    override fun onCreate() {
        super.onCreate()
        sl = ServiceLocator(this)
        Notifier.ensureChannels(this)
        Work.schedule(this)
        com.simha.app.update.UpdateWorker.schedule(this)
    }
}
