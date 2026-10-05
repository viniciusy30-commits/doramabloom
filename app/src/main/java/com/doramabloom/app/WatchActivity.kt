package com.doramabloom.app

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Navegador de assistir: cada dorama tem a sua própria aba (com a sua própria página e histórico),
 * uma não interfere na outra. O contador de episódios fica na barra de baixo.
 */
class WatchActivity : AppCompatActivity() {

    private class Tab(val id: Long, val web: WebView, var season: Int)

    private val tabs = ArrayList<Tab>()
    private var cur = -1

    private lateinit var holder: FrameLayout
    private lateinit var tabRow: LinearLayout
    private lateinit var urlIn: EditText
    private lateinit var progress: ProgressBar
    private lateinit var epText: TextView
    private lateinit var seasonScroll: HorizontalScrollView
    private lateinit var seasonRow: LinearLayout
    private lateinit var fsLayer: FrameLayout
    private lateinit var backBtn: View
    private lateinit var fwdBtn: View
    private var customView: View? = null
    private var customCb: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        val id = intent.getLongExtra("id", -1L)
        if (Store.get(id) == null) {
            finish()
            return
        }
        buildUi()
        openTab(id)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.screen_back_in, R.anim.screen_back_out)
    }

    private fun curTab(): Tab? = tabs.getOrNull(cur)
    private fun curDrama(): Drama? = curTab()?.let { Store.get(it.id) }
    private fun isCur(w: WebView): Boolean = curTab()?.web === w

    // ---------------------------------------------------------------- tela

    private fun buildUi() {
        val root = FrameLayout(this)
        root.background = gradient(Palette.bgTop, Palette.bgBottom)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        root.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))

        // topo: fechar, endereço, menu
        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.setPadding(dp(12), dp(8), dp(12), dp(4))
        top.addView(roundBtn("close", Palette.pink, false, 16) { finish() }, lin(dp(40), dp(40)))

        urlIn = EditText(this)
        urlIn.setSingleLine(true)
        urlIn.hint = "Pesquise ou digite um site"
        urlIn.textSize = 13f
        urlIn.setTextColor(Palette.text)
        urlIn.setHintTextColor(Palette.muted)
        urlIn.background = null
        urlIn.setPadding(dp(4), dp(8), dp(4), dp(8))
        urlIn.imeOptions = EditorInfo.IME_ACTION_GO
        urlIn.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        urlIn.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_GO) {
                go(urlIn.text.toString())
                true
            } else false
        }
        val urlBox = LinearLayout(this)
        urlBox.orientation = LinearLayout.HORIZONTAL
        urlBox.gravity = Gravity.CENTER_VERTICAL
        urlBox.setPadding(dp(14), 0, dp(10), 0)
        urlBox.background = roundRect(Color.WHITE, dp(24).toFloat(), Palette.line, dp(1))
        urlBox.addView(IconView(this, "globe", Palette.muted, 16))
        urlBox.addView(urlIn, lin(0, WRAP, 1f, l = 6))
        top.addView(urlBox, lin(0, dp(40), 1f, l = 8, r = 8))

        val more = roundBtn("more", Palette.pink, false, 18) {}
        more.setOnClickListener { showMenu(more) }
        top.addView(more, lin(dp(40), dp(40)))
        col.addView(top, lin(MATCH, WRAP))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progress.max = 100
        progress.progressTintList = ColorStateList.valueOf(Palette.pink)
        progress.visibility = View.INVISIBLE
        col.addView(progress, lin(MATCH, dp(3), l = 14, r = 14))

        // abas (uma por dorama)
        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        tabRow = LinearLayout(this)
        tabRow.orientation = LinearLayout.HORIZONTAL
        tabRow.gravity = Gravity.CENTER_VERTICAL
        tabRow.setPadding(dp(12), dp(6), dp(12), dp(6))
        hs.addView(tabRow)
        col.addView(hs, lin(MATCH, WRAP))

        // página
        holder = FrameLayout(this)
        holder.background = roundRect(Color.WHITE, dp(22).toFloat(), Palette.line, dp(1))
        holder.clipToOutline = true
        col.addView(holder, lin(MATCH, 0, 1f, l = 10, r = 10))

        // temporadas (só aparece quando o dorama tem mais de uma)
        seasonScroll = HorizontalScrollView(this)
        seasonScroll.isHorizontalScrollBarEnabled = false
        seasonRow = LinearLayout(this)
        seasonRow.orientation = LinearLayout.HORIZONTAL
        seasonRow.gravity = Gravity.CENTER_VERTICAL
        seasonRow.setPadding(dp(14), dp(8), dp(14), dp(2))
        seasonScroll.addView(seasonRow)
        seasonScroll.visibility = View.GONE
        col.addView(seasonScroll, lin(MATCH, WRAP))

        // baixo: voltar, avançar, recarregar e contador de episódios
        val bottom = LinearLayout(this)
        bottom.orientation = LinearLayout.HORIZONTAL
        bottom.gravity = Gravity.CENTER_VERTICAL
        bottom.setPadding(dp(14), dp(8), dp(14), dp(10))
        backBtn = roundBtn("back", Palette.pink, false, 16) { curTab()?.web?.let { if (it.canGoBack()) it.goBack() } }
        fwdBtn = roundBtn("forward", Palette.pink, false, 16) { curTab()?.web?.let { if (it.canGoForward()) it.goForward() } }
        val reload = roundBtn("replay", Palette.pink, false, 16) { curTab()?.web?.reload() }
        bottom.addView(backBtn, lin(dp(40), dp(40)))
        bottom.addView(fwdBtn, lin(dp(40), dp(40), l = 8))
        bottom.addView(reload, lin(dp(40), dp(40), l = 8))
        bottom.addView(View(this), lin(0, dp(1), 1f))

        val counter = LinearLayout(this)
        counter.orientation = LinearLayout.HORIZONTAL
        counter.gravity = Gravity.CENTER_VERTICAL
        counter.setPadding(dp(5), dp(5), dp(5), dp(5))
        counter.background = roundRect(Color.WHITE, dp(28).toFloat(), Palette.line, dp(1))
        counter.addView(roundBtn("minus", Palette.pink, false, 14) { epMinus() }, lin(dp(34), dp(34)))
        epText = label("Ep. 0", 13.5f, Palette.text, true, true)
        epText.gravity = Gravity.CENTER
        epText.maxLines = 1
        epText.minWidth = dp(84)
        counter.addView(epText, lin(WRAP, WRAP, l = 4, r = 4))
        counter.addView(roundBtn("add", Palette.pink, true, 14) { epPlus() }, lin(dp(34), dp(34)))
        bottom.addView(counter, lin(WRAP, WRAP))
        col.addView(bottom, lin(MATCH, WRAP))

        // camada de vídeo em tela cheia
        fsLayer = FrameLayout(this)
        fsLayer.setBackgroundColor(Color.BLACK)
        fsLayer.visibility = View.GONE
        root.addView(fsLayer, FrameLayout.LayoutParams(MATCH, MATCH))

        setContentView(root)
    }

    private fun hideKeyboard() {
        val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(urlIn.windowToken, 0)
    }

    private fun go(raw: String) {
        val t = raw.trim()
        val w = curTab()?.web ?: return
        if (t.isEmpty()) return
        val url = when {
            t.startsWith("http://") || t.startsWith("https://") -> t
            t.contains(" ") || !t.contains(".") ->
                "https://www.google.com/search?q=" + java.net.URLEncoder.encode(t, "UTF-8")
            else -> "https://$t"
        }
        w.loadUrl(url)
        hideKeyboard()
        urlIn.clearFocus()
    }

    // ---------------------------------------------------------------- abas

    @SuppressLint("SetJavaScriptEnabled")
    private fun makeWeb(): WebView {
        val w = WebView(this)
        val s = w.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.textZoom = Store.textZoom
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        w.setBackgroundColor(Color.WHITE)
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, true)
        w.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val u = request?.url?.toString() ?: return false
                return !(u.startsWith("http://") || u.startsWith("https://"))
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                if (view != null && isCur(view)) {
                    if (!urlIn.hasFocus()) urlIn.setText(url ?: "")
                    updateNavButtons()
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (view == null) return
                if (isCur(view)) updateNavButtons()
                tabs.firstOrNull { it.web === view }?.let { saveUrl(it) }
            }
        }
        w.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (view != null && isCur(view)) {
                    progress.progress = newProgress
                    progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.INVISIBLE
                }
            }

            override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
                if (view == null || customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customView = view
                customCb = callback
                fsLayer.addView(view, FrameLayout.LayoutParams(MATCH, MATCH))
                fsLayer.visibility = View.VISIBLE
                setFullscreen(true)
            }

            override fun onHideCustomView() {
                exitCustom()
            }
        }
        return w
    }

    private fun exitCustom() {
        val v = customView ?: return
        fsLayer.removeView(v)
        fsLayer.visibility = View.GONE
        customView = null
        val cb = customCb
        customCb = null
        cb?.onCustomViewHidden()
        setFullscreen(false)
    }

    private fun setFullscreen(on: Boolean) {
        requestedOrientation = if (on) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val c = WindowCompat.getInsetsController(window, window.decorView)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (on) c.hide(WindowInsetsCompat.Type.systemBars()) else c.show(WindowInsetsCompat.Type.systemBars())
    }

    private fun openTab(id: Long) {
        val at = tabs.indexOfFirst { it.id == id }
        if (at >= 0) {
            select(at)
            return
        }
        val d = Store.get(id) ?: return
        val w = makeWeb()
        tabs.add(Tab(id, w, activeSeason(d)))
        val start = when {
            d.lastUrl.isNotBlank() -> d.lastUrl
            d.link.isNotBlank() -> d.link
            else -> Store.homeUrl
        }
        w.loadUrl(start)
        select(tabs.size - 1)
    }

    private fun select(i: Int) {
        if (i !in tabs.indices) return
        curTab()?.web?.onPause()
        cur = i
        val t = tabs[i]
        holder.removeAllViews()
        (t.web.parent as? ViewGroup)?.removeView(t.web)
        holder.addView(t.web, FrameLayout.LayoutParams(MATCH, MATCH))
        t.web.onResume()
        val d = Store.get(t.id)
        urlIn.setText(t.web.url ?: (d?.let { if (it.lastUrl.isNotBlank()) it.lastUrl else it.link } ?: ""))
        if (d != null) {
            progress.progressTintList = ColorStateList.valueOf(Genres.byKey(d.genre).primary)
        }
        progress.visibility = View.INVISIBLE
        renderTabs()
        renderSeasons()
        updateNavButtons()
        updateEp(false)
    }

    private fun renderTabs() {
        tabRow.removeAllViews()
        for (i in tabs.indices) {
            val d = Store.get(tabs[i].id) ?: continue
            val g = Genres.byKey(d.genre)
            val sel = i == cur
            val fg = if (sel) Color.WHITE else Palette.text
            val chip = LinearLayout(this)
            chip.orientation = LinearLayout.HORIZONTAL
            chip.gravity = Gravity.CENTER_VERTICAL
            chip.setPadding(dp(14), 0, dp(8), 0)
            chip.background = if (sel) roundRect(g.primary, dp(22).toFloat())
            else roundRect(Color.WHITE, dp(22).toFloat(), Palette.line, dp(1))
            val tv = label(d.title, 13f, fg, true)
            tv.maxLines = 1
            tv.ellipsize = TextUtils.TruncateAt.END
            tv.maxWidth = dp(130)
            chip.addView(tv)
            val x = IconView(this, "close", if (sel) Color.WHITE else Palette.muted, 14)
            x.setPadding(dp(6), dp(6), dp(6), dp(6))
            x.setOnClickListener { closeTab(i) }
            chip.addView(x, lin(WRAP, WRAP, l = 4))
            chip.setOnClickListener { if (i != cur) select(i) }
            chip.pressable(0.95f)
            tabRow.addView(chip, lin(WRAP, dp(38), r = 8))
        }
        tabRow.addView(roundBtn("add", Palette.pink, true, 16) { pickDrama() }, lin(dp(38), dp(38)))
    }

    private fun closeTab(i: Int) {
        if (i !in tabs.indices) return
        val t = tabs[i]
        saveUrl(t)
        if (i == cur) {
            t.web.onPause()
            cur = -1
        } else if (i < cur) {
            cur -= 1
        }
        (t.web.parent as? ViewGroup)?.removeView(t.web)
        t.web.destroy()
        tabs.removeAt(i)
        if (tabs.isEmpty()) {
            finish()
            return
        }
        if (cur == -1) select(minOf(i, tabs.size - 1)) else renderTabs()
    }

    private fun pickDrama() {
        val list = Store.all().filter { d -> tabs.none { t -> t.id == d.id } }
        if (list.isEmpty()) {
            softToast("Nenhum outro dorama na estante", Palette.pink, "globe")
            return
        }
        val names = list.map { it.title }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Abrir outra aba")
            .setItems(names) { _, which -> openTab(list[which].id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateNavButtons() {
        val w = curTab()?.web ?: return
        backBtn.alpha = if (w.canGoBack()) 1f else 0.4f
        fwdBtn.alpha = if (w.canGoForward()) 1f else 0.4f
    }

    // ---------------------------------------------------------- episódios

    /** Faixa "Temporada: T1 T2 T3": toque para escolher em qual temporada os episódios entram. */
    private fun renderSeasons() {
        seasonRow.removeAllViews()
        val t = curTab()
        val d = curDrama()
        if (t == null || d == null || seasonCount(d) <= 1) {
            seasonScroll.visibility = View.GONE
            return
        }
        seasonScroll.visibility = View.VISIBLE
        val g = Genres.byKey(d.genre)
        seasonRow.addView(label("Temporada", 12f, Palette.muted, true), lin(WRAP, WRAP, r = 8))
        for (i in d.seasonEps.indices) {
            val sel = i == t.season
            val done = seasonDone(d, i)
            val fg = if (sel) Color.WHITE else g.primary
            val chip = pill("T" + (i + 1), if (sel) g.primary else Color.WHITE, fg, 13f, if (done) "check" else null)
            if (!sel) chip.background = roundRect(Color.WHITE, dp(20).toFloat(), g.primary, dp(1))
            chip.setOnClickListener { chooseSeason(i) }
            seasonRow.addView(chip, lin(WRAP, WRAP, r = 6))
        }
    }

    private fun chooseSeason(i: Int) {
        val t = curTab() ?: return
        val d = curDrama() ?: return
        if (i == t.season || i !in d.seasonEps.indices) return
        t.season = i
        Store.setWatchSeason(d, i)
        renderSeasons()
        updateEp(true)
        softToast("Temporada " + (i + 1), Genres.byKey(d.genre).primary, "tv")
    }

    private fun updateEp(animate: Boolean) {
        val d = curDrama() ?: return
        val t = curTab() ?: return
        if (t.season !in d.seasonEps.indices) t.season = activeSeason(d)
        epText.text = counterText(d, t.season)
        if (animate) epText.pop(1.2f)
    }

    private fun epPlus() {
        val d = curDrama() ?: return
        val t = curTab() ?: return
        val s = t.season
        if (s !in d.seasonEps.indices) return
        val total = d.seasonEps[s]
        if (total > 0 && d.watched[s] >= total) {
            val more = if (s + 1 < seasonCount(d)) " Escolha a próxima lá embaixo." else ""
            softToast("Temporada " + (s + 1) + " já está completa." + more, Palette.pink, "check")
            return
        }
        val fin = Store.adjust(d, s, 1)
        updateEp(true)
        renderSeasons()
        if (fin) {
            softToast("Parabéns, você terminou " + d.title + "!", Color.parseColor("#5CC6A0"), "check")
        } else if (total > 0 && d.watched[s] >= total && seasonCount(d) > 1) {
            val more = if (s + 1 < seasonCount(d)) " Toque em T" + (s + 2) + " para seguir." else ""
            softToast("Temporada " + (s + 1) + " completa!" + more, Genres.byKey(d.genre).primary, "check")
        }
    }

    private fun epMinus() {
        val d = curDrama() ?: return
        val t = curTab() ?: return
        val s = t.season
        if (s !in d.seasonEps.indices) return
        if (d.watched[s] <= 0) {
            softToast("Essa temporada ainda está zerada", Palette.pink, "tv")
            return
        }
        Store.adjust(d, s, -1)
        updateEp(true)
        renderSeasons()
    }

    // --------------------------------------------------------------- menu

    private fun showMenu(anchor: View) {
        val t = curTab() ?: return
        val d = Store.get(t.id) ?: return
        val pm = PopupMenu(this, anchor)
        pm.menu.add(0, 1, 0, "Usar esta página como link do dorama")
        pm.menu.add(0, 2, 1, "Ir para a página inicial")
        pm.menu.add(0, 3, 2, "Abrir no navegador do celular")
        pm.menu.add(0, 4, 3, "Copiar endereço")
        pm.setOnMenuItemClickListener { item ->
            val u = t.web.url ?: ""
            when (item.itemId) {
                1 -> if (u.startsWith("http")) {
                    d.link = u
                    d.lastUrl = u
                    Store.save(d)
                    softToast("Link do dorama atualizado!", Palette.pink, "link")
                }
                2 -> t.web.loadUrl(if (d.link.isNotBlank()) d.link else Store.homeUrl)
                3 -> if (u.isNotBlank()) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                    } catch (e: Exception) {
                        softToast("Não consegui abrir", Palette.pink, "close")
                    }
                }
                4 -> if (u.isNotBlank()) {
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("link", u))
                    softToast("Endereço copiado!", Palette.pink, "check")
                }
            }
            true
        }
        pm.show()
    }

    // ------------------------------------------------------------ ciclo

    private fun saveUrl(t: Tab) {
        val d = Store.get(t.id) ?: return
        val u = t.web.url ?: return
        if (u.startsWith("http") && u != d.lastUrl) {
            d.lastUrl = u
            Store.save(d)
        }
    }

    override fun onPause() {
        for (t in tabs) saveUrl(t)
        curTab()?.web?.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        curTab()?.web?.onResume()
        renderSeasons()
        updateEp(false)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (customView != null) {
            exitCustom()
            return
        }
        val w = curTab()?.web
        if (w != null && w.canGoBack()) {
            w.goBack()
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        for (t in tabs) {
            saveUrl(t)
            (t.web.parent as? ViewGroup)?.removeView(t.web)
            t.web.destroy()
        }
        tabs.clear()
        super.onDestroy()
    }
}
