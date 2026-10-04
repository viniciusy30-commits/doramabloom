package com.example.mangashelf

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

const val STATUS_READING = "Lendo"
const val STATUS_PLAN = "Quero ler"
const val STATUS_DONE = "Concluído"
const val STATUS_PAUSED = "Pausado"
const val STATUS_DROPPED = "Dropado"
const val FAV = "Favoritos"
val STATUSES = listOf(STATUS_READING, STATUS_PLAN, STATUS_DONE, STATUS_PAUSED, STATUS_DROPPED)
val TYPES = listOf("Mangá", "Manhwa", "Manhua", "Webtoon", "Outro")
val SORTS = listOf("Recentes", "Título (A-Z)", "Maior nota", "Mais progresso", "Adicionados")
val PRESET_GENRES = listOf(
    "Ação", "Aventura", "Comédia", "Drama", "Fantasia", "Romance", "Slice of Life",
    "Terror", "Isekai", "Sobrenatural", "Esportes", "Mistério", "Sci-Fi", "Escolar",
    "Shounen", "Seinen", "Shoujo", "Josei", "Harem", "Artes Marciais", "Psicológico", "Histórico"
)

data class Link(var label: String, var url: String, var primary: Boolean = false)

class Work(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var altTitle: String = "",
    var author: String = "",
    var type: String = "Mangá",
    var genres: MutableList<String> = mutableListOf(),
    var synopsis: String = "",
    var status: String = STATUS_PLAN,
    var current: Double = 0.0,
    var total: Int = 0,
    var rating: Double = 0.0,
    var tags: MutableList<String> = mutableListOf(),
    var notes: String = "",
    var favorite: Boolean = false,
    var cover: String = "",
    var coverAnim: String = "",
    var links: MutableList<Link> = mutableListOf(),
    var lastUrl: String = "",
    var lastRead: Long = 0L,
    var lastScroll: Int = 0,
    var read: MutableSet<Int> = mutableSetOf(),
    var created: Long = System.currentTimeMillis()
)

fun fmtNum(d: Double): String = if (d == Math.floor(d)) d.toLong().toString() else d.toString()

fun Work.progress(): Int {
    if (status == STATUS_DONE) return 100
    if (total <= 0) return 0
    return Math.min(100, Math.max(0, (current / total * 100).toInt()))
}

fun Work.chaptersRead(): Int = Math.max(read.size, current.toInt())

fun Work.chapterText(): String =
    if (total > 0) "Cap. ${fmtNum(current)} / $total" else "Cap. ${fmtNum(current)}"

fun Work.mainLink(): Link? = links.firstOrNull { it.primary } ?: links.firstOrNull()

fun Work.setChapter(v: Double) {
    current = Math.max(0.0, v)
    lastRead = System.currentTimeMillis()
    if (status == STATUS_PLAN && current > 0) status = STATUS_READING
    if (total > 0 && current >= total && status == STATUS_READING) status = STATUS_DONE
}

fun Work.toJson(): JSONObject {
    val o = JSONObject()
    o.put("id", id)
    o.put("title", title)
    o.put("altTitle", altTitle)
    o.put("author", author)
    o.put("type", type)
    o.put("genres", JSONArray(genres))
    o.put("synopsis", synopsis)
    o.put("status", status)
    o.put("current", current)
    o.put("total", total)
    o.put("rating", rating)
    o.put("tags", JSONArray(tags))
    o.put("notes", notes)
    o.put("favorite", favorite)
    o.put("cover", cover)
    o.put("coverAnim", coverAnim)
    val la = JSONArray()
    for (l in links) {
        val lo = JSONObject()
        lo.put("label", l.label)
        lo.put("url", l.url)
        lo.put("primary", l.primary)
        la.put(lo)
    }
    o.put("links", la)
    o.put("lastUrl", lastUrl)
    o.put("lastRead", lastRead)
    o.put("lastScroll", lastScroll)
    o.put("read", JSONArray(read.toList()))
    o.put("created", created)
    return o
}

fun workFromJson(o: JSONObject): Work {
    var id = o.optString("id", "")
    if (id.isBlank()) id = UUID.randomUUID().toString()
    val w = Work(id = id)
    w.title = o.optString("title", "")
    w.altTitle = o.optString("altTitle", "")
    w.author = o.optString("author", "")
    w.type = o.optString("type", "Mangá")
    w.synopsis = o.optString("synopsis", "")
    w.status = o.optString("status", STATUS_PLAN)
    w.current = o.optDouble("current", 0.0)
    w.total = o.optInt("total", 0)
    w.rating = o.optDouble("rating", 0.0)
    w.notes = o.optString("notes", "")
    w.favorite = o.optBoolean("favorite", false)
    w.cover = o.optString("cover", "")
    w.coverAnim = o.optString("coverAnim", "")
    w.lastUrl = o.optString("lastUrl", "")
    w.lastRead = o.optLong("lastRead", 0L)
    w.lastScroll = o.optInt("lastScroll", 0)
    w.created = o.optLong("created", System.currentTimeMillis())
    o.optJSONArray("genres")?.let { a -> for (i in 0 until a.length()) w.genres.add(a.getString(i)) }
    o.optJSONArray("tags")?.let { a -> for (i in 0 until a.length()) w.tags.add(a.getString(i)) }
    o.optJSONArray("read")?.let { a -> for (i in 0 until a.length()) w.read.add(a.getInt(i)) }
    o.optJSONArray("links")?.let { a ->
        for (i in 0 until a.length()) {
            val lo = a.getJSONObject(i)
            w.links.add(Link(lo.optString("label", ""), lo.optString("url", ""), lo.optBoolean("primary", false)))
        }
    }
    return w
}

class Store(private val ctx: Context) {
    val works = ArrayList<Work>()
    private val file = File(ctx.filesDir, "library.json")
    private val bak = File(ctx.filesDir, "library.bak")
    val coversDir = File(ctx.filesDir, "covers")

    init {
        coversDir.mkdirs()
        if (!tryLoad(file)) tryLoad(bak)
    }

    private fun tryLoad(f: File): Boolean {
        if (!f.exists()) return false
        return try {
            val arr = JSONObject(f.readText()).getJSONArray("works")
            val tmp = ArrayList<Work>()
            for (i in 0 until arr.length()) tmp.add(workFromJson(arr.getJSONObject(i)))
            works.clear()
            works.addAll(tmp)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun save() {
        try {
            val arr = JSONArray()
            for (w in works) arr.put(w.toJson())
            val root = JSONObject()
            root.put("version", 1)
            root.put("works", arr)
            val tmp = File(ctx.filesDir, "library.json.tmp")
            tmp.writeText(root.toString())
            if (file.exists()) file.copyTo(bak, overwrite = true)
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        } catch (e: Exception) {
            // ignora
        }
    }

    fun get(id: String): Work? = works.firstOrNull { it.id == id }

    fun deleteCoverFile(name: String) {
        if (name.isNotEmpty()) File(coversDir, name).delete()
    }

    fun delete(w: Work) {
        works.remove(w)
        deleteCoverFile(w.cover)
        deleteCoverFile(w.coverAnim)
        save()
    }

    fun deleteAll() {
        for (w in works) { deleteCoverFile(w.cover); deleteCoverFile(w.coverAnim) }
        works.clear()
        save()
    }

    fun exportJson(): JSONObject {
        val arr = JSONArray()
        var animBudget = 40L * 1024 * 1024 // limite p/ não estourar a memória no backup
        for (w in works) {
            val o = w.toJson()
            if (w.cover.isNotEmpty()) {
                val f = File(coversDir, w.cover)
                if (f.exists()) o.put("coverData", Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
            }
            if (w.coverAnim.isNotEmpty()) {
                val f = File(coversDir, w.coverAnim)
                if (f.exists() && f.length() <= animBudget) {
                    animBudget -= f.length()
                    o.put("animData", Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
                    o.put("animExt", f.extension)
                }
            }
            arr.put(o)
        }
        val root = JSONObject()
        root.put("app", "MangaDeck")
        root.put("version", 1)
        root.put("exported", System.currentTimeMillis())
        root.put("works", arr)
        return root
    }

    fun importJson(text: String, replace: Boolean): Int {
        val arr = JSONObject(text).getJSONArray("works")
        val incoming = ArrayList<Work>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val w = workFromJson(o)
            val data = o.optString("coverData", "")
            w.cover = ""
            w.coverAnim = ""
            val anim = o.optString("animData", "")
            if (anim.isNotEmpty()) {
                try {
                    val ext = o.optString("animExt", "mp4").ifEmpty { "mp4" }
                    val an = w.id + "_" + System.currentTimeMillis() + "_a." + ext
                    File(coversDir, an).writeBytes(Base64.decode(anim, Base64.DEFAULT))
                    w.coverAnim = an
                } catch (e: Exception) {
                    w.coverAnim = ""
                }
            }
            if (data.isNotEmpty()) {
                try {
                    val name = w.id + "_" + System.currentTimeMillis() + ".jpg"
                    File(coversDir, name).writeBytes(Base64.decode(data, Base64.DEFAULT))
                    w.cover = name
                } catch (e: Exception) {
                    w.cover = ""
                }
            }
            incoming.add(w)
        }
        if (replace) {
            for (w in works) { deleteCoverFile(w.cover); deleteCoverFile(w.coverAnim) }
            works.clear()
        } else {
            for (inc in incoming) {
                val old = works.firstOrNull { it.id == inc.id }
                if (old != null) {
                    deleteCoverFile(old.cover)
                    deleteCoverFile(old.coverAnim)
                    works.remove(old)
                }
            }
        }
        works.addAll(incoming)
        save()
        return incoming.size
    }
}

data class HistItem(val title: String, val url: String, val time: Long)

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    var theme: Int
        get() = sp.getInt("theme", 0)
        set(v) { sp.edit().putInt("theme", v).apply() }

    var zoom: Int
        get() = sp.getInt("zoom", 100)
        set(v) { sp.edit().putInt("zoom", v).apply() }

    var homeUrl: String
        get() = sp.getString("homeUrl", "https://www.google.com") ?: "https://www.google.com"
        set(v) { sp.edit().putString("homeUrl", v).apply() }

    /** Capas animadas (GIF/vídeo). Desligado = mostra só 1 quadro, como imagem. */
    var animCovers: Boolean
        get() = sp.getBoolean("animCovers", true)
        set(v) { sp.edit().putBoolean("animCovers", v).apply() }

    var webDark: Boolean
        get() = sp.getBoolean("webDark", true)
        set(v) { sp.edit().putBoolean("webDark", v).apply() }

    /** Abas abertas do navegador (JSON), para retomar exatamente de onde parou. */
    var browserState: String
        get() = sp.getString("browserState", "") ?: ""
        set(v) { sp.edit().putString("browserState", v).apply() }

    var tutorialSeen: Boolean
        get() = sp.getBoolean("tutorialSeen", false)
        set(v) { sp.edit().putBoolean("tutorialSeen", v).apply() }

    fun history(): List<HistItem> {
        val out = ArrayList<HistItem>()
        try {
            val a = JSONArray(sp.getString("history", "[]") ?: "[]")
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                out.add(HistItem(o.optString("t", ""), o.optString("u", ""), o.optLong("d", 0L)))
            }
        } catch (e: Exception) {
            // ignora
        }
        return out
    }

    fun addHistory(title: String, url: String) {
        val list = ArrayList(history())
        if (list.isNotEmpty() && list[0].url == url) {
            list[0] = HistItem(title, url, System.currentTimeMillis())
        } else {
            list.add(0, HistItem(title, url, System.currentTimeMillis()))
        }
        while (list.size > 200) list.removeAt(list.size - 1)
        val a = JSONArray()
        for (h in list) {
            val o = JSONObject()
            o.put("t", h.title)
            o.put("u", h.url)
            o.put("d", h.time)
            a.put(o)
        }
        sp.edit().putString("history", a.toString()).apply()
    }

    fun clearHistory() {
        sp.edit().putString("history", "[]").apply()
    }
}
