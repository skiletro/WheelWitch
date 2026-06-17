package com.skiletro.wheelwitch.data

import android.content.Context
import java.io.File

object SaveManager {
    /** Relative path of the save file within the storage root. */
    const val SAVE_RELATIVE = "riivolution/save/RetroWFC/RMCP/rksys.dat"

    /** Returns true if the save file exists in [storage]. */
    fun hasSaveFile(storage: PackStorage): Boolean {
        return storage.fileExists(SAVE_RELATIVE)
    }

    /** Deletes the save file from [storage]. Returns true if it existed and was deleted. */
    fun deleteSave(storage: PackStorage): Boolean {
        return storage.deleteFile(SAVE_RELATIVE)
    }

    /** Copies the save file to `cache/save_backup/rksys.dat` for safe keeping during updates. */
    fun backupSaveToCache(context: Context, storage: PackStorage) {
        val data = storage.readBytes(SAVE_RELATIVE) ?: return
        val backupDir = File(context.cacheDir, "save_backup")
        backupDir.mkdirs()
        File(backupDir, "rksys.dat").writeBytes(data)
    }

    /** Removes the cached save backup. */
    fun deleteSaveBackup(context: Context) {
        File(context.cacheDir, "save_backup").deleteRecursively()
    }
}
