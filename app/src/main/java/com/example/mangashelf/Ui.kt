package com.example.mangashelf

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.graphics.Shader
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.text.InputType
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.util.LruCache
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileOutputStream

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

object P {
    var dark = false
    // rosa-framboesa suave (menos chapado), com versões translúcidas
    val accent: Int get() = if (dark) 0xFFEF4B57.toInt() else 0xFFD9343F.toInt()
    val accentDeep: Int get() = if (dark) 0xFFC62A3A.toInt() else 0xFFB3222E.toInt()
    val onAccent: Int get() = 0xFFFFFFFF.toInt()
    val accentSoft: Int get() = if (dark) 0x2BEF4B57 else 0x1FD9343F
    val accentTile: Int get() = if (dark) 0x40EF4B57 else 0x2ED9343F
    val accentLine: Int get() = if (dark) 0x70EF4B57 else 0x59D9343F
    val heroEnd: Int get() = if (dark) 0x4DC62A3A else 0x38C62A3A
    val accent2: Int = 0xFFFF5964.toInt() // coração (favorito)
    val star: Int = 0xFFFFB02E.toInt() // estrela da nota
    val bg: Int get() = if (dark) 0xFF09090C.toInt() else 0xFFFBF7F8.toInt()
    val card: Int get() = if (dark) 0xFF121217.toInt() else 0xFFFFFFFF.toInt()
    val card2: Int get() = if (dark) 0xFF1B1B22.toInt() else 0xFFF6EFF0.toInt()
    val text: Int get() = if (dark) 0xFFF5F5F7.toInt() else 0xFF1B1416.toInt()
    val sub: Int get() = if (dark) 0xFF9C9CA8.toInt() else 0xFF7A6B6D.toInt()
    val line: Int get() = if (dark) 0xFF2A2A33.toInt() else 0xFFEBDFE0.toInt()

    // "vidro": cartões levemente translúcidos com brilho no topo
    val glassTop: Int get() = if (dark) 0x22FFFFFF else 0xFFFFFFFF.toInt()
    val glassBottom: Int get() = if (dark) 0x0AFFFFFF else 0xFFFFF6F7.toInt()
    val glassLine: Int get() = if (dark) 0x2EFFFFFF else 0xFFEBDFE0.toInt()

    // cor de cada categoria (usadas só para separar as seções)
    val cReading: Int get() = if (dark) 0xFF74A9FF.toInt() else 0xFF3F7FE0.toInt()
    val cPlan: Int get() = if (dark) 0xFFF5B85C.toInt() else 0xFFD9902B.toInt()
    val cDone: Int get() = if (dark) 0xFF5FD4A0.toInt() else 0xFF2FA878.toInt()
    val cPaused: Int get() = if (dark) 0xFFB69CFF.toInt() else 0xFF8A68E0.toInt()
    val cFav: Int get() = if (dark) 0xFFFF8A5C.toInt() else 0xFFE0602F.toInt()
}

/** Mesma cor com outra transparência (a = 0..255). */
fun tint(c: Int, a: Int): Int = (c and 0x00FFFFFF) or (a shl 24)

/** Efeito discreto de toque: encolhe um pouquinho ao pressionar. Não bloqueia o clique. */
fun View.pressFx() {
    setOnTouchListener { v, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
        }
        false
    }
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun shape(color: Int, radius: Float, stroke: Int = 0, strokeW: Int = 0): GradientDrawable {
    val g = GradientDrawable()
    if (strokeW > 0 && color == P.card && stroke == P.line) {
        // todo cartão do app vira "vidro": degradê translúcido + borda de luz
        g.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM)
        g.setColors(intArrayOf(P.glassTop, P.glassBottom))
        g.cornerRadius = radius
        g.setStroke(strokeW, P.glassLine)
        return g
    }
    g.setColor(color)
    g.cornerRadius = radius
    if (strokeW > 0) g.setStroke(strokeW, stroke)
    return g
}

/** Dá efeito de ondinha suave (laranja) ao toque, respeitando os cantos arredondados. */
fun Context.rippled(content: Drawable, radius: Float): Drawable {
    val c = ColorStateList.valueOf((P.accent and 0x00FFFFFF) or 0x33000000)
    return RippleDrawable(c, content, shape(Color.WHITE, radius))
}

fun Context.vbox(): LinearLayout {
    val l = LinearLayout(this)
    l.orientation = LinearLayout.VERTICAL
    return l
}

fun Context.hbox(): LinearLayout {
    val l = LinearLayout(this)
    l.orientation = LinearLayout.HORIZONTAL
    l.gravity = Gravity.CENTER_VERTICAL
    return l
}

/** w e h em px (ou MATCH/WRAP); margens em dp. */
fun <T : View> LinearLayout.addv(
    v: T, w: Int = MATCH, h: Int = WRAP, weight: Float = 0f,
    l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0
): T {
    val p = LinearLayout.LayoutParams(w, h, weight)
    p.setMargins(context.dp(l), context.dp(t), context.dp(r), context.dp(b))
    addView(v, p)
    return v
}

fun Context.tv(t: CharSequence, size: Float = 14f, color: Int = P.text, bold: Boolean = false): TextView {
    val x = TextView(this)
    x.text = t
    x.textSize = size
    x.setTextColor(color)
    if (bold) x.typeface = Typeface.DEFAULT_BOLD
    return x
}

fun Context.lbl(t: String): TextView {
    val x = tv(t, 11.5f, P.sub, true)
    x.letterSpacing = 0.08f
    return x
}

fun Context.pill(
    t: String, bg: Int = P.accent, fg: Int = P.onAccent, size: Float = 14f,
    icon: Ic? = null, onClick: () -> Unit
): TextView {
    val x = tv(t, size, fg, true)
    x.gravity = Gravity.CENTER
    x.setPadding(dp(18), dp(13), dp(18), dp(13))
    val r = dp(26).toFloat()
    val base: GradientDrawable = if (bg == P.accent) {
        val g = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(tint(bg, 0xEB), tint(P.accentDeep, 0xDB)))
        g.cornerRadius = r
        g.setStroke(dp(1), tint(Color.WHITE, 0x38))
        g
    } else shape(bg, r)
    x.background = rippled(base, r)
    if (icon != null) x.setIconText(icon, t)
    x.setOnClickListener { onClick() }
    x.pressFx()
    return x
}

fun Context.outlinePill(t: String, size: Float = 13f, icon: Ic? = null, onClick: () -> Unit): TextView {
    val x = tv(t, size, P.text, true)
    x.gravity = Gravity.CENTER
    x.setPadding(dp(12), dp(12), dp(12), dp(12))
    val r = dp(26).toFloat()
    x.background = rippled(shape(P.card, r, P.line, dp(1)), r)
    if (icon != null) x.setIconText(icon, t)
    x.setOnClickListener { onClick() }
    x.pressFx()
    return x
}

fun Context.chip(t: String, selected: Boolean, icon: Ic? = null, onClick: () -> Unit): TextView {
    val x = tv(t, 13f, if (selected) P.accent else P.text, selected)
    x.setPadding(dp(14), dp(8), dp(14), dp(8))
    val r = dp(20).toFloat()
    x.background = rippled(
        if (selected) shape(P.accentSoft, r, P.accent, dp(1)) else shape(P.card, r, P.line, dp(1)), r
    )
    if (icon != null) x.setIconText(icon, t, false, 16)
    x.setOnClickListener { onClick() }
    x.pressFx()
    return x
}

/** Botão redondo só com ícone. */
fun Context.roundBtn(ic: Ic, boxDp: Int = 40, iconDp: Int = 20, color: Int = P.text, filled: Boolean = true, onClick: () -> Unit): IconView {
    val v = IconView(this, ic, color, iconDp)
    val r = dp(boxDp).toFloat()
    v.background = rippled(
        if (filled) shape(P.card, r, P.line, dp(1)) else shape(Color.TRANSPARENT, r), r
    )
    v.setOnClickListener { onClick() }
    v.pressFx()
    return v
}

/** Quadradinho arredondado com ícone (usado em títulos e cartões). */
fun Context.iconTile(ic: Ic, iconDp: Int = 20, color: Int = P.accent): FrameLayout {
    val f = FrameLayout(this)
    f.background = if (color == P.accent) shape(P.accentTile, dp(13).toFloat())
    else shape(tint(color, 0x30), dp(13).toFloat(), tint(color, 0x55), dp(1))
    f.addView(IconView(this, ic, color, iconDp), FrameLayout.LayoutParams(dp(iconDp + 8), dp(iconDp + 8), Gravity.CENTER))
    return f
}

/** Botão grande de destaque (ex.: Continuar lendo). */
fun Context.heroButton(caption: String, label: String, ic: Ic, onClick: () -> Unit): LinearLayout {
    val row = hbox()
    row.setPadding(dp(14), dp(12), dp(16), dp(12))
    val r = dp(18).toFloat()
    val g = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(P.accentSoft, P.heroEnd))
    g.cornerRadius = r
    g.setStroke(dp(1), P.accentLine)
    row.background = rippled(g, r)
    row.addv(iconTile(ic, 22), dp(46), dp(46), 0f, 0, 0, 14, 0)
    val col = vbox()
    val cap = tv(caption, 10.5f, P.accent, true)
    cap.letterSpacing = 0.1f
    col.addv(cap)
    col.addv(tv(label, 17f, P.text, true), MATCH, WRAP, 0f, 0, 1, 0, 0)
    row.addv(col, 0, WRAP, 1f)
    row.addv(IconView(this, Ic.ArrowRight, P.accent, 20), dp(22), dp(22))
    row.setOnClickListener { onClick() }
    row.pressFx()
    return row
}

/** Título de seção com ícone. */
fun Context.sectionTitle(ic: Ic, t: String, size: Float = 18f): LinearLayout {
    val row = hbox()
    row.addv(IconView(this, ic, P.accent, 20), dp(22), dp(22), 0f, 0, 0, 9, 0)
    row.addv(tv(t, size, P.text, true), 0, WRAP, 1f)
    return row
}

/** Campo de busca com lupa. Retorna o contêiner e o campo de texto. */
fun Context.searchBox(hint: String, value: String): Pair<LinearLayout, EditText> {
    val box = hbox()
    box.setPadding(dp(14), 0, dp(12), 0)
    val r = dp(16).toFloat()
    fun bgOf(focused: Boolean) = shape(P.card, r, if (focused) P.accent else P.line, dp(1))
    box.background = bgOf(false)
    box.addv(IconView(this, Ic.Search, P.sub, 20), dp(22), dp(22), 0f, 0, 0, 10, 0)
    val e = EditText(this)
    e.hint = hint
    e.setHintTextColor(P.sub)
    e.setTextColor(P.text)
    e.textSize = 15f
    e.setSingleLine(true)
    e.inputType = InputType.TYPE_CLASS_TEXT
    e.imeOptions = EditorInfo.IME_ACTION_SEARCH
    e.background = null
    e.setPadding(0, dp(13), 0, dp(13))
    e.highlightColor = (P.accent and 0x00FFFFFF) or 0x55000000
    e.setText(value)
    e.setOnFocusChangeListener { _, f -> box.background = bgOf(f) }
    box.addv(e, 0, WRAP, 1f)
    return Pair(box, e)
}

fun Context.inputField(
    hint: String, value: String = "", lines: Int = 1,
    input: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
): EditText {
    val e = EditText(this)
    e.setText(value)
    e.hint = hint
    e.setHintTextColor(P.sub)
    e.setTextColor(P.text)
    e.textSize = 15f
    if (lines > 1) {
        e.inputType = input or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        e.minLines = lines
        e.gravity = Gravity.TOP or Gravity.START
    } else {
        e.inputType = input
        e.setSingleLine(true)
    }
    e.setPadding(dp(14), dp(12), dp(14), dp(12))
    val r = dp(14).toFloat()
    val bgSel = StateListDrawable()
    bgSel.addState(intArrayOf(android.R.attr.state_focused), shape(P.card, r, P.accent, dp(1)))
    bgSel.addState(intArrayOf(), shape(P.card, r, P.line, dp(1)))
    e.background = bgSel
    e.highlightColor = (P.accent and 0x00FFFFFF) or 0x55000000
    return e
}

fun Context.progressBar(pct: Int, heightDp: Int = 5): View {
    val p = pct.coerceIn(0, 100)
    val row = LinearLayout(this)
    row.orientation = LinearLayout.HORIZONTAL
    row.background = shape(P.line, dp(heightDp).toFloat())
    val fill = View(this)
    fill.background = shape(P.accent, dp(heightDp).toFloat())
    row.addView(fill, LinearLayout.LayoutParams(0, dp(heightDp), p.toFloat()))
    row.addView(View(this), LinearLayout.LayoutParams(0, dp(heightDp), (100 - p).toFloat()))
    return row
}

fun Context.toast(m: String) {
    Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}

fun Context.confirmDialog(title: String, msg: String, yes: String = "Sim", onYes: () -> Unit) {
    MaterialAlertDialogBuilder(this).setTitle(title).setMessage(msg)
        .setPositiveButton(yes) { _, _ -> onYes() }
        .setNegativeButton("Cancelar", null).show()
}

fun Context.inputDialog(
    title: String, hint: String, initial: String = "",
    input: Int = InputType.TYPE_CLASS_TEXT, onOk: (String) -> Unit
) {
    val box = FrameLayout(this)
    box.setPadding(dp(20), dp(8), dp(20), 0)
    val et = inputField(hint, initial, 1, input)
    box.addView(et, MATCH, WRAP)
    MaterialAlertDialogBuilder(this).setTitle(title).setView(box)
        .setPositiveButton("OK") { _, _ -> onOk(et.text.toString()) }
        .setNegativeButton("Cancelar", null).show()
}

fun Context.listDialog(title: String, items: List<String>, onPick: (Int) -> Unit) {
    MaterialAlertDialogBuilder(this).setTitle(title)
        .setItems(items.toTypedArray()) { _, i -> onPick(i) }.show()
}

/** Lista de opções com ícone ao lado de cada linha. */
fun Context.iconListDialog(title: String, items: List<Pair<Ic, String>>, onPick: (Int) -> Unit) {
    val col = vbox()
    col.setPadding(dp(8), dp(4), dp(8), dp(8))
    lateinit var dlg: AlertDialog
    items.forEachIndexed { i, item ->
        val row = hbox()
        row.setPadding(dp(14), dp(13), dp(14), dp(13))
        row.background = rippled(shape(Color.TRANSPARENT, dp(14).toFloat()), dp(14).toFloat())
        row.addv(IconView(this, item.first, P.accent, 20), dp(22), dp(22), 0f, 0, 0, 14, 0)
        row.addv(tv(item.second, 15f, P.text), 0, WRAP, 1f)
        row.setOnClickListener {
            dlg.dismiss()
            onPick(i)
        }
        col.addv(row)
    }
    val sv = ScrollView(this)
    sv.addView(col)
    dlg = MaterialAlertDialogBuilder(this).setTitle(title).setView(sv).create()
    dlg.show()
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun normalizeUrl(s: String): String {
    val t = s.trim()
    if (t.isEmpty()) return ""
    return if (t.contains("://")) t else "https://$t"
}

fun hostOf(url: String): String = try {
    Uri.parse(url).host?.lowercase()?.removePrefix("www.") ?: ""
} catch (e: Exception) {
    ""
}

fun rootHost(h: String): String {
    val p = h.split(".")
    return if (p.size >= 2) p.takeLast(2).joinToString(".") else h
}

fun sameSite(a: String, b: String): Boolean = a.isNotEmpty() && b.isNotEmpty() && rootHost(a) == rootHost(b)

fun fmtDate(ms: Long): String =
    if (ms <= 0L) "—" else java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ms))

fun downloadBytes(url: String, maxBytes: Int = 8000000): ByteArray? = try {
    val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    c.connectTimeout = 10000
    c.readTimeout = 15000
    c.instanceFollowRedirects = true
    c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
    c.inputStream.use { ins ->
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        var tooBig = false
        while (true) {
            val n = ins.read(buf)
            if (n < 0) break
            total += n
            if (total > maxBytes) { tooBig = true; break }
            out.write(buf, 0, n)
        }
        if (tooBig) null else out.toByteArray()
    }
} catch (e: Exception) {
    null
}

// ---------- Capas ----------

private val COVER_COLORS = listOf(
    Pair(0xFFE5606B.toInt(), 0xFF6E2230.toInt()),
    Pair(0xFFF0966A.toInt(), 0xFF8A4A32.toInt()),
    Pair(0xFF7FA6F0.toInt(), 0xFF2F4A86.toInt()),
    Pair(0xFF5CC9A0.toInt(), 0xFF1F6650.toInt()),
    Pair(0xFFD888D0.toInt(), 0xFF6E3470.toInt()),
    Pair(0xFF8796B0.toInt(), 0xFF2A3347.toInt())
)

fun coverPalette(title: String): Pair<Int, Int> =
    COVER_COLORS[(title.hashCode() and 0x7fffffff) % COVER_COLORS.size]

object Covers {
    private val blurCache = HashMap<String, Bitmap>()

    /** Versão bem desfocada da capa (pequena, borrada por código e ampliada suavemente). */
    fun blurred(store: Store, name: String): Bitmap? {
        blurCache[name]?.let { return it }
        val src = get(store, name) ?: return null
        return try {
            val w = 40
            val h = Math.max(8, (src.height * w.toFloat() / src.width).toInt())
            val small = Bitmap.createScaledBitmap(src, w, h, true).copy(Bitmap.Config.ARGB_8888, true)
            val px = IntArray(w * h)
            small.getPixels(px, 0, w, 0, 0, w, h)
            repeat(3) {
                boxBlur(px, w, h, 3, true)
                boxBlur(px, w, h, 3, false)
            }
            small.setPixels(px, 0, w, 0, 0, w, h)
            val out = Bitmap.createScaledBitmap(small, w * 8, h * 8, true)
            blurCache[name] = out
            out
        } catch (e: Throwable) {
            null
        }
    }

    private fun boxBlur(px: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val copy = px.copyOf()
        val outer = if (horizontal) h else w
        val inner = if (horizontal) w else h
        for (o in 0 until outer) {
            for (i in 0 until inner) {
                var a = 0; var rr = 0; var g = 0; var b = 0; var n = 0
                for (k in -r..r) {
                    val j = i + k
                    if (j < 0 || j >= inner) continue
                    val idx = if (horizontal) o * w + j else j * w + o
                    val c = copy[idx]
                    a += c ushr 24
                    rr += (c shr 16) and 0xFF
                    g += (c shr 8) and 0xFF
                    b += c and 0xFF
                    n++
                }
                val dst = if (horizontal) o * w + i else i * w + o
                px[dst] = ((a / n) shl 24) or ((rr / n) shl 16) or ((g / n) shl 8) or (b / n)
            }
        }
    }

    private val cache = object : LruCache<String, Bitmap>(20 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun get(store: Store, name: String): Bitmap? {
        val c = cache.get(name)
        if (c != null) return c
        val f = File(store.coversDir, name)
        if (!f.exists()) return null
        val b = BitmapFactory.decodeFile(f.path) ?: return null
        cache.put(name, b)
        return b
    }

    private fun sampleFor(w: Int, h: Int, maxSide: Int): Int {
        var s = 1
        while (w / s > maxSide * 2 || h / s > maxSide * 2) s *= 2
        return s
    }

    fun decodeUri(ctx: Context, uri: Uri, maxSide: Int): Bitmap? = try {
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        val o2 = BitmapFactory.Options()
        o2.inSampleSize = sampleFor(o.outWidth, o.outHeight, maxSide)
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o2) }
    } catch (e: Exception) {
        null
    }

    fun decodeBytes(bytes: ByteArray, maxSide: Int): Bitmap? = try {
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        val o2 = BitmapFactory.Options()
        o2.inSampleSize = sampleFor(o.outWidth, o.outHeight, maxSide)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o2)
    } catch (e: Exception) {
        null
    }

    const val MAX_ANIM_BYTES = 15L * 1024 * 1024

    class Picked(val poster: Bitmap, val anim: File?)

    private fun shrink(src: Bitmap, maxSide: Int): Bitmap {
        val sc = Math.min(1f, maxSide.toFloat() / Math.max(src.width, src.height).toFloat())
        if (sc >= 1f) return src
        return Bitmap.createScaledBitmap(
            src, Math.max(1, (src.width * sc).toInt()), Math.max(1, (src.height * sc).toInt()), true
        )
    }

    /** Copia o arquivo escolhido para o cache (até 15 MB). Devolve null se for grande demais. */
    private fun copyToCache(ctx: Context, uri: Uri, ext: String): File? {
        val out = File(ctx.cacheDir, "pending_cover." + ext)
        try {
            var total = 0L
            ctx.contentResolver.openInputStream(uri)?.use { ins ->
                FileOutputStream(out).use { os ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = ins.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > MAX_ANIM_BYTES) {
                            out.delete()
                            return null
                        }
                        os.write(buf, 0, n)
                    }
                }
            } ?: return null
            return out
        } catch (e: Exception) {
            out.delete()
            return null
        }
    }

    /** Foto, GIF ou vídeo: devolve o quadro estático (poster) e, se for animado, o arquivo temporário. */
    fun loadPicked(ctx: Context, uri: Uri): Picked? {
        val mime = ctx.contentResolver.getType(uri) ?: ""
        try {
            if (mime.startsWith("video/")) {
                val ext = when {
                    mime.contains("webm") -> "webm"
                    mime.contains("3gpp") -> "3gp"
                    else -> "mp4"
                }
                val tmp = copyToCache(ctx, uri, ext) ?: return null
                var frame: Bitmap? = null
                val r = MediaMetadataRetriever()
                try {
                    r.setDataSource(tmp.path)
                    frame = r.getFrameAtTime(300000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: r.getFrameAtTime(0L)
                } catch (e: Exception) {
                    frame = null
                } finally {
                    try { r.release() } catch (e: Exception) { }
                }
                if (frame == null) {
                    tmp.delete()
                    return null
                }
                return Picked(shrink(frame, 1000), tmp)
            }
            val poster = decodeUri(ctx, uri, 1000) ?: return null
            val isGif = mime == "image/gif"
            val isWebp = mime == "image/webp"
            if ((isGif || isWebp) && Build.VERSION.SDK_INT >= 28) {
                val tmp = copyToCache(ctx, uri, if (isGif) "gif" else "webp")
                if (tmp != null && animatedDrawable(tmp, true) != null) return Picked(poster, tmp)
                tmp?.delete()
            }
            return Picked(poster, null)
        } catch (e: Throwable) {
            return null
        }
    }

    /** Só Android 9+: abre GIF/WebP animado. Com checkOnly=true usa amostragem para só conferir. */
    fun animatedDrawable(f: File, checkOnly: Boolean = false, maxSide: Int = 0): Drawable? {
        if (Build.VERSION.SDK_INT < 28) return null
        return try {
            val src = android.graphics.ImageDecoder.createSource(f)
            val d = android.graphics.ImageDecoder.decodeDrawable(src) { dec, info, _ ->
                if (checkOnly) {
                    dec.setTargetSampleSize(4)
                } else if (maxSide > 0) {
                    val big = Math.max(info.size.width, info.size.height)
                    val n = big / maxSide
                    if (n >= 2) dec.setTargetSampleSize(n)
                }
            }
            if (d is android.graphics.drawable.AnimatedImageDrawable) {
                if (!checkOnly) {
                    d.repeatCount = android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE
                    d.start()
                }
                d
            } else null
        } catch (e: Throwable) {
            null
        }
    }

    /** Move o arquivo temporário escolhido para a pasta de capas. */
    fun saveAnim(store: Store, workId: String, tmp: File): String {
        val name = workId + "_" + System.currentTimeMillis() + "_a." + tmp.extension
        tmp.copyTo(File(store.coversDir, name), true)
        tmp.delete()
        return name
    }

    fun save(store: Store, workId: String, src: Bitmap): String {
        val maxSide = 700
        val sc = Math.min(1f, maxSide.toFloat() / Math.max(src.width, src.height).toFloat())
        val b = if (sc < 1f) Bitmap.createScaledBitmap(
            src,
            Math.max(1, (src.width * sc).toInt()),
            Math.max(1, (src.height * sc).toInt()),
            true
        ) else src
        val name = workId + "_" + System.currentTimeMillis() + ".jpg"
        FileOutputStream(File(store.coversDir, name)).use { b.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return name
    }
}

class CoverFrame(ctx: Context) : FrameLayout(ctx) {
    var ratio = 1.45f
    override fun onMeasure(w: Int, h: Int) {
        val width = View.MeasureSpec.getSize(w)
        super.onMeasure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((width * ratio).toInt(), View.MeasureSpec.EXACTLY)
        )
    }
}

fun Context.coverView(store: Store, w: Work, radiusDp: Int = 12): CoverFrame {
    val f = CoverFrame(this)
    val r = dp(radiusDp).toFloat()
    f.clipToOutline = true
    f.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, r)
        }
    }
    val bmp = if (w.cover.isNotEmpty()) Covers.get(store, w.cover) else null
    val animFile: File? = if (bmp != null && w.coverAnim.isNotEmpty() && (this as? MainActivity)?.prefs?.animCovers == true)
        File(store.coversDir, w.coverAnim).takeIf { it.exists() } else null
    if (bmp != null && animFile != null) {
        f.addView(
            AnimFrame(this, bmp, animFile, false, true, true, 420),
            FrameLayout.LayoutParams(MATCH, MATCH)
        )
    } else if (bmp != null) {
        val iv = ImageView(this)
        iv.scaleType = ImageView.ScaleType.CENTER_CROP
        iv.setImageBitmap(bmp)
        f.addView(iv, MATCH, MATCH)
    } else {
        val pal = coverPalette(w.title)
        f.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(pal.first, pal.second))
        val letter = w.title.trim().take(1).uppercase().ifEmpty { "?" }
        val t = tv(letter, 34f, Color.WHITE, true)
        t.gravity = Gravity.CENTER
        f.addView(t, FrameLayout.LayoutParams(MATCH, MATCH))
    }
    // brilho no topo e sombra suave embaixo (dá profundidade)
    val gloss = View(this)
    gloss.background = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(0x2EFFFFFF, 0x00000000, 0x00000000, 0x55000000)
    )
    f.addView(gloss, FrameLayout.LayoutParams(MATCH, MATCH))
    return f
}

class FlowLayout(ctx: Context) : ViewGroup(ctx) {
    var hGap = 0
    var vGap = 0

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val maxW = View.MeasureSpec.getSize(wSpec) - paddingLeft - paddingRight
        var x = 0
        var y = 0
        var rowH = 0
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == View.GONE) continue
            c.measure(
                View.MeasureSpec.makeMeasureSpec(maxW, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val cw = c.measuredWidth
            val ch = c.measuredHeight
            if (x > 0 && x + cw > maxW) {
                x = 0
                y += rowH + vGap
                rowH = 0
            }
            x += cw + hGap
            rowH = Math.max(rowH, ch)
        }
        setMeasuredDimension(
            resolveSize(View.MeasureSpec.getSize(wSpec), wSpec),
            resolveSize(y + rowH + paddingTop + paddingBottom, hSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val maxW = r - l - paddingLeft - paddingRight
        var x = 0
        var y = 0
        var rowH = 0
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == View.GONE) continue
            val cw = c.measuredWidth
            val ch = c.measuredHeight
            if (x > 0 && x + cw > maxW) {
                x = 0
                y += rowH + vGap
                rowH = 0
            }
            c.layout(paddingLeft + x, paddingTop + y, paddingLeft + x + cw, paddingTop + y + ch)
            x += cw + hGap
            rowH = Math.max(rowH, ch)
        }
    }
}

fun statusColor(s: String): Int = when (s) {
    STATUS_READING -> P.cReading
    STATUS_PLAN -> P.cPlan
    STATUS_DONE -> P.cDone
    STATUS_PAUSED -> P.cPaused
    else -> P.accent
}

fun statusIcon(s: String): Ic = when (s) {
    STATUS_READING -> Ic.BookOpen
    STATUS_PLAN -> Ic.Bookmark
    STATUS_DONE -> Ic.CheckCircle
    STATUS_PAUSED -> Ic.Pause
    else -> Ic.BookOpen
}

/** Capa grande no topo da obra: alinhada ao topo e dissolvendo suavemente para o fundo embaixo. */
class FadeCover(
    ctx: Context, private val bmp: Bitmap?, private val c1: Int, private val c2: Int, private val letter: String
) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val fade = Paint()
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        fade.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        txt.color = 0x33FFFFFF
        txt.textAlign = Paint.Align.CENTER
        txt.isFakeBoldText = true
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val layer = c.saveLayer(0f, 0f, w, h, null)
        if (bmp != null) {
            val s = Math.max(w / bmp.width, h / bmp.height)
            val dw = bmp.width * s
            val dh = bmp.height * s
            c.drawBitmap(bmp, null, RectF((w - dw) / 2f, 0f, (w - dw) / 2f + dw, dh), paint)
        } else {
            paint.shader = LinearGradient(0f, 0f, w, h, c1, c2, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
            txt.textSize = h * 0.5f
            c.drawText(letter, w / 2f, h * 0.55f, txt)
        }
        fade.shader = LinearGradient(0f, h * 0.45f, 0f, h, 0xFF000000.toInt(), 0x00000000, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, fade)
        c.restoreToCount(layer)
    }
}


/** Vídeo mudo em loop. Pausa quando a tela some e libera o player ao sair. */
class LoopVideoView(ctx: Context, private val path: String) : TextureView(ctx), TextureView.SurfaceTextureListener {
    private var mp: MediaPlayer? = null
    private var surf: Surface? = null
    private var ready = false

    init {
        surfaceTextureListener = this
        isOpaque = false
    }

    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
        try {
            val s = Surface(st)
            surf = s
            val m = MediaPlayer()
            m.setDataSource(path)
            m.setSurface(s)
            m.isLooping = true
            m.setVolume(0f, 0f)
            m.setOnPreparedListener { p ->
                ready = true
                if (windowVisibility == View.VISIBLE) p.start()
            }
            m.setOnErrorListener { _, _, _ -> true }
            m.prepareAsync()
            mp = m
        } catch (e: Exception) {
            mp = null
        }
    }

    fun stop() {
        try { mp?.release() } catch (e: Exception) { }
        try { surf?.release() } catch (e: Exception) { }
        mp = null
        surf = null
        ready = false
    }

    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}

    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
        try { mp?.release() } catch (e: Exception) { }
        try { surf?.release() } catch (e: Exception) { }
        mp = null
        surf = null
        ready = false
        return true
    }

    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        val m = mp ?: return
        if (!ready) return
        try {
            if (visibility == View.VISIBLE) m.start() else m.pause()
        } catch (e: Exception) { }
    }
}

/**
 * Controla quem está animando. Em listas há um limite (vídeos gastam decodificador do aparelho);
 * quem não conseguir vaga fica com o quadro parado. Com o navegador aberto tudo pausa.
 */
object AnimGate {
    var paused = false
    private val playing = ArrayList<AnimFrame>()
    private const val MAX_LIST = 8
    private const val MAX_LIST_VIDEOS = 3

    fun claim(f: AnimFrame): Boolean {
        if (paused) return false
        if (playing.contains(f)) return true
        if (f.counted) {
            if (playing.count { it.counted } >= MAX_LIST) return false
            if (f.isVideo && playing.count { it.counted && it.isVideo } >= MAX_LIST_VIDEOS) return false
        }
        playing.add(f)
        return true
    }

    fun release(f: AnimFrame) {
        playing.remove(f)
    }

    /** Navegador abriu: para todas as animações e libera a memória. */
    fun pauseAll() {
        paused = true
        for (f in ArrayList(playing)) f.stopMedia()
    }

    /** Navegador fechou: as telas são recriadas e as animações voltam. */
    fun resume() {
        paused = false
    }
}

/** Quadro da capa: mostra a imagem parada e, se possível, toca GIF/vídeo por cima. */
class AnimFrame(
    ctx: Context,
    private val poster: Bitmap,
    private val media: File,
    private val fadeBottom: Boolean,
    private val centerV: Boolean,
    val counted: Boolean,
    private val maxSide: Int
) : ViewGroup(ctx) {
    private val ar = poster.height.toFloat() / poster.width.toFloat()
    private val fade = Paint()
    private val posterView = ImageView(ctx)
    private var video: LoopVideoView? = null
    private var gif: ImageView? = null
    val isVideo: Boolean = media.extension.lowercase().let { it != "gif" && it != "webp" }

    init {
        fade.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        posterView.scaleType = ImageView.ScaleType.FIT_XY
        posterView.setImageBitmap(poster)
        addView(posterView)
    }

    private fun childW(w: Int, h: Int): Int = if (w * ar < h) (h / ar).toInt() else w
    private fun childH(w: Int, h: Int): Int = Math.max(h, (w * ar).toInt())

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = View.MeasureSpec.getSize(wSpec)
        val h = View.MeasureSpec.getSize(hSpec)
        setMeasuredDimension(w, h)
        val cw = childW(w, h)
        val ch = childH(w, h)
        for (i in 0 until childCount) {
            getChildAt(i).measure(
                View.MeasureSpec.makeMeasureSpec(cw, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(ch, View.MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val w = r - l
        val h = b - t
        val cw = childW(w, h)
        val ch = childH(w, h)
        val x = (w - cw) / 2
        val y = if (centerV) (h - ch) / 2 else 0
        for (i in 0 until childCount) getChildAt(i).layout(x, y, x + cw, y + ch)
    }

    override fun dispatchDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (!fadeBottom || w <= 0f || h <= 0f) {
            super.dispatchDraw(c)
            return
        }
        val layer = c.saveLayer(0f, 0f, w, h, null)
        super.dispatchDraw(c)
        fade.shader = LinearGradient(0f, h * 0.45f, 0f, h, 0xFF000000.toInt(), 0x00000000, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, fade)
        c.restoreToCount(layer)
    }

    fun startMedia() {
        if (!isAttachedToWindow || video != null || gif != null) return
        if (!AnimGate.claim(this)) return
        if (isVideo) {
            val v = LoopVideoView(context, media.path)
            video = v
            addView(v)
        } else {
            val d = Covers.animatedDrawable(media, false, maxSide)
            if (d == null) {
                AnimGate.release(this)
                return
            }
            val iv = ImageView(context)
            iv.scaleType = ImageView.ScaleType.FIT_XY
            iv.setImageDrawable(d)
            gif = iv
            addView(iv)
        }
    }

    fun stopMedia() {
        val v = video
        if (v != null) {
            v.stop()
            removeView(v)
            video = null
        }
        val g = gif
        if (g != null) {
            (g.drawable as? android.graphics.drawable.Animatable)?.stop()
            g.setImageDrawable(null)
            removeView(g)
            gif = null
        }
        AnimGate.release(this)
    }

    private val scrollListener = ViewTreeObserver.OnScrollChangedListener { check() }

    private fun visibleNow(): Boolean {
        if (!isShown || width <= 0 || height <= 0) return false
        val r = Rect()
        if (!getGlobalVisibleRect(r)) return false
        return r.width() > width / 3 && r.height() > height / 3
    }

    /** Só anima quem está aparecendo na tela; ao rolar, quem sai para e quem entra começa. */
    fun check() {
        val playing = video != null || gif != null
        val vis = visibleNow()
        if (vis && !playing) startMedia() else if (!vis && playing) stopMedia()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnScrollChangedListener(scrollListener)
        post { check() }
    }

    override fun onDetachedFromWindow() {
        if (viewTreeObserver.isAlive) viewTreeObserver.removeOnScrollChangedListener(scrollListener)
        stopMedia()
        super.onDetachedFromWindow()
    }
}

/** Capa animada (GIF/vídeo) do topo da obra: sem limite de quantidade, dissolve embaixo. */
fun Context.animHero(store: Store, w: Work, poster: Bitmap?, enabled: Boolean): View? {
    if (!enabled || w.coverAnim.isEmpty() || poster == null) return null
    val f = File(store.coversDir, w.coverAnim)
    if (!f.exists()) return null
    return AnimFrame(this, poster, f, true, false, false, 1000)
}
