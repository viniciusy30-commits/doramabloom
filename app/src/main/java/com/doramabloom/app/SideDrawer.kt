package com.doramabloom.app

import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout

/**
 * Raiz da tela com um menu lateral: arraste a partir da borda esquerda para abrir,
 * toque fora ou arraste de volta para fechar. O resto da tela (ex.: trocar de gênero na Estante) não é afetado.
 */
class SideDrawerLayout(ctx: Context) : FrameLayout(ctx) {
    private val scrim = View(ctx)
    private var panel: View? = null
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private val edge = ctx.dp(26)
    private var panelW = ctx.dp(190)
    private var dragging = false
    private var downX = 0f
    private var downY = 0f
    private var armed = false
    private var startOffset = 0f
    private var offset = 0f // 0 = fechado, 1 = aberto
    var isOpen = false
        private set

    init {
        scrim.setBackgroundColor(Color.BLACK)
        scrim.alpha = 0f
        scrim.visibility = View.GONE
        scrim.setOnClickListener { close() }
    }

    /** Coloca o menu por cima de tudo. */
    fun attachPanel(p: View, width: Int) {
        panelW = width
        panel = p
        addView(scrim, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(p, LayoutParams(width, LayoutParams.MATCH_PARENT))
        p.translationX = -width.toFloat()
        p.visibility = View.INVISIBLE
    }

    private fun apply(o: Float) {
        offset = o.coerceIn(0f, 1f)
        val p = panel ?: return
        p.visibility = if (offset > 0f) View.VISIBLE else View.INVISIBLE
        p.translationX = -panelW * (1f - offset)
        scrim.visibility = if (offset > 0f) View.VISIBLE else View.GONE
        scrim.alpha = 0.32f * offset
    }

    private fun animateTo(target: Float) {
        val from = offset
        val a = android.animation.ValueAnimator.ofFloat(from, target)
        a.duration = 220
        a.interpolator = DecelerateInterpolator()
        a.addUpdateListener { apply(it.animatedValue as Float) }
        a.start()
        isOpen = target >= 1f
    }

    fun open() = animateTo(1f)
    fun close() = animateTo(0f)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                dragging = false
                // fechado: só a borda esquerda arma o gesto; aberto: qualquer toque pode arrastar para fechar
                armed = isOpen || ev.x <= edge
                startOffset = offset
            }
            MotionEvent.ACTION_MOVE -> {
                if (armed && !dragging) {
                    val dx = ev.x - downX
                    val dy = ev.y - downY
                    if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy) * 1.4f) {
                        if ((!isOpen && dx > 0) || (isOpen && dx < 0)) {
                            dragging = true
                            parent?.requestDisallowInterceptTouchEvent(true)
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!dragging) return super.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> apply(startOffset + (ev.x - downX) / panelW)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                armed = false
                if (offset > 0.4f) open() else close()
            }
        }
        return true
    }
}
