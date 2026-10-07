package com.doramabloom.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Atualização pelo próprio app: olha a última "versão" publicada no GitHub, baixa o APK
 * e abre a tela de instalar do Android (que sempre pede a sua confirmação).
 */
object Updater {
    private const val REPO = "viniciusy30-commits/doramabloom"
    private const val API = "https://api.github.com/repos/$REPO/releases/latest"
    private const val SIX_HOURS = 6L * 60L * 60L * 1000L

    class Info(val code: Int, val tag: String, val url: String)

    fun currentCode(ctx: Context): Int {
        return try {
            val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode.toInt()
            else @Suppress("DEPRECATION") pi.versionCode
        } catch (e: Exception) {
            0
        }
    }

    /** Busca a versão mais nova publicada (rodar em segundo plano). */
    private fun fetchLatest(): Info? {
        var c: HttpURLConnection? = null
        try {
            c = URL(API).openConnection() as HttpURLConnection
            c.connectTimeout = 10000
            c.readTimeout = 10000
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.setRequestProperty("User-Agent", "MyDoramas")
            if (c.responseCode != 200) return null
            val body = c.inputStream.bufferedReader().use { it.readText() }
            val o = JSONObject(body)
            val tag = o.optString("tag_name", "")
            val code = tag.trimStart('v', 'V').toIntOrNull() ?: return null
            val assets = o.optJSONArray("assets") ?: return null
            var url = ""
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk")) {
                    url = a.optString("browser_download_url")
                    break
                }
            }
            if (url.isEmpty()) return null
            return Info(code, tag, url)
        } catch (e: Exception) {
            return null
        } finally {
            c?.disconnect()
        }
    }

    /** Ao abrir o app: confere de vez em quando (no máximo a cada 6 horas) e só avisa se tiver novidade. */
    fun autoCheck(act: Activity) {
        val sp = act.getSharedPreferences("updater", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - sp.getLong("last", 0L) < SIX_HOURS) return
        sp.edit().putLong("last", now).apply()
        check(act, true)
    }

    /** silent = true: só aparece algo se houver versão nova. */
    fun check(act: Activity, silent: Boolean) {
        if (!silent) act.softToast("Procurando atualização…", Palette.pink, "download")
        Thread {
            val info = fetchLatest()
            act.runOnUiThread {
                if (act.isFinishing || act.isDestroyed) return@runOnUiThread
                val mine = currentCode(act)
                if (info == null) {
                    if (!silent) act.softToast("Não consegui verificar agora. Confira a internet.", Palette.pink, "close")
                } else if (info.code > mine) {
                    ask(act, info, mine)
                } else if (!silent) {
                    act.softToast("Você já está na versão mais nova (v$mine)", Palette.pink, "check")
                }
            }
        }.start()
    }

    private fun ask(act: Activity, info: Info, mine: Int) {
        AlertDialog.Builder(act)
            .setTitle("Nova versão disponível")
            .setMessage(
                "Versão v" + info.code + " (você está na v" + mine + ").\n\n" +
                    "Quer baixar e atualizar agora? Seus doramas e configurações ficam guardados."
            )
            .setPositiveButton("Atualizar") { _, _ -> download(act, info) }
            .setNegativeButton("Depois", null)
            .show()
    }

    private fun download(act: Activity, info: Info) {
        val box = LinearLayout(act)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(act.dp(24), act.dp(16), act.dp(24), act.dp(8))
        val txt = TextView(act)
        txt.text = "Baixando… 0%"
        txt.textSize = 14f
        txt.setTextColor(Palette.text)
        txt.gravity = Gravity.CENTER
        box.addView(txt, LinearLayout.LayoutParams(-1, -2))
        val bar = ProgressBar(act, null, android.R.attr.progressBarStyleHorizontal)
        bar.max = 100
        box.addView(bar, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = act.dp(12) })
        val dlg = AlertDialog.Builder(act)
            .setTitle("Baixando atualização")
            .setView(box)
            .setCancelable(false)
            .create()
        dlg.show()

        Thread {
            var ok = false
            val dir = File(act.cacheDir, "updates")
            val file = File(dir, "MyDoramas.apk")
            try {
                dir.mkdirs()
                dir.listFiles()?.forEach { it.delete() }
                val conn = URL(info.url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "MyDoramas")
                val total = conn.contentLengthLong
                conn.inputStream.use { ins ->
                    FileOutputStream(file).use { out ->
                        val buf = ByteArray(32768)
                        var done = 0L
                        var last = -1
                        while (true) {
                            val n = ins.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (total > 0) {
                                val p = (done * 100 / total).toInt()
                                if (p != last) {
                                    last = p
                                    act.runOnUiThread {
                                        bar.progress = p
                                        txt.text = "Baixando… $p%"
                                    }
                                }
                            }
                        }
                    }
                }
                conn.disconnect()
                ok = file.length() > 100000L
            } catch (e: Exception) {
                ok = false
            }
            act.runOnUiThread {
                dlg.dismiss()
                if (act.isFinishing || act.isDestroyed) return@runOnUiThread
                if (ok) install(act, file)
                else act.softToast("Não consegui baixar. Tente de novo.", Palette.pink, "close")
            }
        }.start()
    }

    private fun install(act: Activity, file: File) {
        if (Build.VERSION.SDK_INT >= 26 && !act.packageManager.canRequestPackageInstalls()) {
            AlertDialog.Builder(act)
                .setTitle("Falta uma permissão")
                .setMessage(
                    "O Android pede que você libere \"Instalar apps desconhecidos\" para o MyDoramas. " +
                        "Na próxima tela, ative a opção, volte e toque em \"Verificar atualização\" de novo."
                )
                .setPositiveButton("Abrir configuração") { _, _ ->
                    val i = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + act.packageName))
                    act.startActivity(i)
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(act, act.packageName + ".fileprovider", file)
            val i = Intent(Intent.ACTION_VIEW)
            i.setDataAndType(uri, "application/vnd.android.package-archive")
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            act.startActivity(i)
        } catch (e: Exception) {
            act.softToast("Não consegui abrir o instalador.", Palette.pink, "close")
        }
    }
}
