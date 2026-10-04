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
    val emoji: String,
    val primary: Int,
    val soft: Int,
    val dark: Int,
    val petals: List<String>,
    val tagline: String
)

object Genres {
    private fun c(s: String): Int = Color.parseColor(s)

    val all: List<Genre> = listOf(
        Genre("romance", "Romance", "💕", c("#FF6B9D"), c("#FFE4EE"), c("#A3305B"),
            listOf("🌸", "💕", "💗"), "Para suspirar abraçada na almofada 💕"),
        Genre("comedia", "Comédia", "🤭", c("#FFB84D"), c("#FFF3D6"), c("#8A5A00"),
            listOf("🌼", "⭐", "🍋"), "Risadinhas garantidas 🤭"),
        Genre("acao", "Ação", "⚔️", c("#FF7A6B"), c("#FFE5E0"), c("#9C2F22"),
            listOf("⚡", "✨", "🌸"), "Coração acelerado e muita adrenalina ⚡"),
        Genre("terror", "Terror", "🌙", c("#9B7EDE"), c("#EDE6FA"), c("#4B2E8F"),
            listOf("🦇", "🌙", "✨", "👻"), "Luz acesa e coberta até o nariz 🌙"),
        Genre("fantasia", "Fantasia", "✨", c("#6FA8FF"), c("#E3EEFF"), c("#1F4F9C"),
            listOf("✨", "🦋", "💫"), "Magia, brilho e mundos encantados ✨"),
        Genre("historico", "Histórico", "🏯", c("#D6536D"), c("#FBE3E8"), c("#8E1F3A"),
            listOf("🌸", "🍁", "🏮"), "Hanboks, palácios e intrigas da corte 🏯"),
        Genre("misterio", "Mistério", "🔍", c("#5FB3A8"), c("#DFF4F1"), c("#1F6B62"),
            listOf("🔍", "🍃", "✨"), "Quem será o culpado? 🔍"),
        Genre("escolar", "Escolar", "🎒", c("#7FD1AE"), c("#E1F7EC"), c("#226B4B"),
            listOf("📚", "🌱", "🍀"), "Uniformes, amizades e primeiros amores 🎒"),
        Genre("drama", "Drama", "🥺", c("#C38BD8"), c("#F6E6FB"), c("#6E2F86"),
            listOf("🌷", "💧", "🍃"), "Lencinho por perto, vai ter choro 🥺")
    )

    fun byKey(k: String): Genre = all.firstOrNull { it.key == k } ?: all[0]
}

data class Status(val key: String, val label: String, val emoji: String, val color: Int)

object Statuses {
    val all: List<Status> = listOf(
        Status("assistindo", "Assistindo", "📺", Color.parseColor("#6FA8FF")),
        Status("quero", "Quero ver", "🌱", Color.parseColor("#C38BD8")),
        Status("concluido", "Concluído", "✅", Color.parseColor("#5CC6A0")),
        Status("pausado", "Pausado", "⏸️", Color.parseColor("#FFB84D")),
        Status("dropado", "Dropado", "🥀", Color.parseColor("#B7A3AE"))
    )

    fun byKey(k: String): Status = all.firstOrNull { it.key == k } ?: all[1]
}

val countries: List<String> = listOf(
    "Coreia do Sul 🇰🇷", "Japão 🇯🇵", "China 🇨🇳", "Tailândia 🇹🇭", "Taiwan 🇹🇼", "Outro 🌏"
)

data class Drama(
    var id: Long,
    var title: String,
    var country: String,
    var genre: String,
    var status: String,
    var rating: Int,
    var epWatched: Int,
    var epTotal: Int,
    var year: String,
    var platform: String,
    var cast: String,
    var notes: String,
    var favorite: Boolean,
    var cover: String,
    var addedAt: Long
)

fun progressText(d: Drama): String =
    if (d.epTotal > 0) "Episódio ${d.epWatched} de ${d.epTotal}" else "Episódio ${d.epWatched}"

fun progressOf(d: Drama): Float =
    if (d.epTotal > 0) d.epWatched.toFloat() / d.epTotal.toFloat() else 0f

private fun Drama.toJson(): JSONObject {
    val o = JSONObject()
    o.put("id", id)
    o.put("title", title)
    o.put("country", country)
    o.put("genre", genre)
    o.put("status", status)
    o.put("rating", rating)
    o.put("epWatched", epWatched)
    o.put("epTotal", epTotal)
    o.put("year", year)
    o.put("platform", platform)
    o.put("cast", cast)
    o.put("notes", notes)
    o.put("favorite", favorite)
    o.put("cover", cover)
    o.put("addedAt", addedAt)
    return o
}

private fun dramaFromJson(o: JSONObject): Drama = Drama(
    id = o.optLong("id", System.currentTimeMillis()),
    title = o.optString("title", ""),
    country = o.optString("country", countries[0]),
    genre = o.optString("genre", "romance"),
    status = o.optString("status", "quero"),
    rating = o.optInt("rating", 0),
    epWatched = o.optInt("epWatched", 0),
    epTotal = o.optInt("epTotal", 0),
    year = o.optString("year", ""),
    platform = o.optString("platform", ""),
    cast = o.optString("cast", ""),
    notes = o.optString("notes", ""),
    favorite = o.optBoolean("favorite", false),
    cover = o.optString("cover", ""),
    addedAt = o.optLong("addedAt", System.currentTimeMillis())
)

object Store {
    private const val PREF = "doramabloom"
    private const val KEY = "dramas"

    private lateinit var prefs: SharedPreferences
    lateinit var appContext: Context
    private val list = ArrayList<Drama>()
    private var loaded = false

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
        val arr = JSONArray()
        for (d in list) arr.put(d.toJson())
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun all(): List<Drama> = list.toList()

    fun get(id: Long): Drama? = list.firstOrNull { it.id == id }

    fun save(d: Drama) {
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

    /** Soma um episódio. Devolve true se acabou de concluir o dorama. */
    fun bump(d: Drama): Boolean {
        var finished = false
        d.epWatched += 1
        if (d.epTotal > 0 && d.epWatched >= d.epTotal) {
            d.epWatched = d.epTotal
            if (d.status != "concluido") finished = true
            d.status = "concluido"
        } else if (d.status != "assistindo") {
            d.status = "assistindo"
        }
        persist()
        return finished
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
