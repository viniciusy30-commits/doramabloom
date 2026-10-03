package com.example.mangashelf

import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

// ---------------- Estatísticas ----------------

private fun MainActivity.statCard(number: String, label: String): LinearLayout {
    val c = vbox()
    c.gravity = Gravity.CENTER
    c.background = shape(P.card, dp(16).toFloat(), P.line, dp(1))
    c.setPadding(dp(10), dp(16), dp(10), dp(16))
    val n = tv(number, 28f, P.accent, true)
    n.gravity = Gravity.CENTER
    c.addv(n, WRAP, WRAP)
    val l = tv(label, 12f, P.sub, true)
    l.gravity = Gravity.CENTER
    c.addv(l, WRAP, WRAP, 0f, 0, 2, 0, 0)
    return c
}

private fun MainActivity.barBlock(col: LinearLayout, title: String, data: List<Pair<String, Int>>) {
    col.addv(tv(title, 16f, P.text, true), MATCH, WRAP, 0f, 0, 22, 0, 8)
    val card = vbox()
    card.background = shape(P.card, dp(16).toFloat(), P.line, dp(1))
    card.setPadding(dp(14), dp(12), dp(14), dp(12))
    if (data.isEmpty()) {
        card.addv(tv("Sem dados ainda.", 13f, P.sub))
    } else {
        val max = Math.max(1, data.maxOf { it.second })
        for ((name, count) in data) {
            val row = hbox()
            val nm = tv(name, 12f, P.text)
            nm.maxLines = 1
            row.addv(nm, dp(96), WRAP)
            val bar = LinearLayout(this)
            bar.orientation = LinearLayout.HORIZONTAL
            bar.background = shape(P.line, dp(5).toFloat())
            val fill = View(this)
            fill.background = shape(P.accent, dp(5).toFloat())
            bar.addView(fill, LinearLayout.LayoutParams(0, dp(10), count.toFloat()))
            bar.addView(View(this), LinearLayout.LayoutParams(0, dp(10), (max - count).toFloat()))
            row.addv(bar, 0, dp(10), 1f, 4, 0, 8, 0)
            row.addv(tv(count.toString(), 12f, P.sub, true), dp(26), WRAP)
            card.addv(row, MATCH, WRAP, 0f, 0, 4, 0, 4)
        }
    }
    col.addv(card)
}

fun MainActivity.buildStats(): View {
    val ws = store.works
    val scroll = ScrollView(this)
    scroll.setBackgroundColor(P.bg)
    val col = vbox()
    col.setPadding(dp(16), dp(16), dp(16), dp(24))
    scroll.addView(col)
    col.addv(tv("📊 Estatísticas", 24f, P.text, true))

    fun row(a: LinearLayout, b: LinearLayout) {
        val r = hbox()
        r.addv(a, 0, WRAP, 1f, 0, 0, 5, 0)
        r.addv(b, 0, WRAP, 1f, 5, 0, 0, 0)
        col.addv(r, MATCH, WRAP, 0f, 0, 10, 0, 0)
    }
    col.addv(View(this), MATCH, dp(4))
    row(statCard(ws.size.toString(), "Total de obras"), statCard(ws.sumOf { it.chaptersRead() }.toString(), "Capítulos lidos"))
    row(statCard(ws.count { it.status == STATUS_READING }.toString(), "Lendo"), statCard(ws.count { it.status == STATUS_DONE }.toString(), "Concluídas"))
    row(statCard(ws.count { it.status == STATUS_PAUSED }.toString(), "Pausadas"), statCard(ws.count { it.status == STATUS_PLAN }.toString(), "Quero ler"))

    val byType = TYPES.map { t -> Pair(t, ws.count { it.type == t }) }.filter { it.second > 0 }
    barBlock(col, "Por tipo", byType)

    val gCount = HashMap<String, Int>()
    for (w in ws) for (g in w.genres) {
        val key = g.trim()
        if (key.isNotEmpty()) gCount[key] = (gCount[key] ?: 0) + 1
    }
    val byGenre = gCount.entries.sortedByDescending { it.value }.take(10).map { Pair(it.key, it.value) }
    barBlock(col, "Por gênero (top 10)", byGenre)
    return scroll
}

// ---------------- Configurações ----------------

private fun MainActivity.settingsCard(col: LinearLayout, title: String): LinearLayout {
    col.addv(tv(title, 15f, P.text, true), MATCH, WRAP, 0f, 0, 20, 0, 8)
    val card = vbox()
    card.background = shape(P.card, dp(16).toFloat(), P.line, dp(1))
    card.setPadding(dp(14), dp(14), dp(14), dp(14))
    col.addv(card)
    return card
}

fun MainActivity.buildSettings(): View {
    val scroll = ScrollView(this)
    scroll.setBackgroundColor(P.bg)
    val col = vbox()
    col.setPadding(dp(16), dp(16), dp(16), dp(24))
    scroll.addView(col)
    col.addv(tv("⚙️ Configurações", 24f, P.text, true))

    // aparência
    val ap = settingsCard(col, "Aparência")
    ap.addv(tv("Tema do aplicativo", 13f, P.sub), MATCH, WRAP, 0f, 0, 0, 0, 8)
    val themeRow = hbox()
    val names = listOf("Sistema", "Claro", "Escuro")
    for (i in names.indices) {
        themeRow.addv(chip(names[i], prefs.theme == i) {
            prefs.theme = i
            applyNightMode()
            themeChanged()
        }, WRAP, WRAP, 0f, 0, 0, 8, 0)
    }
    ap.addv(themeRow)

    // navegador
    val nv = settingsCard(col, "Navegador interno")
    nv.addv(tv("Tamanho do texto nas páginas", 13f, P.sub), MATCH, WRAP, 0f, 0, 0, 0, 8)
    val zr = hbox()
    val zoomTv = tv("${prefs.zoom}%", 16f, P.text, true)
    zoomTv.gravity = Gravity.CENTER
    val zm = outlinePill("−", 18f) {
        val z = Math.max(50, prefs.zoom - 10)
        prefs.zoom = z
        zoomTv.text = "$z%"
        browser?.setZoom(z)
    }
    val zp = outlinePill("+", 18f) {
        val z = Math.min(300, prefs.zoom + 10)
        prefs.zoom = z
        zoomTv.text = "$z%"
        browser?.setZoom(z)
    }
    zr.addv(zm, dp(56), WRAP)
    zr.addv(zoomTv, 0, WRAP, 1f)
    zr.addv(zp, dp(56), WRAP)
    nv.addv(zr)
    val homeTv = tv("Página inicial das novas abas:\n${prefs.homeUrl}", 13f, P.sub)
    nv.addv(homeTv, MATCH, WRAP, 0f, 0, 14, 0, 8)
    nv.addv(outlinePill("Mudar página inicial") {
        inputDialog("Página inicial", "https://…", prefs.homeUrl, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI) { s ->
            val u = normalizeUrl(s)
            if (u.isNotEmpty()) {
                prefs.homeUrl = u
                render()
            }
        }
    })
    nv.addv(outlinePill("🌐 Abrir navegador agora") { openInBrowser(null, null, false) }, MATCH, WRAP, 0f, 0, 8, 0, 0)
    nv.addv(outlinePill("🕘 Limpar histórico") {
        confirmDialog("Limpar histórico", "Apagar o histórico de páginas visitadas?") {
            prefs.clearHistory()
            toast("Histórico limpo")
        }
    }, MATCH, WRAP, 0f, 0, 8, 0, 0)

    // dados
    val dt = settingsCard(col, "Dados e backup")
    dt.addv(
        tv("Suas obras ficam salvas só neste aparelho. Faça backup de vez em quando (as capas vão junto no arquivo).", 12f, P.sub),
        MATCH, WRAP, 0f, 0, 0, 0, 10
    )
    dt.addv(pill("💾  Exportar backup") { exportBackup() })
    dt.addv(outlinePill("📥  Importar / restaurar backup") { importBackup() }, MATCH, WRAP, 0f, 0, 8, 0, 0)
    dt.addv(outlinePill("🗑  Apagar todos os dados") {
        confirmDialog("Apagar tudo", "Isso apaga todas as obras e capas deste aparelho. Faça um backup antes. Continuar?", "Apagar") {
            store.deleteAll()
            toast("Biblioteca apagada")
            goTop("home")
        }
    }, MATCH, WRAP, 0f, 0, 8, 0, 0)

    // ajuda
    val hp = settingsCard(col, "Ajuda")
    hp.addv(outlinePill("❓ Ver tutorial") { go(Route("tutorial")) })
    hp.addv(tv("Manga Shelf 1.0 — sua biblioteca pessoal de leitura.", 12f, P.sub), MATCH, WRAP, 0f, 0, 12, 0, 0)
    return scroll
}

// ---------------- Tutorial ----------------

val TUTORIAL = listOf(
    Pair(
        "📚 Como adicionar uma obra",
        "1. Toque em ➕ Adicionar, na barra de baixo.\n2. Escreva o nome (é o único campo obrigatório).\n3. Escolha tipo, status e gêneros. A capa pode vir da galeria ou de um link de imagem.\n4. Se quiser, cole o endereço do site onde você lê.\n5. Toque em SALVAR. A obra aparece na Biblioteca e na Início."
    ),
    Pair(
        "🔗 Como salvar um site",
        "1. Abra a obra e toque em LINKS.\n2. Toque em ＋ Adicionar site, dê um nome (ex.: Site principal) e cole o endereço.\n3. Você pode salvar vários sites por obra. Toque em Principal para escolher qual é o padrão.\n4. Use Editar ou 🗑 para corrigir ou remover."
    ),
    Pair(
        "🌐 Como usar o navegador interno",
        "Na página da obra, toque em ▶ CONTINUAR LENDO. O site abre aqui dentro, sem sair para o Chrome.\n\n• ✕ fecha o navegador e volta ao app (as abas continuam abertas).\n• Digite um endereço ou uma busca no campo de cima.\n• Os botões ◀ ▶ ⟳ ficam embaixo.\n• ⋮ abre o menu: texto maior/menor, tela cheia, histórico, copiar endereço e abrir no navegador externo.\n• O app lembra o último endereço que você leu em cada obra, então o Continuar lendo volta de onde parou."
    ),
    Pair(
        "🗂 Como usar as abas",
        "• As abas ficam na faixa logo abaixo do campo de endereço.\n• Toque numa aba para trocar; toque no ✕ dela para fechar.\n• Toque no ＋ no fim da faixa para abrir uma aba nova.\n• As abas continuam abertas enquanto você usa o app, mesmo se voltar para a biblioteca.\n• Se você tocar em Continuar lendo e a obra já tiver uma aba aberta, o app volta para ela."
    ),
    Pair(
        "🔢 Como atualizar o capítulo",
        "• Na página da obra, use − e + para mudar o capítulo atual.\n• Toque no número grande para digitar um capítulo exato (aceita 12.5).\n• Dentro do navegador, quando a aba pertence a uma obra, aparece um − Cap. N + embaixo para atualizar sem sair da leitura.\n• Em CAPÍTULOS, toque nos números para marcar como lido; segure um número para defini-lo como atual."
    ),
    Pair(
        "💾 Backup",
        "Em ⚙️ Config. você exporta um arquivo com todas as obras e capas, e pode restaurar depois. Escolha Mesclar para somar ao que já existe, ou Substituir tudo."
    )
)

fun MainActivity.buildTutorial(): View {
    val scroll = ScrollView(this)
    scroll.setBackgroundColor(P.bg)
    val col = vbox()
    col.setPadding(dp(16), dp(14), dp(16), dp(28))
    scroll.addView(col)
    val head = hbox()
    val back = tv("←", 26f, P.text, true)
    back.setPadding(dp(4), dp(4), dp(16), dp(4))
    back.setOnClickListener { pop() }
    head.addv(back, WRAP, WRAP)
    head.addv(tv("❓ Tutorial", 22f, P.text, true), 0, WRAP, 1f)
    col.addv(head)
    for ((title, body) in TUTORIAL) {
        val card = vbox()
        card.background = shape(P.card, dp(16).toFloat(), P.line, dp(1))
        card.setPadding(dp(16), dp(14), dp(16), dp(14))
        card.addv(tv(title, 16f, P.text, true))
        card.addv(tv(body, 14f, P.sub), MATCH, WRAP, 0f, 0, 8, 0, 0)
        col.addv(card, MATCH, WRAP, 0f, 0, 14, 0, 0)
    }
    col.addv(pill("Entendi! 👍", P.accent, Color.WHITE, 15f) { pop() }, MATCH, WRAP, 0f, 0, 18, 0, 0)
    return scroll
}

fun browserHelpText(): String = TUTORIAL[2].second + "\n\n" + TUTORIAL[3].second
