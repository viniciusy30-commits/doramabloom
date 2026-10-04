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
import kotlin.math.sin

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

object Palette {
    val bgTop = Color.parseColor("#FFF7FA")
    val bgBottom = Color.parseColor("#FFE1EC")
    val pink = Color.parseColor("#FF6B9D")
    val pinkSoft = Color.parseColor("#FFE4EE")
    val text = Color.parseColor("#6B2E48")
    val muted = Color.parseColor("#B98AA0")
    val line = Color.parseColor("#F8CFE0")
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

fun Context.pill(s: String, bg: Int, fg: Int, size: Float = 12f): TextView {
    val t = label(s, size, fg, true)
    t.setPadding(dp(12), dp(6), dp(12), dp(6))
    t.background = roundRect(bg, dp(20).toFloat())
    t.gravity = Gravity.CENTER
    return t
}

fun Context.card(pad: Int = 14, radius: Int = 22, bg: Int = Color.WHITE): LinearLayout {
    val c = LinearLayout(this)
    c.orientation = LinearLayout.VERTICAL
    c.setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
    c.background = roundRect(bg, dp(radius).toFloat())
    c.elevation = dp(3).toFloat()
    return c
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

fun hearts(r: Int): String {
    val n = r.coerceIn(0, 5)
    return "♥".repeat(n) + "♡".repeat(5 - n)
}

/** Faixa de chips com seleção única. options = (chave, texto, cor). */
fun Context.chipScroller(options: List<Triple<String, String, Int>>, initial: String, onSelect: (String) -> Unit): HorizontalScrollView {
    val sv = HorizontalScrollView(this)
    sv.isHorizontalScrollBarEnabled = false
    val row = LinearLayout(this)
    row.orientation = LinearLayout.HORIZONTAL
    row.setPadding(0, dp(2), 0, dp(2))
    sv.addView(row)
    val views = ArrayList<TextView>()
    var current = initial

    fun restyle() {
        for (i in options.indices) {
            val col = options[i].third
            val sel = options[i].first == current
            val tv = views[i]
            tv.background = roundRect(if (sel) col else Color.WHITE, dp(20).toFloat(), col, dp(1))
            tv.setTextColor(if (sel) Color.WHITE else col)
        }
    }

    for (i in options.indices) {
        val tv = pill(options[i].second, Color.WHITE, Palette.pink, 13f)
        tv.setOnClickListener {
            current = options[i].first
            restyle()
            onSelect(current)
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

/** Pétalas / emojis caindo suavemente. */
class PetalsView(ctx: Context, private var emojis: List<String>, private val count: Int = 14) : View(ctx) {
    private class P(
        var x: Float, var y: Float, var vy: Float, var size: Float,
        var phase: Float, var rot: Float, var vr: Float, var e: Int
    )

    private val ps = ArrayList<P>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rnd = java.util.Random()
    private var last = 0L

    init {
        paint.alpha = 130
    }

    private fun spawn(anywhere: Boolean): P {
        val size = (dp(13) + rnd.nextInt(dp(13))).toFloat()
        return P(
            rnd.nextFloat() * width,
            if (anywhere) rnd.nextFloat() * height else -size,
            dp(26) + rnd.nextFloat() * dp(36),
            size,
            rnd.nextFloat() * 6.28f,
            rnd.nextFloat() * 360f,
            (rnd.nextFloat() - 0.5f) * 70f,
            rnd.nextInt(emojis.size)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ps.clear()
        if (w > 0 && h > 0) {
            for (i in 0 until count) ps.add(spawn(true))
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        last = 0L
    }

    override fun onDraw(c: Canvas) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 0.016f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
        last = now
        for (i in ps.indices) {
            var p = ps[i]
            p.y += p.vy * dt
            p.phase += dt * 1.3f
            p.rot += p.vr * dt
            if (p.y > height + p.size) {
                p = spawn(false)
                ps[i] = p
            }
            val sx = p.x + (sin(p.phase.toDouble()) * dp(16)).toFloat()
            paint.textSize = p.size
            c.save()
            c.translate(sx, p.y)
            c.rotate(p.rot)
            c.drawText(emojis[p.e % emojis.size], 0f, 0f, paint)
            c.restore()
        }
        postInvalidateOnAnimation()
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

/** Capa arredondada: foto ou, se não houver, um fundo fofo com o emoji do gênero. */
class CoverView(ctx: Context, radiusDp: Int = 16) : FrameLayout(ctx) {
    private val img = ImageView(ctx)
    private val emoji = TextView(ctx)

    init {
        img.scaleType = ImageView.ScaleType.CENTER_CROP
        addView(img, FrameLayout.LayoutParams(MATCH, MATCH))
        emoji.gravity = Gravity.CENTER
        addView(emoji, FrameLayout.LayoutParams(MATCH, MATCH))
        val r = ctx.dp(radiusDp).toFloat()
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, r)
            }
        }
        clipToOutline = true
    }

    fun bind(path: String, genreKey: String, reqW: Int, emojiSize: Float) {
        val g = Genres.byKey(genreKey)
        val bmp = if (path.isNotEmpty()) Covers.load(path, reqW) else null
        if (bmp != null) {
            img.setImageBitmap(bmp)
            img.visibility = View.VISIBLE
            emoji.visibility = View.GONE
            setBackgroundColor(g.soft)
        } else {
            img.visibility = View.GONE
            emoji.visibility = View.VISIBLE
            emoji.text = g.emoji
            emoji.textSize = emojiSize
            background = gradient(g.soft, g.primary, 0f, GradientDrawable.Orientation.TL_BR)
        }
    }

    fun bind(d: Drama, reqW: Int, emojiSize: Float) {
        bind(d.cover, d.genre, reqW, emojiSize)
    }
}
