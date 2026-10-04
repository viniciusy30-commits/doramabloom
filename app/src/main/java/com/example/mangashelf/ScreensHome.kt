package com.example.mangashelf

import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.LinearLayoutManager
import android.graphics.Paint
import android.graphics.Canvas
import android.content.Context
import androidx.core.widget.TextViewCompat
import android.util.TypedValue
import android.view.ViewTreeObserver
import android.view.ViewOutlineProvider
import android.graphics.Outline
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

fun MainActivity.workCard(w: Work, play: Boolean, showStatus: Boolean = true): View {
    val col = vbox()
    val box = FrameLayout(this)
    box.addView(coverView(store, w), FrameLayout.LayoutParams(MATCH, WRAP))

    if (w.favorite) {
        val h = IconView(this, Ic.HeartSolid, P.accent2, 13)
        h.background = shape(0x99000000.toInt(), dp(12).toFloat())
        val lp = FrameLayout.LayoutParams(dp(24), dp(24), Gravity.TOP or Gravity.END)
        lp.setMargins(0, dp(6), dp(6), 0)
        box.addView(h, lp)
    }
    val badge = tv(w.chapterText(), 10.5f, Color.WHITE, true)
    badge.setPadding(dp(7), dp(3), dp(7), dp(3))
    badge.background = shape(0xB3000000.toInt(), dp(10).toFloat())
    val blp = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.START)
    blp.setMargins(dp(6), 0, 0, dp(6))
    box.addView(badge, blp)

    if (play) {
        val p = IconView(this, Ic.PlaySolid, P.onAccent, 14)
        p.background = shape(P.accent, dp(18).toFloat())
        p.setOnClickListener {
            continueReading(w)
        }
        val plp = FrameLayout.LayoutParams(dp(36), dp(36), Gravity.BOTTOM or Gravity.END)
        plp.setMargins(0, 0, dp(6), dp(6))
        box.addView(p, plp)
    }
    col.addv(box, MATCH, WRAP)

    val title = tv(w.title, 13f, P.text, true)
    title.maxLines = 2
    title.ellipsize = TextUtils.TruncateAt.END
    col.addv(title, MATCH, WRAP, 0f, 2, 6, 2, 0)

    if (w.total > 0 || w.status == STATUS_DONE) {
        col.addv(progressBar(w.progress(), 4), MATCH, WRAP, 0f, 2, 5, 2, 0)
    }
    val sub = tv(w.type, 11f, P.sub)
    if (showStatus) {
        val ss = SpannableStringBuilder("${w.type} · ")
        val st = ss.length
        ss.append(w.status)
        ss.setSpan(ForegroundColorSpan(statusColor(w.status)), st, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        ss.setSpan(StyleSpan(android.graphics.Typeface.BOLD), st, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sub.text = ss
    }
    sub.maxLines = 1
    sub.ellipsize = TextUtils.TruncateAt.END
    col.addv(sub, MATCH, WRAP, 0f, 2, 3, 2, 0)

    col.setOnClickListener { go(Route("detail", w.id)) }
    return col
}

/** Cartão de destaque em tela cheia: capa inteira, nome, nota, capítulo e botão Continuar lendo. */
private fun MainActivity.highlightCard(w: Work, pageW: Int, pageH: Int): View {
    val r = dp(26).toFloat()
    val card = FrameLayout(this)
    card.background = shape(P.card, r, P.line, dp(1))
    card.clipToOutline = true
    card.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, r)
        }
    }
    val cover = coverView(store, w, 0)
    if (pageW > 0) cover.ratio = pageH.toFloat() / pageW
    card.addView(cover, FrameLayout.LayoutParams(MATCH, MATCH))

    val shade = View(this)
    shade.background = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(0x40000000, 0x00000000, 0x00000000, 0xB8000000.toInt(), 0xF2000000.toInt())
    )
    card.addView(shade, FrameLayout.LayoutParams(MATCH, MATCH))

    // topo: tipo + favorito (esquerda) e botão de ocultar (direita)
    val topLeft = hbox()
    topLeft.gravity = Gravity.CENTER_VERTICAL
    val typeChip = tv(w.type.uppercase(), 11f, Color.WHITE, true)
    typeChip.letterSpacing = 0.08f
    typeChip.setPadding(dp(11), dp(5), dp(11), dp(5))
    typeChip.background = shape(tint(P.accent, 0xD0), dp(12).toFloat())
    topLeft.addv(typeChip, WRAP, WRAP)
    if (w.favorite) {
        topLeft.addv(IconView(this, Ic.HeartSolid, P.accent2, 20), dp(20), dp(20), 0f, 10, 0, 0, 0)
    }
    val tlp = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.START)
    tlp.setMargins(dp(14), dp(14), 0, 0)
    card.addView(topLeft, tlp)

    val x = roundBtn(Ic.Close, 36, 17, Color.WHITE) {
        w.hideHighlight = true
        store.save()
        toast("Removida dos destaques")
        render()
    }
    x.background = rippled(shape(0x73000000, dp(18).toFloat(), 0x33FFFFFF, dp(1)), dp(18).toFloat())
    val xlp = FrameLayout.LayoutParams(dp(36), dp(36), Gravity.TOP or Gravity.END)
    xlp.setMargins(0, dp(12), dp(12), 0)
    card.addView(x, xlp)

    // base: nome, faixa de números, progresso e botão
    val content = vbox()
    content.setPadding(dp(18), dp(18), dp(18), dp(18))
    val title = tv(w.title, 26f, Color.WHITE, true)
    title.maxLines = 3
    title.ellipsize = TextUtils.TruncateAt.END
    title.setShadowLayer(8f, 0f, 2f, 0xAA000000.toInt())
    content.addv(title, MATCH, WRAP, 0f, 0, 0, 0, 12)

    val strip = hbox()
    strip.background = shape(0x66000000, dp(18).toFloat(), 0x33FFFFFF, dp(1))
    strip.setPadding(dp(14), dp(12), dp(14), dp(12))
    fun cell(label: String, value: String, vc: Int, weight: Float, first: Boolean) {
        val c = vbox()
        c.setPadding(if (first) 0 else dp(12), 0, 0, 0)
        val l = tv(label, 10.5f, 0xB3FFFFFF.toInt(), true)
        l.letterSpacing = 0.1f
        l.maxLines = 1
        c.addv(l)
        val v = tv(value, 22f, vc, true)
        v.maxLines = 1
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(v, 13, 22, 1, TypedValue.COMPLEX_UNIT_SP)
        c.addv(v, MATCH, WRAP, 0f, 0, 3, 0, 0)
        strip.addv(c, 0, WRAP, weight)
    }
    fun divider() {
        val d = View(this)
        d.setBackgroundColor(0x33FFFFFF)
        strip.addv(d, dp(1), dp(38))
    }
    cell("NOTA", if (w.rating > 0) fmtNum(w.rating) else "—", if (w.rating > 0) P.star else 0xB3FFFFFF.toInt(), 0.7f, true)
    divider()
    cell("CAPÍTULO", fmtNum(w.current) + (if (w.total > 0) " / ${w.total}" else ""), Color.WHITE, 1.4f, false)
    divider()
    cell("PROGRESSO", "${w.progress()}%", Color.WHITE, 1.3f, false)
    content.addv(strip, MATCH, WRAP, 0f, 0, 0, 0, 12)

    if (w.total > 0) {
        content.addv(progressBar(w.progress(), 6), MATCH, WRAP, 0f, 0, 0, 0, 14)
    }

    val btn = pill("Continuar lendo", size = 16f, icon = Ic.PlaySolid) { continueReading(w) }
    btn.setPadding(dp(18), dp(15), dp(18), dp(15))
    content.addv(btn, MATCH, WRAP)

    card.addView(content, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))
    card.setOnClickListener { go(Route("detail", w.id)) }
    return card
}

/** Destaques: uma página por obra, deslizando para o lado, com indicador embaixo. */
private fun MainActivity.highlights(col: LinearLayout, scroll: ScrollView, anchor: View) {
    val allReading = store.works.filter { it.status == STATUS_READING }
    val hidden = allReading.filter { it.hideHighlight }.sortedBy { it.title.lowercase() }
    val reading = allReading.filter { !it.hideHighlight }
        .sortedWith(compareByDescending<Work> { it.lastRead }.thenByDescending { it.created })

    fun restoreLink() {
        if (hidden.isEmpty()) return
        val restore = tv("Ocultas dos destaques (${hidden.size})", 12.5f, P.sub, true)
        restore.setPadding(dp(4), dp(8), dp(4), dp(8))
        restore.setOnClickListener {
            listDialog("Voltar para os destaques", hidden.map { it.title }) { i ->
                hidden[i].hideHighlight = false
                store.save()
                render()
            }
        }
        col.addv(restore, WRAP, WRAP)
    }

    if (reading.isEmpty()) {
        val card = vbox()
        card.background = shape(P.card, dp(22).toFloat(), P.line, dp(1))
        card.setPadding(dp(18), dp(18), dp(18), dp(18))
        card.addv(tv(if (hidden.isEmpty()) "Nenhuma obra em leitura" else "Nenhum destaque no momento", 16f, P.text, true))
        card.addv(
            tv(
                if (hidden.isEmpty()) "Marque uma obra como Lendo para ela aparecer aqui em destaque, com botão de continuar."
                else "Suas obras em leitura estão ocultas dos destaques. Toque em Ocultas dos destaques para trazer de volta.",
                13.5f, P.sub
            ),
            MATCH, WRAP, 0f, 0, 6, 0, 12
        )
        card.addv(outlinePill("Abrir Biblioteca", icon = Ic.Library) { goTop("library") })
        col.addv(card, MATCH, WRAP, 0f, 0, 14, 0, 0)
        restoreLink()
        return
    }

    var pageW = 0
    var pageH = 0
    val rv = RecyclerView(this)
    val lm = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    rv.layoutManager = lm
    rv.overScrollMode = View.OVER_SCROLL_NEVER
    rv.clipToPadding = false
    PagerSnapHelper().attachToRecyclerView(rv)

    val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = reading.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val f = FrameLayout(parent.context)
            f.setPadding(parent.context.dp(6), 0, parent.context.dp(6), 0)
            f.layoutParams = RecyclerView.LayoutParams(MATCH, MATCH)
            return object : RecyclerView.ViewHolder(f) {}
        }
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val f = holder.itemView as FrameLayout
            f.removeAllViews()
            f.addView(highlightCard(reading[position], pageW, pageH), FrameLayout.LayoutParams(MATCH, MATCH))
        }
    }
    rv.adapter = adapter
    col.addv(rv, MATCH, dp(480), 0f, -6, 14, -6, 0)

    // indicador visual de páginas
    val dots = PageDots(this)
    dots.count = reading.size
    if (reading.size > 1) col.addv(dots, MATCH, dp(26), 0f, 0, 6, 0, 0)
    restoreLink()

    fun updateDots() {
        val first = lm.findFirstVisibleItemPosition()
        if (first < 0) return
        val v = lm.findViewByPosition(first) ?: return
        val frac = if (v.width > 0) -v.left.toFloat() / v.width else 0f
        dots.set(first + frac)
    }
    rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            updateDots()
        }
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                val pos = lm.findFirstCompletelyVisibleItemPosition()
                if (pos >= 0) homePage = pos
            }
        }
    })
    val startPage = Math.min(homePage, reading.size - 1)
    rv.post {
        if (startPage > 0) lm.scrollToPosition(startPage)
        rv.post { updateDots() }
    }

    // altura = espaço visível entre a barra de pesquisa e a barra de navegação (menos o indicador)
    scroll.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            val sh = scroll.height
            if (sh <= 0 || anchor.bottom <= 0 || rv.width <= 0) return
            val extra = if (reading.size > 1) dp(32) else 0
            val h = Math.max(dp(380), sh - (anchor.bottom + dp(14)) - extra - dp(8))
            val w = rv.width - dp(12)
            if (pageH == h && pageW == w) return
            pageH = h
            pageW = w
            rv.layoutParams.height = h
            rv.requestLayout()
            adapter.notifyDataSetChanged()
        }
    })
}

private var homePage = 0

/** Bolinhas de página: a atual vira uma pílula comprida e a transição acompanha o dedo. */
class PageDots(ctx: Context) : View(ctx) {
    var count = 0
    private var pos = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d = ctx.resources.displayMetrics.density

    fun set(p: Float) {
        pos = p
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        if (count <= 1) return
        val small = 7 * d
        val big = 26 * d
        val gap = 7 * d
        val total = (count - 1) * (small + gap) + big
        val f = Math.min(1f, (width - 8 * d) / total)
        val active = P.accent
        val idle = tint(P.sub, 0x66)
        val ws = FloatArray(count) { i ->
            val t = 1f - Math.min(1f, Math.abs(i - pos))
            (small + (big - small) * t) * f
        }
        var x = (width - (ws.sum() + (count - 1) * gap * f)) / 2f
        val r = small * f / 2f
        val cy = height / 2f
        for (i in 0 until count) {
            val t = 1f - Math.min(1f, Math.abs(i - pos))
            paint.color = androidx.core.graphics.ColorUtils.blendARGB(idle, active, t)
            c.drawRoundRect(x, cy - r, x + ws[i], cy + r, r, r, paint)
            x += ws[i] + gap * f
        }
    }
}

/** Chip de status com a cor da categoria (mesmas cores da Início). */
private fun MainActivity.statusChip(label: String, c: Int, selected: Boolean, onClick: () -> Unit): View {
    val chip = hbox()
    chip.gravity = Gravity.CENTER_VERTICAL
    chip.setPadding(dp(13), dp(8), dp(14), dp(8))
    val r = dp(20).toFloat()
    chip.background = rippled(
        if (selected) shape(tint(c, 0x3A), r, c, dp(1)) else shape(tint(c, 0x14), r, tint(c, 0x55), dp(1)), r
    )
    val dot = View(this)
    dot.background = shape(c, dp(4).toFloat())
    chip.addv(dot, dp(8), dp(8), 0f, 0, 0, 7, 0)
    chip.addv(tv(label, 13f, if (selected) c else P.text, selected), WRAP, WRAP)
    chip.setOnClickListener { onClick() }
    chip.pressFx()
    return chip
}

/** Faixa de resumo: quantas obras em cada categoria; toque abre a Biblioteca já filtrada. */
private fun MainActivity.summaryStrip(col: LinearLayout) {
    val items = listOf(
        Triple(STATUS_READING, "Lendo", P.cReading),
        Triple(STATUS_PLAN, "Quero ler", P.cPlan),
        Triple(STATUS_DONE, "Concluídos", P.cDone),
        Triple(STATUS_PAUSED, "Pausados", P.cPaused),
        Triple(STATUS_DROPPED, "Dropados", P.cDropped),
        Triple(FAV, "Favoritos", P.cFav)
    )
    val hs = HorizontalScrollView(this)
    hs.isHorizontalScrollBarEnabled = false
    val row = hbox()
    for ((key, label, c) in items) {
        val n = if (key == FAV) store.works.count { it.favorite } else store.works.count { it.status == key }
        if (n == 0) continue
        val chip = hbox()
        chip.setPadding(dp(12), dp(8), dp(14), dp(8))
        val r = dp(20).toFloat()
        chip.background = rippled(shape(tint(c, 0x24), r, tint(c, 0x55), dp(1)), r)
        val dot = View(this)
        dot.background = shape(c, dp(4).toFloat())
        chip.addv(dot, dp(8), dp(8), 0f, 0, 0, 8, 0)
        chip.addv(tv("$label  $n", 12.5f, P.text, true), WRAP, WRAP)
        chip.setOnClickListener {
            fStatus = key
            fType = "Todos"
            fGenre = "Todos"
            fQuery = ""
            goTop("library")
        }
        chip.pressFx()
        row.addv(chip, WRAP, WRAP, 0f, 0, 0, 8, 0)
    }
    hs.addView(row)
    col.addv(hs, MATCH, WRAP, 0f, 0, 14, 0, 0)
}

fun MainActivity.buildHome(): View {
    val scroll = ScrollView(this)
    scroll.setBackgroundColor(P.bg)
    val col = vbox()
    col.setPadding(dp(16), dp(16), dp(16), dp(8))
    scroll.addView(col)

    val head = hbox()
    head.gravity = Gravity.CENTER_VERTICAL
    val logo = ImageView(this)
    logo.setImageResource(R.drawable.logo_mangadeck)
    head.addv(logo, dp(48), dp(48), 0f, 0, 0, 12, 0)
    val titles = vbox()
    titles.addv(tv("MangaDeck", 24f, P.text, true))
    head.addv(titles, 0, WRAP, 1f)
    head.addv(roundBtn(Ic.Globe, 42, 20) { openInBrowser(null, null, false) }, dp(42), dp(42), 0f, 0, 0, 8, 0)
    head.addv(roundBtn(Ic.Help, 42, 20) { go(Route("tutorial")) }, dp(42), dp(42))
    col.addv(head)

    val sf = hbox()
    sf.background = rippled(shape(P.card, dp(16).toFloat(), P.line, dp(1)), dp(16).toFloat())
    sf.setPadding(dp(14), dp(14), dp(14), dp(14))
    sf.addv(IconView(this, Ic.Search, P.sub, 20), dp(22), dp(22), 0f, 0, 0, 10, 0)
    sf.addv(tv("Pesquisar obras…", 14f, P.sub), WRAP, WRAP)
    sf.setOnClickListener {
        fQuery = ""
        focusSearch = true
        goTop("library")
    }
    col.addv(sf, MATCH, WRAP, 0f, 0, 14, 0, 0)

    if (store.works.isEmpty()) {
        val card = vbox()
        card.background = shape(P.card, dp(22).toFloat(), P.line, dp(1))
        card.setPadding(dp(18), dp(20), dp(18), dp(20))
        card.addv(iconTile(Ic.Library, 24), dp(48), dp(48), 0f, 0, 0, 0, 12)
        card.addv(tv("Sua estante está vazia", 18f, P.text, true))
        card.addv(
            tv("Cadastre sua primeira obra e acompanhe seu progresso. Ao tocar em Continuar lendo, o site abre aqui dentro, no navegador com abas.", 14f, P.sub),
            MATCH, WRAP, 0f, 0, 6, 0, 14
        )
        card.addv(pill("Adicionar primeira obra", icon = Ic.Plus) { goTop("add") })
        card.addv(outlinePill("Como usar o app", icon = Ic.Help) { go(Route("tutorial")) }, MATCH, WRAP, 0f, 0, 10, 0, 0)
        col.addv(card, MATCH, WRAP, 0f, 0, 18, 0, 0)
        return scroll
    }

    highlights(col, scroll, sf)
    return scroll
}

// ---------------- Biblioteca ----------------

fun MainActivity.filteredWorks(): List<Work> {
    val q = fQuery.trim()
    val list = store.works.filter {
        (fStatus == "Todos" || (fStatus == FAV && it.favorite) || it.status == fStatus) &&
            (fType == "Todos" || it.type == fType) &&
            (fGenre == "Todos" || it.genres.any { g -> g.equals(fGenre, true) }) &&
            (q.isEmpty() || listOf(
                it.title, it.altTitle, it.author,
                it.tags.joinToString(" "), it.genres.joinToString(" ")
            ).any { s -> s.contains(q, ignoreCase = true) })
    }
    return when (fSort) {
        SORTS[1] -> list.sortedBy { it.title.lowercase() }
        SORTS[2] -> list.sortedByDescending { it.rating }
        SORTS[3] -> list.sortedByDescending { it.progress() }
        SORTS[4] -> list.sortedByDescending { it.created }
        else -> list.sortedWith(compareByDescending<Work> { it.lastRead }.thenByDescending { it.created })
    }
}

class LibAdapter(private val act: MainActivity) : RecyclerView.Adapter<LibAdapter.VH>() {
    var items: List<Work> = emptyList()

    class VH(val box: FrameLayout) : RecyclerView.ViewHolder(box)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val f = FrameLayout(act)
        f.layoutParams = RecyclerView.LayoutParams(MATCH, WRAP)
        f.setPadding(act.dp(5), act.dp(5), act.dp(5), act.dp(5))
        return VH(f)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.box.removeAllViews()
        holder.box.addView(act.workCard(items[position], false))
    }

    override fun getItemCount(): Int = items.size
}

fun MainActivity.buildLibrary(): View {
    val root = vbox()
    root.setBackgroundColor(P.bg)
    root.setPadding(dp(12), dp(14), dp(12), 0)

    val head = hbox()
    head.addv(sectionTitle(Ic.Library, "Biblioteca", 22f), 0, WRAP, 1f)
    val countTv = tv("", 13f, P.sub, true)
    head.addv(countTv, WRAP, WRAP)
    root.addv(head, MATCH, WRAP, 0f, 4, 0, 4, 8)

    val (searchWrap, search) = searchBox("Pesquisar título, autor, tag…", fQuery)
    root.addv(searchWrap, MATCH, WRAP, 0f, 0, 0, 0, 10)

    val chipScroll = HorizontalScrollView(this)
    chipScroll.isHorizontalScrollBarEnabled = false
    val chipRow = hbox()
    chipScroll.addView(chipRow)
    root.addv(chipScroll, MATCH, WRAP, 0f, 0, 0, 0, 8)

    val filterRow = hbox()
    val typeBtn = outlinePill("", 12f) {}
    val genreBtn = outlinePill("", 12f) {}
    val sortBtn = outlinePill("", 12f) {}
    filterRow.addv(typeBtn, 0, WRAP, 1f, 0, 0, 6, 0)
    filterRow.addv(genreBtn, 0, WRAP, 1f, 0, 0, 6, 0)
    filterRow.addv(sortBtn, 0, WRAP, 1f)
    root.addv(filterRow, MATCH, WRAP, 0f, 0, 0, 0, 4)

    val adapter = LibAdapter(this)
    val rv = RecyclerView(this)
    val widthDp = resources.displayMetrics.widthPixels / resources.displayMetrics.density
    rv.layoutManager = GridLayoutManager(this, if (widthDp >= 600) 5 else 3)
    rv.clipToPadding = false
    rv.setPadding(0, dp(6), 0, dp(12))
    rv.adapter = adapter

    val empty = tv("Nenhuma obra encontrada.\nAjuste os filtros ou adicione uma obra em Adicionar.", 14f, P.sub)
    empty.gravity = Gravity.CENTER
    empty.setPadding(dp(20), dp(40), dp(20), dp(20))

    val listHolder = FrameLayout(this)
    listHolder.addView(rv, FrameLayout.LayoutParams(MATCH, MATCH))
    listHolder.addView(empty, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.TOP))
    root.addv(listHolder, MATCH, 0, 1f)

    fun refresh() {
        val items = filteredWorks()
        adapter.items = items
        adapter.notifyDataSetChanged()
        countTv.text = "${items.size} obra" + (if (items.size == 1) "" else "s")
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        typeBtn.setIconText(Ic.ChevronDown, "Tipo: $fType", true, 16)
        genreBtn.setIconText(Ic.ChevronDown, "Gênero: $fGenre", true, 16)
        sortBtn.setIconText(Ic.Sort, fSort, false, 16)
    }

    fun buildChips() {
        chipRow.removeAllViews()
        val opts = listOf("Todos", STATUS_READING, STATUS_PLAN, STATUS_DONE, STATUS_PAUSED, STATUS_DROPPED, FAV)
        for (o in opts) {
            val click = {
                fStatus = o
                buildChips()
                refresh()
            }
            val v: View = if (o == "Todos") chip(o, o == fStatus, null, click)
            else statusChip(o, if (o == FAV) P.cFav else statusColor(o), o == fStatus, click)
            chipRow.addv(v, WRAP, WRAP, 0f, 0, 0, 8, 0)
        }
    }

    typeBtn.setOnClickListener {
        val opts = listOf("Todos") + TYPES
        listDialog("Tipo", opts) { i ->
            fType = opts[i]
            refresh()
        }
    }
    genreBtn.setOnClickListener {
        val used = store.works.flatMap { it.genres }
        val all = (PRESET_GENRES + used).distinctBy { it.lowercase() }.sorted()
        val opts = listOf("Todos") + all
        listDialog("Gênero", opts) { i ->
            fGenre = opts[i]
            refresh()
        }
    }
    sortBtn.setOnClickListener {
        listDialog("Ordenar por", SORTS) { i ->
            fSort = SORTS[i]
            refresh()
        }
    }
    search.doAfterTextChanged {
        fQuery = it?.toString() ?: ""
        refresh()
    }

    buildChips()
    refresh()

    if (focusSearch) {
        focusSearch = false
        search.requestFocus()
        search.postDelayed({
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(search, 0)
        }, 250)
    }
    return root
}
