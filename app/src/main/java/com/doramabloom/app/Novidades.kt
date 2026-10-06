package com.doramabloom.app

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog

/**
 * "O que mudou": lê app/src/main/assets/novidades.txt (cada atualização ganha uma seção no topo)
 * e mostra um resumo bonitinho na primeira vez que o app abre depois de atualizar.
 *
 * Formato do arquivo:
 *   #id identificador-unico-da-atualizacao
 *   #titulo Título curto
 *   - uma novidade por linha
 */
object Novidades {

    class Note(val id: String, val title: String, val items: List<String>)

    private const val PREFS = "novidades"
    private const val SEEN = "seen"

    /** Lê as seções do arquivo (a mais nova primeiro). */
    fun load(ctx: Context): List<Note> {
        val text = try {
            ctx.assets.open("novidades.txt").bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            return emptyList()
        }
        val out = ArrayList<Note>()
        var id = ""
        var title = ""
        var items = ArrayList<String>()
        var open = false

        fun close() {
            if (open && (title.isNotBlank() || items.isNotEmpty())) {
                out.add(Note(if (id.isBlank()) title else id, title, items))
            }
            id = ""
            title = ""
            items = ArrayList()
            open = false
        }

        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#id")) {
                close()
                id = line.removePrefix("#id").trim()
                open = true
            } else if (line.startsWith("#titulo")) {
                title = line.removePrefix("#titulo").trim()
                open = true
            } else if (line.startsWith("-")) {
                val t = line.removePrefix("-").trim()
                if (t.isNotEmpty()) {
                    items.add(t)
                    open = true
                }
            }
        }
        close()
        return out
    }

    /**
     * Mostra as novidades que você ainda não viu. Devolve true se mostrou algo
     * ([after] roda quando a janela é fechada).
     */
    fun showIfNew(act: Activity, after: () -> Unit): Boolean {
        val notes = load(act)
        if (notes.isEmpty()) return false
        val sp = act.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seen = sp.getString(SEEN, null)
        val newestId = notes[0].id
        if (seen == newestId) return false
        sp.edit().putString(SEEN, newestId).apply()

        // instalação nova (ainda sem nome nem doramas): não precisa de resumo
        val existingUser = Store.askedName || Store.all().isNotEmpty()
        if (seen == null && !existingUser) return false

        var fresh: List<Note> = notes.takeWhile { it.id != seen }
        if (seen == null || fresh.isEmpty()) fresh = notes.take(2)
        if (fresh.size > 4) fresh = fresh.take(4)

        val head = "Atualizado para a v" + Updater.currentCode(act)
        show(act, fresh, head, "Entendi!", after)
        return true
    }

    /** Lista completa (botão "Ver novidades" nas configurações). */
    fun showAll(act: Activity) {
        val notes = load(act)
        if (notes.isEmpty()) {
            act.softToast("Ainda não há novidades registradas.", Palette.pink, "sparkle")
            return
        }
        show(act, notes.take(15), "Novidades do app", "Fechar", null)
    }

    private fun show(act: Activity, notes: List<Note>, head: String, button: String, after: (() -> Unit)?) {
        val box = LinearLayout(act)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(act.dp(22), act.dp(18), act.dp(22), act.dp(6))

        val hr = LinearLayout(act)
        hr.orientation = LinearLayout.HORIZONTAL
        hr.gravity = Gravity.CENTER_VERTICAL
        hr.addView(IconView(act, "sparkle", Palette.pink, 24))
        hr.addView(act.label(head, 19f, Palette.pinkDark, true, true), act.lin(WRAP, WRAP, l = 10))
        box.addView(hr, act.lin(MATCH, WRAP))
        box.addView(
            act.label("O que mudou:", 12f, Palette.muted, true),
            act.lin(WRAP, WRAP, t = 6, b = 4)
        )

        for ((idx, n) in notes.withIndex()) {
            if (n.title.isNotBlank()) {
                box.addView(
                    act.label(n.title, 15f, Palette.text, true, true),
                    act.lin(MATCH, WRAP, t = if (idx == 0) 8 else 18)
                )
            }
            for (it2 in n.items) {
                val row = LinearLayout(act)
                row.orientation = LinearLayout.HORIZONTAL
                row.addView(IconView(act, "check", Palette.pink, 15), act.lin(WRAP, WRAP, t = 2))
                row.addView(act.label(it2, 13.5f, Palette.text), act.lin(0, WRAP, 1f, l = 8))
                box.addView(row, act.lin(MATCH, WRAP, t = 8))
            }
        }

        val sv = ScrollView(act)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(box)

        val dlg = AlertDialog.Builder(act)
            .setView(sv)
            .setPositiveButton(button, null)
            .create()
        dlg.setOnDismissListener { if (after != null) after() }
        dlg.show()
    }
}
