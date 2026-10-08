package com.doramabloom.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.view.Gravity
import android.view.View
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONTokener
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Navegador para escolher imagem direto da internet, sem baixar pro celular antes.
 * Pesquise (Google, Bing ou DuckDuckGo), toque numa imagem para abrir grande e use o botão
 * "Usar a imagem da tela" (ou segure o dedo em qualquer imagem). A imagem é baixada, você
 * confere numa prévia e ela volta para a tela de edição como se tivesse vindo da galeria.
 *
 * Recebe: "query" (texto inicial da busca) e "title" (título da tela).
 * Devolve: RESULT_OK com "path" (arquivo temporário da imagem, que quem chamou deve apagar depois).
 */
class WebPickActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private lateinit var urlIn: EditText
    private lateinit var progress: ProgressBar
    private val engineBtns = ArrayList<TextView>()
    private var engine = 0
    private var busy = false

    private val engines = listOf(
        "Google" to "https://www.google.com/search?tbm=isch&q=",
        "Bing" to "https://www.bing.com/images/search?q=",
        "DuckDuckGo" to "https://duckduckgo.com/?iax=images&ia=images&q="
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        ThemeMode.refresh(this)
        buildUi()
        val q = intent.getStringExtra("query") ?: ""
        urlIn.setText(q)
        if (q.isNotBlank()) search(q) else web.loadUrl("https://www.google.com/imghp")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildUi() {
        val root = FrameLayout(this)
        root.background = gradient(Palette.bgTop, Palette.bgBottom)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        root.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))

        // topo: fechar + caixa de busca
        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.setPadding(dp(12), dp(8), dp(12), dp(4))
        top.addView(roundBtn("close", Palette.pink, false, 16) { finish() }, lin(dp(40), dp(40)))
        urlIn = EditText(this)
        urlIn.setSingleLine(true)
        urlIn.hint = intent.getStringExtra("title") ?: "Pesquisar imagem"
        urlIn.textSize = 14f
        urlIn.setTextColor(Palette.text)
        urlIn.setHintTextColor(Palette.muted)
        urlIn.background = null
        urlIn.setPadding(dp(4), dp(8), dp(4), dp(8))
        urlIn.imeOptions = EditorInfo.IME_ACTION_SEARCH
        urlIn.inputType = InputType.TYPE_CLASS_TEXT
        urlIn.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_GO) {
                go(urlIn.text.toString())
                true
            } else false
        }
        val box = LinearLayout(this)
        box.orientation = LinearLayout.HORIZONTAL
        box.gravity = Gravity.CENTER_VERTICAL
        box.setPadding(dp(14), 0, dp(10), 0)
        box.background = roundRect(Palette.card, dp(24).toFloat(), Palette.line, dp(1))
        box.addView(IconView(this, "search", Palette.muted, 16))
        box.addView(urlIn, lin(0, WRAP, 1f, l = 6))
        top.addView(box, lin(0, dp(40), 1f, l = 8))
        col.addView(top, lin(MATCH, WRAP))

        // buscadores
        val eng = LinearLayout(this)
        eng.orientation = LinearLayout.HORIZONTAL
        eng.setPadding(dp(12), dp(2), dp(12), dp(6))
        for ((i, e) in engines.withIndex()) {
            val b = pill(e.first, Palette.card, Palette.pink, 12f)
            b.setOnClickListener {
                engine = i
                styleEngines()
                val t = urlIn.text.toString().trim()
                if (t.isNotEmpty()) search(t)
            }
            engineBtns.add(b)
            eng.addView(b, lin(WRAP, WRAP, r = 6))
        }
        styleEngines()
        col.addView(eng, lin(MATCH, WRAP))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progress.max = 100
        progress.progressTintList = android.content.res.ColorStateList.valueOf(Palette.pink)
        progress.visibility = View.INVISIBLE
        col.addView(progress, lin(MATCH, dp(3)))

        web = WebView(this)
        val s = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.loadsImagesAutomatically = true
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                val u = r.url.toString()
                // só segue páginas da web; links de apps (intent://, market://...) são ignorados
                return !(u.startsWith("http://") || u.startsWith("https://"))
            }

            override fun onPageStarted(v: WebView, url: String?, favicon: Bitmap?) {
                progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(v: WebView, url: String?) {
                progress.visibility = View.INVISIBLE
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(v: WebView, p: Int) {
                progress.progress = p
                progress.visibility = if (p in 1..99) View.VISIBLE else View.INVISIBLE
            }
        }
        // segurar o dedo numa imagem usa ela
        web.setOnLongClickListener {
            val r = web.hitTestResult
            val t = r.type
            val ex = r.extra
            if ((t == WebView.HitTestResult.IMAGE_TYPE || t == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) && !ex.isNullOrEmpty()) {
                usar(ex)
                true
            } else false
        }
        col.addView(web, lin(MATCH, 0, 1f))

        // rodapé: dica + botão
        val bottom = LinearLayout(this)
        bottom.orientation = LinearLayout.VERTICAL
        bottom.setPadding(dp(14), dp(8), dp(14), dp(10))
        bottom.background = roundRect(Palette.card, dp(0).toFloat(), Palette.line, dp(1))
        val hint = label(
            "Toque numa imagem para abrir grande e depois use o botão. Ou segure o dedo em cima de uma imagem.",
            11.5f, Palette.muted
        )
        bottom.addView(hint, lin(MATCH, WRAP, b = 8))
        val use = pill("Usar a imagem da tela", Palette.pink, Color.WHITE, 14f, "image")
        use.setPadding(dp(18), dp(12), dp(18), dp(12))
        use.setOnClickListener { usarDaTela() }
        bottom.addView(use, lin(MATCH, WRAP))
        col.addView(bottom, lin(MATCH, WRAP))

        setContentView(root)
        applyBarStyle()
    }

    private fun styleEngines() {
        for ((i, b) in engineBtns.withIndex()) {
            val on = i == engine
            b.background = roundRect(if (on) Palette.pink else Palette.card, dp(20).toFloat(), Palette.pink, dp(1))
            b.setTextColor(if (on) Color.WHITE else Palette.pink)
        }
    }

    private fun hideKeyboard() {
        try {
            val im = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(urlIn.windowToken, 0)
            urlIn.clearFocus()
        } catch (e: Exception) {
        }
    }

    private fun search(q: String) {
        hideKeyboard()
        web.loadUrl(engines[engine].second + URLEncoder.encode(q.trim(), "UTF-8"))
    }

    /** Texto vira busca; endereço de site é aberto direto. */
    private fun go(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        val looksUrl = t.startsWith("http://") || t.startsWith("https://") ||
            (!t.contains(" ") && t.contains(".") && !t.endsWith("."))
        if (looksUrl) {
            hideKeyboard()
            web.loadUrl(if (t.startsWith("http")) t else "https://$t")
        } else {
            search(t)
        }
    }

    /** Procura a maior imagem visível na página (a que você abriu em tamanho grande) e usa ela. */
    private fun usarDaTela() {
        val js = "(function(){var best='',bs=0,vw=innerWidth,vh=innerHeight,im=document.images;" +
            "for(var i=0;i<im.length;i++){var e=im[i],r=e.getBoundingClientRect();" +
            "var w=Math.max(0,Math.min(r.right,vw)-Math.max(r.left,0));" +
            "var h=Math.max(0,Math.min(r.bottom,vh)-Math.max(r.top,0));" +
            "var src=e.currentSrc||e.src;if(!src||e.naturalWidth<120)continue;" +
            "var s=w*h;if(s>bs){bs=s;best=src}}return best})()"
        web.evaluateJavascript(js) { raw ->
            var src = ""
            try {
                src = (JSONTokener(raw ?: "\"\"").nextValue() as? String) ?: ""
            } catch (e: Exception) {
            }
            if (src.isEmpty()) {
                Toast.makeText(this, "Não achei imagem na tela. Toque numa imagem para abrir grande, ou segure o dedo nela.", Toast.LENGTH_LONG).show()
            } else {
                usar(src)
            }
        }
    }

    /** Baixa a imagem, mostra uma prévia e, se você confirmar, devolve para a tela de edição. */
    private fun usar(src: String) {
        if (busy) return
        busy = true
        Toast.makeText(this, "Baixando a imagem…", Toast.LENGTH_SHORT).show()
        val referer = web.url ?: ""
        val ua = web.settings.userAgentString
        Thread {
            val f = baixar(src, referer, ua)
            var bmp: Bitmap? = null
            if (f != null) {
                try {
                    val o = BitmapFactory.Options()
                    o.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(f.absolutePath, o)
                    if (o.outWidth > 0 && o.outHeight > 0) {
                        var sample = 1
                        while (o.outWidth / (sample * 2) >= 700 && o.outHeight / (sample * 2) >= 700) sample *= 2
                        val o2 = BitmapFactory.Options()
                        o2.inSampleSize = sample
                        bmp = BitmapFactory.decodeFile(f.absolutePath, o2)
                    }
                } catch (e: Exception) {
                    bmp = null
                }
            }
            runOnUiThread {
                busy = false
                if (isFinishing || isDestroyed) {
                    f?.delete()
                    return@runOnUiThread
                }
                if (f == null || bmp == null) {
                    f?.delete()
                    Toast.makeText(this, "Não consegui pegar essa imagem. Tente abrir ela grande ou escolher outra.", Toast.LENGTH_LONG).show()
                } else {
                    confirmar(f, bmp)
                }
            }
        }.start()
    }

    private fun confirmar(f: File, bmp: Bitmap) {
        val box = FrameLayout(this)
        box.setPadding(dp(18), dp(14), dp(18), dp(4))
        val iv = ImageView(this)
        iv.setImageBitmap(bmp)
        iv.scaleType = ImageView.ScaleType.FIT_CENTER
        iv.adjustViewBounds = true
        box.addView(iv, FrameLayout.LayoutParams(MATCH, dp(340)))
        AlertDialog.Builder(this)
            .setTitle("Usar esta imagem?")
            .setView(box)
            .setPositiveButton("Usar") { _, _ ->
                val r = Intent()
                r.putExtra("path", f.absolutePath)
                setResult(RESULT_OK, r)
                finish()
            }
            .setNegativeButton("Escolher outra") { _, _ -> f.delete() }
            .setOnCancelListener { f.delete() }
            .show()
    }

    /** Baixa (ou decodifica, se for data:) a imagem para um arquivo temporário. null se falhar. */
    private fun baixar(src: String, referer: String, ua: String): File? {
        val dir = File(cacheDir, "webpick")
        dir.mkdirs()
        // limpa sobras de tentativas anteriores
        dir.listFiles()?.forEach { if (System.currentTimeMillis() - it.lastModified() > 10 * 60 * 1000L) it.delete() }
        val f = File(dir, "p" + System.currentTimeMillis() + ".img")
        return try {
            if (src.startsWith("data:")) {
                val comma = src.indexOf(',')
                if (comma < 0) return null
                val meta = src.substring(0, comma)
                val body = src.substring(comma + 1)
                val bytes = if (meta.contains(";base64")) Base64.decode(body, Base64.DEFAULT)
                else URLDecoder.decode(body, "UTF-8").toByteArray(Charsets.ISO_8859_1)
                FileOutputStream(f).use { it.write(bytes) }
                f
            } else if (src.startsWith("http://") || src.startsWith("https://")) {
                var url = src
                var conn: HttpURLConnection? = null
                // segue redirecionamentos na mão (inclusive de http para https)
                for (hop in 0 until 6) {
                    val c = URL(url).openConnection() as HttpURLConnection
                    c.connectTimeout = 15000
                    c.readTimeout = 30000
                    c.instanceFollowRedirects = false
                    c.setRequestProperty("User-Agent", ua)
                    c.setRequestProperty("Accept", "image/avif,image/webp,image/*,*/*;q=0.8")
                    if (referer.isNotEmpty()) c.setRequestProperty("Referer", referer)
                    val code = c.responseCode
                    if (code in 300..399) {
                        val loc = c.getHeaderField("Location")
                        c.disconnect()
                        if (loc.isNullOrEmpty()) return null
                        url = URL(URL(url), loc).toString()
                        continue
                    }
                    conn = c
                    break
                }
                val c = conn ?: return null
                if (c.responseCode !in 200..299) {
                    c.disconnect()
                    return null
                }
                var total = 0L
                c.inputStream.use { ins ->
                    FileOutputStream(f).use { out ->
                        val buf = ByteArray(32768)
                        while (true) {
                            val n = ins.read(buf)
                            if (n < 0) break
                            total += n
                            if (total > 30L * 1024 * 1024) throw java.io.IOException("grande demais")
                            out.write(buf, 0, n)
                        }
                    }
                }
                c.disconnect()
                if (total < 200) {
                    f.delete()
                    null
                } else f
            } else {
                null
            }
        } catch (e: Exception) {
            try {
                f.delete()
            } catch (x: Exception) {
            }
            null
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        try {
            (web.parent as? android.view.ViewGroup)?.removeView(web)
            web.stopLoading()
            web.destroy()
        } catch (e: Exception) {
        }
        super.onDestroy()
    }
}
