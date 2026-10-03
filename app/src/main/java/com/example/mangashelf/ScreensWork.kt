package com.example.mangashelf

import android.graphics.Bitmap
import android.graphics.Color
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
    scroll.setBackgroundColor(P.bg)
    val col = vbox()
    col.setPadding(dp(16), dp(12), dp(16), dp(28))
    scroll.addView(col)

    val top = hbox()
    val back = tv("←", 26f, P.text, true)
    back.setPadding(dp(4), dp(4), dp(18), dp(4))
    back.setOnClickListener { pop() }
    top.addv(back, WRAP, WRAP)
    top.addv(View(this), 0, 0, 1f)
    val fav = tv(if (w.favorite) "♥" else "♡", 28f, if (w.favorite) P.accent2 else P.sub, true)
    fav.setPadding(dp(10), dp(2), dp(4), dp(2))
    fav.setOnClickListener {
        w.favorite = !w.favorite
        store.save()
        render()
    }
    top.addv(fav, WRAP, WRAP)
    col.addv(top)

    // cabeçalho
    val head = hbox()
    head.gravity = Gravity.TOP
    head.addv(coverView(store, w), dp(120), WRAP)
    val info = vbox()
    info.setPadding(dp(14), 0, 0, 0)
    info.addv(tv(w.title, 22f, P.text, true))
    if (w.altTitle.isNotBlank()) info.addv(tv(w.altTitle, 13f, P.sub), MATCH, WRAP, 0f, 0, 2, 0, 0)
    if (w.author.isNotBlank()) info.addv(tv("✍️ ${w.author}", 13f, P.sub), MATCH, WRAP, 0f, 0, 6, 0, 0)
    val chips = FlowLayout(this)
    chips.hGap = dp(6)
    chips.vGap = dp(6)
    val typeChip = tv(w.type, 12f, P.accent, true)
    typeChip.setPadding(dp(10), dp(5), dp(10), dp(5))
    typeChip.background = shape(P.card, dp(16).toFloat(), P.accent, dp(1))
    chips.addView(typeChip)
    val statusChip = tv(w.status + " ▾", 12f, Color.WHITE, true)
    statusChip.setPadding(dp(10), dp(5), dp(10), dp(5))
    statusChip.background = shape(P.accent, dp(16).toFloat())
    statusChip.setOnClickListener {
        listDialog("Status", STATUSES) { i ->
            w.status = STATUSES[i]
            if (w.status == STATUS_DONE && w.total > 0) w.current = w.total.toDouble()
            store.save()
            render()
        }
    }
    chips.addView(statusChip)
    info.addv(chips, MATCH, WRAP, 0f, 0, 10, 0, 0)
    val rating = tv(if (w.rating > 0) "★ ${fmtNum(w.rating)} / 10" else "☆ Sem nota", 14f, if (w.rating > 0) P.accent2 else P.sub, true)
    info.addv(rating, MATCH, WRAP, 0f, 0, 10, 0, 0)
    head.addv(info, 0, WRAP, 1f)
    col.addv(head, MATCH, WRAP, 0f, 0, 6, 0, 0)

    // progresso
    val card = vbox()
    card.background = shape(P.card, dp(18).toFloat(), P.line, dp(1))
    card.setPadding(dp(16), dp(14), dp(16), dp(16))
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
        pctTv.text = if (w.total > 0 || w.status == STATUS_DONE) "${w.progress()}%" else "—"
        progHolder.removeAllViews()
        progHolder.addView(progressBar(w.progress(), 8), FrameLayout.LayoutParams(MATCH, WRAP))
        statusChip.text = w.status + " ▾"
        lastTv.text = "Última leitura: " + fmtDate(w.lastRead)
    }

    fun change(v: Double) {
        w.setChapter(v)
        store.save()
        refresh()
    }

    val minus = tv("−", 28f, P.text, true)
    minus.gravity = Gravity.CENTER
    minus.background = shape(P.bg, dp(26).toFloat(), P.line, dp(1))
    minus.setOnClickListener { change(Math.max(0.0, Math.ceil(w.current) - 1)) }
    val plus = tv("+", 28f, Color.WHITE, true)
    plus.gravity = Gravity.CENTER
    plus.background = shape(P.accent, dp(26).toFloat())
    plus.setOnClickListener { change(Math.floor(w.current) + 1) }
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
    col.addv(card, MATCH, WRAP, 0f, 0, 14, 0, 0)
    refresh()

    col.addv(pill("▶  CONTINUAR LENDO", P.accent, Color.WHITE, 16f) {
        if (!continueReading(w)) {
            toast("Salve um site primeiro 🔗")
            showLinksDialog(w) { render() }
        }
    }, MATCH, WRAP, 0f, 0, 14, 0, 0)

    val row = hbox()
    row.addv(outlinePill("📑 CAPÍTULOS", 12f) { showChaptersDialog(w) { render() } }, 0, WRAP, 1f, 0, 0, 6, 0)
    row.addv(outlinePill("🔗 LINKS (${w.links.size})", 12f) { showLinksDialog(w) { render() } }, 0, WRAP, 1f, 0, 0, 6, 0)
    row.addv(outlinePill("✏️ EDITAR", 12f) { go(Route("add", w.id)) }, 0, WRAP, 1f)
    col.addv(row, MATCH, WRAP, 0f, 0, 10, 0, 0)

    fun section(title: String, body: View) {
        col.addv(tv(title, 15f, P.text, true), MATCH, WRAP, 0f, 0, 20, 0, 6)
        col.addv(body)
    }

    if (w.synopsis.isNotBlank()) section("Sinopse", tv(w.synopsis, 14f, P.sub))
    if (w.genres.isNotEmpty()) {
        val fl = FlowLayout(this)
        fl.hGap = dp(6)
        fl.vGap = dp(6)
        for (g in w.genres) {
            val c = tv(g, 12f, P.text)
            c.setPadding(dp(10), dp(5), dp(10), dp(5))
            c.background = shape(P.card, dp(16).toFloat(), P.line, dp(1))
            fl.addView(c)
        }
        section("Gêneros", fl)
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
    return scroll
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
        holder.t.setTextColor(if (isRead) Color.WHITE else P.text)
        holder.t.typeface = if (isCur) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        holder.t.background = if (isRead) shape(P.accent, act.dp(8).toFloat())
        else shape(P.card, act.dp(8).toFloat(), if (isCur) P.accent2 else P.line, act.dp(if (isCur) 2 else 1))
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
        tv("Toque para marcar/desmarcar como lido. Segure um número para defini-lo como capítulo atual (borda rosa).", 12f, P.sub),
        MATCH, WRAP, 0f, 0, 0, 0, 8
    )
    val counter = tv("", 13f, P.text, true)
    fun upd() {
        counter.text = "✔ ${w.read.size} lidos" + (if (w.total > 0) " de ${w.total}" else "") + " · atual: ${fmtNum(w.current)}"
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
    val dlg = AlertDialog.Builder(this).setTitle("📑 Capítulos")
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
    AlertDialog.Builder(this).setTitle(if (link == null) "Adicionar site" else "Editar site")
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
    val dlg = AlertDialog.Builder(this).setTitle("🔗 Links de leitura")
        .setView(sv).setPositiveButton("Fechar", null).create()

    fun fix() {
        if (w.links.isNotEmpty() && w.links.none { it.primary }) w.links[0].primary = true
        store.save()
    }

    fun fill() {
        body.removeAllViews()
        body.addv(
            tv("Salve aqui os sites onde você lê esta obra. O site principal é o usado por padrão. Toque em Abrir para ler dentro do app.", 12f, P.sub),
            MATCH, WRAP, 0f, 0, 0, 0, 10
        )
        for (l in w.links.toList()) {
            val item = vbox()
            item.background = shape(P.card, dp(14).toFloat(), P.line, dp(1))
            item.setPadding(dp(12), dp(10), dp(12), dp(10))
            item.addv(tv((if (l.primary) "⭐ " else "") + l.label, 15f, P.text, true))
            val u = tv(l.url, 12f, P.sub)
            u.maxLines = 2
            u.ellipsize = TextUtils.TruncateAt.END
            item.addv(u, MATCH, WRAP, 0f, 0, 2, 0, 8)
            val r = hbox()
            r.addv(pill("Abrir", P.accent, Color.WHITE, 12f) {
                dlg.dismiss()
                w.lastRead = System.currentTimeMillis()
                store.save()
                openInBrowser(l.url, w.id, false)
            }, 0, WRAP, 1f, 0, 0, 4, 0)
            r.addv(outlinePill("Principal", 12f) {
                for (x in w.links) x.primary = false
                l.primary = true
                fix()
                fill()
            }, 0, WRAP, 1f, 0, 0, 4, 0)
            r.addv(outlinePill("Editar", 12f) {
                editLinkDialog(l) { n, uu ->
                    l.label = n
                    l.url = uu
                    fix()
                    fill()
                }
            }, 0, WRAP, 1f, 0, 0, 4, 0)
            r.addv(outlinePill("🗑", 12f) {
                confirmDialog("Excluir link", "Remover \"${l.label}\"?") {
                    w.links.remove(l)
                    if (w.lastUrl.isNotEmpty() && sameSite(hostOf(w.lastUrl), hostOf(l.url)) &&
                        w.links.none { sameSite(hostOf(it.url), hostOf(l.url)) }
                    ) w.lastUrl = ""
                    fix()
                    fill()
                }
            }, 0, WRAP, 1f)
            item.addv(r)
            body.addv(item, MATCH, WRAP, 0f, 0, 0, 0, 10)
        }
        body.addv(pill("＋ Adicionar site") {
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
    if (isEdit) {
        val back = tv("←", 26f, P.text, true)
        back.setPadding(dp(4), dp(4), dp(16), dp(4))
        back.setOnClickListener { pop() }
        head.addv(back, WRAP, WRAP)
    }
    head.addv(tv(if (isEdit) "✏️ Editar obra" else "➕ Adicionar obra", 22f, P.text, true), 0, WRAP, 1f)
    col.addv(head)

    val help = tv(
        if (isEdit) "💡 Altere o que quiser e toque em Salvar. Os sites de leitura são editados na página da obra, no botão LINKS."
        else "💡 Só o nome é obrigatório. Cole o endereço do site de leitura e, depois, o botão Continuar lendo abre ele aqui dentro do app.",
        12f, P.sub
    )
    help.setPadding(dp(12), dp(10), dp(12), dp(10))
    help.background = shape(P.card, dp(12).toFloat(), P.line, dp(1))
    col.addv(help, MATCH, WRAP, 0f, 0, 10, 0, 6)

    // capa
    var pending: Bitmap? = null
    var removeCover = false
    val coverHolder = FrameLayout(this)
    val titleF = inputField("Nome da obra *", w.title)

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
            coverHolder.addView(box, FrameLayout.LayoutParams(MATCH, WRAP))
        } else if (removeCover) {
            coverHolder.addView(coverView(store, Work(title = titleF.text.toString())), FrameLayout.LayoutParams(MATCH, WRAP))
        } else {
            coverHolder.addView(coverView(store, w), FrameLayout.LayoutParams(MATCH, WRAP))
        }
    }
    drawCover()

    val coverRow = hbox()
    coverRow.gravity = Gravity.TOP
    coverRow.addv(coverHolder, dp(110), WRAP)
    val coverBtns = vbox()
    coverBtns.setPadding(dp(12), 0, 0, 0)
    coverBtns.addv(outlinePill("🖼 Escolher da galeria", 12f) {
        pickImage { uri ->
            val b = Covers.decodeUri(this, uri, 1000)
            if (b == null) {
                toast("Não consegui abrir essa imagem")
            } else {
                pending = b
                removeCover = false
                drawCover()
            }
        }
    })
    coverBtns.addv(outlinePill("🔗 Usar link de imagem", 12f) {
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
                        removeCover = false
                        drawCover()
                    }
                }
            }.start()
        }
    }, MATCH, WRAP, 0f, 0, 8, 0, 0)
    coverBtns.addv(outlinePill("🗑 Remover capa", 12f) {
        pending = null
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
        favChip.text = if (favorite) "♥ Nos favoritos" else "♡ Marcar como favorita"
        favChip.setTextColor(if (favorite) Color.WHITE else P.text)
        favChip.background = if (favorite) shape(P.accent2, dp(20).toFloat()) else shape(P.card, dp(20).toFloat(), P.line, dp(1))
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

    col.addv(pill("💾  SALVAR", P.accent, Color.WHITE, 16f) {
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
        } else if (removeCover) {
            store.deleteCoverFile(w.cover)
            w.cover = ""
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
        col.addv(outlinePill("🗑  Excluir obra") {
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
