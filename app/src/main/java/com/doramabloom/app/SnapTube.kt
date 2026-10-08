package com.doramabloom.app

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.net.URLEncoder

/** Uma música que acabou de ser baixada no aparelho. */
class SnapFile(val uri: Uri, val title: String, val addedMs: Long, val fromSnap: Boolean)

/**
 * Ponte com o SnapTube: abre o app já na busca da trilha do dorama e, quando você volta,
 * acha sozinho o MP3 que acabou de baixar para virar a trilha sonora.
 * (O download em si é feito pelo SnapTube; o app só cuida do antes e do depois.)
 */
object SnapTube {
    private val PACKAGES = listOf("com.snaptube.premium", "com.snaptube.mm", "com.snaptube.lite")
    private val EXTS = listOf("mp3", "m4a", "aac", "opus", "ogg", "wav", "flac")
    private val DIRS = listOf(
        "SnapTube Audio", "Snaptube/audio", "snaptube/audio", "SnapTube/Audio",
        "Music/SnapTube Audio", "Music/Snaptube", "Music", "Download/SnapTube Audio", "Download"
    )

    fun installedPackage(ctx: Context): String? = PACKAGES.firstOrNull {
        try {
            ctx.packageManager.getPackageInfo(it, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun permission(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    fun hasPermission(ctx: Context): Boolean =
        ctx.checkSelfPermission(permission()) == PackageManager.PERMISSION_GRANTED

    /** Copia a busca para a área de transferência e abre o SnapTube na pesquisa. false = não deu para abrir. */
    fun open(a: Activity, query: String): Boolean {
        try {
            val cm = a.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("busca", query))
        } catch (e: Exception) {
        }
        val pkg = installedPackage(a) ?: return false
        try {
            val url = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8")
            val view = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            view.setPackage(pkg)
            a.startActivity(view)
            return true
        } catch (e: Exception) {
        }
        return try {
            val main = a.packageManager.getLaunchIntentForPackage(pkg) ?: return false
            a.startActivity(main)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Deixa o nome da música mais limpo: sem extensão, sem "(Official Video)" e sem "- YouTube". */
    fun cleanName(raw: String, hasExt: Boolean): String {
        var s = raw
        if (hasExt && s.contains('.')) s = s.substringBeforeLast('.')
        s = s.replace('_', ' ')
        s = s.replace(Regex("\\s*[\\(\\[][^\\)\\]]*(official|video|vídeo|lyric|lyrics|mv|audio|áudio|hd|4k|m/v)[^\\)\\]]*[\\)\\]]", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\s*-\\s*YouTube\\s*$", RegexOption.IGNORE_CASE), "")
        return s.replace(Regex("\\s+"), " ").trim()
    }

    /** Áudios adicionados ao aparelho depois de sinceMs; os que vieram do SnapTube aparecem primeiro. */
    fun recent(ctx: Context, sinceMs: Long, max: Int = 8): List<SnapFile> {
        val out = ArrayList<SnapFile>()
        try {
            val proj = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATA
            )
            val c = ctx.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                proj,
                MediaStore.Audio.Media.DATE_ADDED + " >= ? AND " + MediaStore.Audio.Media.SIZE + " > 0",
                arrayOf((sinceMs / 1000L).toString()),
                MediaStore.Audio.Media.DATE_ADDED + " DESC"
            )
            c?.use {
                var n = 0
                while (it.moveToNext() && n < 40) {
                    n++
                    val id = it.getLong(0)
                    val disp = it.getString(1) ?: ""
                    val title = it.getString(2) ?: ""
                    val added = it.getLong(3) * 1000L
                    val data = (it.getString(5) ?: "").lowercase()
                    val name = cleanName(if (title.isNotBlank() && !title.startsWith("audio_")) title else disp, title.isBlank() || title.startsWith("audio_"))
                    out.add(
                        SnapFile(
                            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                            name.ifEmpty { cleanName(disp, true) },
                            added,
                            data.contains("snaptube")
                        )
                    )
                }
            }
        } catch (e: Exception) {
        }
        if (out.isEmpty()) out.addAll(scanFolders(sinceMs))
        out.sortWith(Comparator { a, b ->
            if (a.fromSnap != b.fromSnap) (if (a.fromSnap) -1 else 1) else b.addedMs.compareTo(a.addedMs)
        })
        return out.take(max)
    }

    /** Plano B: se a lista de mídia do Android ainda não atualizou, olha direto as pastas onde o SnapTube salva. */
    private fun scanFolders(sinceMs: Long): List<SnapFile> {
        val r = ArrayList<SnapFile>()
        try {
            val root = Environment.getExternalStorageDirectory()
            for (d in DIRS) {
                val dir = File(root, d)
                val files = dir.listFiles() ?: continue
                for (f in files) {
                    if (!f.isFile || f.length() <= 0L || f.lastModified() < sinceMs) continue
                    if (f.extension.lowercase() !in EXTS) continue
                    r.add(SnapFile(Uri.fromFile(f), cleanName(f.name, true), f.lastModified(), f.path.lowercase().contains("snaptube")))
                }
            }
        } catch (e: Exception) {
        }
        return r.distinctBy { it.uri.toString() }
    }
}
