package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual fun getDatabaseBuilder(context: Any?): RoomDatabase.Builder<BankDatabase> {
    val appContext = (context as? Context)?.applicationContext ?: throw IllegalArgumentException("Android Context required")
    val dbFile = appContext.getDatabasePath("bank_tracker_db")
    return Room.databaseBuilder(
        context = appContext,
        name = dbFile.absolutePath
    )
}
