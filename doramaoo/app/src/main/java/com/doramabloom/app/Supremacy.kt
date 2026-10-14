package com.doramabloom.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val SUP_HI = Color.parseColor("#FFEDB3")
private val SUP_GOLD = Color.parseColor("#F5B83D")
private val SUP_DEEP = Color.parseColor("#B9791A")

private fun roman(n: Int): String {
    val v = intArrayOf(10, 9, 5, 4, 1)
    val s = arrayOf("X", "IX", "V", "IV", "I")
    var x = n
    val sb = StringBuilder()
    for (i in v.indices) while (x >= v[i]) {
        sb.append(s[i])
        x -= v[i]
    }
    return if (n > 10) n.toString() else sb.toString()
}

/** Fundo animado: raios de luz saindo do topo, brilho dourado e estrelinhas flutuando. */
private class GoldBackdrop(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val star = starPath()
    private val seeds = FloatArray(48) { ((it * 7919) % 1000) / 1000f }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f) return
        val t = clockSec()
        p.shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(Color.parseColor("#2A1A06"), Color.parseColor("#140C03"), Color.parseColor("#0B0702")), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        // halo no topo
        p.shader = RadialGradient(w / 2f, 0f, w * 0.95f, Color.argb(120, 245, 184, 61), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        // raios girando devagar
        c.save()
        c.translate(w / 2f, -h * 0.05f)
        c.rotate(t * 3f)
        p.style = Paint.Style.FILL
        for (i in 0 until 12) {
            p.color = Color.argb(if (i % 2 == 0) 34 else 16, 255, 214, 120)
            val a = Math.toRadians((i * 30.0))
            val a2 = Math.toRadians(i * 30.0 + 12.0)
            val r = h * 1.2f
            val path = android.graphics.Path()
            path.moveTo(0f, 0f)
            path.lineTo((cos(a) * r).toFloat(), (sin(a) * r).toFloat())
            path.lineTo((cos(a2) * r).toFloat(), (sin(a2) * r).toFloat())
            path.close()
            c.drawPath(path, p)
        }
        c.restore()
        // estrelinhas
        for (i in 0 until 16) {
            val sx = seeds[i * 3]
            val sp = 0.02f + seeds[i * 3 + 1] * 0.04f
            val fy = 1f - ((seeds[i * 3 + 2] + t * sp) % 1f)
            val tw = 0.5f + 0.5f * sin(t * (1.5f + seeds[i * 3] * 2f) + i)
            val sz = dp(4) + dp(9) * seeds[i * 3 + 1]
            p.color = Color.argb((40 + 150 * tw).toInt(), 255, 226, 150)
            c.save()
            c.translate(w * sx, h * fy)
            c.scale(sz, sz)
            c.drawPath(star, p)
            c.restore()
        }
        if (isAttachedToWindow) postInvalidateDelayed(40)
    }
}

/** Brilho diagonal que passa de vez em quando por cima do pôster. */
private class ShineOverlay(ctx: Context, private val phase: Float) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f) return
        val cyc = ((clockSec() * 0.28f + phase) % 1f)
        if (cyc > 0.45f) {
            if (isAttachedToWindow) postInvalidateDelayed(80)
            return
        }
        val x = -w * 0.4f + (w * 1.8f) * (cyc / 0.45f)
        p.shader = LinearGradient(x, 0f, x + w * 0.35f, h * 0.4f,
            intArrayOf(Color.TRANSPARENT, Color.argb(110, 255, 255, 255), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        if (isAttachedToWindow) postInvalidateDelayed(33)
    }
}

private fun Context.goldText(s: String, size: Float, widthPx: Int): TextView {
    val t = label(s, size, SUP_HI, true, true)
    t.letterSpacing = 0.12f
    t.gravity = Gravity.CENTER
    t.setShadowLayer(dp(10).toFloat(), 0f, 0f, Color.argb(160, 245, 184, 61))
    t.post {
        val w = maxOf(1, t.width)
        t.paint.shader = LinearGradient(0f, 0f, w.toFloat(), 0f, intArrayOf(SUP_HI, SUP_GOLD, Color.WHITE, SUP_GOLD, SUP_DEEP), null, Shader.TileMode.CLAMP)
        t.invalidate()
    }
    return t
}

private fun Context.glassCard(): LinearLayout {
    val c = LinearLayout(this)
    c.orientation = LinearLayout.VERTICAL
    val bg = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.argb(46, 255, 214, 120), Color.argb(14, 255, 214, 120)))
    bg.cornerRadius = dp(20).toFloat()
    bg.setStroke(dp(1), Color.argb(110, 245, 184, 61))
    c.background = bg
    return c
}

/** Um pôster do carrossel: moldura de ouro, brilho, número romano e sombra dourada. */
private fun Context.posterCard(d: Drama, rank: Int, w: Int, h: Int): View {
    val frame = FrameLayout(this)
    val gold = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(SUP_HI, SUP_GOLD, SUP_DEEP, SUP_GOLD))
    gold.cornerRadius = dp(26).toFloat()
    frame.background = gold
    frame.setPadding(dp(4), dp(4), dp(4), dp(4))
    frame.elevation = dp(14).toFloat()
    if (android.os.Build.VERSION.SDK_INT >= 28) {
        frame.outlineSpotShadowColor = SUP_GOLD
        frame.outlineAmbientShadowColor = SUP_GOLD
    }
    val inner = FrameLayout(this)
    inner.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(v: View, o: Outline) {
            o.setRoundRect(0, 0, v.width, v.height, dp(22).toFloat())
        }
    }
    inner.clipToOutline = true
    val cv = CoverView(this, 22)
    cv.bind(d, w)
    inner.addView(cv, FrameLayout.LayoutParams(MATCH, MATCH))
    inner.addView(ShineOverlay(this, rank * 0.17f), FrameLayout.LayoutParams(MATCH, MATCH))
    frame.addView(inner, FrameLayout.LayoutParams(MATCH, MATCH))

    val badge = FrameLayout(this)
    badge.background = ovalGradient(SUP_HI, SUP_DEEP)
    badge.elevation = dp(6).toFloat()
    val rn = label(roman(rank), if (rank > 3) 13f else 15f, Color.parseColor("#4A2C05"), true, true)
    rn.gravity = Gravity.CENTER
    badge.addView(rn, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
    val blp = FrameLayout.LayoutParams(dp(40), dp(40), Gravity.TOP or Gravity.START)
    blp.setMargins(dp(10), dp(10), 0, 0)
    frame.addView(badge, blp)
    if (rank == 1) {
        val crown = CrownView(this, 1, SUP_GOLD)
        frame.addView(crown, FrameLayout.LayoutParams(dp(56), dp(44), Gravity.TOP or Gravity.CENTER_HORIZONTAL).also { it.topMargin = dp(-30) })
        frame.clipChildren = false
    }
    return frame
}

/**
 * A aba Supremacy: o salão dourado dos poucos doramas acima de todos os favoritos.
 * Carrossel de pôsteres com zoom no que está no centro, painel de detalhes e ranking.
 */
fun Context.buildSupremacyTab(list: List<Drama>, onOpen: (Drama, List<Drama>) -> Unit): View {
    val root = FrameLayout(this)
    root.addView(GoldBackdrop(this), FrameLayout.LayoutParams(MATCH, MATCH))

    val sv = ScrollView(this)
    sv.isVerticalScrollBarEnabled = false
    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    col.gravity = Gravity.CENTER_HORIZONTAL
    col.setPadding(0, dp(16), 0, dp(26))
    sv.addView(col)
    root.addView(sv, FrameLayout.LayoutParams(MATCH, MATCH))

    val screenW = resources.displayMetrics.widthPixels

    // cabeçalho: coroa grande + título dourado
    val crown = CrownView(this, 1, SUP_GOLD)
    col.addView(crown, lin(dp(110), dp(86)))
    crown.fadeScaleIn(0L, 600L)
    val title = goldText("SUPREMACY", 30f, screenW)
    col.addView(title, lin(WRAP, WRAP, t = 2))
    title.riseIn(120L)
    val sub = label("Acima de todos os favoritos", 12.5f, Color.parseColor("#E6FFE9B0"), true)
    sub.letterSpacing = 0.06f
    col.addView(sub, lin(WRAP, WRAP, t = 2))
    sub.riseIn(200L)

    if (list.isEmpty()) {
        val box = glassCard()
        box.gravity = Gravity.CENTER_HORIZONTAL
        box.setPadding(dp(22), dp(28), dp(22), dp(26))
        box.addView(IconView(this, "crown", SUP_GOLD, 54))
        box.addView(label("O salão está vazio", 19f, SUP_HI, true, true), lin(WRAP, WRAP, t = 12))
        val m = label("Abra um dorama, toque em Editar e use “Marcar como Supremacy”. Só os pouquíssimos que ficam acima de todos os favoritos entram aqui.", 13f, Color.parseColor("#D9FFE9B0"))
        m.gravity = Gravity.CENTER
        box.addView(m, lin(MATCH, WRAP, t = 8))
        col.addView(box, lin(MATCH, WRAP, l = 22, r = 22, t = 26))
        box.riseIn(300L)
        return root
    }

    val sorted = list.sortedWith(compareByDescending<Drama> { it.score }.thenBy { it.title.lowercase() })

    // faixa de números do salão
    val rated = sorted.filter { it.score > 0 }
    val avg = if (rated.isEmpty()) "-" else "%.1f".format(rated.map { it.score }.sum().toDouble() / rated.size)
    val eps = sorted.map { watchedEps(it) }.sum()
    val hrs = sorted.map { hoursWatched(it) }.sum()
    val stats = LinearLayout(this)
    stats.orientation = LinearLayout.HORIZONTAL
    fun stat(v: String, l: String): View {
        val g = glassCard()
        g.gravity = Gravity.CENTER_HORIZONTAL
        g.setPadding(dp(6), dp(9), dp(6), dp(9))
        g.addView(label(v, 18f, SUP_HI, true, true))
        g.addView(label(l, 10.5f, Color.parseColor("#CCFFE9B0"), true))
        return g
    }
    stats.addView(stat(sorted.size.toString(), if (sorted.size == 1) "dorama" else "doramas"), lin(0, WRAP, 1f, r = 6))
    stats.addView(stat(avg, "nota média"), lin(0, WRAP, 1f, r = 6))
    stats.addView(stat(eps.toString(), "episódios"), lin(0, WRAP, 1f, r = 6))
    stats.addView(stat("%.0f h".format(hrs), "assistidas"), lin(0, WRAP, 1f))
    col.addView(stats, lin(MATCH, WRAP, l = 16, r = 16, t = 18))
    stats.riseIn(260L)

    // carrossel
    val posterW = minOf((screenW * 0.60f).toInt(), dp(250))
    val posterH = (posterW * 1.48f).toInt()
    val side = (screenW - posterW) / 2
    val rv = RecyclerView(this)
    rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    rv.clipToPadding = false
    rv.clipChildren = false
    rv.setPadding(side, dp(46), side, dp(18))
    rv.overScrollMode = View.OVER_SCROLL_NEVER
    val snap = PagerSnapHelper()
    snap.attachToRecyclerView(rv)

    class Holder(v: FrameLayout) : RecyclerView.ViewHolder(v)
    val ctx = this
    rv.adapter = object : RecyclerView.Adapter<Holder>() {
        override fun onCreateViewHolder(parent: ViewGroup, vt: Int): Holder {
            val f = FrameLayout(ctx)
            f.clipChildren = false
            f.layoutParams = RecyclerView.LayoutParams(posterW + dp(20), posterH + dp(8))
            return Holder(f)
        }

        override fun onBindViewHolder(h: Holder, i: Int) {
            val f = h.itemView as FrameLayout
            f.removeAllViews()
            val d = sorted[i]
            val card = ctx.posterCard(d, i + 1, posterW, posterH)
            f.addView(card, FrameLayout.LayoutParams(posterW, posterH, Gravity.CENTER))
            card.setOnClickListener { onOpen(d, sorted) }
            card.pressable(0.97f)
        }

        override fun getItemCount(): Int = sorted.size
    }
    col.addView(rv, lin(MATCH, posterH + dp(70)))
    rv.fadeScaleIn(340L, 520L)

    // painel do dorama que está no centro
    val info = glassCard()
    info.gravity = Gravity.CENTER_HORIZONTAL
    info.setPadding(dp(16), dp(14), dp(16), dp(14))
    val nm = label("", 21f, Color.WHITE, true, true)
    nm.gravity = Gravity.CENTER
    nm.maxLines = 2
    nm.ellipsize = TextUtils.TruncateAt.END
    nm.setShadowLayer(dp(6).toFloat(), 0f, 0f, Color.argb(150, 245, 184, 61))
    val meta = label("", 12f, Color.parseColor("#D9FFE9B0"), true)
    meta.gravity = Gravity.CENTER
    val rating = RatingView(this, 20, false)
    rating.color = SUP_GOLD
    val noteTv = label("", 13f, SUP_HI, true)
    val syn = label("", 12f, Color.parseColor("#BFFFE9B0"))
    syn.gravity = Gravity.CENTER
    syn.maxLines = 3
    syn.ellipsize = TextUtils.TruncateAt.END
    info.addView(nm, lin(MATCH, WRAP))
    info.addView(meta, lin(MATCH, WRAP, t = 3))
    val rr = LinearLayout(this)
    rr.gravity = Gravity.CENTER_VERTICAL
    rr.addView(rating, lin(WRAP, WRAP))
    rr.addView(noteTv, lin(WRAP, WRAP, l = 8))
    info.addView(rr, lin(WRAP, WRAP, t = 8))
    info.addView(syn, lin(MATCH, WRAP, t = 8))
    col.addView(info, lin(MATCH, WRAP, l = 16, r = 16))

    var shown = -1
    fun show(i: Int) {
        if (i < 0 || i >= sorted.size || i == shown) return
        shown = i
        val d = sorted[i]
        val g = Genres.byKey(d.genre)
        nm.text = d.title
        val bits = ArrayList<String>()
        bits.add(g.label)
        if (d.year.isNotBlank()) bits.add(d.year)
        if (d.country.isNotBlank()) bits.add(d.country)
        meta.text = bits.joinToString(" · ")
        rating.score = d.score
        noteTv.text = if (d.score > 0) "Nota ${d.score}" else "Sem nota"
        syn.text = d.synopsis
        syn.visibility = if (d.synopsis.isBlank()) View.GONE else View.VISIBLE
        nm.fadeScaleIn(0L, 260L)
    }
    show(0)

    rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(r: RecyclerView, dx: Int, dy: Int) {
            val mid = r.width / 2f
            var best = -1
            var bestD = Float.MAX_VALUE
            for (k in 0 until r.childCount) {
                val v = r.getChildAt(k)
                val dist = abs((v.left + v.right) / 2f - mid)
                val f = (1f - (dist / (r.width * 0.55f))).coerceIn(0f, 1f)
                val sc = 0.80f + 0.20f * f
                v.scaleX = sc
                v.scaleY = sc
                v.alpha = 0.45f + 0.55f * f
                if (dist < bestD) {
                    bestD = dist
                    best = r.getChildAdapterPosition(v)
                }
            }
            show(best)
        }
    })
    rv.post { rv.scrollBy(1, 0) } // dispara o primeiro cálculo de escala

    // ranking completo
    if (sorted.size > 1) {
        val head = LinearLayout(this)
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(IconView(this, "crown", SUP_GOLD, 18))
        head.addView(label("Salão da fama", 16f, SUP_HI, true, true), lin(WRAP, WRAP, l = 8))
        col.addView(head, lin(MATCH, WRAP, l = 20, r = 20, t = 22, b = 8))
        for ((i, d) in sorted.withIndex()) {
            val row = glassCard()
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(dp(10), dp(8), dp(12), dp(8))
            val rk = label(roman(i + 1), 15f, SUP_GOLD, true, true)
            rk.gravity = Gravity.CENTER
            row.addView(rk, lin(dp(30), WRAP))
            val th = CoverView(this, 10)
            th.bind(d, dp(60))
            row.addView(th, lin(dp(40), dp(58), l = 4, r = 12))
            val tx = LinearLayout(this)
            tx.orientation = LinearLayout.VERTICAL
            val t1 = label(d.title, 14f, Color.WHITE, true)
            t1.maxLines = 1
            t1.ellipsize = TextUtils.TruncateAt.END
            tx.addView(t1)
            tx.addView(label(Genres.byKey(d.genre).label + (if (d.year.isNotBlank()) " · " + d.year else ""), 11f, Color.parseColor("#BFFFE9B0")))
            row.addView(tx, lin(0, WRAP, 1f))
            val sc = label(if (d.score > 0) d.score.toString() else "-", 20f, SUP_HI, true, true)
            row.addView(sc, lin(WRAP, WRAP, l = 8))
            row.setOnClickListener { onOpen(d, sorted) }
            row.pressable(0.97f)
            col.addView(row, lin(MATCH, WRAP, l = 16, r = 16, t = 6))
        }
    }
    return root
}
