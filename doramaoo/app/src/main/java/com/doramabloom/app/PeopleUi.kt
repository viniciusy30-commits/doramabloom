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
    square: Boolean = false
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
    return f
}
