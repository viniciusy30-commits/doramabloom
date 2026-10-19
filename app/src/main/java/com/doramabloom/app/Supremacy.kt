package com.doramabloom.app

import android.content.Context
import android.app.Activity
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

private fun withA(c: Int, a: Int): Int = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))

/** Fundo animado: raios de luz, halo que muda de cor com o dorama central, estrelinhas e brasas subindo. */
private class GoldBackdrop(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val star = starPath()
    private val seeds = FloatArray(48) { ((it * 7919) % 1000) / 1000f }
    private val embers = FloatArray(75) { ((it * 6271 + 13) % 1000) / 1000f }
    private var tint = SUP_GOLD

    /** Troca a cor do halo com uma transição suave (usa a cor do gênero do dorama em destaque). */
    fun setTint(c: Int) {
        val from = tint
        val a = android.animation.ValueAnimator.ofFloat(0f, 1f)
        a.duration = 800
        a.addUpdateListener {
            tint = mixColor(from, c, it.animatedValue as Float)
            invalidate()
        }
        a.start()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f) return
        val t = clockSec()
        val glow = mixColor(tint, SUP_GOLD, 0.35f)
        p.shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(mixColor(Color.parseColor("#2A1A06"), tint, 0.16f), Color.parseColor("#140C03"), Color.parseColor("#0B0702")), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        // halo no topo (respira)
        val breathe = 0.85f + 0.15f * sin(t * 1.2f)
        p.shader = RadialGradient(w / 2f, 0f, w * 0.95f * breathe, withA(glow, 130), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        // brilho de fundo mais embaixo, na cor do dorama
        p.shader = RadialGradient(w / 2f, h * 0.55f, w * 0.9f, withA(tint, 46), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        // raios girando devagar
        c.save()
        c.translate(w / 2f, -h * 0.05f)
        c.rotate(t * 3f)
        p.style = Paint.Style.FILL
        for (i in 0 until 12) {
            p.color = withA(mixColor(Color.rgb(255, 214, 120), tint, 0.3f), if (i % 2 == 0) 34 else 16)
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
        // brasas douradas subindo e balançando
        for (i in 0 until 25) {
            val sx = embers[i * 3]
            val sp = 0.035f + embers[i * 3 + 1] * 0.06f
            val fy = 1f - ((embers[i * 3 + 2] + t * sp) % 1f)
            val sway = sin(t * (0.8f + embers[i * 3 + 1] * 1.4f) + i * 1.7f) * dp(14)
            val fade = (fy * (1f - fy) * 4f).coerceIn(0f, 1f)
            p.color = withA(mixColor(Color.rgb(255, 200, 90), tint, 0.25f), (150 * fade).toInt())
            c.drawCircle(w * sx + sway, h * fy, dp(1) + dp(2) * embers[i * 3 + 1], p)
        }
        if (isAttachedToWindow) postInvalidateDelayed(40)
    }
}

/** Barra de progresso dourada. */
private class GoldBar(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    var frac: Float = 0f
        set(v) {
            field = v.coerceIn(0f, 1f)
            invalidate()
        }
    var fillColor: Int = SUP_GOLD
        set(v) {
            field = v
            invalidate()
        }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f) return
        val r = h / 2f
        p.shader = null
        p.color = Color.argb(46, 255, 226, 150)
        c.drawRoundRect(0f, 0f, w, h, r, r, p)
        if (frac > 0f) {
            val fw = maxOf(h, w * frac)
            p.shader = LinearGradient(0f, 0f, fw, 0f, intArrayOf(SUP_DEEP, fillColor, SUP_HI), null, Shader.TileMode.CLAMP)
            c.drawRoundRect(0f, 0f, fw, h, r, r, p)
            p.shader = null
        }
    }
}

private val MEDAL_SILVER = Color.parseColor("#D9DEE8")
private val MEDAL_BRONZE = Color.parseColor("#D08A4E")

private fun medalOf(rank: Int): Int = when (rank) {
    1 -> SUP_GOLD
    2 -> MEDAL_SILVER
    3 -> MEDAL_BRONZE
    else -> SUP_DEEP
}

private fun tierOf(rank: Int): String = when (rank) {
    1 -> "SOBERANO"
    2 -> "HERDEIRO"
    3 -> "NOBREZA"
    else -> "CORTE REAL"
}

private fun epsFrac(d: Drama): Float {
    val t = totalEps(d)
    return if (t <= 0) 0f else watchedEps(d).toFloat() / t
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
    return frame
}

/** Ordem do salão: a sua ordem de favoritismo (quando existe); quem ainda não tem posição vem depois, por nota. */
fun supremacySorted(list: List<Drama>): List<Drama> =
    list.sortedWith(
        compareBy<Drama> { if (it.supOrder > 0L) it.supOrder else Long.MAX_VALUE }
            .thenByDescending { it.score }
            .thenBy { it.title.lowercase() }
    )

/** Segure e arraste para escolher quais doramas do Supremacy você mais ama (o primeiro é o seu favorito). */
fun Activity.showSupremacyReorder(current: List<Drama>, onDone: () -> Unit) {
    val items = ArrayList(current)
    if (items.size < 2) return
    val box = LinearLayout(this)
    box.orientation = LinearLayout.VERTICAL
    box.setPadding(dp(12), dp(12), dp(12), dp(4))
    box.addView(
        label("Segure e arraste pelo ícone à direita: o primeiro é o seu dorama favorito.", 12.5f, Palette.muted),
        lin(MATCH, WRAP, l = 6, r = 6, b = 8)
    )
    val rv = RecyclerView(this)
    rv.layoutManager = LinearLayoutManager(this)
    rv.overScrollMode = View.OVER_SCROLL_NEVER
    val maxH = (resources.displayMetrics.heightPixels * 0.6f).toInt()
    box.addView(rv, lin(MATCH, maxH))
    lateinit var helper: androidx.recyclerview.widget.ItemTouchHelper
    val act = this

    class VH(val row: LinearLayout, val num: TextView, val thumb: FrameLayout, val name: TextView, val handle: View) :
        RecyclerView.ViewHolder(row)

    val adapter = object : RecyclerView.Adapter<VH>() {
        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val row = LinearLayout(act)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(dp(8), dp(6), dp(10), dp(6))
            row.layoutParams = ViewGroup.LayoutParams(MATCH, WRAP)
            val num = label("1", 14f, SUP_DEEP, true, true)
            num.gravity = Gravity.CENTER
            row.addView(num, lin(dp(30), WRAP))
            val tb = FrameLayout(act)
            row.addView(tb, lin(dp(38), dp(54), l = 2, r = 10))
            val nm = label("", 14f, Palette.text, true)
            nm.maxLines = 2
            nm.ellipsize = TextUtils.TruncateAt.END
            row.addView(nm, lin(0, WRAP, 1f))
            val h = IconView(act, "sort", SUP_DEEP, 22)
            row.addView(h, lin(dp(34), dp(34), l = 6))
            return VH(row, num, tb, nm, h)
        }

        override fun onBindViewHolder(h: VH, pos: Int) {
            val d = items[pos]
            h.num.text = roman(pos + 1)
            h.thumb.removeAllViews()
            val cv = CoverView(act, 8)
            cv.bind(d, dp(54))
            h.thumb.addView(cv, FrameLayout.LayoutParams(MATCH, MATCH))
            h.name.text = d.title
            h.row.background = roundRect(
                if (pos == 0) mixColor(Palette.card, SUP_GOLD, if (Palette.dark) 0.22f else 0.18f) else Palette.card,
                dp(16).toFloat(), if (pos == 0) SUP_GOLD else Palette.line, dp(1)
            )
            h.handle.setOnTouchListener { _, ev ->
                if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) helper.startDrag(h)
                false
            }
        }
    }
    helper = androidx.recyclerview.widget.ItemTouchHelper(object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
        androidx.recyclerview.widget.ItemTouchHelper.UP or androidx.recyclerview.widget.ItemTouchHelper.DOWN, 0
    ) {
        override fun onMove(r: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
            val a = from.bindingAdapterPosition
            val b = to.bindingAdapterPosition
            if (a < 0 || b < 0) return false
            items.add(b, items.removeAt(a))
            adapter.notifyItemMoved(a, b)
            return true
        }

        override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {}

        override fun clearView(r: RecyclerView, vh: RecyclerView.ViewHolder) {
            super.clearView(r, vh)
            vh.itemView.alpha = 1f
            r.post { adapter.notifyDataSetChanged() }
        }

        override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, state: Int) {
            super.onSelectedChanged(vh, state)
            if (state == androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG) vh?.itemView?.alpha = 0.85f
        }
    })
    rv.adapter = adapter
    rv.addItemDecoration(object : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(out: android.graphics.Rect, v: View, p: RecyclerView, st: RecyclerView.State) {
            out.set(0, 0, 0, dp(6))
        }
    })
    helper.attachToRecyclerView(rv)

    androidx.appcompat.app.AlertDialog.Builder(this)
        .setTitle("Meus favoritos do Supremacy")
        .setView(box)
        .setPositiveButton("Salvar") { _, _ ->
            Store.setSupOrder(items.map { it.id })
            onDone()
        }
        .setNegativeButton("Cancelar", null)
        .setNeutralButton("Por nota") { _, _ ->
            Store.setSupOrder(emptyList())
            onDone()
        }
        .show()
}

/**
 * A aba Supremacy: uma única tela, sem repetição. O pôster do dorama no centro é o herói,
 * com o essencial logo embaixo (posto, nome, nota) e os botões de ação.
 */
fun Context.buildSupremacyTab(list: List<Drama>, onOpen: (Drama, List<Drama>) -> Unit, onReordered: () -> Unit = {}): View {
    val root = FrameLayout(this)
    val backdrop = GoldBackdrop(this)
    root.addView(backdrop, FrameLayout.LayoutParams(MATCH, MATCH))

    val sv = ScrollView(this)
    sv.isVerticalScrollBarEnabled = false
    sv.isFillViewport = true
    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    col.gravity = Gravity.CENTER_HORIZONTAL
    col.setPadding(0, dp(12), 0, dp(20))
    sv.addView(col, FrameLayout.LayoutParams(MATCH, WRAP))
    root.addView(sv, FrameLayout.LayoutParams(MATCH, MATCH))

    val screenW = resources.displayMetrics.widthPixels
    val screenH = resources.displayMetrics.heightPixels

    // cabeçalho enxuto: coroa + título + uma linha de números
    val crown = CrownView(this, 1, SUP_GOLD)
    col.addView(crown, lin(dp(74), dp(58)))
    crown.fadeScaleIn(0L, 600L)
    val title = goldText("SUPREMACY", 26f, screenW)
    col.addView(title, lin(WRAP, WRAP, t = 0))
    title.riseIn(120L)
    val sub = label("Acima de todos os favoritos", 12f, Color.parseColor("#CCFFE9B0"), true)
    sub.letterSpacing = 0.05f
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

    val sorted = supremacySorted(list)
    val n = sorted.size
    var shown = -1

    val rated = sorted.filter { it.score > 0 }
    val avg = if (rated.isEmpty()) "-" else "%.1f".format(rated.map { it.score }.sum().toDouble() / rated.size)
    val hrs = sorted.map { hoursWatched(it) }.sum()
    sub.text = (if (n == 1) "1 lenda" else "$n lendas") + "  ·  nota média " + avg + "  ·  " + "%.0f".format(hrs) + " h"

    // carrossel: o pôster manda na tela
    val posterH = minOf((screenH * 0.42f).toInt(), (screenW * 0.64f * 1.48f).toInt())
    val posterW = (posterH / 1.48f).toInt()
    val side = (screenW - posterW) / 2
    val rv = RecyclerView(this)
    rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    rv.clipToPadding = false
    rv.clipChildren = false
    rv.setPadding(side, dp(20), side, dp(14))
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
    col.addView(rv, lin(MATCH, posterH + dp(42), t = 8))
    rv.fadeScaleIn(340L, 520L)

    // indicador de posição
    val dotViews = ArrayList<View>()
    val counter = label("", 11f, Color.parseColor("#CCFFE9B0"), true)
    if (n > 1) {
        if (n <= 10) {
            val dots = LinearLayout(this)
            dots.gravity = Gravity.CENTER
            for (i in 0 until n) {
                val v = View(this)
                v.setOnClickListener { rv.smoothScrollToPosition(i) }
                dots.addView(v, lin(dp(6), dp(6), l = 3, r = 3))
                dotViews.add(v)
            }
            col.addView(dots, lin(WRAP, WRAP, t = 0))
        } else {
            col.addView(counter, lin(WRAP, WRAP))
        }
    }

    // o essencial do dorama do centro
    val tierTv = label("", 11f, SUP_HI, true, true)
    tierTv.letterSpacing = 0.14f
    tierTv.gravity = Gravity.CENTER
    tierTv.setPadding(dp(14), dp(5), dp(14), dp(5))
    val nm = label("", 24f, Color.WHITE, true, true)
    nm.gravity = Gravity.CENTER
    nm.maxLines = 2
    nm.ellipsize = TextUtils.TruncateAt.END
    nm.setShadowLayer(dp(8).toFloat(), 0f, 0f, Color.argb(170, 245, 184, 61))
    val meta = label("", 12.5f, Color.parseColor("#D9FFE9B0"), true)
    meta.gravity = Gravity.CENTER
    val rating = RatingView(this, 20, false)
    rating.color = SUP_GOLD
    val noteTv = label("", 13f, SUP_HI, true)
    val prog = GoldBar(this)
    val progTv = label("", 11f, Color.parseColor("#CCFFE9B0"), true)
    val castRow = LinearLayout(this)
    castRow.gravity = Gravity.CENTER

    col.addView(tierTv, lin(WRAP, WRAP, t = 10))
    col.addView(nm, lin(MATCH, WRAP, l = 24, r = 24, t = 10))
    col.addView(meta, lin(MATCH, WRAP, l = 24, r = 24, t = 3))
    val rr = LinearLayout(this)
    rr.gravity = Gravity.CENTER_VERTICAL
    rr.addView(rating, lin(WRAP, WRAP))
    rr.addView(noteTv, lin(WRAP, WRAP, l = 8))
    col.addView(rr, lin(WRAP, WRAP, t = 10))
    col.addView(prog, lin(dp(190), dp(6), t = 12))
    col.addView(progTv, lin(WRAP, WRAP, t = 4))
    col.addView(castRow, lin(WRAP, WRAP, t = 12))

    // ações: abrir, sortear, ordenar
    fun circBtn(icon: String, onClick: () -> Unit): View {
        val f = FrameLayout(this)
        f.background = roundRect(Color.argb(40, 255, 214, 120), dp(24).toFloat(), withA(SUP_GOLD, 130), dp(1))
        f.addView(IconView(this, icon, SUP_HI, 20), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
        f.setOnClickListener { onClick() }
        f.pressable(0.92f)
        return f
    }
    val actions = LinearLayout(this)
    actions.gravity = Gravity.CENTER_VERTICAL
    val openBtn = pill("Abrir dorama", SUP_GOLD, Color.parseColor("#4A2C05"), 14f, "play")
    openBtn.setPadding(dp(26), dp(12), dp(26), dp(12))
    openBtn.setOnClickListener { if (shown in sorted.indices) onOpen(sorted[shown], sorted) }
    if (n > 1) {
        actions.addView(circBtn("sort") { (this as? Activity)?.showSupremacyReorder(sorted, onReordered) }, lin(dp(48), dp(48), r = 12))
    }
    actions.addView(openBtn, lin(WRAP, WRAP))
    if (n > 1) {
        actions.addView(circBtn("sparkle") {
            var k = (0 until n).random()
            if (k == shown) k = (k + 1) % n
            rv.smoothScrollToPosition(k)
        }, lin(dp(48), dp(48), l = 12))
    }
    col.addView(actions, lin(WRAP, WRAP, t = 18))
    actions.riseIn(380L)

    fun show(i: Int) {
        if (i < 0 || i >= sorted.size || i == shown) return
        shown = i
        val d = sorted[i]
        val g = Genres.byKey(d.genre)
        val medal = medalOf(i + 1)
        backdrop.setTint(g.primary)
        tierTv.text = tierOf(i + 1) + "  ·  Nº " + (i + 1)
        tierTv.setTextColor(medal)
        tierTv.background = roundRect(withA(medal, 46), dp(14).toFloat(), withA(medal, 190), dp(1))
        nm.text = d.title
        val bits = ArrayList<String>()
        bits.add(g.label)
        if (d.year.isNotBlank()) bits.add(d.year)
        if (d.country.isNotBlank()) bits.add(d.country)
        meta.text = bits.joinToString("  ·  ")
        rating.score = d.score
        noteTv.text = if (d.score > 0) "Nota ${d.score}" else "Sem nota"
        val tot = totalEps(d)
        val watching = d.status == "assistindo" && tot > 0
        prog.visibility = if (watching) View.VISIBLE else View.GONE
        progTv.visibility = if (watching) View.VISIBLE else View.GONE
        prog.frac = epsFrac(d)
        prog.fillColor = g.primary
        progTv.text = "Ep. ${watchedEps(d)} de $tot"
        castRow.removeAllViews()
        for ((k, cp) in d.castPeople.take(5).withIndex()) {
            castRow.addView(avatarView(cp.photo, 34, SUP_GOLD, Color.argb(40, 255, 214, 120), SUP_HI), lin(dp(34), dp(34), l = if (k == 0) 0 else -8))
        }
        castRow.visibility = if (d.castPeople.isEmpty()) View.GONE else View.VISIBLE
        for ((k, v) in dotViews.withIndex()) {
            val lp = v.layoutParams as LinearLayout.LayoutParams
            lp.width = dp(if (k == i) 20 else 6)
            v.layoutParams = lp
            v.background = roundRect(if (k == i) SUP_GOLD else Color.argb(80, 255, 226, 150), dp(3).toFloat())
        }
        counter.text = "${i + 1} / $n"
        nm.fadeScaleIn(0L, 260L)
    }
    show(0)

    val camera = resources.displayMetrics.density * 9000f
    rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(r: RecyclerView, dx: Int, dy: Int) {
            val mid = r.width / 2f
            var bestPos = -1
            var bestD = Float.MAX_VALUE
            for (k in 0 until r.childCount) {
                val v = r.getChildAt(k)
                val center = (v.left + v.right) / 2f
                val dist = abs(center - mid)
                val f = (1f - (dist / (r.width * 0.55f))).coerceIn(0f, 1f)
                val sc = 0.78f + 0.22f * f
                v.cameraDistance = camera
                v.scaleX = sc
                v.scaleY = sc
                v.alpha = 0.40f + 0.60f * f
                v.rotationY = (if (center < mid) 1f else -1f) * 34f * (1f - f)
                if (dist < bestD) {
                    bestD = dist
                    bestPos = r.getChildAdapterPosition(v)
                }
            }
            show(bestPos)
        }
    })
    rv.post { rv.scrollBy(1, 0) }
    return root
}
