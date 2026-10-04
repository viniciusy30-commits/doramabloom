package com.example.mangashelf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

// ---------------- Página da obra ----------------

fun MainActivity.buildDetail(id: String): View {
    val w = store.get(id)
    if (w == null) {
        val t = tv("Obra não encontrada. Toque para voltar.", 16f)
        t.setPadding(dp(20), dp(40), dp(20), 0)
        t.setOnClickListener { pop() }
        return t
    }
    val scroll = ScrollView(this)
    scroll.setBackgroundColor(Color.TRANSPARENT)
    scroll.isVerticalScrollBarEnabled = false
    val col = vbox()
    scroll.addView(col)

    // ---------- topo: capa grande dissolvendo no fundo, título por cima ----------
    val hero = FrameLayout(this)
    val bmp = if (w.cover.isNotEmpty()) Covers.get(store, w.cover) else null
    val pal = coverPalette(w.title)
    val animTop: View? = animHero(store, w, bmp, prefs.animCovers)
    hero.addView(
        animTop ?: FadeCover(this, bmp, pal.first, pal.second, w.title.trim().take(1).uppercase().ifEmpty { "?" }),
        FrameLayout.LayoutParams(MATCH, MATCH)
    )

    fun glassCircle(ic: Ic, color: Int, onClick: () -> Unit): IconView {
        val b = roundBtn(ic, 42, 21, color, true, onClick)
        val r = dp(42).toFloat()
        b.background = rippled(shape(0x73000000, r, 0x33FFFFFF, dp(1)), r)
        return b
    }

    val topRow = hbox()
    topRow.setPadding(dp(16), dp(12), dp(16), 0)
    topRow.addv(glassCircle(Ic.ChevronLeft, Color.WHITE) { pop() }, dp(42), dp(42))
    topRow.addv(View(this), 0, 0, 1f)
    topRow.addv(
        glassCircle(if (w.favorite) Ic.HeartSolid else Ic.Heart, if (w.favorite) P.accent2 else Color.WHITE) {
            w.favorite = !w.favorite
            store.save()
            render()
        }, dp(42), dp(42)
    )
    hero.addView(topRow, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.TOP))

    val titleBox = vbox()
    titleBox.setPadding(dp(20), 0, dp(20), dp(14))
    val typeTag = tv(w.type.uppercase(), 11.5f, P.accent, true)
    typeTag.letterSpacing = 0.14f
    titleBox.addv(typeTag)
    val titleTv = tv(w.title, 30f, P.text, true)
    titleTv.maxLines = 3
    titleTv.ellipsize = TextUtils.TruncateAt.END
    if (P.dark) titleTv.setShadowLayer(10f, 0f, 2f, 0xCC000000.toInt())
    titleBox.addv(titleTv, MATCH, WRAP, 0f, 0, 4, 0, 0)
    hero.addView(titleBox, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))
    col.addv(hero, MATCH, dp(400))

    val body = vbox()
    body.setPadding(dp(20), dp(2), dp(20), dp(28))
    col.addv(body)

    // ---------- dados ----------
    fun metaRow(label: String, value: String) {
        val r = hbox()
        r.gravity = Gravity.TOP
        r.addv(lbl(label), dp(104), WRAP)
        r.addv(tv(value, 14f, P.text, true), 0, WRAP, 1f)
        body.addv(r, MATCH, WRAP, 0f, 0, 0, 0, 8)
    }
    if (w.altTitle.isNotBlank()) metaRow("TÍTULO ALT.", w.altTitle)
    if (w.author.isNotBlank()) metaRow("AUTOR", w.author)

    // faixa de números: nota | capítulo | progresso
    val stats = hbox()
    stats.background = shape(P.card, dp(18).toFloat(), P.line, dp(1))
    stats.setPadding(dp(16), dp(14), dp(16), dp(14))
    fun statCell(label: String, first: Boolean): TextView {
        val c = vbox()
        c.setPadding(if (first) 0 else dp(14), 0, 0, 0)
        c.addv(lbl(label))
        val v = tv("", 22f, P.text, true)
        c.addv(v, WRAP, WRAP, 0f, 0, 4, 0, 0)
        stats.addv(c, 0, WRAP, 1f)
        return v
    }
    fun divider() {
        val d = View(this)
        d.setBackgroundColor(P.line)
        stats.addv(d, dp(1), dp(42))
    }
    val ratingStat = statCell("NOTA", true)
    divider()
    val chapStat = statCell("CAPÍTULO", false)
    divider()
    val pctStat = statCell("PROGRESSO", false)
    body.addv(stats, MATCH, WRAP, 0f, 0, 6, 0, 0)

    if (w.genres.isNotEmpty()) {
        val fl = FlowLayout(this)
        fl.hGap = dp(8)
        fl.vGap = dp(8)
        for (g in w.genres) {
            val c = tv(g, 13f, P.text, true)
            c.setPadding(dp(14), dp(8), dp(14), dp(8))
            c.background = shape(P.card, dp(18).toFloat(), P.line, dp(1))
            fl.addView(c)
        }
        body.addv(fl, MATCH, WRAP, 0f, 0, 14, 0, 0)
    }

    // ---------- ação principal ----------
    body.addv(
        heroButton(if (w.lastUrl.isNotBlank()) "DE ONDE VOCÊ PAROU" else "ABRIR NO NAVEGADOR", "Continuar lendo", Ic.PlaySolid) {
            continueReading(w)
        }, MATCH, WRAP, 0f, 0, 16, 0, 0
    )

    // status + atalhos (capítulos, links, editar)
    val sr = hbox()
    val sc = statusColor(w.status)
    val statusBtn = hbox()
    statusBtn.setPadding(dp(16), 0, dp(14), 0)
    statusBtn.background = rippled(shape(P.card, dp(16).toFloat(), P.line, dp(1)), dp(16).toFloat())
    statusBtn.addv(IconView(this, statusIcon(w.status), sc, 20), dp(22), dp(22), 0f, 0, 0, 10, 0)
    statusBtn.addv(tv(w.status, 15f, P.text, true), 0, WRAP, 1f)
    statusBtn.addv(IconView(this, Ic.ChevronDown, sc, 20), dp(22), dp(22))
    statusBtn.setOnClickListener {
        listDialog("Status", STATUSES) { i ->
            w.status = STATUSES[i]
            if (w.status == STATUS_DONE && w.total > 0) w.current = w.total.toDouble()
            store.save()
            render()
        }
    }
    statusBtn.pressFx()
    sr.addv(statusBtn, 0, dp(52), 1f, 0, 0, 8, 0)

    fun squareBtn(ic: Ic, onClick: () -> Unit): IconView {
        val v = IconView(this, ic, P.text, 22)
        val r = dp(16).toFloat()
        v.background = rippled(shape(P.card, r, P.line, dp(1)), r)
        v.setOnClickListener { onClick() }
        v.pressFx()
        return v
    }
    sr.addv(squareBtn(Ic.Rows) { showChaptersDialog(w) { render() } }, dp(52), dp(52), 0f, 0, 0, 8, 0)
    val linkWrap = FrameLayout(this)
    linkWrap.addView(squareBtn(Ic.Chain) { showLinksDialog(w) { render() } }, FrameLayout.LayoutParams(MATCH, MATCH))
    if (w.links.isNotEmpty()) {
        val badge = tv(w.links.size.toString(), 10f, Color.WHITE, true)
        badge.gravity = Gravity.CENTER
        badge.background = shape(P.accentDeep, dp(9).toFloat(), P.bg, dp(1))
        val blp = FrameLayout.LayoutParams(dp(18), dp(18), Gravity.TOP or Gravity.END)
        blp.setMargins(0, dp(4), dp(4), 0)
        linkWrap.addView(badge, blp)
    }
    sr.addv(linkWrap, dp(52), dp(52), 0f, 0, 0, 8, 0)
    sr.addv(squareBtn(Ic.Pen) { go(Route("add", w.id)) }, dp(52), dp(52))
    body.addv(sr, MATCH, WRAP, 0f, 0, 12, 0, 0)

    // ---------- capítulo atual ----------
    body.addv(tv("Capítulo atual", 15f, P.text, true), MATCH, WRAP, 0f, 0, 22, 0, 8)
    val card = vbox()
    card.background = shape(P.card, dp(22).toFloat(), P.line, dp(1))
    card.setPadding(dp(16), dp(16), dp(16), dp(18))
    val chapRow = hbox()
    chapRow.gravity = Gravity.CENTER
    val chapterTv = tv("", 30f, P.text, true)
    chapterTv.gravity = Gravity.CENTER
    val totalTv = tv("", 12f, P.sub)
    totalTv.gravity = Gravity.CENTER
    val progHolder = FrameLayout(this)
    val pctTv = tv("", 13f, P.accent, true)
    val lastTv = tv("", 12f, P.sub)

    fun refresh() {
        chapterTv.text = fmtNum(w.current)
        totalTv.text = if (w.total > 0) "de ${w.total} capítulos · toque no número para digitar" else "capítulo atual · toque no número para digitar"
        val pct = if (w.total > 0 || w.status == STATUS_DONE) "${w.progress()}%" else "—"
        pctTv.text = pct
        pctStat.text = pct
        chapStat.text = fmtNum(w.current) + (if (w.total > 0) " / ${w.total}" else "")
        ratingStat.text = if (w.rating > 0) fmtNum(w.rating) else "—"
        ratingStat.setTextColor(if (w.rating > 0) P.star else P.sub)
        progHolder.removeAllViews()
        progHolder.addView(progressBar(w.progress(), 8), FrameLayout.LayoutParams(MATCH, WRAP))
        lastTv.text = "Última leitura: " + fmtDate(w.lastRead)
    }

    fun change(v: Double) {
        w.setChapter(v)
        store.save()
        refresh()
    }

    val minus = roundBtn(Ic.Minus, 52, 24) { change(Math.max(0.0, Math.ceil(w.current) - 1)) }
    val plus = IconView(this, Ic.Plus, P.onAccent, 24)
    plus.background = rippled(shape(P.accent, dp(26).toFloat()), dp(26).toFloat())
    plus.setOnClickListener { change(Math.floor(w.current) + 1) }
    plus.pressFx()
    val mid = vbox()
    mid.gravity = Gravity.CENTER
    mid.addv(chapterTv, WRAP, WRAP)
    mid.addv(totalTv, WRAP, WRAP)
    mid.setOnClickListener {
        inputDialog(
            "Capítulo atual", "Ex.: 125 ou 12.5", fmtNum(w.current),
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        ) { s ->
            val v = s.trim().replace(',', '.').toDoubleOrNull()
            if (v == null) toast("Número inválido") else change(v)
        }
    }
    chapRow.addv(minus, dp(52), dp(52))
    chapRow.addv(mid, 0, WRAP, 1f)
    chapRow.addv(plus, dp(52), dp(52))
    card.addv(chapRow)
    val pr = hbox()
    pr.addv(tv("Progresso", 12f, P.sub, true), 0, WRAP, 1f)
    pr.addv(pctTv, WRAP, WRAP)
    card.addv(pr, MATCH, WRAP, 0f, 0, 14, 0, 6)
    card.addv(progHolder, MATCH, WRAP)
    card.addv(lastTv, MATCH, WRAP, 0f, 0, 10, 0, 0)
    body.addv(card, MATCH, WRAP, 0f, 0, 0, 0, 0)
    refresh()

    fun section(title: String, v: View) {
        body.addv(tv(title, 15f, P.text, true), MATCH, WRAP, 0f, 0, 22, 0, 6)
        body.addv(v)
    }

    if (w.synopsis.isNotBlank()) {
        val syn = tv(w.synopsis, 14f, P.sub)
        syn.setLineSpacing(0f, 1.15f)
        var open = false
        syn.maxLines = 5
        syn.ellipsize = TextUtils.TruncateAt.END
        syn.setOnClickListener {
            open = !open
            syn.maxLines = if (open) 100 else 5
        }
        section("Sinopse", syn)
    }
    if (w.tags.isNotEmpty()) {
        val fl = FlowLayout(this)
        fl.hGap = dp(6)
        fl.vGap = dp(6)
        for (g in w.tags) {
            val c = tv("#$g", 12f, P.accent, true)
            c.setPadding(dp(10), dp(5), dp(10), dp(5))
            fl.addView(c)
        }
        section("Tags", fl)
    }
    if (w.notes.isNotBlank()) section("Observações", tv(w.notes, 14f, P.sub))
    if (w.lastUrl.isNotBlank()) {
        val u = tv(w.lastUrl, 12f, P.sub)
        u.maxLines = 2
        u.ellipsize = TextUtils.TruncateAt.END
        section("Último endereço lido", u)
    }

    // fundo: a capa da obra, bem desfocada, com um véu para manter o texto legível
    val root = FrameLayout(this)
    root.setBackgroundColor(P.bg)
    val bgView = ImageView(this)
    bgView.scaleType = ImageView.ScaleType.CENTER_CROP
    val blurred = if (w.cover.isNotEmpty()) Covers.blurred(store, w.cover) else null
    if (blurred != null) {
        bgView.setImageBitmap(blurred)
        bgView.scaleX = 1.3f
        bgView.scaleY = 1.3f
    } else {
        bgView.setImageDrawable(
            GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(pal.first, pal.second))
        )
    }
    bgView.alpha = if (P.dark) 0.9f else 0.75f
    root.addView(bgView, FrameLayout.LayoutParams(MATCH, MATCH))
    val veil = View(this)
    veil.background = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(tint(P.bg, 0x4D), tint(P.bg, 0xA6), tint(P.bg, 0xE0), tint(P.bg, 0xF5))
    )
    root.addView(veil, FrameLayout.LayoutParams(MATCH, MATCH))
    root.addView(scroll, FrameLayout.LayoutParams(MATCH, MATCH))
    return root
}

// ---------------- Capítulos ----------------

class ChapterAdapter(private val act: MainActivity, private val w: Work, var count: Int, private val onChange: () -> Unit = {}) :
    RecyclerView.Adapter<ChapterAdapter.VH>() {

    class VH(val t: TextView) : RecyclerView.ViewHolder(t)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val t = TextView(act)
        t.gravity = Gravity.CENTER
        t.textSize = 13f
        val lp = RecyclerView.LayoutParams(MATCH, act.dp(40))
        lp.setMargins(act.dp(3), act.dp(3), act.dp(3), act.dp(3))
        t.layoutParams = lp
        return VH(t)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val n = position + 1
        val isRead = w.read.contains(n)
        val isCur = w.current.toInt() == n
        holder.t.text = n.toString()
        holder.t.setTextColor(if (isRead) P.accent else P.text)
        holder.t.typeface = if (isCur || isRead) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        val bw = act.dp(if (isCur) 2 else 1)
        holder.t.background = if (isRead) shape(P.accentSoft, act.dp(10).toFloat(), if (isCur) P.accent else P.accentLine, bw)
        else shape(P.card, act.dp(10).toFloat(), if (isCur) P.accent else P.line, bw)
        holder.t.setOnClickListener {
            if (w.read.contains(n)) w.read.remove(n) else w.read.add(n)
            act.store.save()
            notifyItemChanged(position)
            onChange()
        }
        holder.t.setOnLongClickListener {
            w.setChapter(n.toDouble())
            act.store.save()
            notifyDataSetChanged()
            onChange()
            act.toast("Capítulo atual: $n")
            true
        }
    }

    override fun getItemCount(): Int = count
}

fun MainActivity.showChaptersDialog(w: Work, onClose: () -> Unit) {
    val maxRead = w.read.maxOrNull() ?: 0
    val n = if (w.total > 0) w.total else Math.max(w.current.toInt(), maxRead) + 30
    val box = vbox()
    box.setPadding(dp(12), dp(4), dp(12), 0)
    box.addv(
        tv("Toque para marcar/desmarcar como lido. Segure um número para defini-lo como capítulo atual (contorno laranja mais grosso).", 12f, P.sub),
        MATCH, WRAP, 0f, 0, 0, 0, 8
    )
    val counter = tv("", 13f, P.text, true)
    fun upd() {
        counter.text = "${w.read.size} lidos" + (if (w.total > 0) " de ${w.total}" else "") + " · atual: ${fmtNum(w.current)}"
    }
    upd()
    box.addv(counter, MATCH, WRAP, 0f, 0, 0, 0, 8)
    val ad = ChapterAdapter(this, w, n) { upd() }
    val btns = hbox()
    btns.addv(outlinePill("Marcar até o atual", 12f) {
        for (i in 1..w.current.toInt()) w.read.add(i)
        store.save()
        ad.notifyDataSetChanged()
        upd()
    }, 0, WRAP, 1f, 0, 0, 6, 0)
    btns.addv(outlinePill("Limpar", 12f) {
        w.read.clear()
        store.save()
        ad.notifyDataSetChanged()
        upd()
    }, 0, WRAP, 1f, 0, 0, 6, 0)
    if (w.total <= 0) {
        btns.addv(outlinePill("+30", 12f) {
            ad.count += 30
            ad.notifyDataSetChanged()
        }, 0, WRAP, 1f)
    }
    box.addv(btns, MATCH, WRAP, 0f, 0, 0, 0, 8)
    val rv = RecyclerView(this)
    rv.layoutManager = GridLayoutManager(this, 6)
    rv.adapter = ad
    rv.scrollToPosition(Math.max(0, w.current.toInt() - 4))
    box.addv(rv, MATCH, (resources.displayMetrics.heightPixels * 0.5).toInt())
    val dlg = MaterialAlertDialogBuilder(this).setTitle("Capítulos")
        .setView(box).setPositiveButton("Fechar", null).create()
    dlg.setOnDismissListener { onClose() }
    dlg.show()
}

// ---------------- Links ----------------

fun MainActivity.editLinkDialog(link: Link?, onSave: (String, String) -> Unit) {
    val box = vbox()
    box.setPadding(dp(20), dp(8), dp(20), 0)
    val name = inputField("Nome (ex.: Site principal)", link?.label ?: "")
    val url = inputField("Endereço (https://…)", link?.url ?: "", 1, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
    box.addv(name)
    box.addv(url, MATCH, WRAP, 0f, 0, 8, 0, 0)
    MaterialAlertDialogBuilder(this).setTitle(if (link == null) "Adicionar site" else "Editar site")
        .setView(box)
        .setPositiveButton("Salvar") { _, _ ->
            val u = normalizeUrl(url.text.toString())
            if (u.isEmpty()) {
                toast("Digite o endereço do site")
            } else {
                val n = name.text.toString().trim().ifEmpty { hostOf(u).ifEmpty { "Site" } }
                onSave(n, u)
            }
        }
        .setNegativeButton("Cancelar", null).show()
}

fun MainActivity.showLinksDialog(w: Work, onClose: () -> Unit) {
    val body = vbox()
    body.setPadding(dp(16), dp(8), dp(16), dp(8))
    val sv = ScrollView(this)
    sv.addView(body)
    val dlg = MaterialAlertDialogBuilder(this).setTitle("Links de leitura")
        .setView(sv).setPositiveButton("Fechar", null).create()

    fun fix() {
        if (w.links.isNotEmpty() && w.links.none { it.primary }) w.links[0].primary = true
        store.save()
    }

    fun fill() {
        body.removeAllViews()
        body.addv(
            tv("Guarde aqui sites diferentes onde você lê esta obra, para lembrar depois. A estrela marca o principal. Dica: dentro do navegador, menu ⋮ > Salvar site nos links desta obra.", 12f, P.sub),
            MATCH, WRAP, 0f, 0, 0, 0, 10
        )
        for (l in w.links.toList()) {
            val item = vbox()
            item.background = shape(P.card, dp(18).toFloat(), P.line, dp(1))
            item.setPadding(dp(12), dp(10), dp(12), dp(10))
            val tr = hbox()
            if (l.primary) tr.addv(IconView(this, Ic.StarSolid, P.star, 15), dp(16), dp(16), 0f, 0, 0, 6, 0)
            tr.addv(tv(l.label, 15f, P.text, true), 0, WRAP, 1f)
            item.addv(tr)
            val u = tv(l.url, 12f, P.sub)
            u.maxLines = 2
            u.ellipsize = TextUtils.TruncateAt.END
            item.addv(u, MATCH, WRAP, 0f, 0, 2, 0, 8)
            val r = hbox()
            r.addv(pill("Abrir", P.accent, Color.WHITE, 13f, Ic.External) {
                dlg.dismiss()
                w.lastRead = System.currentTimeMillis()
                store.save()
                openInBrowser(l.url, w.id, false)
            }, 0, WRAP, 1f, 0, 0, 8, 0)
            r.addv(roundBtn(if (l.primary) Ic.StarSolid else Ic.Star, 42, 19, if (l.primary) P.star else P.sub) {
                for (x in w.links) x.primary = false
                l.primary = true
                fix()
                toast("Definido como site principal")
                fill()
            }, dp(42), dp(42), 0f, 0, 0, 6, 0)
            r.addv(roundBtn(Ic.Pen, 42, 18) {
                editLinkDialog(l) { n, uu ->
                    l.label = n
                    l.url = uu
                    fix()
                    fill()
                }
            }, dp(42), dp(42), 0f, 0, 0, 6, 0)
            r.addv(roundBtn(Ic.Trash, 42, 18, P.accent) {
                confirmDialog("Excluir link", "Remover \"${l.label}\"?") {
                    w.links.remove(l)
                    if (w.lastUrl.isNotEmpty() && sameSite(hostOf(w.lastUrl), hostOf(l.url)) &&
                        w.links.none { sameSite(hostOf(it.url), hostOf(l.url)) }
                    ) w.lastUrl = ""
                    fix()
                    fill()
                }
            }, dp(42), dp(42))
            item.addv(r)
            body.addv(item, MATCH, WRAP, 0f, 0, 0, 0, 10)
        }
        body.addv(pill("Adicionar site", icon = Ic.Plus) {
            editLinkDialog(null) { n, uu ->
                w.links.add(Link(n, uu, w.links.isEmpty()))
                fix()
                fill()
            }
        })
    }

    fill()
    dlg.setOnDismissListener { onClose() }
    dlg.show()
}

// ---------------- Adicionar / Editar ----------------

fun MainActivity.buildForm(editId: String?): View {
    val existing = if (editId != null) store.get(editId) else null
    val isEdit = existing != null
    val w = existing ?: Work()

    val scroll = ScrollView(this)
    scroll.setBackgroundColor(P.bg)
    val col = vbox()
    col.setPadding(dp(16), dp(16), dp(16), dp(32))
    scroll.addView(col)

    val head = hbox()
    if (isEdit) head.addv(roundBtn(Ic.ChevronLeft, 42, 22) { pop() }, dp(42), dp(42), 0f, 0, 0, 12, 0)
    head.addv(
        sectionTitle(if (isEdit) Ic.Pen else Ic.Plus, if (isEdit) "Editar obra" else "Adicionar obra", 22f),
        0, WRAP, 1f
    )
    col.addv(head)

    val help = tv(
        if (isEdit) "Altere o que quiser e toque em Salvar. Os sites de leitura são editados na página da obra, no botão Links."
        else "Só o nome é obrigatório. Cole o endereço do site de leitura e, depois, o botão Continuar lendo abre ele aqui dentro do app.",
        12f, P.sub
    )
    val helpBox = hbox()
    helpBox.gravity = Gravity.TOP
    helpBox.setPadding(dp(14), dp(12), dp(14), dp(12))
    helpBox.background = shape(P.accentSoft, dp(16).toFloat(), P.accentLine, dp(1))
    helpBox.addv(IconView(this, Ic.Bulb, P.accent, 18), dp(20), dp(20), 0f, 0, 1, 10, 0)
    helpBox.addv(help, 0, WRAP, 1f)
    col.addv(helpBox, MATCH, WRAP, 0f, 0, 12, 0, 6)

    // capa
    var pending: Bitmap? = null
    var pendingAnim: java.io.File? = null
    var removeCover = false
    val coverHolder = FrameLayout(this)
    val titleF = inputField("Nome da obra *", w.title)

    fun addAnimTag(box: FrameLayout) {
        val tag = tv("ANIMADA", 9.5f, Color.WHITE, true)
        tag.setPadding(dp(6), dp(2), dp(6), dp(2))
        tag.background = shape(0xB3000000.toInt(), dp(8).toFloat())
        val lp = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.END)
        lp.setMargins(0, dp(6), dp(6), 0)
        box.addView(tag, lp)
    }

    fun drawCover() {
        coverHolder.removeAllViews()
        val pb = pending
        if (pb != null) {
            val box = CoverFrame(this)
            val iv = ImageView(this)
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            iv.setImageBitmap(pb)
            box.addView(iv, MATCH, MATCH)
            box.background = shape(P.line, dp(12).toFloat())
            box.clipToOutline = true
            if (pendingAnim != null) addAnimTag(box)
            coverHolder.addView(box, FrameLayout.LayoutParams(MATCH, WRAP))
        } else if (removeCover) {
            coverHolder.addView(coverView(store, Work(title = titleF.text.toString())), FrameLayout.LayoutParams(MATCH, WRAP))
        } else {
            val cv = coverView(store, w)
            if (w.coverAnim.isNotEmpty()) addAnimTag(cv)
            coverHolder.addView(cv, FrameLayout.LayoutParams(MATCH, WRAP))
        }
    }
    drawCover()

    val coverRow = hbox()
    coverRow.gravity = Gravity.TOP
    coverRow.addv(coverHolder, dp(110), WRAP)
    val coverBtns = vbox()
    coverBtns.setPadding(dp(12), 0, 0, 0)
    coverBtns.addv(outlinePill("Foto, GIF ou vídeo", 12f, Ic.Picture) {
        pickMedia { uri ->
            toast("Carregando…")
            Thread {
                val pk = Covers.loadPicked(this, uri)
                runOnUiThread {
                    if (pk == null) {
                        toast("Não consegui abrir esse arquivo (GIF e vídeo: até 15 MB)")
                    } else {
                        pending = pk.poster
                        val old = pendingAnim
                        if (old != null && old.path != pk.anim?.path) old.delete()
                        pendingAnim = pk.anim
                        removeCover = false
                        drawCover()
                    }
                }
            }.start()
        }
    })
    coverBtns.addv(outlinePill("Usar link de imagem", 12f, Ic.Chain) {
        inputDialog("Link da imagem da capa", "https://…/capa.jpg", "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI) { s ->
            val u = normalizeUrl(s)
            if (u.isEmpty()) return@inputDialog
            toast("Baixando capa…")
            Thread {
                val bytes = downloadBytes(u)
                val b = if (bytes != null) Covers.decodeBytes(bytes, 1000) else null
                runOnUiThread {
                    if (b == null) {
                        toast("Não consegui baixar essa imagem")
                    } else {
                        pending = b
                        pendingAnim?.delete()
                        pendingAnim = null
                        removeCover = false
                        drawCover()
                    }
                }
            }.start()
        }
    }, MATCH, WRAP, 0f, 0, 8, 0, 0)
    coverBtns.addv(outlinePill("Remover capa", 12f, Ic.Trash) {
        pending = null
        pendingAnim?.delete()
        pendingAnim = null
        removeCover = true
        drawCover()
    }, MATCH, WRAP, 0f, 0, 8, 0, 0)
    coverRow.addv(coverBtns, 0, WRAP, 1f)
    col.addv(coverRow, MATCH, WRAP, 0f, 0, 10, 0, 0)

    // campos de texto
    col.addv(lbl("NOME"), MATCH, WRAP, 0f, 0, 14, 0, 4)
    col.addv(titleF)
    val altF = inputField("Nome alternativo", w.altTitle)
    col.addv(lbl("NOME ALTERNATIVO"), MATCH, WRAP, 0f, 0, 12, 0, 4)
    col.addv(altF)
    val authorF = inputField("Autor", w.author)
    col.addv(lbl("AUTOR"), MATCH, WRAP, 0f, 0, 12, 0, 4)
    col.addv(authorF)

    var type = w.type
    col.addv(lbl("TIPO"), MATCH, WRAP, 0f, 0, 12, 0, 6)
    val typeChoice = FlowLayout(this)
    typeChoice.hGap = dp(8)
    typeChoice.vGap = dp(8)
    fun buildType() {
        typeChoice.removeAllViews()
        for (o in TYPES) typeChoice.addView(chip(o, o == type) {
            type = o
            buildType()
        })
    }
    buildType()
    col.addv(typeChoice)

    var status = w.status
    col.addv(lbl("STATUS"), MATCH, WRAP, 0f, 0, 12, 0, 6)
    val statusChoice = FlowLayout(this)
    statusChoice.hGap = dp(8)
    statusChoice.vGap = dp(8)
    fun buildStatus() {
        statusChoice.removeAllViews()
        for (o in STATUSES) statusChoice.addView(chip(o, o == status) {
            status = o
            buildStatus()
        })
    }
    buildStatus()
    col.addv(statusChoice)

    val genres = HashSet<String>()
    for (g in w.genres) if (PRESET_GENRES.contains(g)) genres.add(g)
    col.addv(lbl("GÊNEROS"), MATCH, WRAP, 0f, 0, 12, 0, 6)
    val genreChoice = FlowLayout(this)
    genreChoice.hGap = dp(8)
    genreChoice.vGap = dp(8)
    fun buildGenres() {
        genreChoice.removeAllViews()
        for (o in PRESET_GENRES) genreChoice.addView(chip(o, genres.contains(o)) {
            if (genres.contains(o)) genres.remove(o) else genres.add(o)
            buildGenres()
        })
    }
    buildGenres()
    col.addv(genreChoice)
    val extraGenresF = inputField("Outros gêneros (separe por vírgula)", w.genres.filter { !PRESET_GENRES.contains(it) }.joinToString(", "))
    col.addv(extraGenresF, MATCH, WRAP, 0f, 0, 8, 0, 0)

    val synF = inputField("Sinopse", w.synopsis, 4)
    col.addv(lbl("SINOPSE"), MATCH, WRAP, 0f, 0, 12, 0, 4)
    col.addv(synF)

    val dec = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    val curF = inputField("0", if (isEdit) fmtNum(w.current) else "", 1, dec)
    val totF = inputField("Se souber", if (w.total > 0) w.total.toString() else "", 1, InputType.TYPE_CLASS_NUMBER)
    val numRow = hbox()
    val c1 = vbox()
    c1.addv(lbl("CAPÍTULO ATUAL"), MATCH, WRAP, 0f, 0, 0, 0, 4)
    c1.addv(curF)
    val c2 = vbox()
    c2.addv(lbl("TOTAL DE CAPÍTULOS"), MATCH, WRAP, 0f, 0, 0, 0, 4)
    c2.addv(totF)
    numRow.addv(c1, 0, WRAP, 1f, 0, 0, 6, 0)
    numRow.addv(c2, 0, WRAP, 1f)
    col.addv(numRow, MATCH, WRAP, 0f, 0, 12, 0, 0)

    val ratF = inputField("0 a 10", if (w.rating > 0) fmtNum(w.rating) else "", 1, dec)
    col.addv(lbl("NOTA PESSOAL (0 A 10)"), MATCH, WRAP, 0f, 0, 12, 0, 4)
    col.addv(ratF)
    val tagsF = inputField("Tags (separe por vírgula)", w.tags.joinToString(", "))
    col.addv(lbl("TAGS"), MATCH, WRAP, 0f, 0, 12, 0, 4)
    col.addv(tagsF)
    val notesF = inputField("Observações", w.notes, 3)
    col.addv(lbl("OBSERVAÇÕES"), MATCH, WRAP, 0f, 0, 12, 0, 4)
    col.addv(notesF)

    var siteF: android.widget.EditText? = null
    if (!isEdit) {
        val s = inputField("https://site.com/manga/obra", "", 1, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        siteF = s
        col.addv(lbl("SITE DE LEITURA (OPCIONAL)"), MATCH, WRAP, 0f, 0, 12, 0, 4)
        col.addv(s)
    }

    var favorite = w.favorite
    val favChip = tv("", 14f, P.text, true)
    fun styleFav() {
        favChip.setIconText(
            if (favorite) Ic.HeartSolid else Ic.Heart,
            if (favorite) "Nos favoritos" else "Marcar como favorita", false, 18
        )
        favChip.setTextColor(if (favorite) P.accent2 else P.text)
        favChip.background = if (favorite) shape(0x22FF6B81, dp(22).toFloat(), P.accent2, dp(1))
        else shape(P.card, dp(22).toFloat(), P.line, dp(1))
    }
    favChip.gravity = Gravity.CENTER
    favChip.setPadding(dp(14), dp(10), dp(14), dp(10))
    favChip.setOnClickListener {
        favorite = !favorite
        styleFav()
    }
    styleFav()
    col.addv(favChip, MATCH, WRAP, 0f, 0, 16, 0, 0)

    fun parseD(s: String): Double = s.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
    fun splitList(s: String): MutableList<String> =
        s.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.toMutableList()

    col.addv(pill("Salvar", P.accent, P.onAccent, 16f, Ic.Check) {
        val title = titleF.text.toString().trim()
        if (title.isEmpty()) {
            toast("Digite o nome da obra")
            titleF.requestFocus()
            return@pill
        }
        w.title = title
        w.altTitle = altF.text.toString().trim()
        w.author = authorF.text.toString().trim()
        w.type = type
        w.status = status
        val allGenres = ArrayList<String>()
        for (g in PRESET_GENRES) if (genres.contains(g)) allGenres.add(g)
        allGenres.addAll(splitList(extraGenresF.text.toString()))
        w.genres = allGenres.distinctBy { it.lowercase() }.toMutableList()
        w.synopsis = synF.text.toString().trim()
        w.current = Math.max(0.0, parseD(curF.text.toString()))
        w.total = totF.text.toString().trim().toIntOrNull()?.let { Math.max(0, it) } ?: 0
        w.rating = Math.min(10.0, Math.max(0.0, parseD(ratF.text.toString())))
        w.tags = splitList(tagsF.text.toString())
        w.notes = notesF.text.toString().trim()
        w.favorite = favorite
        if (w.status == STATUS_DONE && w.total > 0) w.current = w.total.toDouble()

        val pb = pending
        if (pb != null) {
            store.deleteCoverFile(w.cover)
            w.cover = Covers.save(store, w.id, pb)
            store.deleteCoverFile(w.coverAnim)
            w.coverAnim = ""
            val pa = pendingAnim
            if (pa != null) w.coverAnim = Covers.saveAnim(store, w.id, pa)
        } else if (removeCover) {
            store.deleteCoverFile(w.cover)
            store.deleteCoverFile(w.coverAnim)
            w.cover = ""
            w.coverAnim = ""
        }
        if (!isEdit) {
            val site = normalizeUrl(siteF?.text?.toString() ?: "")
            if (site.isNotEmpty()) w.links.add(Link(hostOf(site).ifEmpty { "Site principal" }, site, true))
            store.works.add(0, w)
        }
        store.save()
        if (isEdit) pop() else reset(Route("library"), Route("detail", w.id))
    }, MATCH, WRAP, 0f, 0, 20, 0, 0)

    if (isEdit) {
        col.addv(outlinePill("Excluir obra", 13f, Ic.Trash) {
            confirmDialog("Excluir obra", "Excluir \"${w.title}\" definitivamente? Isso não pode ser desfeito.", "Excluir") {
                store.delete(w)
                popN(2)
            }
        }, MATCH, WRAP, 0f, 0, 10, 0, 0)
    } else {
        col.addv(outlinePill("Cancelar") { goTop("home") }, MATCH, WRAP, 0f, 0, 10, 0, 0)
    }
    return scroll
}
