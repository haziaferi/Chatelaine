package com.hazia.chatelaine.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

fun openChatelaineDatabase(file: File, now: () -> Long = System::currentTimeMillis): DatabaseOpen<ChatelaineDatabase> =
    openDatabase(
        file = file,
        builder = { Room.databaseBuilder<ChatelaineDatabase>(name = file.absolutePath) },
        driver = BundledSQLiteDriver(),
        now = now,
    )
