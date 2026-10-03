package com.example.mangashelf

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.URLEncoder

class BrowserTab(val web: WebView, var workId: String?) {
    var title: String = "Nova aba"
    var url: String = ""
}

class BrowserController(private val act: MainActivity) {
    val view = FrameLayout(act)
    private val tabs = ArrayList<BrowserTab>()
    private var active: BrowserTab? = null
    var isOpen = false
        private set
    private var fullscreen = false
    private val webHolder = FrameLayout(act)

    private lateinit var topBar: LinearLayout
    private lateinit var tabScroll: HorizontalScrollView
    private lateinit var tabStrip: LinearLayout
    private lateinit var bottomBar: LinearLayout
    private lateinit var urlField: EditText
    private lateinit var progress: ProgressBar
    private lateinit var fsButton: View
    private lateinit var btnBack: View
    private lateinit var btnFwd: View
    private lateinit var chapGroup: LinearLayout
    private lateinit var chapText: TextView

    init {
        view.visibility = View.GONE
        buildChrome()
    }

    private fun dp(v: Int): Int = act.dp(v)

    private fun iconBtn(ic: Ic, onClick: () -> Unit): IconView {
        val x = IconView(act, ic, P.text, 22)
        val r = dp(22).toFloat()
        x.background = act.rippled(shape(Color.TRANSPARENT, r), r)
        x.setOnClickListener { onClick() }
        return x
    }

    // ---------- interface ----------
    private fun buildChrome() {
        (webHolder.parent as? ViewGroup)?.removeView(webHolder)
        view.removeAllViews()
        webHolder.setBackgroundColor(P.bg)

        val col = LinearLayout(act)
        col.orientation = LinearLayout.VERTICAL
        col.setBackgroundColor(P.bg)

        topBar = LinearLayout(act)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setBackgroundColor(P.card)
        topBar.setPadding(dp(6), dp(6), dp(6), dp(6))
        val closeBtn = iconBtn(Ic.Close) { close() }
        urlField = EditText(act)
        urlField.setSingleLine(true)
        urlField.textSize = 14f
        urlField.setTextColor(P.text)
        urlField.setHintTextColor(P.sub)
        urlField.hint = "Buscar ou digitar endereço"
        urlField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        urlField.imeOptions = EditorInfo.IME_ACTION_GO
        urlField.setSelectAllOnFocus(true)
        urlField.setPadding(dp(14), 0, dp(14), 0)
        val ub = android.graphics.drawable.StateListDrawable()
        ub.addState(intArrayOf(android.R.attr.state_focused), shape(P.bg, dp(20).toFloat(), P.accent, dp(1)))
        ub.addState(intArrayOf(), shape(P.bg, dp(20).toFloat(), P.line, dp(1)))
        urlField.background = ub
        urlField.highlightColor = (P.accent and 0x00FFFFFF) or 0x55000000
        urlField.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                go(urlField.text.toString())
                v.hideKeyboard()
                urlField.clearFocus()
                true
            } else {
                false
            }
        }
        val menu = iconBtn(Ic.More) { showMenu() }
        topBar.addv(closeBtn, dp(40), dp(40))
        topBar.addv(urlField, 0, dp(40), 1f, 4, 0, 4, 0)
        topBar.addv(menu, dp(40), dp(40))
        col.addv(topBar)

        progress = ProgressBar(act, null, android.R.attr.progressBarStyleHorizontal)
        progress.max = 100
        progress.progressTintList = ColorStateList.valueOf(P.accent)
        progress.visibility = View.GONE
        col.addv(progress, MATCH, dp(3))

        tabStrip = LinearLayout(act)
        tabStrip.orientation = LinearLayout.HORIZONTAL
        tabStrip.gravity = Gravity.CENTER_VERTICAL
        tabStrip.setPadding(dp(6), dp(5), dp(6), dp(5))
        tabScroll = HorizontalScrollView(act)
        tabScroll.isHorizontalScrollBarEnabled = false
        tabScroll.setBackgroundColor(P.bg)
        tabScroll.addView(tabStrip)
        col.addv(tabScroll)

        col.addv(webHolder, MATCH, 0, 1f)

        bottomBar = LinearLayout(act)
        bottomBar.orientation = LinearLayout.HORIZONTAL
        bottomBar.gravity = Gravity.CENTER_VERTICAL
        bottomBar.setBackgroundColor(P.card)
        bottomBar.setPadding(dp(6), dp(4), dp(6), dp(4))
        btnBack = iconBtn(Ic.ChevronLeft) {
            val w = active?.web
            if (w != null && w.canGoBack()) w.goBack()
        }
        btnFwd = iconBtn(Ic.ChevronRight) {
            val w = active?.web
            if (w != null && w.canGoForward()) w.goForward()
        }
        val reload = iconBtn(Ic.Refresh) { active?.web?.reload() }
        bottomBar.addv(btnBack, dp(44), dp(44))
        bottomBar.addv(btnFwd, dp(44), dp(44))
        bottomBar.addv(reload, dp(44), dp(44))
        bottomBar.addv(View(act), 0, 0, 1f)

        chapGroup = LinearLayout(act)
        chapGroup.orientation = LinearLayout.HORIZONTAL
        chapGroup.gravity = Gravity.CENTER_VERTICAL
        chapGroup.background = shape(P.bg, dp(22).toFloat(), P.line, dp(1))
        chapText = act.tv("Cap. 0", 13f, P.text, true)
        chapText.gravity = Gravity.CENTER
        chapGroup.addv(iconBtn(Ic.Minus) { bumpChapter(-1) }, dp(40), dp(40))
        chapGroup.addv(chapText, WRAP, WRAP, 0f, 4, 0, 4, 0)
        val chapPlus = iconBtn(Ic.Plus) { bumpChapter(1) }
        chapPlus.color = P.accent
        chapGroup.addv(chapPlus, dp(40), dp(40))
        bottomBar.addv(chapGroup, WRAP, WRAP)
        col.addv(bottomBar)

        view.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))

        fsButton = IconView(act, Ic.Minimize, Color.WHITE, 22)
        fsButton.background = shape(0x99000000.toInt(), dp(22).toFloat())
        fsButton.setOnClickListener { toggleFullscreen() }
        val flp = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.END or Gravity.BOTTOM)
        flp.setMargins(0, 0, dp(14), dp(28))
        view.addView(fsButton, flp)

        refreshTabs()
        refreshNav()
        refreshChapterBar()
        val a = active
        if (a != null) urlField.setText(a.url)
        applyFullscreen()
    }

    /** Escurece os sites quando o app está no tema escuro (e a opção está ligada). */
    @Suppress("DEPRECATION")
    private fun applyWebTheme(web: WebView) {
        web.setBackgroundColor(P.bg)
        val on = P.dark && act.prefs.webDark
        if (Build.VERSION.SDK_INT >= 33) {
            web.settings.isAlgorithmicDarkeningAllowed = on
        } else if (Build.VERSION.SDK_INT >= 29) {
            web.settings.forceDark = if (on) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
        }
    }

    fun retheme() {
        buildChrome()
        for (t in tabs) applyWebTheme(t.web)
    }

    private fun refreshTabs() {
        tabStrip.removeAllViews()
        for (t in tabs) {
            val sel = t === active
            val chip = LinearLayout(act)
            chip.orientation = LinearLayout.HORIZONTAL
            chip.gravity = Gravity.CENTER_VERTICAL
            chip.setPadding(dp(12), dp(6), dp(4), dp(6))
            chip.background = if (sel) shape(P.accentSoft, dp(18).toFloat(), P.accent, dp(1))
            else shape(P.card, dp(18).toFloat(), P.line, dp(1))
            val title = act.tv(t.title.ifBlank { "Nova aba" }, 12f, if (sel) P.text else P.sub, sel)
            title.maxLines = 1
            title.ellipsize = TextUtils.TruncateAt.END
            title.maxWidth = dp(120)
            chip.addv(title, WRAP, WRAP)
            val x = IconView(act, Ic.Close, if (sel) P.accent else P.sub, 14)
            x.setOnClickListener { closeTab(t) }
            chip.addv(x, dp(28), dp(24), 0f, 2, 0, 0, 0)
            chip.setOnClickListener { select(t) }
            tabStrip.addv(chip, WRAP, WRAP, 0f, 0, 0, 6, 0)
        }
        val plus = IconView(act, Ic.Plus, P.accent, 18)
        plus.background = act.rippled(shape(P.card, dp(18).toFloat(), P.line, dp(1)), dp(18).toFloat())
        plus.setOnClickListener { newTab(null, null) }
        tabStrip.addv(plus, dp(44), dp(34))
    }

    private fun refreshNav() {
        val w = active?.web
        btnBack.alpha = if (w != null && w.canGoBack()) 1f else 0.35f
        btnFwd.alpha = if (w != null && w.canGoForward()) 1f else 0.35f
    }

    private fun refreshChapterBar() {
        val id = active?.workId
        val w = if (id != null) act.store.get(id) else null
        if (w == null) {
            chapGroup.visibility = View.GONE
        } else {
            chapGroup.visibility = View.VISIBLE
            chapText.text = "Cap. ${fmtNum(w.current)}"
        }
    }

    private fun bumpChapter(delta: Int) {
        val id = active?.workId ?: return
        val w = act.store.get(id) ?: return
        val v = if (delta > 0) Math.floor(w.current) + 1 else Math.max(0.0, Math.ceil(w.current) - 1)
        w.setChapter(v)
        act.store.save()
        refreshChapterBar()
    }

    // ---------- abas ----------
    private fun configure(web: WebView, tab: BrowserTab) {
        val s = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        s.javaScriptCanOpenWindowsAutomatically = false
        s.setSupportMultipleWindows(false)
        s.allowFileAccess = false
        s.textZoom = act.prefs.zoom
        applyWebTheme(web)
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        cm.setAcceptThirdPartyCookies(web, true)

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val u = request?.url?.toString() ?: return false
                val ok = u.startsWith("http://") || u.startsWith("https://") ||
                    u.startsWith("about:") || u.startsWith("data:") || u.startsWith("blob:")
                return !ok
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (url != null) tab.url = url
                if (tab === active) {
                    if (!urlField.hasFocus()) urlField.setText(tab.url)
                    refreshNav()
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (url != null) tab.url = url
                val t = view?.title
                tab.title = if (!t.isNullOrBlank()) t else tab.url
                if (tab.url.startsWith("http")) act.prefs.addHistory(tab.title, tab.url)
                track(tab, tab.url)
                if (tab === active) {
                    if (!urlField.hasFocus()) urlField.setText(tab.url)
                    refreshNav()
                }
                refreshTabs()
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                if (url != null) {
                    tab.url = url
                    track(tab, url)
                    if (tab === active && !urlField.hasFocus()) urlField.setText(url)
                }
                if (tab === active) refreshNav()
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (tab === active) {
                    progress.progress = newProgress
                    progress.visibility = if (newProgress in 1..99 && !fullscreen) View.VISIBLE else View.GONE
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) {
                    tab.title = title
                    refreshTabs()
                }
            }
        }

        web.setDownloadListener { url, _, _, _, _ -> openExternal(url) }
    }

    private fun track(tab: BrowserTab, url: String?) {
        val id = tab.workId ?: return
        val u = url ?: return
        if (!u.startsWith("http")) return
        val w = act.store.get(id) ?: return
        val host = hostOf(u)
        if (w.links.none { sameSite(hostOf(it.url), host) }) return
        if (w.lastUrl != u) {
            w.lastUrl = u
            w.lastRead = System.currentTimeMillis()
            act.store.save()
        }
    }

    fun newTab(url: String?, workId: String?) {
        val web = WebView(act)
        val tab = BrowserTab(web, workId)
        configure(web, tab)
        tabs.add(tab)
        select(tab)
        val target = url ?: act.prefs.homeUrl
        tab.url = target
        urlField.setText(target)
        web.loadUrl(target)
    }

    private fun select(t: BrowserTab) {
        active = t
        (t.web.parent as? ViewGroup)?.removeView(t.web)
        webHolder.removeAllViews()
        webHolder.addView(t.web, FrameLayout.LayoutParams(MATCH, MATCH))
        urlField.setText(t.url)
        progress.visibility = View.GONE
        refreshTabs()
        refreshNav()
        refreshChapterBar()
    }

    private fun disposeWeb(w: WebView) {
        (w.parent as? ViewGroup)?.removeView(w)
        w.stopLoading()
        w.destroy()
    }

    private fun closeTab(t: BrowserTab) {
        val idx = tabs.indexOf(t)
        if (idx < 0) return
        tabs.removeAt(idx)
        if (t === active) {
            if (tabs.isEmpty()) {
                active = null
                webHolder.removeAllViews()
                disposeWeb(t.web)
                refreshTabs()
                close()
                return
            }
            select(tabs[Math.min(idx, tabs.size - 1)])
        }
        disposeWeb(t.web)
        refreshTabs()
    }

    private fun closeAll() {
        val copy = ArrayList(tabs)
        tabs.clear()
        active = null
        webHolder.removeAllViews()
        for (t in copy) disposeWeb(t.web)
        refreshTabs()
        close()
    }

    // ---------- abrir / fechar ----------
    fun open(url: String?, workId: String?, reuse: Boolean) {
        isOpen = true
        view.animate().cancel()
        if (view.visibility != View.VISIBLE) {
            view.alpha = 0f
            view.visibility = View.VISIBLE
            view.animate().alpha(1f).setDuration(170).start()
        } else {
            view.alpha = 1f
        }
        view.bringToFront()
        if (url == null) {
            val a = active
            if (tabs.isEmpty()) newTab(null, null) else if (a != null) select(a)
            return
        }
        if (reuse && workId != null) {
            val ex = tabs.firstOrNull { it.workId == workId }
            if (ex != null) {
                select(ex)
                return
            }
        }
        newTab(url, workId)
    }

    fun close() {
        exitFullscreen()
        view.hideKeyboard()
        isOpen = false
        view.animate().cancel()
        view.animate().alpha(0f).setDuration(140).withEndAction {
            view.visibility = View.GONE
            view.alpha = 1f
        }.start()
        act.render()
    }

    fun onBack() {
        if (fullscreen) {
            exitFullscreen()
            return
        }
        val w = active?.web
        if (w != null && w.canGoBack()) w.goBack() else close()
    }

    fun pause() {
        active?.web?.onPause()
    }

    fun resume() {
        active?.web?.onResume()
    }

    fun destroy() {
        for (t in tabs) disposeWeb(t.web)
        tabs.clear()
        active = null
    }

    // ---------- entrada de endereço ----------
    private fun resolveInput(text: String): String {
        val t = text.trim()
        if (t.isEmpty()) return ""
        if (t.contains("://")) return t
        if (!t.contains(" ") && t.contains(".")) return "https://$t"
        return "https://www.google.com/search?q=" + URLEncoder.encode(t, "UTF-8")
    }

    private fun go(input: String) {
        val u = resolveInput(input)
        if (u.isEmpty()) return
        val a = active
        if (a == null) newTab(u, null) else a.web.loadUrl(u)
    }

    private fun openExternal(url: String?) {
        if (url.isNullOrBlank()) return
        try {
            act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            act.toast("Não consegui abrir no navegador externo")
        }
    }

    // ---------- menu ----------
    private fun showMenu() {
        val items = listOf(
            Pair(Ic.Plus, "Nova aba"),
            Pair(Ic.External, "Abrir no navegador externo"),
            Pair(Ic.ZoomIn, "Aumentar texto"),
            Pair(Ic.ZoomOut, "Diminuir texto"),
            Pair(Ic.Maximize, "Tela cheia"),
            Pair(Ic.Clock, "Histórico"),
            Pair(Ic.Copy, "Copiar endereço"),
            Pair(Ic.Trash, "Fechar todas as abas"),
            Pair(Ic.Help, "Ajuda do navegador"),
            Pair(Ic.Moon, if (act.prefs.webDark) "Sites no escuro: ligado (tocar p/ desligar)" else "Sites no escuro: desligado (tocar p/ ligar)")
        )
        act.iconListDialog("Navegador", items) { i ->
            when (i) {
                0 -> newTab(null, null)
                1 -> openExternal(active?.url)
                2 -> setZoom(act.prefs.zoom + 10)
                3 -> setZoom(act.prefs.zoom - 10)
                4 -> toggleFullscreen()
                5 -> showHistory()
                6 -> {
                    val u = active?.url ?: ""
                    if (u.isNotBlank()) {
                        val cb = act.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("url", u))
                        act.toast("Endereço copiado")
                    }
                }
                7 -> act.confirmDialog("Fechar abas", "Fechar todas as abas e sair do navegador?") { closeAll() }
                8 -> MaterialAlertDialogBuilder(act).setTitle("Ajuda do navegador")
                    .setMessage(browserHelpText()).setPositiveButton("Entendi", null).show()
                9 -> {
                    act.prefs.webDark = !act.prefs.webDark
                    for (t in tabs) {
                        applyWebTheme(t.web)
                        t.web.reload()
                    }
                    act.toast(if (act.prefs.webDark) "Sites escurecidos no tema escuro" else "Sites com o visual original")
                }
                else -> {}
            }
        }
    }

    fun setZoom(v: Int) {
        val z = v.coerceIn(50, 300)
        act.prefs.zoom = z
        for (t in tabs) t.web.settings.textZoom = z
        act.toast("Texto: $z%")
    }

    private fun showHistory() {
        val h = act.prefs.history()
        if (h.isEmpty()) {
            act.toast("Histórico vazio")
            return
        }
        val labels = h.map { (if (it.title.isBlank()) it.url else it.title).take(60) + "\n" + it.url.take(70) }
            .toTypedArray()
        MaterialAlertDialogBuilder(act).setTitle("Histórico")
            .setItems(labels) { _, i -> go(h[i].url) }
            .setNeutralButton("Limpar") { _, _ -> act.prefs.clearHistory() }
            .setNegativeButton("Fechar", null).show()
    }

    // ---------- tela cheia ----------
    private fun toggleFullscreen() {
        fullscreen = !fullscreen
        applyFullscreen()
    }

    private fun exitFullscreen() {
        if (fullscreen) {
            fullscreen = false
            applyFullscreen()
        }
    }

    private fun applyFullscreen() {
        val vis = if (fullscreen) View.GONE else View.VISIBLE
        topBar.visibility = vis
        tabScroll.visibility = vis
        bottomBar.visibility = vis
        if (fullscreen) progress.visibility = View.GONE
        fsButton.visibility = if (fullscreen) View.VISIBLE else View.GONE
        val c = WindowCompat.getInsetsController(act.window, act.window.decorView)
        if (fullscreen) {
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            c.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
