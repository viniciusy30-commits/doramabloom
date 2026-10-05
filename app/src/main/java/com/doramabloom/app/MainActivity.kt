package com.doramabloom.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private class NavItem(val box: LinearLayout, val icon: IconView, val text: TextView)

    private lateinit var contentFrame: FrameLayout
    private val navItems = ArrayList<NavItem>()
    private var tab = 0

    private var statusFilter = "all"
    private var genreFilter = "all"
    private var countryFilter = "all"
    private var query = ""
    private var sortMode = 0
    private var gridMode = false
    private var filtersOpen = false

    private var listAdapter: DramaAdapter? = null
    private var listEmpty: TextView? = null
    private var listInfo: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)

        val root = FrameLayout(this)
        root.background = gradient(Palette.bgTop, Palette.bgBottom)

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        root.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))

        col.addView(buildHeader(), lin(MATCH, WRAP))
        contentFrame = FrameLayout(this)
        col.addView(contentFrame, lin(MATCH, 0, 1f))
        col.addView(buildNav(), lin(MATCH, WRAP))

        root.addView(
            PetalsView(this, listOf("petal", "petal", "blossom"), Palette.pink, 14),
            FrameLayout.LayoutParams(MATCH, MATCH)
        )

        val fab = FrameLayout(this)
        fab.background = ovalGradient(Color.parseColor("#FF8FB7"), Color.parseColor("#FF5C93"))
        fab.elevation = dp(10).toFloat()
        fab.addView(IconView(this, "add", Color.WHITE, 28), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
        fab.setOnClickListener { startActivity(Intent(this, EditActivity::class.java)) }
        val flp = FrameLayout.LayoutParams(dp(62), dp(62))
        flp.gravity = Gravity.BOTTOM or Gravity.END
        flp.setMargins(0, 0, dp(22), dp(102))
        root.addView(fab, flp)

        setContentView(root)

        if (!Store.askedName) {
            root.post { askName() }
        }
    }

    override fun onResume() {
        super.onResume()
        showTab(tab)
    }

    // ------------------------------------------------------- cabeçalho e menu

    private fun buildHeader(): View {
        val f = FrameLayout(this)
        val r = dp(30).toFloat()
        val bg = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#FFDCE9"), Color.parseColor("#FFC2D8"))
        )
        bg.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
        f.background = bg
        f.elevation = dp(4).toFloat()
        f.addView(HeaderArt(this), FrameLayout.LayoutParams(MATCH, MATCH))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(22), dp(8), dp(22), dp(18))
        row.addView(IconView(this, "blossom", Color.WHITE, 34))
        val txt = LinearLayout(this)
        txt.orientation = LinearLayout.VERTICAL
        txt.addView(label("Dorama Bloom", 27f, Palette.pinkDark, true, true))
        txt.addView(label("minha estante de doramas", 12f, Palette.pinkDark))
        row.addView(txt, lin(WRAP, WRAP, l = 10))
        f.addView(row, FrameLayout.LayoutParams(MATCH, WRAP))
        return f
    }

    private fun buildNav(): View {
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.setPadding(dp(8), dp(8), dp(8), dp(8))
        bar.background = roundRect(Color.WHITE, dp(30).toFloat(), Palette.line, dp(1))
        bar.elevation = dp(8).toFloat()

        val holder = FrameLayout(this)
        holder.clipToPadding = false
        holder.clipChildren = false
        holder.setPadding(dp(14), dp(4), dp(14), dp(12))
        holder.addView(bar, FrameLayout.LayoutParams(MATCH, WRAP))

        navItems.clear()
        val icons = listOf("home", "grid", "chart")
        val names = listOf("Início", "Estante", "Números")
        for (i in icons.indices) {
            val box = LinearLayout(this)
            box.orientation = LinearLayout.VERTICAL
            box.gravity = Gravity.CENTER
            box.setPadding(dp(4), dp(8), dp(4), dp(8))
            val ic = IconView(this, icons[i], Palette.muted, 22)
            val tx = label(names[i], 11f, Palette.muted, true)
            box.addView(ic, lin(WRAP, WRAP))
            box.addView(tx, lin(WRAP, WRAP, t = 2))
            box.setOnClickListener { showTab(i) }
            bar.addView(box, lin(0, WRAP, 1f, l = 2, r = 2))
            navItems.add(NavItem(box, ic, tx))
        }
        return holder
    }

    private fun updateNav() {
        for (i in navItems.indices) {
            val sel = i == tab
            val n = navItems[i]
            if (sel) n.box.background = roundRect(Palette.pink, dp(22).toFloat()) else n.box.background = null
            n.icon.tint = if (sel) Color.WHITE else Palette.muted
            n.text.setTextColor(if (sel) Color.WHITE else Palette.muted)
        }
    }

    private fun showTab(t: Int) {
        tab = t
        contentFrame.removeAllViews()
        val v: View = when (t) {
            0 -> buildHome()
            1 -> buildListTab()
            else -> buildStats()
        }
        contentFrame.addView(v, FrameLayout.LayoutParams(MATCH, MATCH))
        updateNav()
    }

    private fun open(d: Drama) {
        val i = Intent(this, DetailActivity::class.java)
        i.putExtra("id", d.id)
        startActivity(i)
    }

    private fun section(t: String, icon: String): View {
        val v = sectionTitle(t, icon)
        v.setPadding(dp(4), dp(20), 0, dp(10))
        return v
    }

    // ---------------------------------------------------------------- INÍCIO

    private fun buildHome(): View {
        val all = Store.all()
        val watching = all.filter { it.status == "assistindo" }
        val done = all.filter { it.status == "concluido" }

        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(14), dp(16), dp(160))
        sv.addView(col)

        // cartão de boas-vindas
        val name = Store.userName
        val hiWrap = FrameLayout(this)
        hiWrap.background = gradient(
            Color.parseColor("#FF9DBF"), Color.parseColor("#FF6B9D"),
            dp(28).toFloat(), GradientDrawable.Orientation.TL_BR
        )
        hiWrap.elevation = dp(4).toFloat()
        val deco = IconView(this, "blossom", Color.parseColor("#44FFFFFF"), 96)
        val dlp = FrameLayout.LayoutParams(WRAP, WRAP)
        dlp.gravity = Gravity.END or Gravity.TOP
        dlp.setMargins(0, dp(-14), dp(-10), 0)
        hiWrap.addView(deco, dlp)
        val deco2 = IconView(this, "petal", Color.parseColor("#55FFFFFF"), 30)
        val dlp2 = FrameLayout.LayoutParams(WRAP, WRAP)
        dlp2.gravity = Gravity.END or Gravity.BOTTOM
        dlp2.setMargins(0, 0, dp(70), dp(16))
        hiWrap.addView(deco2, dlp2)

        val hi = LinearLayout(this)
        hi.orientation = LinearLayout.VERTICAL
        hi.setPadding(dp(20), dp(18), dp(20), dp(18))
        hi.addView(label(if (name.isBlank()) "Oi, bem-vinda!" else "Oi, $name!", 25f, Color.WHITE, true, true))
        hi.addView(label("O que vamos assistir hoje?", 13f, Color.WHITE), lin(WRAP, WRAP, t = 2))
        val stats = LinearLayout(this)
        stats.orientation = LinearLayout.HORIZONTAL
        val glass = Color.parseColor("#44FFFFFF")
        stats.addView(pill(all.size.toString() + " doramas", glass, Color.WHITE, 11.5f, "heart"), lin(WRAP, WRAP, r = 6))
        stats.addView(pill(watching.size.toString() + " vendo", glass, Color.WHITE, 11.5f, "play"), lin(WRAP, WRAP, r = 6))
        stats.addView(pill(done.size.toString() + " fim", glass, Color.WHITE, 11.5f, "check"), lin(WRAP, WRAP))
        hi.addView(stats, lin(WRAP, WRAP, t = 12))
        hiWrap.addView(hi, FrameLayout.LayoutParams(MATCH, WRAP))
        hiWrap.setOnClickListener { askName() }
        col.addView(hiWrap, lin(MATCH, WRAP))

        // assistindo agora
        col.addView(section("Assistindo agora", "play"))
        if (watching.isEmpty()) {
            val e = card(16, 22)
            e.addView(label("Nada em andamento ainda", 15f, Palette.text, true, true))
            e.addView(label("Toque no botão + para começar um dorama novo!", 13f, Palette.muted), lin(WRAP, WRAP, t = 4))
            col.addView(e, lin(MATCH, WRAP))
        } else {
            val rv = RecyclerView(this)
            rv.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
            rv.clipToPadding = false
            val ad = DramaAdapter(1, { open(it) }, { d ->
                val finished = Store.bump(d)
                if (finished) {
                    Toast.makeText(this, "Parabéns, você terminou " + d.title + "!", Toast.LENGTH_LONG).show()
                }
                val lm = rv.layoutManager as LinearLayoutManager
                val pos = lm.findFirstVisibleItemPosition()
                rv.adapter?.notifyDataSetChanged()
                if (pos >= 0) lm.scrollToPosition(pos)
            })
            ad.submit(watching)
            rv.adapter = ad
            PagerSnapHelper().attachToRecyclerView(rv)
            col.addView(rv, lin(MATCH, WRAP))

            if (watching.size > 1) {
                val dots = LinearLayout(this)
                dots.gravity = Gravity.CENTER_HORIZONTAL
                fun setDots(active: Int) {
                    dots.removeAllViews()
                    for (i in watching.indices) {
                        val dot = View(this)
                        dot.background = roundRect(
                            if (i == active) Palette.pink else Color.parseColor("#F3C6D8"),
                            dp(4).toFloat()
                        )
                        dots.addView(dot, lin(dp(if (i == active) 18 else 8), dp(8), l = 3, r = 3))
                    }
                }
                setDots(0)
                col.addView(dots, lin(MATCH, WRAP, t = 2))
                rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(r: RecyclerView, newState: Int) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            val lm = r.layoutManager as LinearLayoutManager
                            val p = lm.findFirstCompletelyVisibleItemPosition()
                            if (p >= 0) setDots(p)
                        }
                    }
                })
            }
        }

        addMiniRow(col, "Quero ver", "bookmark", all.filter { it.status == "quero" })
        addMiniRow(col, "Favoritos", "heart", all.filter { it.favorite })
        addMiniRow(col, "Concluídos", "check", done)

        if (all.isEmpty()) {
            val e = card(18, 24)
            e.addView(label("Sua estante está vazia", 17f, Palette.text, true, true))
            e.addView(
                label("Toque no botão + para adicionar seu primeiro dorama, com capa, nota, temporadas e muito carinho.", 13f, Palette.muted),
                lin(WRAP, WRAP, t = 4)
            )
            col.addView(e, lin(MATCH, WRAP, t = 18))
        }
        return sv
    }

    private fun addMiniRow(col: LinearLayout, title: String, icon: String, list: List<Drama>) {
        if (list.isEmpty()) return
        col.addView(section(title, icon))
        val rv = RecyclerView(this)
        rv.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        val ad = DramaAdapter(2, { open(it) })
        ad.submit(list)
        rv.adapter = ad
        col.addView(rv, lin(MATCH, WRAP))
    }

    private fun askName() {
        val et = EditText(this)
        et.hint = "Seu nome"
        et.setSingleLine(true)
        et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        et.setText(Store.userName)
        val box = FrameLayout(this)
        box.setPadding(dp(22), dp(8), dp(22), 0)
        box.addView(et, FrameLayout.LayoutParams(MATCH, WRAP))
        AlertDialog.Builder(this)
            .setTitle("Como posso te chamar?")
            .setView(box)
            .setPositiveButton("Salvar") { _, _ ->
                Store.userName = et.text.toString().trim()
                Store.askedName = true
                showTab(tab)
            }
            .setNegativeButton("Depois") { _, _ ->
                Store.askedName = true
            }
            .show()
    }

    // --------------------------------------------------------------- ESTANTE

    private fun filtered(): List<Drama> {
        var l = Store.all()
        if (statusFilter == "fav") {
            l = l.filter { it.favorite }
        } else if (statusFilter != "all") {
            l = l.filter { it.status == statusFilter }
        }
        if (genreFilter != "all") l = l.filter { it.genre == genreFilter || it.tags.contains(genreFilter) }
        if (countryFilter != "all") l = l.filter { it.country == countryFilter }
        if (query.isNotBlank()) {
            l = l.filter {
                it.title.contains(query, true) || it.original.contains(query, true) ||
                    it.cast.contains(query, true) || it.platform.contains(query, true) ||
                    it.couple.contains(query, true)
            }
        }
        return when (sortMode) {
            0 -> l.sortedByDescending { it.addedAt }
            1 -> l.sortedBy { it.title.lowercase() }
            2 -> l.sortedByDescending { it.score }
            else -> l.sortedByDescending { it.year }
        }
    }

    private fun sortLabel(): String = when (sortMode) {
        0 -> "Recentes"
        1 -> "A–Z"
        2 -> "Melhor nota"
        else -> "Ano"
    }

    private fun refreshList() {
        val l = filtered()
        listAdapter?.submit(l)
        listEmpty?.visibility = if (l.isEmpty()) View.VISIBLE else View.GONE
        listEmpty?.text = if (Store.all().isEmpty()) {
            "Sua estante está vazia.\nToque no + para adicionar!"
        } else {
            "Nenhum dorama por aqui."
        }
        listInfo?.text = l.size.toString() + (if (l.size == 1) " dorama" else " doramas")
    }

    private fun applyView(rv: RecyclerView) {
        rv.layoutManager = if (gridMode) GridLayoutManager(this, 3) else LinearLayoutManager(this)
        listAdapter = DramaAdapter(if (gridMode) 3 else 0, { open(it) })
        rv.adapter = listAdapter
        refreshList()
    }

    private fun squareBtn(icon: String, selected: Boolean): FrameLayout {
        val f = FrameLayout(this)
        f.background = roundRect(if (selected) Palette.pink else Color.WHITE, dp(16).toFloat(), Palette.line, dp(1))
        f.addView(
            IconView(this, icon, if (selected) Color.WHITE else Palette.pink, 20),
            FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER)
        )
        return f
    }

    private fun buildListTab(): View {
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(14), dp(12), dp(14), 0)

        // busca + alternar lista/grade
        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val sbox = LinearLayout(this)
        sbox.orientation = LinearLayout.HORIZONTAL
        sbox.gravity = Gravity.CENTER_VERTICAL
        sbox.setPadding(dp(14), 0, dp(10), 0)
        sbox.background = roundRect(Color.WHITE, dp(24).toFloat(), Palette.line, dp(1))
        sbox.addView(IconView(this, "search", Palette.muted, 20))
        val search = EditText(this)
        search.hint = "Buscar título, elenco, plataforma"
        search.textSize = 14f
        search.setSingleLine(true)
        search.setTextColor(Palette.text)
        search.setHintTextColor(Palette.muted)
        search.background = null
        search.setText(query)
        search.doAfterTextChanged {
            query = it?.toString() ?: ""
            refreshList()
        }
        sbox.addView(search, lin(0, WRAP, 1f, l = 8))
        top.addView(sbox, lin(0, dp(46), 1f))

        val listBtn = squareBtn("list", !gridMode)
        val gridBtn = squareBtn("grid", gridMode)
        top.addView(listBtn, lin(dp(46), dp(46), l = 8))
        top.addView(gridBtn, lin(dp(46), dp(46), l = 6))
        col.addView(top, lin(MATCH, WRAP))

        // status
        val statusOpts = ArrayList<Opt>()
        statusOpts.add(Opt("all", "Todos", Palette.pink, "heart"))
        statusOpts.add(Opt("fav", "Favoritos", Palette.pink, "star"))
        for (s in Statuses.all) statusOpts.add(Opt(s.key, s.label, s.color, s.icon))
        col.addView(chipScroller(statusOpts, statusFilter) {
            statusFilter = it
            refreshList()
        }, lin(MATCH, WRAP, t = 10))

        // filtros extras (gênero e país)
        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.visibility = if (filtersOpen) View.VISIBLE else View.GONE
        val genreOpts = ArrayList<Opt>()
        genreOpts.add(Opt("all", "Todos os gêneros", Palette.pink, "tag"))
        for (g in Genres.all) genreOpts.add(Opt(g.key, g.label, g.primary, g.icon))
        panel.addView(chipScroller(genreOpts, genreFilter) {
            genreFilter = it
            refreshList()
        }, lin(MATCH, WRAP, t = 4))
        val countryOpts = ArrayList<Opt>()
        countryOpts.add(Opt("all", "Todos os países", Palette.pink, "flag"))
        for (c in countries) countryOpts.add(Opt(c, c, Palette.pinkDark, "flag"))
        panel.addView(chipScroller(countryOpts, countryFilter) {
            countryFilter = it
            refreshList()
        }, lin(MATCH, WRAP, t = 4))
        col.addView(panel, lin(MATCH, WRAP))

        // contagem + filtros + ordenar
        val info = LinearLayout(this)
        info.orientation = LinearLayout.HORIZONTAL
        info.gravity = Gravity.CENTER_VERTICAL
        val count = label("", 13f, Palette.muted, true)
        listInfo = count
        info.addView(count, lin(0, WRAP, 1f))
        val fb = pill("Filtros", Color.WHITE, Palette.pink, 12f, "filter")
        fb.setOnClickListener {
            filtersOpen = !filtersOpen
            panel.visibility = if (filtersOpen) View.VISIBLE else View.GONE
        }
        info.addView(fb, lin(WRAP, WRAP, r = 6))
        val sb = pill(sortLabel(), Color.WHITE, Palette.pink, 12f, "sort")
        sb.setOnClickListener {
            sortMode = (sortMode + 1) % 4
            sb.text = sortLabel()
            refreshList()
        }
        info.addView(sb, lin(WRAP, WRAP))
        col.addView(info, lin(MATCH, WRAP, t = 8, b = 2))

        val rv = RecyclerView(this)
        rv.clipToPadding = false
        rv.setPadding(0, dp(4), 0, dp(160))

        val empty = label("", 15f, Palette.muted, true, true)
        empty.gravity = Gravity.CENTER
        listEmpty = empty

        val frame = FrameLayout(this)
        frame.addView(rv, FrameLayout.LayoutParams(MATCH, MATCH))
        frame.addView(empty, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
        col.addView(frame, lin(MATCH, 0, 1f))

        listBtn.setOnClickListener {
            if (gridMode) {
                gridMode = false
                showTab(1)
            }
        }
        gridBtn.setOnClickListener {
            if (!gridMode) {
                gridMode = true
                showTab(1)
            }
        }

        applyView(rv)
        return col
    }

    // --------------------------------------------------------------- NÚMEROS

    private fun statBox(icon: String, value: String, name: String): LinearLayout {
        val b = card(12, 20)
        b.gravity = Gravity.CENTER_HORIZONTAL
        b.addView(IconView(this, icon, Palette.pink, 22))
        b.addView(label(value, 19f, Palette.pinkDark, true, true), lin(WRAP, WRAP, t = 4))
        b.addView(label(name, 11f, Palette.muted), lin(WRAP, WRAP, t = 2))
        return b
    }

    private fun barRow(name: String, n: Int, frac: Float, color: Int): View {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.addView(label(name, 13f, Palette.text, true), lin(dp(96), WRAP))
        val bar = SoftBar(this)
        bar.progress = frac
        bar.barColor = color
        r.addView(bar, lin(0, dp(10), 1f, l = 4, r = 8))
        r.addView(label(n.toString(), 13f, Palette.muted, true), lin(dp(24), WRAP))
        return r
    }

    private fun buildStats(): View {
        val all = Store.all()
        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(14), dp(16), dp(160))
        sv.addView(col)

        val rated = all.filter { it.score > 0 }
        val avg = if (rated.isEmpty()) 0.0 else rated.map { it.score }.sum().toDouble() / rated.size
        val eps = all.map { watchedEps(it) }.sum()
        val hours = all.map { hoursWatched(it) }.sum()

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.addView(statBox("heart", all.size.toString(), "doramas"), lin(0, WRAP, 1f, r = 6))
        top.addView(statBox("play", eps.toString(), "episódios"), lin(0, WRAP, 1f, r = 6))
        top.addView(statBox("clock", "%.0f h".format(hours), "assistidas"), lin(0, WRAP, 1f, r = 6))
        top.addView(statBox("star", if (rated.isEmpty()) "-" else "%.1f".format(avg), "nota média"), lin(0, WRAP, 1f))
        col.addView(top, lin(MATCH, WRAP))

        if (all.isEmpty()) {
            val e = card(16, 22)
            e.addView(label("Ainda sem números", 16f, Palette.text, true, true))
            e.addView(label("Adicione doramas e acompanhe suas estatísticas aqui.", 13f, Palette.muted), lin(WRAP, WRAP, t = 4))
            col.addView(e, lin(MATCH, WRAP, t = 16))
        } else {
            val byGenre = all.groupBy { it.genre }.entries.sortedByDescending { it.value.size }
            val fav = Genres.byKey(byGenre[0].key)
            val favCard = card(16, 24, fav.soft)
            val favRow = LinearLayout(this)
            favRow.orientation = LinearLayout.HORIZONTAL
            favRow.gravity = Gravity.CENTER_VERTICAL
            favRow.addView(IconView(this, fav.icon, fav.primary, 38))
            val ft = LinearLayout(this)
            ft.orientation = LinearLayout.VERTICAL
            ft.addView(label("Seu gênero favorito", 11.5f, Palette.muted))
            ft.addView(label(fav.label, 22f, fav.dark, true, true))
            ft.addView(label(fav.tagline, 12f, Palette.muted))
            favRow.addView(ft, lin(WRAP, WRAP, l = 14))
            favCard.addView(favRow)
            col.addView(favCard, lin(MATCH, WRAP, t = 14))

            col.addView(section("Por status", "bookmark"))
            val sc = card(14, 22)
            val maxN = maxOf(1, all.size)
            for (s in Statuses.all) {
                val n = all.count { it.status == s.key }
                sc.addView(barRow(s.label, n, n.toFloat() / maxN, s.color), lin(MATCH, WRAP, t = 6))
            }
            col.addView(sc, lin(MATCH, WRAP))

            col.addView(section("Por gênero", "tag"))
            val gc = card(14, 22)
            val maxG = maxOf(1, byGenre[0].value.size)
            for (e in byGenre) {
                val g = Genres.byKey(e.key)
                gc.addView(barRow(g.label, e.value.size, e.value.size.toFloat() / maxG, g.primary), lin(MATCH, WRAP, t = 6))
            }
            col.addView(gc, lin(MATCH, WRAP))

            col.addView(section("Por país", "flag"))
            val cc = card(14, 22)
            val byCountry = all.groupBy { it.country }.entries.sortedByDescending { it.value.size }
            val maxC = maxOf(1, byCountry[0].value.size)
            for (e in byCountry) {
                cc.addView(barRow(e.key, e.value.size, e.value.size.toFloat() / maxC, Palette.pinkDark), lin(MATCH, WRAP, t = 6))
            }
            col.addView(cc, lin(MATCH, WRAP))

            if (rated.isNotEmpty()) {
                col.addView(section("Distribuição das notas", "star"))
                val dc = card(14, 22)
                val maxS = maxOf(1, (1..10).map { s -> rated.count { it.score == s } }.maxOrNull() ?: 1)
                for (s in 10 downTo 1) {
                    val n = rated.count { it.score == s }
                    dc.addView(barRow("Nota $s", n, n.toFloat() / maxS, Palette.pink), lin(MATCH, WRAP, t = 4))
                }
                col.addView(dc, lin(MATCH, WRAP))

                col.addView(section("Mais bem avaliados", "heart"))
                val tc = card(14, 22)
                for (d in rated.sortedByDescending { it.score }.take(5)) {
                    val r = LinearLayout(this)
                    r.orientation = LinearLayout.HORIZONTAL
                    r.gravity = Gravity.CENTER_VERTICAL
                    r.setPadding(0, dp(6), 0, dp(6))
                    val t = label(d.title, 14f, Palette.text, true)
                    t.maxLines = 1
                    t.ellipsize = android.text.TextUtils.TruncateAt.END
                    r.addView(t, lin(0, WRAP, 1f))
                    val rv = RatingView(this, 14, false)
                    rv.score = d.score
                    rv.color = Genres.byKey(d.genre).primary
                    r.addView(rv, lin(WRAP, WRAP, l = 8))
                    r.setOnClickListener { open(d) }
                    tc.addView(r, lin(MATCH, WRAP))
                }
                col.addView(tc, lin(MATCH, WRAP))
            }
        }

        col.addView(section("Backup", "download"))
        val bc = card(14, 22)
        bc.addView(
            label(
                "Guarde uma cópia da sua lista (sem as capas) para levar a outro celular, ou restaure uma cópia.",
                12f, Palette.muted
            )
        )
        val brow = LinearLayout(this)
        brow.orientation = LinearLayout.HORIZONTAL
        val exp = pill("Exportar", Palette.pink, Color.WHITE, 13f, "upload")
        exp.setOnClickListener { exportBackup() }
        val imp = pill("Importar", Color.parseColor("#C38BD8"), Color.WHITE, 13f, "download")
        imp.setOnClickListener { importBackup() }
        brow.addView(exp, lin(WRAP, WRAP, r = 8))
        brow.addView(imp, lin(WRAP, WRAP))
        bc.addView(brow, lin(WRAP, WRAP, t = 10))
        col.addView(bc, lin(MATCH, WRAP))

        return sv
    }

    private fun exportBackup() {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_TEXT, Store.exportJson())
        startActivity(Intent.createChooser(i, "Salvar backup"))
    }

    private fun importBackup() {
        val et = EditText(this)
        et.hint = "Cole aqui o backup"
        et.minLines = 4
        et.gravity = Gravity.TOP or Gravity.START
        val box = FrameLayout(this)
        box.setPadding(dp(22), dp(8), dp(22), 0)
        box.addView(et, FrameLayout.LayoutParams(MATCH, WRAP))
        AlertDialog.Builder(this)
            .setTitle("Importar backup")
            .setView(box)
            .setPositiveButton("Importar") { _, _ ->
                val n = Store.importJson(et.text.toString())
                if (n < 0) {
                    Toast.makeText(this, "Esse texto não parece um backup.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, n.toString() + " doramas adicionados.", Toast.LENGTH_LONG).show()
                    showTab(2)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
