package com.skiletro.wheelwitch.data

import android.content.Context
import java.io.File

object SaveManager {
    const val SAVE_RELATIVE = "riivolution/save/RetroWFC/RMCP/rksys.dat"

    fun hasSaveFile(storage: PackStorage): Boolean {
        return storage.fileExists(SAVE_RELATIVE)
    }

    fun deleteSave(storage: PackStorage): Boolean {
        return storage.deleteFile(SAVE_RELATIVE)
    }

    fun backupSaveToCache(context: Context, storage: PackStorage) {
        val data = storage.readBytes(SAVE_RELATIVE) ?: return
        val backupDir = File(context.cacheDir, "save_backup")
        backupDir.mkdirs()
        File(backupDir, "rksys.dat").writeBytes(data)
    }

    fun deleteSaveBackup(context: Context) {
        File(context.cacheDir, "save_backup").deleteRecursively()
    }
}
