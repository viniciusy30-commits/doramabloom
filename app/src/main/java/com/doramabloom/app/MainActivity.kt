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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var contentFrame: FrameLayout
    private val navViews = ArrayList<TextView>()
    private var tab = 0

    private var statusFilter = "all"
    private var genreFilter = "all"
    private var query = ""
    private var sortMode = 0

    private var listAdapter: DramaAdapter? = null
    private var listEmpty: TextView? = null
    private var listInfo: TextView? = null
    private var sortBtn: TextView? = null

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

        root.addView(PetalsView(this, listOf("🌸"), 16), FrameLayout.LayoutParams(MATCH, MATCH))

        val fabBg = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#FF8FB7"), Color.parseColor("#FF5C93")))
        fabBg.shape = GradientDrawable.OVAL
        val fab = TextView(this)
        fab.text = "+"
        fab.textSize = 32f
        fab.setTextColor(Color.WHITE)
        fab.gravity = Gravity.CENTER
        fab.background = fabBg
        fab.elevation = dp(10).toFloat()
        fab.setOnClickListener { startActivity(Intent(this, EditActivity::class.java)) }
        val flp = FrameLayout.LayoutParams(dp(62), dp(62))
        flp.gravity = Gravity.BOTTOM or Gravity.END
        flp.setMargins(0, 0, dp(22), dp(100))
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

    private fun buildHeader(): View {
        val h = LinearLayout(this)
        h.orientation = LinearLayout.HORIZONTAL
        h.gravity = Gravity.CENTER_VERTICAL
        h.setPadding(dp(20), dp(10), dp(20), dp(6))
        h.addView(label("Dorama Bloom", 28f, Palette.pink, true, true))
        h.addView(label("🌸", 26f), lin(WRAP, WRAP, l = 8))
        return h
    }

    private fun buildNav(): View {
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.setPadding(dp(8), dp(8), dp(8), dp(8))
        bar.background = roundRect(Color.WHITE, dp(30).toFloat())
        bar.elevation = dp(8).toFloat()

        val holder = FrameLayout(this)
        holder.clipToPadding = false
        holder.clipChildren = false
        holder.setPadding(dp(14), dp(4), dp(14), dp(12))
        holder.addView(bar, FrameLayout.LayoutParams(MATCH, WRAP))

        navViews.clear()
        val items = listOf("🏠" to "Início", "📖" to "Lista", "📊" to "Números")
        for (i in items.indices) {
            val tv = label(items[i].first + "\n" + items[i].second, 12f, Palette.muted, true)
            tv.gravity = Gravity.CENTER
            tv.setPadding(dp(4), dp(8), dp(4), dp(8))
            tv.setOnClickListener { showTab(i) }
            navViews.add(tv)
            bar.addView(tv, lin(0, WRAP, 1f, l = 2, r = 2))
        }
        return holder
    }

    private fun updateNav() {
        for (i in navViews.indices) {
            val sel = i == tab
            val tv = navViews[i]
            if (sel) tv.background = roundRect(Palette.pink, dp(22).toFloat()) else tv.background = null
            tv.setTextColor(if (sel) Color.WHITE else Palette.muted)
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

    private fun section(t: String): TextView {
        val v = label(t, 18f, Palette.text, true, true)
        v.setPadding(dp(4), dp(18), 0, dp(8))
        return v
    }

    // ---------------------------------------------------------------- INÍCIO

    private fun buildHome(): View {
        val all = Store.all()
        val watching = all.filter { it.status == "assistindo" }

        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(6), dp(16), dp(150))
        sv.addView(col)

        val name = Store.userName
        val hi = card(18, 26, Color.TRANSPARENT)
        hi.background = gradient(
            Color.parseColor("#FF9DBF"), Color.parseColor("#FF6B9D"),
            dp(26).toFloat(), GradientDrawable.Orientation.TL_BR
        )
        hi.addView(label(if (name.isBlank()) "Oi, bem-vinda! 🌸" else "Oi, $name! 🌸", 24f, Color.WHITE, true, true))
        hi.addView(
            label(all.size.toString() + " doramas na estante · " + watching.size + " em andamento", 13f, Color.WHITE),
            lin(WRAP, WRAP, t = 4)
        )
        hi.setOnClickListener { askName() }
        col.addView(hi, lin(MATCH, WRAP))

        col.addView(section("📺 Assistindo agora"))
        if (watching.isEmpty()) {
            val e = card(16, 22)
            e.addView(label("Nada em andamento ainda 🌱", 15f, Palette.text, true, true))
            e.addView(label("Toque no + para começar um dorama novo!", 13f, Palette.muted), lin(WRAP, WRAP, t = 4))
            col.addView(e, lin(MATCH, WRAP))
        } else {
            val rv = RecyclerView(this)
            rv.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
            val ad = DramaAdapter(1, { open(it) }, { d ->
                val finished = Store.bump(d)
                if (finished) {
                    Toast.makeText(this, "Parabéns, você terminou " + d.title + "! 🎉", Toast.LENGTH_LONG).show()
                }
                showTabKeepingScroll(rv)
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

        addMiniRow(col, "🌱 Quero ver", all.filter { it.status == "quero" })
        addMiniRow(col, "💖 Favoritos", all.filter { it.favorite })
        addMiniRow(col, "✅ Concluídos", all.filter { it.status == "concluido" })

        if (all.isEmpty()) {
            val e = card(16, 22)
            e.addView(label("Sua estante está vazia 🌸", 16f, Palette.text, true, true))
            e.addView(
                label("Toque no botão + para adicionar seu primeiro dorama, com capa, nota e muito carinho.", 13f, Palette.muted),
                lin(WRAP, WRAP, t = 4)
            )
            col.addView(e, lin(MATCH, WRAP, t = 16))
        }
        return sv
    }

    /** Atualiza o texto do carrossel sem voltar para o primeiro item. */
    private fun showTabKeepingScroll(rv: RecyclerView) {
        val lm = rv.layoutManager as LinearLayoutManager
        val pos = lm.findFirstVisibleItemPosition()
        val ad = rv.adapter
        if (ad != null) ad.notifyDataSetChanged()
        if (pos >= 0) lm.scrollToPosition(pos)
    }

    private fun addMiniRow(col: LinearLayout, title: String, list: List<Drama>) {
        if (list.isEmpty()) return
        col.addView(section(title))
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
            .setTitle("Como posso te chamar? 🌸")
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

    // ----------------------------------------------------------------- LISTA

    private fun filtered(): List<Drama> {
        var l = Store.all()
        if (statusFilter == "fav") {
            l = l.filter { it.favorite }
        } else if (statusFilter != "all") {
            l = l.filter { it.status == statusFilter }
        }
        if (genreFilter != "all") l = l.filter { it.genre == genreFilter }
        if (query.isNotBlank()) {
            l = l.filter {
                it.title.contains(query, true) || it.cast.contains(query, true) || it.platform.contains(query, true)
            }
        }
        return when (sortMode) {
            0 -> l.sortedByDescending { it.addedAt }
            1 -> l.sortedBy { it.title.lowercase() }
            else -> l.sortedByDescending { it.rating }
        }
    }

    private fun refreshList() {
        val l = filtered()
        listAdapter?.submit(l)
        listEmpty?.visibility = if (l.isEmpty()) View.VISIBLE else View.GONE
        listEmpty?.text = if (Store.all().isEmpty()) {
            "Sua estante está vazia 🌸\nToque no + para adicionar!"
        } else {
            "Nenhum dorama por aqui 🥺"
        }
        listInfo?.text = l.size.toString() + " doramas"
    }

    private fun buildListTab(): View {
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(14), dp(4), dp(14), 0)

        val search = input("Buscar por título, elenco ou plataforma 🔍")
        search.setText(query)
        search.doAfterTextChanged {
            query = it?.toString() ?: ""
            refreshList()
        }
        col.addView(search, lin(MATCH, WRAP))

        val statusOpts = ArrayList<Triple<String, String, Int>>()
        statusOpts.add(Triple("all", "🌸 Todos", Palette.pink))
        statusOpts.add(Triple("fav", "💖 Favoritos", Color.parseColor("#FF6B9D")))
        for (s in Statuses.all) statusOpts.add(Triple(s.key, s.emoji + " " + s.label, s.color))
        col.addView(chipScroller(statusOpts, statusFilter) {
            statusFilter = it
            refreshList()
        }, lin(MATCH, WRAP, t = 10))

        val genreOpts = ArrayList<Triple<String, String, Int>>()
        genreOpts.add(Triple("all", "🎀 Todos os gêneros", Palette.pink))
        for (g in Genres.all) genreOpts.add(Triple(g.key, g.emoji + " " + g.label, g.primary))
        col.addView(chipScroller(genreOpts, genreFilter) {
            genreFilter = it
            refreshList()
        }, lin(MATCH, WRAP, t = 4))

        val info = LinearLayout(this)
        info.orientation = LinearLayout.HORIZONTAL
        info.gravity = Gravity.CENTER_VERTICAL
        val count = label("", 13f, Palette.muted, true)
        listInfo = count
        info.addView(count, lin(0, WRAP, 1f))
        val sb = pill(sortLabel(), Color.WHITE, Palette.pink, 12f)
        sb.setOnClickListener {
            sortMode = (sortMode + 1) % 3
            sb.text = sortLabel()
            refreshList()
        }
        sortBtn = sb
        info.addView(sb, lin(WRAP, WRAP))
        col.addView(info, lin(MATCH, WRAP, t = 8, b = 2))

        val rv = RecyclerView(this)
        rv.layoutManager = LinearLayoutManager(this)
        rv.clipToPadding = false
        rv.setPadding(0, dp(4), 0, dp(150))
        listAdapter = DramaAdapter(0, { open(it) })
        rv.adapter = listAdapter

        val empty = label("", 15f, Palette.muted, true, true)
        empty.gravity = Gravity.CENTER
        listEmpty = empty

        val frame = FrameLayout(this)
        frame.addView(rv, FrameLayout.LayoutParams(MATCH, MATCH))
        frame.addView(empty, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
        col.addView(frame, lin(MATCH, 0, 1f))

        refreshList()
        return col
    }

    private fun sortLabel(): String = when (sortMode) {
        0 -> "↕ Mais recentes"
        1 -> "↕ A–Z"
        else -> "↕ Melhor nota"
    }

    // --------------------------------------------------------------- NÚMEROS

    private fun statBox(emoji: String, value: String, name: String): LinearLayout {
        val b = card(12, 20)
        b.gravity = Gravity.CENTER_HORIZONTAL
        b.addView(label(emoji, 22f))
        b.addView(label(value, 20f, Palette.pink, true, true), lin(WRAP, WRAP, t = 2))
        b.addView(label(name, 11f, Palette.muted), lin(WRAP, WRAP, t = 2))
        return b
    }

    private fun buildStats(): View {
        val all = Store.all()
        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(6), dp(16), dp(150))
        sv.addView(col)

        val rated = all.filter { it.rating > 0 }
        val avg = if (rated.isEmpty()) 0.0 else rated.map { it.rating }.sum().toDouble() / rated.size
        val eps = all.map { it.epWatched }.sum()

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.addView(statBox("📚", all.size.toString(), "doramas"), lin(0, WRAP, 1f, r = 8))
        top.addView(statBox("📺", eps.toString(), "episódios"), lin(0, WRAP, 1f, r = 8))
        top.addView(statBox("💗", if (rated.isEmpty()) "–" else "%.1f".format(avg), "nota média"), lin(0, WRAP, 1f))
        col.addView(top, lin(MATCH, WRAP, t = 4))

        if (all.isEmpty()) {
            val e = card(16, 22)
            e.addView(label("Ainda sem números 🌸", 16f, Palette.text, true, true))
            e.addView(label("Adicione doramas e acompanhe suas estatísticas aqui.", 13f, Palette.muted), lin(WRAP, WRAP, t = 4))
            col.addView(e, lin(MATCH, WRAP, t = 16))
        } else {
            val byGenre = all.groupBy { it.genre }.entries.sortedByDescending { it.value.size }
            val fav = Genres.byKey(byGenre[0].key)
            val favCard = card(16, 22, Palette.pinkSoft)
            favCard.addView(label("Seu gênero favorito", 12f, Palette.muted))
            favCard.addView(label(fav.emoji + " " + fav.label, 22f, fav.dark, true, true), lin(WRAP, WRAP, t = 2))
            favCard.addView(label(fav.tagline, 12f, Palette.muted), lin(WRAP, WRAP, t = 2))
            col.addView(favCard, lin(MATCH, WRAP, t = 14))

            col.addView(section("📌 Por status"))
            val sc = card(14, 22)
            val maxN = maxOf(1, all.size)
            for (s in Statuses.all) {
                val n = all.count { it.status == s.key }
                sc.addView(barRow(s.emoji + " " + s.label, n, n.toFloat() / maxN, s.color), lin(MATCH, WRAP, t = 6))
            }
            col.addView(sc, lin(MATCH, WRAP))

            col.addView(section("🎀 Por gênero"))
            val gc = card(14, 22)
            val maxG = maxOf(1, byGenre[0].value.size)
            for (e in byGenre) {
                val g = Genres.byKey(e.key)
                gc.addView(barRow(g.emoji + " " + g.label, e.value.size, e.value.size.toFloat() / maxG, g.primary), lin(MATCH, WRAP, t = 6))
            }
            col.addView(gc, lin(MATCH, WRAP))

            if (rated.isNotEmpty()) {
                col.addView(section("💖 Mais bem avaliados"))
                val tc = card(14, 22)
                for (d in rated.sortedByDescending { it.rating }.take(5)) {
                    val r = LinearLayout(this)
                    r.orientation = LinearLayout.HORIZONTAL
                    r.gravity = Gravity.CENTER_VERTICAL
                    r.setPadding(0, dp(6), 0, dp(6))
                    val t = label(d.title, 14f, Palette.text, true)
                    t.maxLines = 1
                    t.ellipsize = android.text.TextUtils.TruncateAt.END
                    r.addView(t, lin(0, WRAP, 1f))
                    r.addView(label(hearts(d.rating), 14f, Genres.byKey(d.genre).primary, true), lin(WRAP, WRAP, l = 8))
                    r.setOnClickListener { open(d) }
                    tc.addView(r, lin(MATCH, WRAP))
                }
                col.addView(tc, lin(MATCH, WRAP))
            }
        }

        col.addView(section("💾 Backup"))
        val bc = card(14, 22)
        bc.addView(
            label(
                "Guarde uma cópia da sua lista (sem as capas) para levar a outro celular, ou restaure uma cópia.",
                12f, Palette.muted
            )
        )
        val brow = LinearLayout(this)
        brow.orientation = LinearLayout.HORIZONTAL
        val exp = pill("📤 Exportar", Palette.pink, Color.WHITE, 13f)
        exp.setOnClickListener { exportBackup() }
        val imp = pill("📥 Importar", Color.parseColor("#C38BD8"), Color.WHITE, 13f)
        imp.setOnClickListener { importBackup() }
        brow.addView(exp, lin(WRAP, WRAP, r = 8))
        brow.addView(imp, lin(WRAP, WRAP))
        bc.addView(brow, lin(WRAP, WRAP, t = 10))
        col.addView(bc, lin(MATCH, WRAP))

        return sv
    }

    private fun barRow(name: String, n: Int, frac: Float, color: Int): View {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        val nm = label(name, 13f, Palette.text, true)
        r.addView(nm, lin(dp(104), WRAP))
        val bar = SoftBar(this)
        bar.progress = frac
        bar.barColor = color
        r.addView(bar, lin(0, dp(10), 1f, l = 4, r = 8))
        r.addView(label(n.toString(), 13f, Palette.muted, true), lin(dp(24), WRAP))
        return r
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
            .setTitle("Importar backup 📥")
            .setView(box)
            .setPositiveButton("Importar") { _, _ ->
                val n = Store.importJson(et.text.toString())
                if (n < 0) {
                    Toast.makeText(this, "Esse texto não parece um backup 🥺", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, n.toString() + " doramas adicionados 🌸", Toast.LENGTH_LONG).show()
                    showTab(2)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
