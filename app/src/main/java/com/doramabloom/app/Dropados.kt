package com.doramabloom.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Lilás acinzentado do tema Dropados: o tom de quem ficou pelo caminho, mas sem perder o carinho. */
val DROP_TINT: Int = Color.parseColor("#A78BC7")

/** Pétalas murchas caindo devagar, balançando de leve. */
private class WiltBackdrop(ctx: Context) : View(ctx) {
    private class Petal(val fx: Float, val speed: Float, val phase: Float, val size: Float, val sway: Float, val spin: Float)

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val petals: List<Petal>

    init {
        val r = java.util.Random(11L)
        petals = List(16) {
            Petal(
                r.nextFloat(), 0.018f + r.nextFloat() * 0.03f, r.nextFloat(),
                dp(5) + r.nextInt(dp(7)).toFloat(), 0.02f + r.nextFloat() * 0.04f, 20f + r.nextFloat() * 50f
            )
        }
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val t = SystemClock.uptimeMillis() / 1000f
        c.drawColor(Color.TRANSPARENT)
        val top = if (Palette.dark) Color.parseColor("#1E1726") else Color.parseColor("#F6F0F8")
        val bottom = if (Palette.dark) Color.parseColor("#120E16") else Color.parseColor("#E6DCEC")
        p.shader = android.graphics.LinearGradient(0f, 0f, 0f, h, top, bottom, android.graphics.Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        p.style = Paint.Style.FILL
        for (pt in petals) {
            val prog = (pt.phase + t * pt.speed) % 1f
            val x = w * (pt.fx + pt.sway * Math.sin((t * 0.7f + pt.phase * 6f).toDouble()).toFloat())
            val y = -pt.size * 2 + (h + pt.size * 4) * prog
            val edge = minOf(prog, 1f - prog)
            p.color = if (Palette.dark) Color.parseColor("#B79AD6") else Color.parseColor("#9C7CBF")
            p.alpha = (110 * minOf(1f, edge * 6f)).toInt()
            c.save()
            c.translate(x, y)
            c.rotate(t * pt.spin + pt.phase * 360f)
            c.drawOval(-pt.size * 0.5f, -pt.size, pt.size * 0.5f, pt.size, p)
            c.restore()
        }
        if (isAttachedToWindow) postInvalidateDelayed(40)
    }
}

/** Ordem em que os tipos aparecem na aba. */
private val DROP_ORDER = listOf("retomar", "talvez", "nunca", DropKinds.NONE)

private fun kindOf(d: Drama): String = if (d.dropKind.isEmpty()) DropKinds.NONE else d.dropKind

private fun kindColor(k: String): Int = DropKinds.all.firstOrNull { it.key == k }?.color ?: Color.parseColor("#B7A3AE")

private fun kindIcon(k: String): String = DropKinds.all.firstOrNull { it.key == k }?.icon ?: "tag"

private fun kindName(k: String): String = if (k == DropKinds.NONE) "Sem classificação" else DropKinds.label(k)

/** Aba Dropados: tudo o que você largou, separado em três tipos, com onde parou e o motivo. */
fun Context.buildDroppedTab(all: List<Drama>, onOpen: (Drama, List<Drama>) -> Unit): View {
    val root = FrameLayout(this)
    root.addView(WiltBackdrop(this), FrameLayout.LayoutParams(MATCH, MATCH))

    val sv = ScrollView(this)
    sv.isVerticalScrollBarEnabled = false
    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    col.setPadding(dp(16), dp(10), dp(16), dp(28))
    sv.addView(col, FrameLayout.LayoutParams(MATCH, WRAP))
    root.addView(sv, FrameLayout.LayoutParams(MATCH, MATCH))

    val counts = DROP_ORDER.associateWith { k -> all.count { kindOf(it) == k } }

    // cabeçalho
    val head = LinearLayout(this)
    head.orientation = LinearLayout.HORIZONTAL
    head.gravity = Gravity.CENTER_VERTICAL
    head.setPadding(dp(16), dp(14), dp(16), dp(14))
    val hb = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(mixColor(DROP_TINT, Color.WHITE, 0.12f), mixColor(DROP_TINT, Color.parseColor("#4A3566"), 0.55f))
    )
    hb.cornerRadius = dp(26).toFloat()
    head.background = hb
    val bubble = FrameLayout(this)
    val bg = GradientDrawable()
    bg.shape = GradientDrawable.OVAL
    bg.setColor(Color.argb(70, 255, 255, 255))
    bubble.background = bg
    bubble.addView(IconView(this, "heartbreak", Color.WHITE, 26), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
    head.addView(bubble, lin(dp(50), dp(50), r = 12))
    val tt = LinearLayout(this)
    tt.orientation = LinearLayout.VERTICAL
    val title = label("Dropados", 22f, Color.WHITE, true, true)
    title.setShadowLayer(dp(2).toFloat(), 0f, dp(1).toFloat(), Color.argb(110, 0, 0, 0))
    tt.addView(title)
    val subText = when (all.size) {
        0 -> "Nada largado pelo caminho"
        1 -> "1 obra ficou pelo caminho"
        else -> all.size.toString() + " obras ficaram pelo caminho"
    }
    tt.addView(label(subText, 12f, Color.argb(225, 255, 255, 255), true), lin(WRAP, WRAP, t = 1))
    head.addView(tt, lin(0, WRAP, 1f))
    col.addView(head, lin(MATCH, WRAP))
    head.riseIn(0L, 12, 360L)

    if (all.isEmpty()) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER_HORIZONTAL
        box.setPadding(dp(22), dp(34), dp(22), dp(30))
        box.background = roundRect(mixColor(Palette.card, DROP_TINT, 0.08f), dp(26).toFloat(), mixColor(Palette.line, DROP_TINT, 0.4f), dp(1))
        box.addView(IconView(this, "cloud", DROP_TINT, 48))
        box.addView(label("Nenhum dorama dropado", 16f, Palette.text, true, true), lin(WRAP, WRAP, t = 10))
        val m = label("Quando você largar um, é só marcar o status Dropado na edição. Ele aparece aqui com o episódio em que parou e o motivo.", 12.5f, Palette.muted)
        m.gravity = Gravity.CENTER
        box.addView(m, lin(MATCH, WRAP, t = 6))
        col.addView(box, lin(MATCH, WRAP, t = 16))
        box.riseIn(120L)
        return root
    }

    // três cartinhas de número: também funcionam como filtro
    var filter = "all"
    val listBox = LinearLayout(this)
    listBox.orientation = LinearLayout.VERTICAL

    fun rebuild() {
        listBox.removeAllViews()
        val shown = all.filter { filter == "all" || kindOf(it) == filter }
            .sortedWith(compareBy<Drama> { DROP_ORDER.indexOf(kindOf(it)) }.thenBy { it.title.lowercase() })
        var lastKind = ""
        var idx = 0
        for (d in shown) {
            val k = kindOf(d)
            if (filter == "all" && k != lastKind) {
                lastKind = k
                val h = LinearLayout(this)
                h.orientation = LinearLayout.HORIZONTAL
                h.gravity = Gravity.CENTER_VERTICAL
                h.addView(IconView(this, kindIcon(k), kindColor(k), 16), lin(WRAP, WRAP, r = 6))
                h.addView(label(kindName(k), 13.5f, kindColor(k), true, true), lin(WRAP, WRAP))
                h.addView(label("  " + (counts[k] ?: 0), 12f, Palette.muted, true), lin(WRAP, WRAP))
                listBox.addView(h, lin(MATCH, WRAP, t = if (listBox.childCount == 0) 4 else 18, b = 8))
            }
            val card = droppedCard(d) { onOpen(d, shown) }
            listBox.addView(card, lin(MATCH, WRAP, t = if (filter != "all" && idx == 0) 8 else 0, b = 10))
            card.riseIn(minOf(idx, 8) * 45L, 16, 320L)
            idx++
        }
        if (shown.isEmpty()) {
            val m = label("Nada por aqui nesse tipo.", 13f, Palette.muted)
            m.gravity = Gravity.CENTER
            listBox.addView(m, lin(MATCH, WRAP, t = 24))
        }
    }

    val opts = ArrayList<Opt>()
    opts.add(Opt("all", "Todos · " + all.size, DROP_TINT, "heartbreak"))
    for (k in DROP_ORDER) {
        val n = counts[k] ?: 0
        if (k == DropKinds.NONE && n == 0) continue
        opts.add(Opt(k, kindName(k) + " · " + n, kindColor(k), kindIcon(k)))
    }
    col.addView(chipScroller(opts, filter) {
        filter = it
        rebuild()
    }, lin(MATCH, WRAP, t = 12))

    col.addView(listBox, lin(MATCH, WRAP, t = 8))
    rebuild()
    return root
}

/** Cartão de um dropado: capa, nome, tipo, onde parou e o motivo. */
private fun Context.droppedCard(d: Drama, onClick: () -> Unit): View {
    val g = Genres.byKey(d.genre)
    val k = kindOf(d)
    val kc = kindColor(k)
    val row = LinearLayout(this)
    row.orientation = LinearLayout.HORIZONTAL
    row.setPadding(dp(10), dp(10), dp(14), dp(10))
    row.background = roundRect(mixColor(Palette.card, kc, 0.07f), dp(22).toFloat(), mixColor(Palette.line, kc, 0.45f), dp(1))

    val cv = CoverView(this, 16)
    cv.elevation = 0f
    cv.bind(d, 220)
    row.addView(cv, lin(dp(66), dp(96), r = 12))

    val mid = LinearLayout(this)
    mid.orientation = LinearLayout.VERTICAL
    val tt = label(d.title, 15f, Palette.text, true)
    tt.maxLines = 2
    tt.ellipsize = TextUtils.TruncateAt.END
    mid.addView(tt, lin(MATCH, WRAP))

    val badge = label(kindName(k), 10.5f, Color.WHITE, true)
    badge.setPadding(dp(9), dp(3), dp(10), dp(3))
    badge.background = roundRect(kc, dp(12).toFloat())
    mid.addView(badge, lin(WRAP, WRAP, t = 5))

    val bits = ArrayList<String>()
    if (d.kind == "filme") bits.add("Filme")
    bits.add(g.label)
    if (d.year.isNotBlank()) bits.add(d.year)
    mid.addView(label(bits.joinToString("  ·  "), 11f, Palette.muted), lin(MATCH, WRAP, t = 5))

    if (d.dropEp.isNotBlank()) {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.addView(IconView(this, "pause", kc, 12), lin(WRAP, WRAP, r = 5))
        r.addView(label("Parei em " + d.dropEp, 12f, Palette.text, true), lin(WRAP, WRAP))
        mid.addView(r, lin(MATCH, WRAP, t = 5))
    }
    if (d.dropReason.isNotBlank()) {
        val why: TextView = label("“" + d.dropReason + "”", 12f, Palette.muted)
        why.maxLines = 2
        why.ellipsize = TextUtils.TruncateAt.END
        mid.addView(why, lin(MATCH, WRAP, t = 4))
    }

    val total = totalEps(d)
    val seen = watchedEps(d)
    if (total > 0 && seen > 0) {
        val pct = (seen.toFloat() / total).coerceIn(0f, 1f)
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.background = roundRect(mixColor(kc, Palette.card, 0.8f), dp(3).toFloat())
        val fill = View(this)
        fill.background = roundRect(kc, dp(3).toFloat())
        bar.addView(fill, LinearLayout.LayoutParams(0, MATCH, pct))
        bar.addView(View(this), LinearLayout.LayoutParams(0, MATCH, 1f - pct))
        mid.addView(bar, lin(MATCH, dp(5), t = 8))
    }
    row.addView(mid, lin(0, WRAP, 1f))

    row.setOnClickListener { onClick() }
    row.pressable(0.97f)
    return row
}
