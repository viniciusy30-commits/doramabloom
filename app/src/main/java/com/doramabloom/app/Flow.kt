
package com.doramabloom.app

import android.content.Context
import android.view.View
import android.view.ViewGroup

/** Linha que quebra para a próxima quando falta espaço: nada fica cortado. Com center = true, cada linha é centralizada. */
class FlowLayout(ctx: Context) : ViewGroup(ctx) {
    var hGap = 0
    var vGap = 0
    var center = false

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val mode = MeasureSpec.getMode(widthSpec)
        val avail = if (mode == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE / 4 else MeasureSpec.getSize(widthSpec)
        val maxW = avail - paddingLeft - paddingRight
        var x = 0
        var y = 0
        var rowH = 0
        var used = 0
        var any = false
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == View.GONE) continue
            c.measure(
                MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            val cw = c.measuredWidth
            val ch = c.measuredHeight
            if (x > 0 && x + cw > maxW) {
                x = 0
                y += rowH + vGap
                rowH = 0
            }
            x += cw + hGap
            if (ch > rowH) rowH = ch
            if (x - hGap > used) used = x - hGap
            any = true
        }
        val h = if (any) y + rowH + paddingTop + paddingBottom else paddingTop + paddingBottom
        val w = if (mode == MeasureSpec.EXACTLY) avail else used + paddingLeft + paddingRight
        setMeasuredDimension(w, resolveSize(h, heightSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val maxW = r - l - paddingLeft - paddingRight
        // agrupa os filhos em linhas
        val rows = ArrayList<ArrayList<View>>()
        var cur = ArrayList<View>()
        var x = 0
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == View.GONE) continue
            val cw = c.measuredWidth
            if (x > 0 && x + cw > maxW) {
                rows.add(cur)
                cur = ArrayList()
                x = 0
            }
            cur.add(c)
            x += cw + hGap
        }
        if (cur.isNotEmpty()) rows.add(cur)

        var y = 0
        for (row in rows) {
            var rowW = 0
            var rowH = 0
            for (c in row) {
                rowW += c.measuredWidth
                if (c.measuredHeight > rowH) rowH = c.measuredHeight
            }
            rowW += hGap * (row.size - 1)
            var px = paddingLeft + if (center) ((maxW - rowW) / 2).coerceAtLeast(0) else 0
            val top = paddingTop + y
            for (c in row) {
                c.layout(px, top, px + c.measuredWidth, top + c.measuredHeight)
                px += c.measuredWidth + hGap
            }
            y += rowH + vGap
        }
    }
}
