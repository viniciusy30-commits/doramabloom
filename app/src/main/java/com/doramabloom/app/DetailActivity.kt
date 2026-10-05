package com.doramabloom.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A tela é montada UMA vez; cada ação só atualiza os pedaços que mudam (com animação).
 * Antes ela era remontada inteira a cada toque, o que fazia a tela piscar.
 */
class DetailActivity : AppCompatActivity() {

    private var id = -1L
    private var seen = -1
    private var scroll: ScrollView? = null
    private lateinit var petals: PetalsView
    private val updaters = ArrayList<() -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        id = intent.getLongExtra("id", -1L)
        if (Store.get(id) == null) {
            finish()
            return
        }
        build(true)
    }

    override fun onResume() {
        super.onResume()
        if (Store.get(id) == null) {
            finish()
            return
        }
        // voltou da edição: reconstrói só se os dados mudaram
        if (seen != -1 && seen != Store.version) build(false)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.screen_back_in, R.anim.screen_back_out)
    }

    private fun sync() {
        seen = Store.version
    }

    private fun fmt(ms: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(ms))

    private fun refreshAll() {
        for (u in updaters) u()
    }

    private fun build(entrance: Boolean) {
        val d = Store.get(id)
        if (d == null) {
            finish()
            return
        }
        seen = Store.version
        updaters.clear()
        val keep = scroll?.scrollY ?: 0
        val g = Genres.byKey(d.genre)
        window.statusBarColor = g.soft

        val root = FrameLayout(this)
        root.background = gradient(g.soft, Color.WHITE)

        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(10), dp(16), dp(40))
        col.clipChildren = false
        col.clipToPadding = false
        sv.clipChildren = false
        sv.addView(col)
        root.addView(sv, FrameLayout.LayoutParams(MATCH, MATCH))

        petals = PetalsView(this, g.petals, g.primary, 14)
        val glass = Color.parseColor("#44FFFFFF")

        // ---- barra superior
        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.addView(roundBtn("back", g.dark, false, 18) { finish() }, lin(dp(40), dp(40)))
        top.addView(View(this), lin(0, dp(1), 1f))
        top.addView(roundBtn("edit", g.dark, false, 18) {
            val i = Intent(this, EditActivity::class.java)
            i.putExtra("id", d.id)
            startActivity(i)
            overridePendingTransition(R.anim.screen_in, R.anim.screen_out_back)
        }, lin(dp(40), dp(40), r = 8))

        val favBtn = FrameLayout(this)
        val favIcon = IconView(this, "heart", g.primary, 18)
        favBtn.addView(favIcon, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
        fun styleFav() {
            val bg = GradientDrawable()
            bg.shape = GradientDrawable.OVAL
            bg.setColor(if (d.favorite) g.primary else Color.WHITE)
            bg.setStroke(dp(2), g.primary)
            favBtn.background = bg
            favIcon.tint = if (d.favorite) Color.WHITE else g.primary
        }
        styleFav()
        favBtn.pressable(0.86f)
        favBtn.setOnClickListener {
            d.favorite = !d.favorite
            Store.save(d)
            sync()
            styleFav()
            favIcon.pop(1.9f)
            if (d.favorite) {
                petals.burstFrom(favBtn, listOf("heart", "heart", "sparkle"), listOf(g.primary, Palette.pink, Color.WHITE), 14)
            }
            updatePetals(d)
        }
        top.addView(favBtn, lin(dp(40), dp(40)))
        col.addView(top, lin(MATCH, WRAP))

        // ---- hero
        val hero = FrameLayout(this)
        hero.background = gradient(g.primary, g.dark, dp(32).toFloat(), GradientDrawable.Orientation.TL_BR)
        hero.elevation = 0f
        hero.clipToOutline = true
        val deco = IconView(this, "blossom", Color.parseColor("#22FFFFFF"), 130)
        val dlp = FrameLayout.LayoutParams(WRAP, WRAP)
        dlp.gravity = Gravity.END or Gravity.BOTTOM
        dlp.setMargins(0, 0, dp(-26), dp(-26))
        hero.addView(deco, dlp)
        val deco2 = IconView(this, "petal", Color.parseColor("#33FFFFFF"), 34)
        val d2lp = FrameLayout.LayoutParams(WRAP, WRAP)
        d2lp.gravity = Gravity.END or Gravity.TOP
        d2lp.setMargins(0, dp(10), dp(54), 0)
        hero.addView(deco2, d2lp)
        val spark = IconView(this, "sparkle", Color.parseColor("#77FFFFFF"), 20)
        val slp = FrameLayout.LayoutParams(WRAP, WRAP)
        slp.gravity = Gravity.END or Gravity.TOP
        slp.setMargins(0, dp(14), dp(16), 0)
        hero.addView(spark, slp)

        val heroCol = LinearLayout(this)
        heroCol.orientation = LinearLayout.VERTICAL

        val hrow = LinearLayout(this)
        hrow.orientation = LinearLayout.HORIZONTAL
        hrow.setPadding(dp(16), dp(16), dp(16), dp(12))
        val ring = FrameLayout(this)
        ring.setPadding(dp(3), dp(3), dp(3), dp(3))
        ring.background = roundRect(Color.WHITE, dp(23).toFloat())
        val cover = CoverView(this, 20)
        cover.bind(d, 600)
        ring.addView(cover, FrameLayout.LayoutParams(dp(112), dp(166)))
        hrow.addView(ring, lin(WRAP, WRAP, r = 14))

        val info = LinearLayout(this)
        info.orientation = LinearLayout.VERTICAL
        val title = label(d.title, 22f, Color.WHITE, true, true)
        title.maxLines = 4
        title.ellipsize = TextUtils.TruncateAt.END
        info.addView(title)
        if (d.original.isNotBlank()) {
            info.addView(label(d.original, 12f, Color.parseColor("#E6FFFFFF")), lin(WRAP, WRAP, t = 2))
        }
        info.addView(label(subtitle(d), 12f, Color.parseColor("#E6FFFFFF")), lin(WRAP, WRAP, t = 6))

        // gêneros, status e país: as pílulas quebram de linha, nada é cortado
        val flow = FlowLayout(this)
        flow.hGap = dp(6)
        flow.vGap = dp(6)
        flow.addView(pill(g.label, glass, Color.WHITE, 11f, g.icon))
        for (tk in d.tags) {
            if (tk == d.genre) continue
            val tg = Genres.byKey(tk)
            flow.addView(pill(tg.label, glass, Color.WHITE, 11f, tg.icon))
        }
        val statusPill = pill("", Palette.pink, Color.WHITE, 11f, "play")
        flow.addView(statusPill)
        flow.addView(pill(d.country, glass, Color.WHITE, 11f, "flag"))
        info.addView(flow, lin(MATCH, WRAP, t = 10))
        hrow.addView(info, lin(0, WRAP, 1f))
        heroCol.addView(hrow, lin(MATCH, WRAP))

        // faixa da nota
        val strip = LinearLayout(this)
        strip.orientation = LinearLayout.HORIZONTAL
        strip.gravity = Gravity.CENTER_VERTICAL
        strip.setPadding(dp(10), dp(8), dp(16), dp(8))
        strip.background = roundRect(Color.parseColor("#33FFFFFF"), dp(28).toFloat())
        val badge = scoreBadge(46, 19f)
        badge.setTextColor(g.primary)
        val bbg = GradientDrawable()
        bbg.shape = GradientDrawable.OVAL
        bbg.setColor(Color.WHITE)
        badge.background = bbg
        strip.addView(badge, lin(dp(46), dp(46), r = 12))
        val rcol = LinearLayout(this)
        rcol.orientation = LinearLayout.VERTICAL
        val rv = RatingView(this, 20, false)
        rv.color = Color.WHITE
        rcol.addView(rv)
        rcol.addView(label("nota de 10", 11f, Color.parseColor("#E6FFFFFF")), lin(WRAP, WRAP, t = 3))
        strip.addView(rcol, lin(0, WRAP, 1f))
        heroCol.addView(strip, lin(MATCH, WRAP, l = 14, r = 14, b = 14))

        hero.addView(heroCol, FrameLayout.LayoutParams(MATCH, WRAP))
        col.addView(hero, lin(MATCH, WRAP, t = 10))

        updaters.add {
            val st = Statuses.byKey(d.status)
            statusPill.text = st.label
            statusPill.background = roundRect(st.color, dp(20).toFloat(), Color.WHITE, dp(1))
            (statusPill.compoundDrawables[0] as? IconDrawable)?.let {
                it.name = st.icon
                it.color = Color.WHITE
            }
            val newText = if (d.score <= 0) "-" else d.score.toString()
            if (badge.text.toString() != newText) {
                badge.text = newText
                badge.pop(1.6f)
            }
            rv.score = d.score
        }

        // ---- assistir: abre a aba só deste dorama
        val watchRow = LinearLayout(this)
        watchRow.orientation = LinearLayout.HORIZONTAL
        watchRow.gravity = Gravity.CENTER_VERTICAL
        watchRow.clipChildren = false
        val wcard = LinearLayout(this)
        wcard.orientation = LinearLayout.HORIZONTAL
        wcard.gravity = Gravity.CENTER_VERTICAL
        wcard.setPadding(dp(14), dp(14), dp(18), dp(14))
        wcard.background = gradient(g.primary, g.dark, dp(26).toFloat(), GradientDrawable.Orientation.LEFT_RIGHT)
        wcard.elevation = 0f
        val pbub = FrameLayout(this)
        pbub.background = roundRect(Color.parseColor("#33FFFFFF"), dp(18).toFloat())
        pbub.addView(IconView(this, "play", Color.WHITE, 24), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
        wcard.addView(pbub, lin(dp(50), dp(50), r = 14))
        val wtx = LinearLayout(this)
        wtx.orientation = LinearLayout.VERTICAL
        val wsmall = label("", 10.5f, Color.parseColor("#E6FFFFFF"), true)
        wsmall.letterSpacing = 0.08f
        val wbig = label("", 17f, Color.WHITE, true, true)
        wtx.addView(wsmall)
        wtx.addView(wbig, lin(WRAP, WRAP, t = 1))
        wcard.addView(wtx, lin(0, WRAP, 1f))
        wcard.addView(IconView(this, "forward", Color.WHITE, 20))
        wcard.pressable(0.97f)
        wcard.setOnClickListener { openWatch(d) }
        watchRow.addView(wcard, lin(0, WRAP, 1f))
        watchRow.addView(roundBtn("link", g.primary, false, 20) { askLink(d, g) }, lin(dp(52), dp(52), l = 10))
        col.addView(watchRow, lin(MATCH, WRAP, t = 14))
        updaters.add {
            if (d.lastUrl.isBlank() && watchedEps(d) == 0) {
                wsmall.text = "PRONTO PARA COMEÇAR"
                wbig.text = "Assistir agora"
            } else {
                wsmall.text = "DE ONDE VOCÊ PAROU"
                wbig.text = "Continuar assistindo"
            }
        }

        // ---- tiles
        val tiles = LinearLayout(this)
        tiles.orientation = LinearLayout.HORIZONTAL
        tiles.clipChildren = false
        tiles.clipToPadding = false
        val tw = totalEps(d)
        val tSeasons = tile("tv", seasonCount(d).toString(), "temporadas", g.primary)
        val tEps = tile("play", "", "episódios", g.primary)
        val tMin = tile("clock", if (d.epMinutes > 0) d.epMinutes.toString() + " min" else "-", "por episódio", g.primary)
        val tRew = tile("replay", "", "reassistido", g.primary)
        tiles.addView(tSeasons.first, lin(0, WRAP, 1f, r = 6))
        tiles.addView(tEps.first, lin(0, WRAP, 1f, r = 6))
        tiles.addView(tMin.first, lin(0, WRAP, 1f, r = 6))
        tiles.addView(tRew.first, lin(0, WRAP, 1f))
        col.addView(tiles, lin(MATCH, WRAP, t = 14))
        updaters.add {
            val txt = if (tw > 0) watchedEps(d).toString() + "/" + tw else watchedEps(d).toString()
            if (tEps.second.text.toString() != txt) {
                tEps.second.text = txt
                tEps.second.pop(1.3f)
            }
            val rt = d.rewatch.toString() + "x"
            if (tRew.second.text.toString() != rt) {
                tRew.second.text = rt
                tRew.second.pop(1.3f)
            }
        }

        // ---- status
        val stCard = card(14, 22)
        stCard.addView(sectionTitle("Status", "bookmark", g.primary))
        val stOpts = ArrayList<Opt>()
        for (s in Statuses.all) stOpts.add(Opt(s.key, s.label, s.color, s.icon))
        val statusChips = chipScroller(stOpts, d.status) { key ->
            val before = d.status
            Store.setStatus(d, key)
            sync()
            refreshAll()
            if (before != key) {
                val st = Statuses.byKey(key)
                val icons = if (key == "concluido") listOf("check", "heart", "star", "sparkle") else listOf(st.icon, "sparkle", "petal")
                petals.burst(petals.width / 2f, petals.height * 0.30f, icons, listOf(st.color, g.primary, Color.WHITE), 22)
            }
        }
        stCard.addView(statusChips, lin(MATCH, WRAP, t = 8))
        col.addView(stCard, lin(MATCH, WRAP, t = 14))

        // ---- progresso por temporada
        val pc = card(14, 22)
        pc.addView(sectionTitle("Meu progresso", "play", g.primary))
        val overall = label("", 14f, Palette.text, true)
        pc.addView(overall, lin(WRAP, WRAP, t = 8))
        val obar = SoftBar(this)
        obar.barColor = g.primary
        pc.addView(obar, lin(MATCH, dp(10), t = 6))

        val plus = pill("+1 episódio", g.primary, Color.WHITE, 14f, "play")
        plus.setPadding(dp(18), dp(12), dp(18), dp(12))
        pc.addView(plus, lin(MATCH, WRAP, t = 12))

        val seasonBars = ArrayList<SoftBar>()
        val seasonTxt = ArrayList<TextView>()
        for (i in d.seasonEps.indices) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.addView(label("Temp. " + (i + 1), 13f, g.dark, true), lin(dp(62), WRAP))
            val b = SoftBar(this)
            b.barColor = g.primary
            seasonBars.add(b)
            row.addView(b, lin(0, dp(9), 1f, l = 2, r = 8))
            val cnt = label("", 12f, Palette.muted, true)
            seasonTxt.add(cnt)
            row.addView(cnt, lin(dp(46), WRAP))
            row.addView(roundBtn("minus", g.primary, false, 14) {
                Store.adjust(d, i, -1)
                sync()
                refreshAll()
                statusChips.selectChip(d.status)
            }, lin(dp(30), dp(30), l = 4))
            val addB = roundBtn("add", g.primary, true, 14) {}
            addB.setOnClickListener {
                val fin = Store.adjust(d, i, 1)
                sync()
                refreshAll()
                statusChips.selectChip(d.status)
                petals.burstFrom(addB, listOf(g.icon, "heart", "sparkle"), listOf(g.primary, Palette.pink), 8)
                if (fin) celebrate(d, g, addB, statusChips)
            }
            row.addView(addB, lin(dp(30), dp(30), l = 6))
            pc.addView(row, lin(MATCH, WRAP, t = 10))
        }

        plus.setOnClickListener {
            val finished = Store.bump(d)
            sync()
            refreshAll()
            statusChips.selectChip(d.status)
            petals.burstFrom(plus, listOf(g.icon, "heart", "sparkle", "blossom"), listOf(g.primary, Palette.pink, Color.WHITE), 14)
            if (finished) celebrate(d, g, plus, statusChips)
        }

        val rw = LinearLayout(this)
        rw.orientation = LinearLayout.HORIZONTAL
        rw.gravity = Gravity.CENTER_VERTICAL
        rw.addView(IconView(this, "replay", g.primary, 16))
        rw.addView(label("Vezes que reassisti", 13f, Palette.text, true), lin(0, WRAP, 1f, l = 8))
        val rewTxt = label("", 15f, g.dark, true)
        rw.addView(roundBtn("minus", g.primary, false, 14) {
            if (d.rewatch > 0) {
                d.rewatch -= 1
                Store.save(d)
                sync()
                refreshAll()
            }
        }, lin(dp(30), dp(30)))
        rewTxt.gravity = Gravity.CENTER
        rw.addView(rewTxt, lin(dp(34), WRAP))
        val rewAdd = roundBtn("add", g.primary, true, 14) {}
        rewAdd.setOnClickListener {
            d.rewatch += 1
            Store.save(d)
            sync()
            refreshAll()
            petals.burstFrom(rewAdd, listOf("replay", "heart"), listOf(g.primary, Palette.pink), 8)
        }
        rw.addView(rewAdd, lin(dp(30), dp(30)))
        pc.addView(rw, lin(MATCH, WRAP, t = 16))
        col.addView(pc, lin(MATCH, WRAP, t = 12))

        var firstProgress = true
        updaters.add {
            val total = totalEps(d)
            val w = watchedEps(d)
            val txt = if (total > 0) "$w de $total episódios" else "$w episódios assistidos"
            if (overall.text.toString() != txt) {
                overall.text = txt
                if (!firstProgress) overall.pop(1.06f)
            }
            val frac = progressOf(d)
            if (firstProgress) obar.animateTo(frac, 0f, 350L, 800L) else obar.animateTo(frac)
            for (i in seasonBars.indices) {
                val t = d.seasonEps[i]
                val wi = d.watched[i]
                val f = if (t > 0) wi.toFloat() / t.toFloat() else 0f
                if (firstProgress) seasonBars[i].animateTo(f, 0f, 450L + i * 80L, 700L) else seasonBars[i].animateTo(f)
                val ct = if (t > 0) "$wi/$t" else "$wi"
                if (seasonTxt[i].text.toString() != ct) {
                    seasonTxt[i].text = ct
                    if (!firstProgress) seasonTxt[i].pop(1.3f)
                }
            }
            val complete = total > 0 && d.seasonEps.all { it > 0 } && w >= total
            plus.text = if (complete) "Tudo assistido!" else "+1 episódio"
            (plus.compoundDrawables[0] as? IconDrawable)?.name = if (complete) "check" else "play"
            plus.alpha = if (complete) 0.6f else 1f
            val rt = d.rewatch.toString()
            if (rewTxt.text.toString() != rt) {
                rewTxt.text = rt
                if (!firstProgress) rewTxt.pop(1.4f)
            }
            firstProgress = false
        }

        // ---- nota
        val rc = card(14, 22)
        rc.addView(sectionTitle("Minha nota", "heart", g.primary))
        val scoreTxt = label("", 15f, g.dark, true, true)
        val big = RatingView(this, 34, true)
        big.color = g.primary
        big.onChange = { ns ->
            d.score = ns
            Store.save(d)
            sync()
            scoreTxt.text = if (ns <= 0) "Sem nota ainda" else ns.toString() + " / 10"
            scoreTxt.pop(1.15f)
            big.pop(1.08f)
            refreshAll()
            if (ns >= 9) {
                petals.burstFrom(big, listOf("heart", "star", "sparkle"), listOf(g.primary, Palette.pink, Color.parseColor("#FFB84D")), 18)
            }
        }
        rc.addView(big, lin(WRAP, WRAP, t = 10))
        rc.addView(scoreTxt, lin(WRAP, WRAP, t = 6))
        rc.addView(label("Toque na metade esquerda do coração para meio ponto.", 11f, Palette.muted), lin(WRAP, WRAP, t = 2))
        col.addView(rc, lin(MATCH, WRAP, t = 12))
        scoreTxt.text = if (d.score <= 0) "Sem nota ainda" else d.score.toString() + " / 10"

        // ---- sinopse
        if (d.synopsis.isNotBlank()) {
            val sc = card(14, 22)
            sc.addView(sectionTitle("Sinopse", "book", g.primary))
            val syn = label(d.synopsis, 14f, Palette.text)
            syn.maxLines = 4
            syn.ellipsize = TextUtils.TruncateAt.END
            sc.addView(syn, lin(MATCH, WRAP, t = 8))
            if (d.synopsis.length > 140) {
                var expanded = false
                val more = label("Ler mais", 12f, g.primary, true)
                more.setOnClickListener {
                    expanded = !expanded
                    android.transition.TransitionManager.beginDelayedTransition(
                        col, android.transition.AutoTransition().setDuration(280)
                    )
                    syn.maxLines = if (expanded) 100 else 4
                    more.text = if (expanded) "Ler menos" else "Ler mais"
                }
                more.pressable(0.92f)
                sc.addView(more, lin(WRAP, WRAP, t = 6))
            }
            col.addView(sc, lin(MATCH, WRAP, t = 12))
        }

        // ---- informações
        val ic = card(14, 22)
        col.addView(ic, lin(MATCH, WRAP, t = 12))
        var firstInfo = true
        updaters.add {
            while (ic.childCount > 0) ic.removeViewAt(0)
            ic.addView(sectionTitle("Informações", "tag", g.primary))
            if (d.platform.isNotBlank()) ic.addView(infoRow("tv", "Onde assistir", d.platform, g.primary))
            if (d.year.isNotBlank()) ic.addView(infoRow("calendar", "Ano de lançamento", d.year, g.primary))
            if (d.cast.isNotBlank()) ic.addView(infoRow("person", "Elenco", d.cast, g.primary))
            if (d.couple.isNotBlank()) ic.addView(infoRow("heart", "Casal favorito", d.couple, g.primary))
            if (d.startDate > 0L) ic.addView(infoRow("play", "Comecei em", fmt(d.startDate), g.primary))
            if (d.endDate > 0L) ic.addView(infoRow("check", "Terminei em", fmt(d.endDate), g.primary))
            ic.addView(infoRow("calendar", "Adicionado em", fmt(d.addedAt), g.primary))
            if (!firstInfo) {
                val last = ic.getChildAt(ic.childCount - 1)
                for (k in 1 until ic.childCount) ic.getChildAt(k).fadeScaleIn(0L, 260L)
                last.alpha = 1f
            }
            firstInfo = false
        }

        // ---- resenha
        if (d.notes.isNotBlank()) {
            val nc = card(14, 22)
            nc.addView(sectionTitle("Minha resenha", "edit", g.primary))
            nc.addView(label(d.notes, 14f, Palette.text), lin(MATCH, WRAP, t = 8))
            col.addView(nc, lin(MATCH, WRAP, t = 12))
        }

        // ---- excluir
        val del = pill("Excluir dorama", Color.parseColor("#FFE0E6"), Color.parseColor("#C2185B"), 14f, "delete")
        del.setPadding(dp(18), dp(12), dp(18), dp(12))
        del.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Excluir dorama?")
                .setMessage("\"" + d.title + "\" será removido da sua estante.")
                .setPositiveButton("Excluir") { _, _ ->
                    sv.animate().alpha(0f).translationY(dp(30).toFloat()).setDuration(260).withEndAction {
                        Store.delete(d.id)
                        sync()
                        finish()
                    }.start()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        col.addView(del, lin(MATCH, WRAP, t = 18))

        root.addView(petals, FrameLayout.LayoutParams(MATCH, MATCH))

        refreshAll()
        updatePetals(d)
        setContentView(root)
        scroll = sv
        if (keep > 0) sv.post { sv.scrollTo(0, keep) }

        if (entrance) {
            hero.fadeScaleIn(0L, 450L)
            ring.pop(1.25f)
            for (i in 1 until col.childCount) col.getChildAt(i).riseIn(80L + i * 55L, 20, 400L)
        }
    }

    private fun openWatch(d: Drama) {
        val i = Intent(this, WatchActivity::class.java)
        i.putExtra("id", d.id)
        startActivity(i)
        overridePendingTransition(R.anim.screen_in, R.anim.screen_out_back)
    }

    private fun askLink(d: Drama, g: Genre) {
        val et = EditText(this)
        et.hint = "https://..."
        et.setSingleLine(true)
        et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        et.setText(d.link)
        val box = FrameLayout(this)
        box.setPadding(dp(22), dp(8), dp(22), 0)
        box.addView(et, FrameLayout.LayoutParams(MATCH, WRAP))
        val b = AlertDialog.Builder(this)
            .setTitle("Link de início")
            .setMessage("Opcional: a página onde você assiste. Sem link, a aba abre no Google. Depois, ela sempre volta de onde você parou.")
            .setView(box)
            .setPositiveButton("Salvar") { _, _ ->
                var v = et.text.toString().trim()
                if (v.isNotEmpty() && !v.startsWith("http://") && !v.startsWith("https://")) v = "https://$v"
                if (v != d.link) d.lastUrl = ""
                d.link = v
                Store.save(d)
                sync()
                refreshAll()
                if (v.isNotEmpty()) softToast("Link salvo!", g.primary, "link")
            }
            .setNegativeButton("Cancelar", null)
        if (d.link.isNotBlank()) {
            b.setNeutralButton("Remover") { _, _ ->
                d.link = ""
                d.lastUrl = ""
                Store.save(d)
                sync()
                refreshAll()
            }
        }
        b.show()
    }

    private fun updatePetals(d: Drama) {
        val a = Atmosphere.ofDrama(d)
        petals.setTheme(a.icons, a.tints)
    }

    private fun celebrate(d: Drama, g: Genre, from: View, chips: View) {
        val green = Color.parseColor("#5CC6A0")
        petals.burstFrom(from, listOf("check", "heart", "star", "sparkle", "blossom"), listOf(g.primary, Palette.pink, green, Color.parseColor("#FFB84D")), 34)
        softToast("Parabéns, você terminou!", green, "check")
        chips.selectChip(d.status)
        updatePetals(d)
    }

    private fun tile(icon: String, value: String, name: String, color: Int): Pair<LinearLayout, TextView> {
        val b = card(10, 18)
        b.gravity = Gravity.CENTER_HORIZONTAL
        b.addView(IconView(this, icon, color, 18))
        val v = label(value, 14f, Palette.text, true, true)
        v.maxLines = 1
        b.addView(v, lin(WRAP, WRAP, t = 4))
        val n = label(name, 10f, Palette.muted)
        n.maxLines = 1
        b.addView(n, lin(WRAP, WRAP, t = 1))
        b.pressable(0.94f)
        return Pair(b, v)
    }

    private fun infoRow(icon: String, name: String, value: String, color: Int): View {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.setPadding(0, dp(10), 0, 0)
        r.addView(IconView(this, icon, color, 18), lin(WRAP, WRAP, t = 2))
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.addView(label(name, 11f, Palette.muted, true))
        c.addView(label(value, 14f, Palette.text), lin(MATCH, WRAP, t = 1))
        r.addView(c, lin(0, WRAP, 1f, l = 10))
        return r
    }
}
