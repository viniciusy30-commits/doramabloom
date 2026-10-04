package com.example.mangashelf

import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Route(val name: String, val arg: String? = null)

class MainActivity : AppCompatActivity() {
    lateinit var store: Store
    lateinit var prefs: Prefs
    private lateinit var rootFrame: FrameLayout
    private lateinit var contentFrame: FrameLayout
    private lateinit var navBar: LinearLayout
    var browser: BrowserController? = null
    val stack = ArrayList<Route>()

    // estado dos filtros da biblioteca
    var fStatus = "Todos"
    var fType = "Todos"
    var fGenre = "Todos"
    var fQuery = ""
    var fSort = SORTS[0]
    var focusSearch = false

    private var pickCallback: ((Uri) -> Unit)? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) pickCallback?.invoke(uri)
        }
    private val createBackupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) writeBackup(uri)
        }
    private val openBackupLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) readBackup(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)
        applyNightMode()
        super.onCreate(savedInstanceState)
        store = Store(this)

        rootFrame = FrameLayout(this)
        val app = vbox()
        contentFrame = FrameLayout(this)
        app.addv(contentFrame, MATCH, 0, 1f)
        navBar = hbox()
        app.addv(navBar, MATCH, WRAP)
        rootFrame.addView(app, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(rootFrame)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBack()
            }
        })

        stack.add(Route("home"))
        render()
        restoreBrowser()

        if (!prefs.tutorialSeen) {
            prefs.tutorialSeen = true
            MaterialAlertDialogBuilder(this)
                .setTitle("Bem-vindo!")
                .setMessage("Quer ver um tutorial rápido de como usar o app?")
                .setPositiveButton("Ver agora") { _, _ -> go(Route("tutorial")) }
                .setNegativeButton("Depois", null)
                .show()
        }
    }

    // ---------- tema ----------
    fun isDarkNow(): Boolean = when (prefs.theme) {
        1 -> false
        2 -> true
        else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    fun applyNightMode() {
        AppCompatDelegate.setDefaultNightMode(
            when (prefs.theme) {
                1 -> AppCompatDelegate.MODE_NIGHT_NO
                2 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    fun themeChanged() {
        render()
        browser?.retheme()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        themeChanged()
    }

    // ---------- navegação ----------
    fun go(r: Route) {
        stack.add(r)
        render()
    }

    fun goTop(name: String) {
        stack.clear()
        stack.add(Route(name))
        render()
    }

    fun reset(vararg routes: Route) {
        stack.clear()
        for (r in routes) stack.add(r)
        render()
    }

    fun pop() {
        if (stack.size > 1) {
            stack.removeAt(stack.size - 1)
            render()
        } else {
            goTop("home")
        }
    }

    fun popN(n: Int) {
        repeat(n) { if (stack.size > 1) stack.removeAt(stack.size - 1) }
        render()
    }

    private fun handleBack() {
        val b = browser
        if (b != null && b.isOpen) {
            b.onBack()
            return
        }
        if (stack.size > 1) {
            stack.removeAt(stack.size - 1)
            render()
            return
        }
        if (stack[0].name != "home") {
            goTop("home")
            return
        }
        finish()
    }

    private var lastKey = ""

    fun render() {
        P.dark = isDarkNow()
        window.statusBarColor = P.bg
        window.navigationBarColor = P.card
        val c = WindowCompat.getInsetsController(window, window.decorView)
        c.isAppearanceLightStatusBars = !P.dark
        c.isAppearanceLightNavigationBars = !P.dark
        rootFrame.setBackgroundColor(P.bg)
        contentFrame.removeAllViews()
        val r = stack.last()
        val v: View = when (r.name) {
            "home" -> buildHome()
            "library" -> buildLibrary()
            "add" -> buildForm(r.arg)
            "stats" -> buildStats()
            "settings" -> buildSettings()
            "detail" -> buildDetail(r.arg ?: "")
            "tutorial" -> buildTutorial()
            else -> buildHome()
        }
        contentFrame.addView(v, FrameLayout.LayoutParams(MATCH, MATCH))
        val key = r.name + "|" + (r.arg ?: "")
        if (key != lastKey) {
            lastKey = key
            v.alpha = 0f
            v.translationY = dp(8).toFloat()
            v.animate().alpha(1f).translationY(0f).setDuration(180).start()
        }
        buildNav()
    }

    private fun buildNav() {
        navBar.removeAllViews()
        val ld = android.graphics.drawable.LayerDrawable(
            arrayOf(
                android.graphics.drawable.ColorDrawable(P.line),
                android.graphics.drawable.ColorDrawable(P.card)
            )
        )
        ld.setLayerInset(1, 0, dp(1), 0, 0)
        navBar.background = ld
        navBar.setPadding(dp(6), dp(8), dp(6), dp(8))
        val items = listOf(
            Triple("home", Ic.Home, "Início"),
            Triple("library", Ic.Library, "Biblioteca"),
            Triple("add", Ic.Plus, "Adicionar"),
            Triple("stats", Ic.Chart, "Estatísticas"),
            Triple("settings", Ic.Sliders, "Config.")
        )
        val cur = stack.firstOrNull()?.name ?: "home"
        for ((key, ic, label) in items) {
            val sel = key == cur
            val special = key == "add"
            val color = if (special) P.accent else if (sel) P.text else P.sub
            val r = dp(16).toFloat()
            val col = vbox()
            col.gravity = Gravity.CENTER_HORIZONTAL
            col.setPadding(0, dp(5), 0, dp(7))
            col.background = when {
                sel -> rippled(shape(P.card2, r, P.line, dp(1)), r)
                special -> rippled(shape(P.accentSoft, r, P.accentLine, dp(1)), r)
                else -> rippled(shape(Color.TRANSPARENT, r), r)
            }
            val bar = View(this)
            bar.background = shape(P.accent, dp(2).toFloat())
            bar.visibility = if (sel) View.VISIBLE else View.INVISIBLE
            col.addv(bar, dp(18), dp(2), 0f, 0, 0, 0, 5)
            col.addv(IconView(this, ic, color, 22), dp(24), dp(24))
            val l = tv(label, 10.5f, color, sel)
            l.maxLines = 1
            col.addv(l, WRAP, WRAP, 0f, 0, 3, 0, 0)
            col.setOnClickListener { goTop(key) }
            col.pressFx()
            navBar.addv(col, 0, WRAP, 1f, 2, 0, 2, 0)
        }
    }

    // ---------- navegador interno ----------
    /** Reabre as abas (e o site aberto) exatamente de onde você parou na última vez. */
    private fun restoreBrowser() {
        val saved = prefs.browserState
        if (saved.isBlank() || browser != null) return
        try {
            val b = BrowserController(this)
            b.restore(saved)
            browser = b
            rootFrame.addView(b.view, FrameLayout.LayoutParams(MATCH, MATCH))
            if (b.isOpen) b.view.bringToFront()
        } catch (e: Exception) {
            prefs.browserState = ""
        }
    }

    fun openInBrowser(url: String?, workId: String?, reuse: Boolean) {
        var b = browser
        if (b == null) {
            b = BrowserController(this)
            browser = b
            rootFrame.addView(b.view, FrameLayout.LayoutParams(MATCH, MATCH))
        }
        b.open(url, workId, reuse)
    }

    /** Abre o navegador direto: no último ponto, no link principal ou na página inicial. */
    fun continueReading(w: Work): Boolean {
        val target = when {
            w.lastUrl.isNotBlank() -> w.lastUrl
            w.mainLink() != null -> w.mainLink()!!.url
            else -> prefs.homeUrl
        }
        w.lastRead = System.currentTimeMillis()
        if (w.status == STATUS_PLAN) w.status = STATUS_READING
        store.save()
        openInBrowser(target, w.id, true)
        return true
    }

    // ---------- seletor de imagem ----------
    fun pickImage(cb: (Uri) -> Unit) {
        pickCallback = cb
        pickImageLauncher.launch("image/*")
    }

    // ---------- backup ----------
    fun exportBackup() {
        val name = "mangadeck-backup-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".json"
        createBackupLauncher.launch(name)
    }

    fun importBackup() {
        openBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
    }

    private fun writeBackup(uri: Uri) {
        toast("Salvando backup…")
        Thread {
            try {
                val s = store.exportJson().toString()
                contentResolver.openOutputStream(uri)?.use { it.write(s.toByteArray(Charsets.UTF_8)) }
                runOnUiThread { toast("Backup salvo") }
            } catch (e: Exception) {
                runOnUiThread { toast("Erro ao salvar: ${e.message}") }
            }
        }.start()
    }

    private fun readBackup(uri: Uri) {
        Thread {
            try {
                val txt = contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
                val n = JSONObject(txt).getJSONArray("works").length()
                runOnUiThread { askImportMode(txt, n) }
            } catch (e: Exception) {
                runOnUiThread { toast("Arquivo inválido: não parece um backup do app") }
            }
        }.start()
    }

    private fun askImportMode(txt: String, n: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Restaurar backup")
            .setMessage("O arquivo tem $n obras. Como importar?")
            .setPositiveButton("Mesclar") { _, _ -> doImport(txt, false) }
            .setNeutralButton("Substituir tudo") { _, _ -> doImport(txt, true) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun doImport(txt: String, replace: Boolean) {
        val dlg = MaterialAlertDialogBuilder(this).setTitle("Importando…").setMessage("Aguarde um instante.")
            .setCancelable(false).create()
        dlg.show()
        Thread {
            try {
                val count = store.importJson(txt, replace)
                runOnUiThread {
                    dlg.dismiss()
                    render()
                    toast("$count obras importadas")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    dlg.dismiss()
                    toast("Erro ao importar: ${e.message}")
                }
            }
        }.start()
    }

    // ---------- ciclo de vida ----------
    override fun onPause() {
        super.onPause()
        browser?.pause()
        CookieManager.getInstance().flush()
    }

    override fun onResume() {
        super.onResume()
        browser?.resume()
    }

    override fun onDestroy() {
        if (isFinishing) browser?.destroy()
        super.onDestroy()
    }
}
