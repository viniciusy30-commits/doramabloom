package com.doramabloom.app

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
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
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.AccelerateInterpolator
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
    t.pressable()
    return t
}

fun Context.card(pad: Int = 14, radius: Int = 22, bg: Int = Color.WHITE): LinearLayout {
    val c = LinearLayout(this)
    c.orientation = LinearLayout.VERTICAL
    c.setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
    c.background = roundRect(bg, dp(radius).toFloat(), Palette.line, dp(1))
    c.elevation = 0f
    return c
}

/** Título de seção: ícone + texto fofo. */
fun Context.sectionTitle(text: String, icon: String, color: Int = Palette.pink): LinearLayout {
    val r = LinearLayout(this)
    r.orientation = LinearLayout.HORIZONTAL
    r.gravity = Gravity.CENTER_VERTICAL
    val bubble = FrameLayout(this)
    bubble.background = ovalGradient((color and 0x00FFFFFF) or 0x33000000, (color and 0x00FFFFFF) or 0x14000000)
    bubble.addView(IconView(this, icon, color, 16), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
    r.addView(bubble, lin(dp(32), dp(32)))
    r.addView(label(text, 17f, Palette.text, true, true), lin(WRAP, WRAP, l = 10))
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
    t.elevation = 0f
    return t
}

class Opt(val key: String, val label: String, val color: Int, val icon: String? = null)

private fun restyleChip(tv: TextView, col: Int, sel: Boolean, dpPx: Int) {
    tv.background = roundRect(if (sel) col else Color.WHITE, dpPx * 20f, col, dpPx)
    val fg = if (sel) Color.WHITE else col
    tv.setTextColor(fg)
    val dr = tv.compoundDrawables[0]
    if (dr is IconDrawable) dr.color = fg
    if (sel && tv.getTag(TAG_SEL) != true) tv.pop()
    tv.setTag(TAG_SEL, sel)
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

    val setter: (String) -> Unit = { k ->
        current = k
        restyle()
    }
    sv.setTag(TAG_SELECT, setter)

    for (i in options.indices) {
        val o = options[i]
        val tv = pill(o.label, Color.WHITE, o.color, 13f, o.icon)
        tv.setOnClickListener {
            if (current != o.key) {
                current = o.key
                restyle()
                onSelect(current)
            } else {
                tv.pop(1.2f)
            }
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

/** Barrinha de progresso arredondada, com animação suave. */
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
    private var anim: ValueAnimator? = null

    /** Vai até o valor com uma animação; pode começar de outro ponto. */
    fun animateTo(target: Float, from: Float = progress, delay: Long = 0L, dur: Long = 600L) {
        anim?.cancel()
        val t = target.coerceIn(0f, 1f)
        progress = from
        val a = ValueAnimator.ofFloat(from.coerceIn(0f, 1f), t)
        a.duration = dur
        a.startDelay = delay
        a.interpolator = DecelerateInterpolator(1.6f)
        a.addUpdateListener { progress = it.animatedValue as Float }
        anim = a
        a.start()
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        super.onDetachedFromWindow()
    }

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
    f.pressable(0.86f)
    return f
}


// ================================================================ ANIMAÇÕES

const val TAG_SEL = 0x7f0a0001
const val TAG_SELECT = 0x7f0a0002

/** Seleciona um chip de fora (usado quando o status muda sozinho). */
@Suppress("UNCHECKED_CAST")
fun View.selectChip(key: String) {
    (getTag(TAG_SELECT) as? ((String) -> Unit))?.invoke(key)
}

/** Encolhe um pouquinho ao tocar e volta com mola ao soltar. Não atrapalha o clique. */
fun View.pressable(scale: Float = 0.95f) {
    setOnTouchListener { v, e ->
        if (!v.isClickable) return@setOnTouchListener false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN ->
                v.animate().scaleX(scale).scaleY(scale).setDuration(90).setInterpolator(DecelerateInterpolator()).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(OvershootInterpolator(3f)).start()
        }
        false
    }
}

/** Pulinho de alegria (usado ao selecionar algo). */
fun View.pop(peak: Float = 1.14f) {
    animate().cancel()
    scaleX = 0.9f
    scaleY = 0.9f
    animate().scaleX(1f).scaleY(1f).setDuration(320).setInterpolator(OvershootInterpolator(peak * 3f)).start()
}

/** Entra deslizando de baixo com fade. */
fun View.riseIn(delay: Long = 0L, distDp: Int = 22, dur: Long = 420L) {
    animate().cancel()
    alpha = 0f
    translationY = dp(distDp).toFloat()
    animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(dur)
        .setInterpolator(DecelerateInterpolator(1.8f)).start()
}

/** Aparece com fade e leve escala. */
fun View.fadeScaleIn(delay: Long = 0L, dur: Long = 380L) {
    animate().cancel()
    alpha = 0f
    scaleX = 0.92f
    scaleY = 0.92f
    animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(delay).setDuration(dur)
        .setInterpolator(DecelerateInterpolator(1.6f)).start()
}

/** Entrada em cascata dos filhos de um LinearLayout. */
fun LinearLayout.staggerIn(step: Long = 55L, maxItems: Int = 10) {
    for (i in 0 until childCount) {
        getChildAt(i).riseIn(minOf(i, maxItems) * step)
    }
}

/** Troca de tela: entra deslizando de lado. dir = 1 (veio da direita) ou -1. */
fun View.slideIn(dir: Int) {
    animate().cancel()
    alpha = 0f
    translationX = dir * dp(36).toFloat()
    animate().alpha(1f).translationX(0f).setDuration(320).setInterpolator(DecelerateInterpolator(1.8f)).start()
}

/** Conta de um número até outro. */
fun TextView.countTo(target: Int, from: Int = 0, delay: Long = 0L, dur: Long = 700L, fmt: (Int) -> String = { it.toString() }) {
    val a = ValueAnimator.ofInt(from, target)
    a.duration = dur
    a.startDelay = delay
    a.interpolator = DecelerateInterpolator(1.5f)
    text = fmt(from)
    a.addUpdateListener { text = fmt(it.animatedValue as Int) }
    a.start()
}

/** Muda a cor de um fundo/ícone com transição suave. */
fun animateColor(from: Int, to: Int, dur: Long = 260L, onUpdate: (Int) -> Unit) {
    val a = ValueAnimator.ofObject(ArgbEvaluator(), from, to)
    a.duration = dur
    a.addUpdateListener { onUpdate(it.animatedValue as Int) }
    a.start()
}

/** Abre uma área com altura animada. */
fun View.expand(dur: Long = 320L) {
    if (visibility == View.VISIBLE && layoutParams.height == WRAP && alpha == 1f) return
    animate().cancel()
    measure(
        View.MeasureSpec.makeMeasureSpec((parent as View).width.coerceAtLeast(1), View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
    val target = measuredHeight
    layoutParams.height = 0
    alpha = 0f
    visibility = View.VISIBLE
    val a = ValueAnimator.ofInt(0, target)
    a.duration = dur
    a.interpolator = DecelerateInterpolator(1.6f)
    a.addUpdateListener {
        layoutParams.height = it.animatedValue as Int
        alpha = it.animatedFraction
        requestLayout()
    }
    a.addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) {
            layoutParams.height = WRAP
            alpha = 1f
            requestLayout()
        }
    })
    a.start()
}

/** Fecha uma área com altura animada. */
fun View.collapse(dur: Long = 260L) {
    if (visibility == View.GONE) return
    val start = height
    val a = ValueAnimator.ofInt(start, 0)
    a.duration = dur
    a.interpolator = AccelerateInterpolator(1.2f)
    a.addUpdateListener {
        layoutParams.height = it.animatedValue as Int
        alpha = 1f - it.animatedFraction
        requestLayout()
    }
    a.addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) {
            visibility = View.GONE
            layoutParams.height = WRAP
            alpha = 1f
        }
    })
    a.start()
}

/** Aviso fofo que desce do topo e some sozinho. */
fun Activity.softToast(msg: String, color: Int = Palette.pink, icon: String = "sparkle") {
    val host = findViewById<ViewGroup>(android.R.id.content) ?: return
    val t = pill(msg, color, Color.WHITE, 14f, icon)
    t.setPadding(dp(18), dp(12), dp(20), dp(12))
    t.elevation = dp(12).toFloat()
    val lp = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
    lp.topMargin = dp(36)
    host.addView(t, lp)
    t.alpha = 0f
    t.translationY = -dp(60).toFloat()
    t.scaleX = 0.8f
    t.scaleY = 0.8f
    t.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(420)
        .setInterpolator(OvershootInterpolator(1.6f)).start()
    t.postDelayed({
        t.animate().alpha(0f).translationY(-dp(40).toFloat()).setDuration(300).withEndAction { host.removeView(t) }.start()
    }, 2600)
}

/** Cor de status/gênero/país para os "efeitos" dos filtros. */
class Atmos(val icons: List<String>, val tints: List<Int>)

object Atmosphere {
    private fun c(s: String) = Color.parseColor(s)

    fun country(name: String): Pair<String, Int> = when (name) {
        "Coreia do Sul" -> Pair("blossom", c("#FF8FB7"))
        "Japão" -> Pair("blossom", c("#E8505B"))
        "China" -> Pair("star", c("#E5A93B"))
        "Tailândia" -> Pair("leaf", c("#4FA8C7"))
        "Taiwan" -> Pair("sparkle", c("#5B7FD9"))
        else -> Pair("sparkle", c("#B98AA0"))
    }

    fun status(key: String): Atmos = when (key) {
        "fav" -> Atmos(listOf("heart", "star"), listOf(Palette.pink, c("#FFB84D")))
        else -> {
            val s = Statuses.byKey(key)
            Atmos(listOf(s.icon, "sparkle"), listOf(s.color, s.color))
        }
    }

    /** Mistura o que está ativo: gênero manda; status e país entram como acento. */
    fun of(genre: String, status: String, country: String): Atmos {
        val icons = ArrayList<String>()
        val tints = ArrayList<Int>()
        if (genre != "all") {
            val g = Genres.byKey(genre)
            for (ic in g.petals) {
                icons.add(ic)
                tints.add(g.primary)
            }
        }
        if (status != "all") {
            val a = status(status)
            for (i in a.icons.indices) {
                icons.add(a.icons[i])
                tints.add(a.tints[i])
            }
        }
        if (country != "all") {
            val (ic, col) = country(country)
            icons.add(ic)
            tints.add(col)
            icons.add("petal")
            tints.add(col)
        }
        if (icons.isEmpty()) {
            icons.addAll(listOf("petal", "petal", "blossom"))
            tints.addAll(listOf(Palette.pink, Palette.pink, Palette.pink))
        }
        return Atmos(icons, tints)
    }

    /** Tela de um dorama: pétalas do gênero + símbolo do status. */
    fun ofDrama(d: Drama): Atmos {
        val g = Genres.byKey(d.genre)
        val s = Statuses.byKey(d.status)
        val icons = ArrayList<String>(g.petals)
        val tints = ArrayList<Int>()
        for (i in g.petals.indices) tints.add(g.primary)
        icons.add(s.icon)
        tints.add(s.color)
        if (d.favorite) {
            icons.add("heart")
            tints.add(Palette.pink)
        }
        return Atmos(icons, tints)
    }
}
