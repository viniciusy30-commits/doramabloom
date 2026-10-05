package com.doramabloom.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.LruCache
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

object Palette {
    val bgTop = Color.parseColor("#FFF8FB")
    val bgBottom = Color.parseColor("#FFE8F1")
    val pink = Color.parseColor("#FF6B9D")
    val pinkDark = Color.parseColor("#E0487F")
    val pinkSoft = Color.parseColor("#FFE4EE")
    val text = Color.parseColor("#6B2E48")
    val muted = Color.parseColor("#B98AA0")
    val line = Color.parseColor("#F8D9E6")
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
fun View.dp(v: Int): Int = context.dp(v)

fun roundRect(color: Int, radius: Float, stroke: Int = Color.TRANSPARENT, strokeW: Int = 0): GradientDrawable {
    val d = GradientDrawable()
    d.shape = GradientDrawable.RECTANGLE
    d.cornerRadius = radius
    d.setColor(color)
    if (strokeW > 0) d.setStroke(strokeW, stroke)
    return d
}

fun gradient(
    top: Int,
    bottom: Int,
    radius: Float = 0f,
    orientation: GradientDrawable.Orientation = GradientDrawable.Orientation.TOP_BOTTOM
): GradientDrawable {
    val d = GradientDrawable(orientation, intArrayOf(top, bottom))
    d.cornerRadius = radius
    return d
}

fun ovalGradient(c1: Int, c2: Int): GradientDrawable {
    val d = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c1, c2))
    d.shape = GradientDrawable.OVAL
    return d
}

/** LayoutParams de LinearLayout; tamanhos em px, margens em dp. */
fun Context.lin(w: Int, h: Int, weight: Float = 0f, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0): LinearLayout.LayoutParams {
    val p = LinearLayout.LayoutParams(w, h, weight)
    p.setMargins(dp(l), dp(t), dp(r), dp(b))
    return p
}

fun Context.label(s: String, size: Float = 14f, color: Int = Palette.text, bold: Boolean = false, cute: Boolean = false): TextView {
    val t = TextView(this)
    t.text = s
    t.textSize = size
    t.setTextColor(color)
    if (cute) {
        t.typeface = Typeface.create("casual", if (bold) Typeface.BOLD else Typeface.NORMAL)
    } else if (bold) {
        t.typeface = Typeface.DEFAULT_BOLD
    }
    return t
}

/** Pílula com texto e (opcionalmente) um ícone próprio à esquerda. */
fun Context.pill(s: String, bg: Int, fg: Int, size: Float = 12f, icon: String? = null): TextView {
    val t = label(s, size, fg, true)
    t.setPadding(dp(12), dp(6), dp(12), dp(6))
    t.background = roundRect(bg, dp(20).toFloat())
    t.gravity = Gravity.CENTER
    if (icon != null) {
        val px = dp(size.toInt() + 4)
        t.setCompoundDrawables(iconDrawable(icon, fg, px), null, null, null)
        t.compoundDrawablePadding = dp(6)
    }
    return t
}

fun Context.card(pad: Int = 14, radius: Int = 22, bg: Int = Color.WHITE): LinearLayout {
    val c = LinearLayout(this)
    c.orientation = LinearLayout.VERTICAL
    c.setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
    c.background = roundRect(bg, dp(radius).toFloat(), Palette.line, dp(1))
    c.elevation = dp(3).toFloat()
    return c
}

/** Título de seção: ícone + texto fofo. */
fun Context.sectionTitle(text: String, icon: String, color: Int = Palette.pink): LinearLayout {
    val r = LinearLayout(this)
    r.orientation = LinearLayout.HORIZONTAL
    r.gravity = Gravity.CENTER_VERTICAL
    r.addView(IconView(this, icon, color, 18))
    r.addView(label(text, 17f, Palette.text, true, true), lin(WRAP, WRAP, l = 8))
    return r
}

fun Context.input(hint: String, text: String = "", type: Int = InputType.TYPE_CLASS_TEXT, multi: Boolean = false): EditText {
    val e = EditText(this)
    e.hint = hint
    e.setText(text)
    e.textSize = 15f
    e.setTextColor(Palette.text)
    e.setHintTextColor(Palette.muted)
    e.background = roundRect(Color.WHITE, dp(18).toFloat(), Palette.line, dp(1))
    e.setPadding(dp(16), dp(12), dp(16), dp(12))
    if (multi) {
        e.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        e.minLines = 3
        e.gravity = Gravity.TOP or Gravity.START
    } else {
        e.setSingleLine(true)
        e.inputType = type
    }
    return e
}

/** Bolinha com a nota (0 a 10). */
fun styleBadge(t: TextView, score: Int, color: Int) {
    t.text = if (score <= 0) "-" else score.toString()
    val d = GradientDrawable()
    d.shape = GradientDrawable.OVAL
    d.setColor(if (score <= 0) Color.parseColor("#D9C4CE") else color)
    t.background = d
}

fun Context.scoreBadge(sizeDp: Int, textSp: Float): TextView {
    val t = label("", textSp, Color.WHITE, true, true)
    t.gravity = Gravity.CENTER
    t.minWidth = dp(sizeDp)
    t.minHeight = dp(sizeDp)
    t.elevation = dp(2).toFloat()
    return t
}

class Opt(val key: String, val label: String, val color: Int, val icon: String? = null)

private fun restyleChip(tv: TextView, col: Int, sel: Boolean, dpPx: Int) {
    tv.background = roundRect(if (sel) col else Color.WHITE, dpPx * 20f, col, dpPx)
    val fg = if (sel) Color.WHITE else col
    tv.setTextColor(fg)
    val dr = tv.compoundDrawables[0]
    if (dr is IconDrawable) dr.color = fg
}

/** Faixa de chips com seleção única. */
fun Context.chipScroller(options: List<Opt>, initial: String, onSelect: (String) -> Unit): HorizontalScrollView {
    val sv = HorizontalScrollView(this)
    sv.isHorizontalScrollBarEnabled = false
    val row = LinearLayout(this)
    row.orientation = LinearLayout.HORIZONTAL
    row.setPadding(0, dp(2), 0, dp(2))
    sv.addView(row)
    val views = ArrayList<TextView>()
    var current = initial
    val one = dp(1)

    fun restyle() {
        for (i in options.indices) {
            restyleChip(views[i], options[i].color, options[i].key == current, one)
        }
    }

    for (i in options.indices) {
        val o = options[i]
        val tv = pill(o.label, Color.WHITE, o.color, 13f, o.icon)
        tv.setOnClickListener {
            current = o.key
            restyle()
            onSelect(current)
        }
        views.add(tv)
        row.addView(tv, lin(WRAP, WRAP, r = 8))
    }
    restyle()
    return sv
}

/** Faixa de chips com seleção múltipla. */
fun Context.multiChips(options: List<Opt>, initial: Set<String>, onChange: (Set<String>) -> Unit): HorizontalScrollView {
    val sv = HorizontalScrollView(this)
    sv.isHorizontalScrollBarEnabled = false
    val row = LinearLayout(this)
    row.orientation = LinearLayout.HORIZONTAL
    row.setPadding(0, dp(2), 0, dp(2))
    sv.addView(row)
    val views = ArrayList<TextView>()
    val chosen = HashSet<String>(initial)
    val one = dp(1)

    fun restyle() {
        for (i in options.indices) {
            restyleChip(views[i], options[i].color, chosen.contains(options[i].key), one)
        }
    }

    for (i in options.indices) {
        val o = options[i]
        val tv = pill(o.label, Color.WHITE, o.color, 13f, o.icon)
        tv.setOnClickListener {
            if (chosen.contains(o.key)) chosen.remove(o.key) else chosen.add(o.key)
            restyle()
            onChange(HashSet<String>(chosen))
        }
        views.add(tv)
        row.addView(tv, lin(WRAP, WRAP, r = 8))
    }
    restyle()
    return sv
}

/** Barrinha de progresso arredondada. */
class SoftBar(ctx: Context) : View(ctx) {
    var progress: Float = 0f
        set(v) {
            field = v.coerceIn(0f, 1f)
            invalidate()
        }
    var barColor: Int = Palette.pink
        set(v) {
            field = v
            invalidate()
        }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    override fun onDraw(c: Canvas) {
        val h = height.toFloat()
        val r = h / 2f
        p.color = Color.parseColor("#F6DCE6")
        rect.set(0f, 0f, width.toFloat(), h)
        c.drawRoundRect(rect, r, r, p)
        if (progress > 0f) {
            p.color = barColor
            rect.set(0f, 0f, maxOf(h, width * progress), h)
            c.drawRoundRect(rect, r, r, p)
        }
    }
}

/** Cache simples de capas. */
object Covers {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun load(path: String, reqW: Int): Bitmap? {
        val key = "$path@$reqW"
        val hit = cache.get(key)
        if (hit != null) return hit
        return try {
            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, o)
            var s = 1
            while (o.outWidth / (s * 2) >= reqW) s *= 2
            val o2 = BitmapFactory.Options()
            o2.inSampleSize = s
            val b = BitmapFactory.decodeFile(path, o2)
            if (b != null) cache.put(key, b)
            b
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        cache.evictAll()
    }
}

/** Capa arredondada: foto ou, se não houver, um fundo fofo com o símbolo do gênero. */
class CoverView(ctx: Context, radiusDp: Int = 16) : FrameLayout(ctx) {
    private class Placeholder(ctx: Context) : View(ctx) {
        val d = IconDrawable("heart", Color.parseColor("#E6FFFFFF"))
        val blossom = IconDrawable("blossom", Color.parseColor("#55FFFFFF"))

        override fun onDraw(c: Canvas) {
            val s = (minOf(width, height) * 0.42f).toInt()
            val l = (width - s) / 2
            val t = (height - s) / 2
            d.setBounds(l, t, l + s, t + s)
            d.draw(c)
            val bs = (minOf(width, height) * 0.30f).toInt()
            blossom.setBounds(width - bs - bs / 4, bs / 5, width - bs / 4, bs / 5 + bs)
            blossom.draw(c)
        }
    }

    private val img = ImageView(ctx)
    private val ph = Placeholder(ctx)

    init {
        img.scaleType = ImageView.ScaleType.CENTER_CROP
        addView(img, FrameLayout.LayoutParams(MATCH, MATCH))
        addView(ph, FrameLayout.LayoutParams(MATCH, MATCH))
        val r = ctx.dp(radiusDp).toFloat()
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, r)
            }
        }
        clipToOutline = true
    }

    fun bind(path: String, genreKey: String, reqW: Int) {
        val g = Genres.byKey(genreKey)
        val bmp = if (path.isNotEmpty()) Covers.load(path, reqW) else null
        if (bmp != null) {
            img.setImageBitmap(bmp)
            img.visibility = View.VISIBLE
            ph.visibility = View.GONE
            setBackgroundColor(g.soft)
        } else {
            img.visibility = View.GONE
            ph.visibility = View.VISIBLE
            ph.d.name = g.icon
            ph.invalidate()
            background = gradient(g.soft, g.primary, 0f, GradientDrawable.Orientation.TL_BR)
        }
    }

    fun bind(d: Drama, reqW: Int) {
        bind(d.cover, d.genre, reqW)
    }
}

/** Botão redondo com ícone (usado em passos de contagem, + e -). */
fun Context.roundBtn(icon: String, color: Int, filled: Boolean, sizeDp: Int = 16, onClick: () -> Unit): FrameLayout {
    val f = FrameLayout(this)
    val d = GradientDrawable()
    d.shape = GradientDrawable.OVAL
    d.setColor(if (filled) color else Color.WHITE)
    d.setStroke(dp(2), color)
    f.background = d
    f.addView(
        IconView(this, icon, if (filled) Color.WHITE else color, sizeDp),
        FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER)
    )
    f.setOnClickListener { onClick() }
    return f
}
