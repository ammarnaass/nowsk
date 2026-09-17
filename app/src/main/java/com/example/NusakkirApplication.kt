package com.example

import android.app.Application
import com.example.data.api.AladhanRepository
import com.example.data.local.NusakkirDatabase
import com.example.data.repository.NusakkirRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NusakkirApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: NusakkirDatabase by lazy {
        NusakkirDatabase.getDatabase(this, applicationScope)
    }

    val repository: NusakkirRepository by lazy {
        NusakkirRepository(database)
    }

    val aladhanRepository: AladhanRepository by lazy {
        AladhanRepository(database = database)
    }

    companion object {
        lateinit var instance: NusakkirApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.example.ads.AdManager.initialize(this)
    }
}
