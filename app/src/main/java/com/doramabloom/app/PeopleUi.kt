package com.doramabloom.app

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import java.io.File

/**
 * Foto de ator, atriz ou casal: redonda (ou quadrada arredondada) com anel colorido.
 * Sem foto, mostra um fundo suave com um símbolo. O tamanho quem define é quem usa (lin).
 */
fun Context.avatarView(
    path: String,
    sizeDp: Int,
    ring: Int,
    soft: Int,
    glyph: Int,
    icon: String = "person",
    square: Boolean = false,
    zoomable: Boolean = false,
    caption: String = ""
): FrameLayout {
    val f = FrameLayout(this)
    val bg = GradientDrawable()
    if (square) {
        bg.shape = GradientDrawable.RECTANGLE
        bg.cornerRadius = dp(26).toFloat()
    } else {
        bg.shape = GradientDrawable.OVAL
    }
    bg.setColor(ring)
    f.background = bg

    val inner = FrameLayout(this)
    inner.setBackgroundColor(soft)
    inner.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(v: View, o: Outline) {
            if (square) o.setRoundRect(0, 0, v.width, v.height, dp(23).toFloat()) else o.setOval(0, 0, v.width, v.height)
        }
    }
    inner.clipToOutline = true
    val bmp = if (path.isNotEmpty() && File(path).exists()) Covers.load(path, dp(sizeDp)) else null
    if (bmp != null) {
        val iv = ImageView(this)
        iv.scaleType = ImageView.ScaleType.CENTER_CROP
        iv.setImageBitmap(bmp)
        inner.addView(iv, FrameLayout.LayoutParams(MATCH, MATCH))
    } else {
        inner.addView(IconView(this, icon, glyph, maxOf(14, sizeDp / 2)), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
    }
    val lp = FrameLayout.LayoutParams(MATCH, MATCH)
    lp.setMargins(dp(3), dp(3), dp(3), dp(3))
    f.addView(inner, lp)
    f.elevation = dp(2).toFloat()
    // tocar na foto abre ela em tela cheia (só quando há foto de verdade)
    if (zoomable && bmp != null) {
        f.setOnClickListener { showPhotoViewer(path, caption) }
    }
    return f
}

/** Foto em tela cheia: toque em qualquer lugar para fechar. */
fun Context.showPhotoViewer(path: String, caption: String = "") {
    if (path.isEmpty() || !File(path).exists()) return
    val dm = resources.displayMetrics
    val target = maxOf(dm.widthPixels, dm.heightPixels)
    val gif = isGifFile(path)
    val bmp = if (gif) null else try {
        val o = android.graphics.BitmapFactory.Options()
        o.inJustDecodeBounds = true
        android.graphics.BitmapFactory.decodeFile(path, o)
        var sample = 1
        while (maxOf(o.outWidth, o.outHeight) / (sample * 2) >= target) sample *= 2
        val o2 = android.graphics.BitmapFactory.Options()
        o2.inSampleSize = sample
        android.graphics.BitmapFactory.decodeFile(path, o2)
    } catch (e: Throwable) {
        null
    }
    if (!gif && bmp == null) return
    val dlg = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    val root = FrameLayout(this)
    root.setBackgroundColor(Color.argb(235, 0, 0, 0))
    val iv: View = if (gif) {
        AnimatedGifView(this, File(path), false, 0).also { it.setPadding(dp(12), dp(40), dp(12), dp(40)) }
    } else {
        ImageView(this).also {
            it.scaleType = ImageView.ScaleType.FIT_CENTER
            it.setImageBitmap(bmp)
            it.setPadding(dp(12), dp(40), dp(12), dp(40))
        }
    }
    root.addView(iv, FrameLayout.LayoutParams(MATCH, MATCH))
    if (caption.isNotBlank()) {
        val tv = android.widget.TextView(this)
        tv.text = caption
        tv.setTextColor(Color.WHITE)
        tv.textSize = 16f
        tv.setTypeface(tv.typeface, android.graphics.Typeface.BOLD)
        tv.gravity = Gravity.CENTER
        tv.setPadding(dp(16), dp(10), dp(16), dp(28))
        root.addView(tv, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))
    }
    root.setOnClickListener { dlg.dismiss() }
    iv.setOnClickListener { dlg.dismiss() }
    dlg.setContentView(root)
    dlg.show()
}
