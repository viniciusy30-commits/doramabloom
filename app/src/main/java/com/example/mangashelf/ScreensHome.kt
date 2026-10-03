package com.example.mangashelf

import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

fun MainActivity.workCard(w: Work, play: Boolean): View {
    val col = vbox()
    val box = FrameLayout(this)
    box.addView(coverView(store, w), FrameLayout.LayoutParams(MATCH, WRAP))

    if (w.favorite) {
        val h = tv("♥", 13f, Color.WHITE, true)
        h.setPadding(dp(6), dp(2), dp(6), dp(2))
        h.background = shape(P.accent2, dp(10).toFloat())
        val lp = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.END)
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
        val p = tv("▶", 14f, Color.WHITE, true)
        p.gravity = Gravity.CENTER
        p.background = shape(P.accent, dp(18).toFloat())
        p.setOnClickListener {
            if (!continueReading(w)) {
                toast("Salve um site em LINKS primeiro 🔗")
                go(Route("detail", w.id))
            }
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
    val sub = tv("${w.type} · ${w.status}", 11f, P.sub)
    sub.maxLines = 1
    sub.ellipsize = TextUtils.TruncateAt.END
    col.addv(sub, MATCH, WRAP, 0f, 2, 3, 2, 0)

    col.setOnClickListener { go(Route("detail", w.id)) }
    return col
}

private fun MainActivity.rail(col: LinearLayout, title: String, list: List<Work>, filter: String?, play: Boolean = false) {
    if (list.isEmpty()) return
    val head = hbox()
    head.addv(tv(title, 18f, P.text, true), 0, WRAP, 1f)
    if (filter != null) {
        val more = tv("Ver tudo ›", 13f, P.accent, true)
        more.setPadding(dp(8), dp(4), 0, dp(4))
        more.setOnClickListener {
            fStatus = filter
            fType = "Todos"
            fGenre = "Todos"
            fQuery = ""
            goTop("library")
        }
        head.addv(more, WRAP, WRAP)
    }
    col.addv(head, MATCH, WRAP, 0f, 0, 22, 0, 10)
    val hs = HorizontalScrollView(this)
    hs.isHorizontalScrollBarEnabled = false
    val row = hbox()
    row.gravity = Gravity.TOP
    for (w in list.take(15)) row.addv(workCard(w, play), dp(112), WRAP, 0f, 0, 0, 12, 0)
    hs.addView(row)
    col.addv(hs, MATCH, WRAP)
}

fun MainActivity.buildHome(): View {
    val scroll = ScrollView(this)
    scroll.setBackgroundColor(P.bg)
    val col = vbox()
    col.setPadding(dp(16), dp(16), dp(16), dp(24))
    scroll.addView(col)

    val head = hbox()
    val titles = vbox()
    titles.addv(tv("📖 Minha Estante", 26f, P.text, true))
    titles.addv(tv("${store.works.size} obras na sua biblioteca", 13f, P.sub), MATCH, WRAP, 0f, 0, 2, 0, 0)
    head.addv(titles, 0, WRAP, 1f)
    val web = tv("🌐", 24f)
    web.setPadding(dp(10), dp(6), dp(10), dp(6))
    web.setOnClickListener { openInBrowser(null, null, false) }
    head.addv(web, WRAP, WRAP)
    val help = tv("❓", 22f)
    help.setPadding(dp(6), dp(6), dp(2), dp(6))
    help.setOnClickListener { go(Route("tutorial")) }
    head.addv(help, WRAP, WRAP)
    col.addv(head)

    val sf = hbox()
    sf.background = shape(P.card, dp(14).toFloat(), P.line, dp(1))
    sf.setPadding(dp(14), dp(12), dp(14), dp(12))
    sf.addv(tv("🔍  Pesquisar obras…", 14f, P.sub), WRAP, WRAP)
    sf.setOnClickListener {
        fQuery = ""
        focusSearch = true
        goTop("library")
    }
    col.addv(sf, MATCH, WRAP, 0f, 0, 14, 0, 0)

    if (store.works.isEmpty()) {
        val card = vbox()
        card.background = shape(P.card, dp(18).toFloat(), P.line, dp(1))
        card.setPadding(dp(18), dp(20), dp(18), dp(20))
        card.addv(tv("Sua estante está vazia 📚", 18f, P.text, true))
        card.addv(
            tv("Cadastre sua primeira obra e acompanhe seu progresso. Ao tocar em Continuar lendo, o site abre aqui dentro, no navegador com abas.", 14f, P.sub),
            MATCH, WRAP, 0f, 0, 6, 0, 14
        )
        card.addv(pill("➕  Adicionar primeira obra") { goTop("add") })
        card.addv(outlinePill("❓  Como usar o app") { go(Route("tutorial")) }, MATCH, WRAP, 0f, 0, 10, 0, 0)
        col.addv(card, MATCH, WRAP, 0f, 0, 18, 0, 0)
        return scroll
    }

    val reading = store.works.filter { it.status == STATUS_READING }
    val cont = reading.filter { it.lastRead > 0 }.sortedByDescending { it.lastRead }
    rail(col, "▶ Continuar lendo", cont, null, true)
    rail(col, "📖 Lendo", reading.sortedBy { it.title.lowercase() }, STATUS_READING)
    rail(col, "🔖 Quero ler", store.works.filter { it.status == STATUS_PLAN }, STATUS_PLAN)
    rail(col, "✅ Concluídos", store.works.filter { it.status == STATUS_DONE }, STATUS_DONE)
    rail(col, "⏸ Pausados", store.works.filter { it.status == STATUS_PAUSED }, STATUS_PAUSED)
    rail(col, "♥ Favoritos", store.works.filter { it.favorite }, FAV)
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
    head.addv(tv("📚 Biblioteca", 22f, P.text, true), 0, WRAP, 1f)
    val countTv = tv("", 13f, P.sub, true)
    head.addv(countTv, WRAP, WRAP)
    root.addv(head, MATCH, WRAP, 0f, 4, 0, 4, 8)

    val search = inputField(
        "🔍 Pesquisar título, autor, tag…", fQuery, 1,
        android.text.InputType.TYPE_CLASS_TEXT
    )
    root.addv(search, MATCH, WRAP, 0f, 0, 0, 0, 8)

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

    val empty = tv("Nenhuma obra encontrada.\nAjuste os filtros ou adicione uma obra em ➕.", 14f, P.sub)
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
        typeBtn.text = "Tipo: $fType ▾"
        genreBtn.text = "Gênero: $fGenre ▾"
        sortBtn.text = "↕ $fSort"
    }

    fun buildChips() {
        chipRow.removeAllViews()
        val opts = listOf("Todos", STATUS_READING, STATUS_PLAN, STATUS_DONE, STATUS_PAUSED, FAV)
        for (o in opts) {
            chipRow.addv(chip(o, o == fStatus) {
                fStatus = o
                buildChips()
                refresh()
            }, WRAP, WRAP, 0f, 0, 0, 8, 0)
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
