package com.doramabloom.app

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputFilter
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged

/** Ícones que você pode escolher ao criar um gênero. */
val genreIconChoices: List<String> = listOf(
    "heart", "smile", "bolt", "ghost", "sparkle", "moon", "star", "drop",
    "pagoda", "search", "school", "eye", "skull", "cross", "music", "trophy",
    "crown", "rocket", "coffee", "leaf", "blossom", "home", "book", "tv"
)

/** Cores que você pode escolher ao criar um gênero. */
val genreColorChoices: List<Int> = listOf(
    "#FF6B9D", "#FF7A6B", "#FFB84D", "#E0A93B", "#7FD1AE", "#5DB56E", "#3FB6C9",
    "#6FA8FF", "#6C63FF", "#9B7EDE", "#C38BD8", "#E36BC4", "#B03A5B", "#8A6D5A"
).map { Color.parseColor(it) }

/**
 * Selo do gênero: um adesivo de bordas onduladas com o ícone do gênero no meio.
 * Com label = true aparece uma fitinha com o nome embaixo (para tamanhos grandes).
 */
class SealView(ctx: Context, private val withLabel: Boolean = false) : View(ctx) {
    private var icon = "heart"
    private var color = Palette.pink
    private var dark = Palette.pinkDark
    private var text = ""
    private val scallops = 12
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val ic = IconDrawable("heart", Color.WHITE)

    fun set(g: Genre) {
        icon = g.icon
        color = g.primary
        dark = g.dark
        text = g.label
        ic.name = icon
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(dp(46), widthMeasureSpec),
            resolveSize(dp(46), heightMeasureSpec)
        )
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = minOf(w, h) / 2f
        val big = if (withLabel) r * 0.78f else r
        val cx = w / 2f
        val cy = if (withLabel) big + r * 0.04f else h / 2f

        p.style = Paint.Style.FILL
        p.color = color
        for (i in 0 until scallops) {
            val a = 2.0 * Math.PI * i / scallops
            c.drawCircle(
                cx + (Math.cos(a) * big * 0.82).toFloat(),
                cy + (Math.sin(a) * big * 0.82).toFloat(),
                big * 0.19f, p
            )
        }
        c.drawCircle(cx, cy, big * 0.84f, p)

        p.style = Paint.Style.STROKE
        p.strokeWidth = maxOf(1f, big * 0.05f)
        p.color = Color.argb(190, 255, 255, 255)
        c.drawCircle(cx, cy, big * 0.66f, p)

        val s = (big * 0.92f).toInt()
        val l = (cx - s / 2f).toInt()
        val t = (cy - s / 2f).toInt()
        ic.color = Color.WHITE
        ic.setBounds(l, t, l + s, t + s)
        ic.draw(c)

        if (withLabel && text.isNotEmpty()) {
            val rw = minOf(w, r * 1.9f)
            val rh = r * 0.38f
            val top = cy + big - r * 0.16f
            p.style = Paint.Style.FILL
            p.color = dark
            rect.set(cx - rw / 2f, top, cx + rw / 2f, top + rh)
            c.drawRoundRect(rect, rh / 2f, rh / 2f, p)
            val label = text.uppercase()
            tp.color = Color.WHITE
            tp.typeface = Typeface.DEFAULT_BOLD
            tp.textAlign = Paint.Align.CENTER
            var ts = rh * 0.56f
            tp.textSize = ts
            while (tp.measureText(label) > rw - rh * 0.6f && ts > 6f) {
                ts -= 1f
                tp.textSize = ts
            }
            c.drawText(label, cx, top + rh / 2f - (tp.ascent() + tp.descent()) / 2f, tp)
        }
    }
}

/** Janela para criar um gênero novo: nome, frase, cor e ícone, com prévia ao vivo. */
fun Activity.showGenreCreator(onDone: (Genre) -> Unit) {
    if (Genres.custom().size >= 30) {
        softToast("Você já criou muitos gêneros!", Palette.pink, "tag")
        return
    }
    var color = genreColorChoices[0]
    var icon = genreIconChoices[0]

    val sv = ScrollView(this)
    sv.isVerticalScrollBarEnabled = false
    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    col.setPadding(dp(20), dp(8), dp(20), dp(6))
    sv.addView(col)

    // prévia
    val prevBox = LinearLayout(this)
    prevBox.orientation = LinearLayout.HORIZONTAL
    prevBox.gravity = Gravity.CENTER_VERTICAL
    prevBox.setPadding(dp(12), dp(10), dp(14), dp(10))
    val seal = SealView(this)
    prevBox.addView(seal, lin(dp(54), dp(54), r = 12))
    val ptx = LinearLayout(this)
    ptx.orientation = LinearLayout.VERTICAL
    val prevLabel = label("Seu gênero", 18f, Palette.text, true, true)
    val prevTag = label("", 12f, Palette.muted)
    ptx.addView(prevLabel)
    ptx.addView(prevTag, lin(MATCH, WRAP, t = 2))
    prevBox.addView(ptx, lin(0, WRAP, 1f))
    col.addView(prevBox, lin(MATCH, WRAP))

    val nameIn = input("Nome do gênero (ex.: Sobrenatural)", "", android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS)
    nameIn.filters = arrayOf(InputFilter.LengthFilter(18))
    val tagIn = input("Frase fofa (opcional)")
    tagIn.filters = arrayOf(InputFilter.LengthFilter(60))
    col.addView(label("Nome", 12.5f, Palette.muted, true), lin(WRAP, WRAP, t = 14, b = 6))
    col.addView(nameIn, lin(MATCH, WRAP))
    col.addView(label("Frase do gênero", 12.5f, Palette.muted, true), lin(WRAP, WRAP, t = 12, b = 6))
    col.addView(tagIn, lin(MATCH, WRAP))

    // cores
    col.addView(label("Cor", 12.5f, Palette.muted, true), lin(WRAP, WRAP, t = 14, b = 8))
    val swFlow = FlowLayout(this)
    swFlow.hGap = dp(8)
    swFlow.vGap = dp(8)
    val swatches = ArrayList<FrameLayout>()
    val iconCells = ArrayList<FrameLayout>()
    val iconViews = ArrayList<IconView>()

    fun previewGenre(): Genre {
        val nm = nameIn.text.toString().trim()
        return Genres.makeCustom("preview", if (nm.isEmpty()) "Seu gênero" else nm, icon, color, tagIn.text.toString().trim())
    }

    fun restyle() {
        val g = previewGenre()
        seal.set(g)
        prevLabel.text = g.label
        prevLabel.setTextColor(g.dark)
        prevTag.text = g.tagline
        prevBox.background = roundRect(g.soft, dp(20).toFloat(), mixColor(g.primary, Color.WHITE, 0.5f), dp(1))
        for (i in swatches.indices) {
            val d = GradientDrawable()
            d.shape = GradientDrawable.OVAL
            d.setColor(genreColorChoices[i])
            if (genreColorChoices[i] == color) d.setStroke(dp(3), Palette.text) else d.setStroke(dp(2), Color.WHITE)
            swatches[i].background = d
        }
        for (i in iconCells.indices) {
            val sel = genreIconChoices[i] == icon
            iconCells[i].background = roundRect(if (sel) color else Palette.pinkSoft, dp(16).toFloat())
            iconViews[i].tint = if (sel) Color.WHITE else color
        }
    }

    for (i in genreColorChoices.indices) {
        val cell = FrameLayout(this)
        cell.addView(View(this), FrameLayout.LayoutParams(dp(32), dp(32)))
        cell.setOnClickListener {
            color = genreColorChoices[i]
            restyle()
            seal.pop(1.3f)
        }
        cell.pressable(0.88f)
        swatches.add(cell)
        swFlow.addView(cell)
    }
    col.addView(swFlow, lin(MATCH, WRAP))

    // ícones
    col.addView(label("Ícone", 12.5f, Palette.muted, true), lin(WRAP, WRAP, t = 14, b = 8))
    val icFlow = FlowLayout(this)
    icFlow.hGap = dp(8)
    icFlow.vGap = dp(8)
    for (i in genreIconChoices.indices) {
        val cell = FrameLayout(this)
        cell.setPadding(dp(10), dp(10), dp(10), dp(10))
        val iv = IconView(this, genreIconChoices[i], Palette.pink, 22)
        cell.addView(iv, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
        cell.setOnClickListener {
            icon = genreIconChoices[i]
            restyle()
            seal.pop(1.3f)
        }
        cell.pressable(0.88f)
        iconCells.add(cell)
        iconViews.add(iv)
        icFlow.addView(cell)
    }
    col.addView(icFlow, lin(MATCH, WRAP))

    nameIn.doAfterTextChanged { restyle() }
    tagIn.doAfterTextChanged { restyle() }
    restyle()

    val dlg = AlertDialog.Builder(this)
        .setTitle("Novo gênero")
        .setView(sv)
        .setPositiveButton("Criar", null)
        .setNegativeButton("Cancelar", null)
        .create()
    dlg.show()
    dlg.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
        val nm = nameIn.text.toString().trim()
        if (nm.isEmpty()) {
            nameIn.error = "Dê um nome ao gênero"
            return@setOnClickListener
        }
        if (Genres.all.any { it.label.equals(nm, true) }) {
            nameIn.error = "Esse gênero já existe"
            return@setOnClickListener
        }
        val g = Genres.makeCustom("c" + System.currentTimeMillis(), nm, icon, color, tagIn.text.toString().trim())
        Store.addGenre(g)
        dlg.dismiss()
        onDone(g)
    }
}
