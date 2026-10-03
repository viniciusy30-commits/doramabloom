package com.example.mangashelf

import android.content.Configuration
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

        if (!prefs.tutorialSeen) {
            prefs.tutorialSeen = true
            AlertDialog.Builder(this)
                .setTitle("Bem-vindo! 📖")
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
        buildNav()
    }

    private fun buildNav() {
        navBar.removeAllViews()
        navBar.setBackgroundColor(P.card)
        val items = listOf(
            Triple("home", "🏠", "Início"),
            Triple("library", "📚", "Biblioteca"),
            Triple("add", "➕", "Adicionar"),
            Triple("stats", "📊", "Estatísticas"),
            Triple("settings", "⚙️", "Config.")
        )
        val cur = stack.firstOrNull()?.name ?: "home"
        for ((key, emoji, label) in items) {
            val col = vbox()
            col.gravity = Gravity.CENTER
            col.setPadding(0, dp(8), 0, dp(8))
            val e = tv(emoji, 20f)
            e.gravity = Gravity.CENTER
            e.alpha = if (key == cur) 1f else 0.55f
            val l = tv(label, 10.5f, if (key == cur) P.accent else P.sub, key == cur)
            l.maxLines = 1
            col.addv(e, WRAP, WRAP)
            col.addv(l, WRAP, WRAP)
            col.setOnClickListener { goTop(key) }
            navBar.addv(col, 0, WRAP, 1f)
        }
    }

    // ---------- navegador interno ----------
    fun openInBrowser(url: String?, workId: String?, reuse: Boolean) {
        var b = browser
        if (b == null) {
            b = BrowserController(this)
            browser = b
            rootFrame.addView(b.view, FrameLayout.LayoutParams(MATCH, MATCH))
        }
        b.open(url, workId, reuse)
    }

    /** Retorna false se a obra não tem nenhum link salvo. */
    fun continueReading(w: Work): Boolean {
        val target = if (w.lastUrl.isNotBlank()) w.lastUrl else (w.mainLink()?.url ?: "")
        if (target.isBlank()) return false
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
        val name = "mangashelf-backup-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".json"
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
                runOnUiThread { toast("Backup salvo ✅") }
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
        AlertDialog.Builder(this)
            .setTitle("Restaurar backup")
            .setMessage("O arquivo tem $n obras. Como importar?")
            .setPositiveButton("Mesclar") { _, _ -> doImport(txt, false) }
            .setNeutralButton("Substituir tudo") { _, _ -> doImport(txt, true) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun doImport(txt: String, replace: Boolean) {
        val dlg = AlertDialog.Builder(this).setTitle("Importando…").setMessage("Aguarde um instante.")
            .setCancelable(false).create()
        dlg.show()
        Thread {
            try {
                val count = store.importJson(txt, replace)
                runOnUiThread {
                    dlg.dismiss()
                    render()
                    toast("$count obras importadas ✅")
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
