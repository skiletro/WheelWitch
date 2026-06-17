package com.skiletro.wheelwitch.service

object SaveManager {
    const val SAVE_RELATIVE = "riivolution/save/RetroWFC/RMCP/rksys.dat"

    fun hasSaveFile(storage: PackStorage): Boolean {
        return storage.fileExists(SAVE_RELATIVE)
    }

    fun deleteSave(storage: PackStorage): Boolean {
        return storage.deleteFile(SAVE_RELATIVE)
    }
}
