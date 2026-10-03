package com.example.mangashelf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.InputType
import android.util.LruCache
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.io.FileOutputStream

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

object P {
    var dark = false
    val accent: Int = 0xFF7C5CFF.toInt()
    val accent2: Int = 0xFFFF6B9A.toInt()
    val bg: Int get() = if (dark) 0xFF0E0D14.toInt() else 0xFFF6F5FB.toInt()
    val card: Int get() = if (dark) 0xFF1A1824.toInt() else 0xFFFFFFFF.toInt()
    val text: Int get() = if (dark) 0xFFF2F0FA.toInt() else 0xFF15131F.toInt()
    val sub: Int get() = if (dark) 0xFF9A97B0.toInt() else 0xFF6B6880.toInt()
    val line: Int get() = if (dark) 0xFF2A2738.toInt() else 0xFFE4E1F0.toInt()
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun shape(color: Int, radius: Float, stroke: Int = 0, strokeW: Int = 0): GradientDrawable {
    val g = GradientDrawable()
    g.setColor(color)
    g.cornerRadius = radius
    if (strokeW > 0) g.setStroke(strokeW, stroke)
    return g
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

fun Context.lbl(t: String): TextView = tv(t, 12f, P.sub, true)

fun Context.pill(t: String, bg: Int = P.accent, fg: Int = Color.WHITE, size: Float = 14f, onClick: () -> Unit): TextView {
    val x = tv(t, size, fg, true)
    x.gravity = Gravity.CENTER
    x.setPadding(dp(16), dp(11), dp(16), dp(11))
    x.background = shape(bg, dp(26).toFloat())
    x.setOnClickListener { onClick() }
    return x
}

fun Context.outlinePill(t: String, size: Float = 13f, onClick: () -> Unit): TextView {
    val x = tv(t, size, P.text, true)
    x.gravity = Gravity.CENTER
    x.setPadding(dp(10), dp(11), dp(10), dp(11))
    x.background = shape(P.card, dp(26).toFloat(), P.line, dp(1))
    x.setOnClickListener { onClick() }
    return x
}

fun Context.chip(t: String, selected: Boolean, onClick: () -> Unit): TextView {
    val x = tv(t, 13f, if (selected) Color.WHITE else P.text, selected)
    x.setPadding(dp(12), dp(7), dp(12), dp(7))
    x.background = if (selected) shape(P.accent, dp(20).toFloat())
    else shape(P.card, dp(20).toFloat(), P.line, dp(1))
    x.setOnClickListener { onClick() }
    return x
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
    e.setPadding(dp(12), dp(10), dp(12), dp(10))
    e.background = shape(P.card, dp(12).toFloat(), P.line, dp(1))
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
    AlertDialog.Builder(this).setTitle(title).setMessage(msg)
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
    AlertDialog.Builder(this).setTitle(title).setView(box)
        .setPositiveButton("OK") { _, _ -> onOk(et.text.toString()) }
        .setNegativeButton("Cancelar", null).show()
}

fun Context.listDialog(title: String, items: List<String>, onPick: (Int) -> Unit) {
    AlertDialog.Builder(this).setTitle(title)
        .setItems(items.toTypedArray()) { _, i -> onPick(i) }.show()
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
    Pair(0xFF7C5CFF.toInt(), 0xFFFF6B9A.toInt()),
    Pair(0xFF3B82F6.toInt(), 0xFF22D3EE.toInt()),
    Pair(0xFFF97316.toInt(), 0xFFEF4444.toInt()),
    Pair(0xFF10B981.toInt(), 0xFF3B82F6.toInt()),
    Pair(0xFFEC4899.toInt(), 0xFF8B5CF6.toInt()),
    Pair(0xFFF59E0B.toInt(), 0xFFEC4899.toInt())
)

object Covers {
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
    if (bmp != null) {
        val iv = ImageView(this)
        iv.scaleType = ImageView.ScaleType.CENTER_CROP
        iv.setImageBitmap(bmp)
        f.addView(iv, MATCH, MATCH)
    } else {
        val pal = COVER_COLORS[(w.title.hashCode() and 0x7fffffff) % COVER_COLORS.size]
        f.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(pal.first, pal.second))
        val letter = w.title.trim().take(1).uppercase().ifEmpty { "?" }
        val t = tv(letter, 34f, Color.WHITE, true)
        t.gravity = Gravity.CENTER
        f.addView(t, FrameLayout.LayoutParams(MATCH, MATCH))
    }
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
