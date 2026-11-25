package com.hazia.chatelaine.data

import android.content.Context
import androidx.room.Room

fun openChatelaineDatabase(context: Context): DatabaseOpen<ChatelaineDatabase> {
    val app = context.applicationContext
    return openDatabase(
        file = app.getDatabasePath(DATABASE_NAME),
        builder = { Room.databaseBuilder<ChatelaineDatabase>(app, DATABASE_NAME) },
        driver = KeepFileOnCorruptionDriver(),
    )
}
