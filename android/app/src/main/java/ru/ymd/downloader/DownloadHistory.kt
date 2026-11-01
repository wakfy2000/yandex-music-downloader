package ru.ymd.downloader

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * История успешно сохранённых треков. Ключ — постоянный ID трека Яндекс.Музыки,
 * токен намеренно не участвует, поэтому его замена не запускает загрузку заново.
 */
class DownloadHistory(private val context: Context) {
    private val prefs = context.getSharedPreferences("download_history", Context.MODE_PRIVATE)

    fun isDownloaded(track: Track): Boolean {
        if (prefs.getBoolean(track.trackId, false)) return true

        // Подхватываем файлы, скачанные предыдущими версиями приложения,
        // до появления локальной истории.
        val base = track.baseName()
        val extensions = listOf("flac", "mp3", "m4a")
        val found = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME} IN (?,?,?)"
            val names = extensions.map { "$base.$it" }
            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                arrayOf(Environment.DIRECTORY_MUSIC + File.separator + "MusicDownloader" + File.separator) + names,
                null
            )?.use { it.moveToFirst() } ?: false
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "MusicDownloader"
            )
            extensions.any { File(dir, "$base.$it").isFile }
        }

        if (found) markDownloaded(track)
        return found
    }

    fun markDownloaded(track: Track) {
        prefs.edit().putBoolean(track.trackId, true).apply()
    }
}
