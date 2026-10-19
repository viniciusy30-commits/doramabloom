package com.doramabloom.app

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewParent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * Alça de arrastar para listas de linhas (usada na ordem dos gêneros).
 * Segure o ícone e mova para cima ou para baixo; as outras linhas abrem espaço enquanto você move.
 * [onDrop] recebe a posição de origem e a de destino e roda fora do toque (a tela pode se refazer).
 */
fun Context.reorderHandle(
    row: View,
    rows: List<View>,
    index: Int,
    tint: Int,
    onDrop: (from: Int, to: Int) -> Unit
): View {
    val handle = IconView(this, "sort", tint, 24)
    val hbox = FrameLayout(this)
    hbox.setPadding(dp(8), dp(6), dp(2), dp(6))
    hbox.addView(handle, FrameLayout.LayoutParams(dp(26), dp(26)))
    var startY = 0f
    var scrolled = 0f
    var target = index
    hbox.setOnTouchListener { v, ev ->
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                v.parent.requestDisallowInterceptTouchEvent(true)
                startY = ev.rawY
                scrolled = 0f
                target = index
                row.elevation = dp(10).toFloat()
                row.alpha = 0.92f
                true
            }
            MotionEvent.ACTION_MOVE -> {
                // rola a tela sozinha quando o dedo chega perto da borda
                var sc: ViewParent? = row.parent
                while (sc != null && sc !is ScrollView) sc = sc.parent
                if (sc is ScrollView) {
                    val loc = IntArray(2)
                    sc.getLocationOnScreen(loc)
                    val step = dp(14)
                    val before = sc.scrollY
                    if (ev.rawY < loc[1] + dp(90)) sc.scrollBy(0, -step)
                    else if (ev.rawY > loc[1] + sc.height - dp(90)) sc.scrollBy(0, step)
                    scrolled += (sc.scrollY - before).toFloat()
                }
                val dy = ev.rawY - startY + scrolled
                row.translationY = dy
                val lp = row.layoutParams as? LinearLayout.LayoutParams
                val gap = (lp?.topMargin ?: 0)
                val h = (row.height + gap).toFloat()
                target = (index + Math.round(dy / h)).coerceIn(0, rows.size - 1)
                for (j in rows.indices) {
                    if (j == index) continue
                    val shift = when {
                        index < target && j in (index + 1)..target -> -h
                        index > target && j in target until index -> h
                        else -> 0f
                    }
                    rows[j].animate().translationY(shift).setDuration(120).start()
                }
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.parent.requestDisallowInterceptTouchEvent(false)
                val drop = ev.actionMasked == MotionEvent.ACTION_UP && target != index
                val to = target
                rows.forEach { it.animate().cancel() }
                // não mexe na lista dentro do toque: refazer a tela aqui quebra a hierarquia
                v.post { if (drop) onDrop(index, to) }
                if (!drop) {
                    row.translationY = 0f
                    row.alpha = 1f
                    row.elevation = 0f
                    rows.forEach { it.translationY = 0f }
                }
                true
            }
            else -> true
        }
    }
    return hbox
}
