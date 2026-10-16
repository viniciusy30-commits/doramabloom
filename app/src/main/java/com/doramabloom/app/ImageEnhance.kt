package com.doramabloom.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Melhora a qualidade de fotos dentro do app, sem internet: amplia fotos pequenas com suavização
 * (em etapas), realça os detalhes (máscara de nitidez) e dá um toque leve de contraste e cor.
 * Não inventa detalhes que a foto não tem, mas deixa fotos pequenas ou "mole" bem mais nítidas.
 * Tudo aqui é pesado: chame sempre em segundo plano.
 */
object ImageEnhance {
    /** Lado maior até onde fotos pequenas são ampliadas. */
    private const val TARGET = 1600

    /** Abaixo disso (lado maior) a foto é melhorada sozinha ao ser salva. */
    private const val SMALL = 1000

    fun autoIfSmall(b: Bitmap): Bitmap {
        return if (maxOf(b.width, b.height) < SMALL) enhance(b) else b
    }

    fun enhance(src: Bitmap): Bitmap {
        return try {
            var b = src
            var big = maxOf(b.width, b.height)
            // ampliação em etapas de no máximo 1,5x (fica mais suave que ampliar tudo de uma vez)
            var guard = 0
            while (big < TARGET && guard < 4) {
                val k = minOf(1.5f, TARGET.toFloat() / big)
                if (k < 1.05f) break
                b = Bitmap.createScaledBitmap(b, (b.width * k).roundToInt(), (b.height * k).roundToInt(), true)
                big = maxOf(b.width, b.height)
                guard++
            }
            sharpen(b, if (b === src) 0.45f else 0.7f)
        } catch (e: Throwable) {
            src
        }
    }

    /** Máscara de nitidez: soma à foto a diferença entre ela e uma versão desfocada. */
    private fun sharpen(b: Bitmap, amount: Float): Bitmap {
        val w = b.width
        val h = b.height
        val px = IntArray(w * h)
        b.getPixels(px, 0, w, 0, 0, w, h)
        val blur = blur121(px, w, h)
        val out = IntArray(w * h)
        for (i in px.indices) {
            val c = px[i]
            val bl = blur[i]
            val r = mix((c shr 16) and 0xFF, (bl shr 16) and 0xFF, amount)
            val g = mix((c shr 8) and 0xFF, (bl shr 8) and 0xFF, amount)
            val bb = mix(c and 0xFF, bl and 0xFF, amount)
            out[i] = (c and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or bb
        }
        val res = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        res.setPixels(out, 0, w, 0, 0, w, h)
        // toque leve de contraste e cor
        val cm = ColorMatrix()
        cm.setSaturation(1.08f)
        val ct = 1.05f
        val off = (-0.5f * ct + 0.5f) * 255f
        cm.postConcat(ColorMatrix(floatArrayOf(ct, 0f, 0f, 0f, off, 0f, ct, 0f, 0f, off, 0f, 0f, ct, 0f, off, 0f, 0f, 0f, 1f, 0f)))
        val p = Paint(Paint.FILTER_BITMAP_FLAG)
        p.colorFilter = ColorMatrixColorFilter(cm)
        val fin = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(fin).drawBitmap(res, 0f, 0f, p)
        res.recycle()
        return fin
    }

    /** orig + amount * (orig - desfocada); diferenças bem pequenas (ruído da compressão) são ignoradas. */
    private fun mix(o: Int, bl: Int, amount: Float): Int {
        val d = o - bl
        if (abs(d) < 3) return o
        return (o + amount * d).roundToInt().coerceIn(0, 255)
    }

    /** Desfoque leve [1 2 1] na horizontal e na vertical. */
    private fun blur121(px: IntArray, w: Int, h: Int): IntArray {
        val tmp = IntArray(w * h)
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val a = px[row + maxOf(0, x - 1)]
                val c = px[row + x]
                val e = px[row + minOf(w - 1, x + 1)]
                tmp[row + x] = avg(a, c, e)
            }
        }
        val out = IntArray(w * h)
        for (y in 0 until h) {
            val y0 = maxOf(0, y - 1) * w
            val y1 = y * w
            val y2 = minOf(h - 1, y + 1) * w
            for (x in 0 until w) out[y1 + x] = avg(tmp[y0 + x], tmp[y1 + x], tmp[y2 + x])
        }
        return out
    }

    private fun avg(a: Int, c: Int, e: Int): Int {
        val r = (((a shr 16) and 0xFF) + 2 * ((c shr 16) and 0xFF) + ((e shr 16) and 0xFF)) shr 2
        val g = (((a shr 8) and 0xFF) + 2 * ((c shr 8) and 0xFF) + ((e shr 8) and 0xFF)) shr 2
        val b = ((a and 0xFF) + 2 * (c and 0xFF) + (e and 0xFF)) shr 2
        return (r shl 16) or (g shl 8) or b
    }

    /** Melhora uma foto que já está salva e devolve o caminho da nova (null se falhou). Em segundo plano. */
    fun enhanceFile(path: String): String? {
        return try {
            if (!File(path).exists()) return null
            val src = BitmapFactory.decodeFile(path) ?: return null
            val out = enhance(src)
            Store.saveBitmap(out)
        } catch (e: Throwable) {
            null
        }
    }
}
