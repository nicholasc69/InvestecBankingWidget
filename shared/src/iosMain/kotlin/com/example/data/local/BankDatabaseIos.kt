@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSUserDomainMask

actual fun getDatabaseBuilder(context: Any?): RoomDatabase.Builder<BankDatabase> {
    val fileManager = NSFileManager.defaultManager
    val documentDirectory = fileManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )?.path ?: (NSHomeDirectory() + "/Documents")

    val dbFilePath = "$documentDirectory/bank_tracker_db"
    return Room.databaseBuilder<BankDatabase>(
        name = dbFilePath,
        factory = { BankDatabaseConstructor.initialize() }
    ).setDriver(BundledSQLiteDriver())
}
