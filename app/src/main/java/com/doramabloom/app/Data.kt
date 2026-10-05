package com.doramabloom.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class Genre(
    val key: String,
    val label: String,
    val icon: String,
    val primary: Int,
    val soft: Int,
    val dark: Int,
    val petals: List<String>,
    val tagline: String
)

object Genres {
    private fun c(s: String): Int = Color.parseColor(s)

    val all: List<Genre> = listOf(
        Genre("romance", "Romance", "heart", c("#FF6B9D"), c("#FFE4EE"), c("#A3305B"),
            listOf("petal", "heart", "blossom"), "Para suspirar abraçada na almofada"),
        Genre("comedia", "Comédia", "smile", c("#FFB84D"), c("#FFF3D6"), c("#8A5A00"),
            listOf("star", "blossom", "sparkle"), "Risadinhas garantidas"),
        Genre("acao", "Ação", "bolt", c("#FF7A6B"), c("#FFE5E0"), c("#9C2F22"),
            listOf("bolt", "sparkle", "petal"), "Coração acelerado e muita adrenalina"),
        Genre("terror", "Terror", "ghost", c("#9B7EDE"), c("#EDE6FA"), c("#4B2E8F"),
            listOf("moon", "ghost", "sparkle"), "Luz acesa e coberta até o nariz"),
        Genre("fantasia", "Fantasia", "sparkle", c("#6FA8FF"), c("#E3EEFF"), c("#1F4F9C"),
            listOf("sparkle", "star", "petal"), "Magia, brilho e mundos encantados"),
        Genre("historico", "Histórico", "pagoda", c("#D6536D"), c("#FBE3E8"), c("#8E1F3A"),
            listOf("petal", "blossom", "leaf"), "Hanboks, palácios e intrigas da corte"),
        Genre("misterio", "Mistério", "search", c("#5FB3A8"), c("#DFF4F1"), c("#1F6B62"),
            listOf("leaf", "sparkle", "star"), "Quem será o culpado?"),
        Genre("escolar", "Escolar", "school", c("#7FD1AE"), c("#E1F7EC"), c("#226B4B"),
            listOf("star", "leaf", "blossom"), "Uniformes, amizades e primeiros amores"),
        Genre("drama", "Drama", "drop", c("#C38BD8"), c("#F6E6FB"), c("#6E2F86"),
            listOf("drop", "petal", "leaf"), "Lencinho por perto, vai ter choro")
    )

    fun byKey(k: String): Genre = all.firstOrNull { it.key == k } ?: all[0]
}

data class Status(val key: String, val label: String, val icon: String, val color: Int)

object Statuses {
    val all: List<Status> = listOf(
        Status("assistindo", "Assistindo", "play", Color.parseColor("#6FA8FF")),
        Status("quero", "Quero ver", "bookmark", Color.parseColor("#C38BD8")),
        Status("concluido", "Concluído", "check", Color.parseColor("#5CC6A0")),
        Status("pausado", "Pausado", "pause", Color.parseColor("#FFB84D")),
        Status("dropado", "Dropado", "close", Color.parseColor("#B7A3AE"))
    )

    fun byKey(k: String): Status = all.firstOrNull { it.key == k } ?: all[1]
}

val countries: List<String> = listOf(
    "Coreia do Sul", "Japão", "China", "Tailândia", "Taiwan", "Outro"
)

data class Drama(
    var id: Long,
    var title: String,
    var original: String,
    var synopsis: String,
    var country: String,
    var genre: String,
    var tags: List<String>,
    var status: String,
    var score: Int,
    var seasonEps: List<Int>,
    var watched: List<Int>,
    var epMinutes: Int,
    var year: String,
    var platform: String,
    var cast: String,
    var couple: String,
    var startDate: Long,
    var endDate: Long,
    var rewatch: Int,
    var notes: String,
    var favorite: Boolean,
    var cover: String,
    var addedAt: Long,
    var link: String = "",
    var lastUrl: String = ""
)

fun totalEps(d: Drama): Int = d.seasonEps.sum()
fun watchedEps(d: Drama): Int = d.watched.sum()
fun seasonCount(d: Drama): Int = d.seasonEps.size

fun currentSeason(d: Drama): Int {
    for (i in d.seasonEps.indices) {
        val t = d.seasonEps[i]
        if (t == 0 || d.watched[i] < t) return i
    }
    return maxOf(0, d.seasonEps.size - 1)
}

fun progressText(d: Drama): String {
    val t = totalEps(d)
    val w = watchedEps(d)
    if (seasonCount(d) <= 1) {
        return if (t > 0) "Ep. $w de $t" else "Ep. $w"
    }
    val cs = currentSeason(d)
    val st = d.seasonEps[cs]
    val part = if (st > 0) "${d.watched[cs]}/$st" else "${d.watched[cs]}"
    return "Temp. ${cs + 1} · Ep. $part"
}

fun progressOf(d: Drama): Float {
    val t = totalEps(d)
    return if (t > 0) watchedEps(d).toFloat() / t.toFloat() else 0f
}

fun subtitle(d: Drama): String {
    val parts = ArrayList<String>()
    parts.add(d.country)
    if (d.year.isNotBlank()) parts.add(d.year)
    if (seasonCount(d) > 1) parts.add(seasonCount(d).toString() + " temp.")
    val t = totalEps(d)
    if (t > 0) parts.add(t.toString() + " eps")
    return parts.joinToString(" · ")
}

fun hoursWatched(d: Drama): Double =
    watchedEps(d) * (if (d.epMinutes > 0) d.epMinutes else 60) / 60.0

fun normalize(d: Drama) {
    val e = d.seasonEps.toMutableList()
    val w = d.watched.toMutableList()
    if (e.isEmpty()) e.add(0)
    while (w.size < e.size) w.add(0)
    while (w.size > e.size) w.removeAt(w.size - 1)
    for (i in e.indices) {
        if (e[i] < 0) e[i] = 0
        if (w[i] < 0) w[i] = 0
        if (e[i] > 0 && w[i] > e[i]) w[i] = e[i]
    }
    d.seasonEps = e
    d.watched = w
}

fun applyStatus(d: Drama, key: String) {
    d.status = key
    val now = System.currentTimeMillis()
    if (key == "assistindo" && d.startDate == 0L) d.startDate = now
    if (key == "concluido") {
        val w = ArrayList<Int>()
        for (i in d.seasonEps.indices) {
            w.add(if (d.seasonEps[i] > 0) d.seasonEps[i] else d.watched[i])
        }
        d.watched = w
        if (d.startDate == 0L) d.startDate = now
        if (d.endDate == 0L) d.endDate = now
    }
}

private fun intsToJson(l: List<Int>): JSONArray {
    val a = JSONArray()
    for (v in l) a.put(v)
    return a
}

private fun jsonInts(a: JSONArray?): List<Int> {
    val r = ArrayList<Int>()
    if (a != null) {
        for (i in 0 until a.length()) r.add(a.optInt(i, 0))
    }
    return r
}

private fun jsonStrings(a: JSONArray?): List<String> {
    val r = ArrayList<String>()
    if (a != null) {
        for (i in 0 until a.length()) r.add(a.optString(i, ""))
    }
    return r
}

private fun Drama.toJson(): JSONObject {
    val o = JSONObject()
    o.put("id", id)
    o.put("title", title)
    o.put("original", original)
    o.put("synopsis", synopsis)
    o.put("country", country)
    o.put("genre", genre)
    val ta = JSONArray()
    for (t in tags) ta.put(t)
    o.put("tags", ta)
    o.put("status", status)
    o.put("score", score)
    o.put("seasonEps", intsToJson(seasonEps))
    o.put("watched", intsToJson(watched))
    o.put("epMinutes", epMinutes)
    o.put("year", year)
    o.put("platform", platform)
    o.put("cast", cast)
    o.put("couple", couple)
    o.put("startDate", startDate)
    o.put("endDate", endDate)
    o.put("rewatch", rewatch)
    o.put("notes", notes)
    o.put("favorite", favorite)
    o.put("cover", cover)
    o.put("addedAt", addedAt)
    o.put("link", link)
    o.put("lastUrl", lastUrl)
    return o
}

private fun dramaFromJson(o: JSONObject): Drama {
    var eps = jsonInts(o.optJSONArray("seasonEps"))
    var wat = jsonInts(o.optJSONArray("watched"))
    if (eps.isEmpty()) {
        // versão antiga: um único bloco de episódios
        eps = listOf(o.optInt("epTotal", 0))
        wat = listOf(o.optInt("epWatched", 0))
    }
    val score = if (o.has("score")) o.optInt("score", 0) else o.optInt("rating", 0) * 2
    val d = Drama(
        id = o.optLong("id", System.currentTimeMillis()),
        title = o.optString("title", ""),
        original = o.optString("original", ""),
        synopsis = o.optString("synopsis", ""),
        country = o.optString("country", countries[0]),
        genre = o.optString("genre", "romance"),
        tags = jsonStrings(o.optJSONArray("tags")),
        status = o.optString("status", "quero"),
        score = score,
        seasonEps = eps,
        watched = wat,
        epMinutes = o.optInt("epMinutes", 0),
        year = o.optString("year", ""),
        platform = o.optString("platform", ""),
        cast = o.optString("cast", ""),
        couple = o.optString("couple", ""),
        startDate = o.optLong("startDate", 0L),
        endDate = o.optLong("endDate", 0L),
        rewatch = o.optInt("rewatch", 0),
        notes = o.optString("notes", ""),
        favorite = o.optBoolean("favorite", false),
        cover = o.optString("cover", ""),
        addedAt = o.optLong("addedAt", System.currentTimeMillis()),
        link = o.optString("link", ""),
        lastUrl = o.optString("lastUrl", "")
    )
    normalize(d)
    return d
}

object Store {
    private const val PREF = "doramabloom"
    private const val KEY = "dramas"

    private lateinit var prefs: SharedPreferences
    lateinit var appContext: Context
    private val list = ArrayList<Drama>()
    private var loaded = false

    /** Sobe a cada gravação; as telas usam para saber se precisam se atualizar. */
    var version = 0
        private set

    fun init(c: Context) {
        if (loaded) return
        appContext = c.applicationContext
        prefs = appContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        load()
        loaded = true
    }

    var userName: String
        get() = prefs.getString("userName", "") ?: ""
        set(v) {
            prefs.edit().putString("userName", v).apply()
        }

    var askedName: Boolean
        get() = prefs.getBoolean("askedName", false)
        set(v) {
            prefs.edit().putBoolean("askedName", v).apply()
        }

    private fun load() {
        list.clear()
        try {
            val arr = JSONArray(prefs.getString(KEY, "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                list.add(dramaFromJson(arr.getJSONObject(i)))
            }
        } catch (e: Exception) {
            // dados corrompidos: começa vazio
        }
    }

    private fun persist() {
        version++
        val arr = JSONArray()
        for (d in list) arr.put(d.toJson())
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun all(): List<Drama> = list.toList()

    fun get(id: Long): Drama? = list.firstOrNull { it.id == id }

    fun save(d: Drama) {
        normalize(d)
        val i = list.indexOfFirst { it.id == d.id }
        if (i >= 0) list[i] = d else list.add(0, d)
        persist()
    }

    fun delete(id: Long) {
        val d = get(id) ?: return
        if (d.cover.isNotEmpty()) {
            try {
                File(d.cover).delete()
            } catch (e: Exception) {
            }
            Covers.clear()
        }
        list.removeAll { it.id == id }
        persist()
    }

    /** Soma um episódio na temporada em andamento. Devolve true se acabou de concluir o dorama. */
    fun bump(d: Drama): Boolean {
        val w = d.watched.toMutableList()
        var moved = false
        for (i in d.seasonEps.indices) {
            val t = d.seasonEps[i]
            if (t == 0 || w[i] < t) {
                w[i] = w[i] + 1
                moved = true
                break
            }
        }
        if (!moved) return false
        d.watched = w
        return afterProgress(d)
    }

    /** Ajusta os episódios de uma temporada específica (delta positivo ou negativo). */
    fun adjust(d: Drama, season: Int, delta: Int): Boolean {
        if (season < 0 || season >= d.seasonEps.size) return false
        val w = d.watched.toMutableList()
        val t = d.seasonEps[season]
        var nv = w[season] + delta
        if (nv < 0) nv = 0
        if (t > 0 && nv > t) nv = t
        w[season] = nv
        d.watched = w
        if (delta < 0 && d.status == "concluido") d.status = "assistindo"
        return afterProgress(d)
    }

    private fun afterProgress(d: Drama): Boolean {
        var finished = false
        val total = totalEps(d)
        if (watchedEps(d) > 0 && total > 0 && d.seasonEps.all { it > 0 } && watchedEps(d) >= total) {
            if (d.status != "concluido") finished = true
            applyStatus(d, "concluido")
        } else if (watchedEps(d) > 0 && d.status != "assistindo" && d.status != "concluido") {
            applyStatus(d, "assistindo")
        }
        persist()
        return finished
    }

    fun setStatus(d: Drama, key: String) {
        applyStatus(d, key)
        persist()
    }

    fun persistNow() {
        persist()
    }

    fun exportJson(): String {
        val arr = JSONArray()
        for (d in list) {
            val o = d.toJson()
            o.put("cover", "")
            arr.put(o)
        }
        return arr.toString()
    }

    /** Importa um backup. Devolve quantos foram adicionados, ou -1 se o texto for inválido. */
    fun importJson(s: String): Int {
        return try {
            val arr = JSONArray(s.trim())
            var n = 0
            for (i in 0 until arr.length()) {
                val d = dramaFromJson(arr.getJSONObject(i))
                if (d.title.isBlank()) continue
                if (list.any { it.title.equals(d.title, true) && it.year == d.year }) continue
                d.cover = ""
                d.id = System.currentTimeMillis() + i
                list.add(d)
                n++
            }
            persist()
            n
        } catch (e: Exception) {
            -1
        }
    }

    /** Copia a imagem escolhida para a pasta do app (reduzida) e devolve o caminho. */
    fun saveCover(uri: Uri): String? {
        return try {
            val cr = appContext.contentResolver
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 1400 || bounds.outHeight / sample > 1400) sample *= 2
            val opts = BitmapFactory.Options()
            opts.inSampleSize = sample
            var bmp: Bitmap = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            var rot = 0
            try {
                cr.openInputStream(uri)?.use { s ->
                    val ex = ExifInterface(s)
                    val o = ex.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    rot = when (o) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            } catch (e: Exception) {
            }
            if (rot != 0) {
                val m = Matrix()
                m.postRotate(rot.toFloat())
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            }
            val dir = File(appContext.filesDir, "covers")
            dir.mkdirs()
            val f = File(dir, "c" + System.currentTimeMillis() + ".jpg")
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            f.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
