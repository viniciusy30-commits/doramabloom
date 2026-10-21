package com.doramabloom.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu

/**
 * Raiz da tela com um menu lateral. O menu abre ao tocar na bolinha flutuante (com o símbolo do app):
 * a bolinha pode ser arrastada para qualquer lugar e muda de tamanho em vários níveis
 * (gesto de pinça ou segurando o dedo nela). Toque fora ou arraste o menu de volta para fechar.
 * O resto da tela (ex.: trocar de gênero na Estante) não é afetado.
 */
class SideDrawerLayout(ctx: Context) : FrameLayout(ctx) {
    private val scrim = View(ctx)
    private var panel: View? = null
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private val edge = ctx.dp(52)
    private var panelW = ctx.dp(190)
    private var dragging = false
    private var downX = 0f
    private var downY = 0f
    private var armed = false
    private var startOffset = 0f
    private var offset = 0f // 0 = fechado, 1 = aberto
    var isOpen = false
        private set

    // ---- bolinha flutuante
    private val bubble = ImageView(ctx)
    private val bPrefs = ctx.getSharedPreferences("bubble", Context.MODE_PRIVATE)
    private val bSizes = intArrayOf(26, 34, 42, 52, 64, 78, 94, 112)
    private var bIdx = bPrefs.getInt("size", 3).coerceIn(0, bSizes.size - 1)
    private var bFx = bPrefs.getFloat("fx", 0f)
    private var bFy = bPrefs.getFloat("fy", 0.5f)
    private var bDownX = 0f
    private var bDownY = 0f
    private var bStartX = 0f
    private var bStartY = 0f
    private var bMoved = false
    private var bLong = false
    private var bPinch = false
    private val bLongRun = Runnable {
        if (!bMoved && !bPinch) {
            bLong = true
            showSizeMenu()
        }
    }
    private val bScale = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var acc = 1f
        override fun onScaleBegin(d: ScaleGestureDetector): Boolean {
            acc = 1f
            return true
        }

        override fun onScale(d: ScaleGestureDetector): Boolean {
            acc *= d.scaleFactor
            if (acc > 1.18f) {
                stepBubble(1)
                acc = 1f
            } else if (acc < 0.85f) {
                stepBubble(-1)
                acc = 1f
            }
            return true
        }
    })

    private fun bubblePx() = context.dp(bSizes[bIdx])

    private fun applyBubblePos() {
        val sz = bubblePx()
        val maxX = (width - sz).coerceAtLeast(0)
        val maxY = (height - sz).coerceAtLeast(0)
        bubble.translationX = bFx.coerceIn(0f, 1f) * maxX
        bubble.translationY = bFy.coerceIn(0f, 1f) * maxY
    }

    private fun placeBubble(x: Float, y: Float) {
        val sz = bubblePx()
        val maxX = (width - sz).coerceAtLeast(0).toFloat()
        val maxY = (height - sz).coerceAtLeast(0).toFloat()
        val cx = x.coerceIn(0f, maxX)
        val cy = y.coerceIn(0f, maxY)
        bubble.translationX = cx
        bubble.translationY = cy
        bFx = if (maxX > 0f) cx / maxX else 0f
        bFy = if (maxY > 0f) cy / maxY else 0f
    }

    private fun saveBubble() {
        bPrefs.edit().putInt("size", bIdx).putFloat("fx", bFx).putFloat("fy", bFy).apply()
    }

    /** Muda o tamanho um nível para cima ou para baixo, mantendo o centro da bolinha no lugar. */
    private fun stepBubble(delta: Int) {
        val n = (bIdx + delta).coerceIn(0, bSizes.size - 1)
        setBubbleLevel(n)
    }

    private fun setBubbleLevel(n: Int) {
        if (n == bIdx) return
        val old = bubblePx()
        val cx = bubble.translationX + old / 2f
        val cy = bubble.translationY + old / 2f
        bIdx = n
        val sz = bubblePx()
        val lp = bubble.layoutParams
        lp.width = sz
        lp.height = sz
        bubble.layoutParams = lp
        placeBubble(cx - sz / 2f, cy - sz / 2f)
        saveBubble()
    }

    private fun showSizeMenu() {
        val pm = PopupMenu(context, bubble)
        pm.menu.add(0, 1, 0, "Abrir menu")
        pm.menu.add(0, 2, 1, "Aumentar bolinha")
        pm.menu.add(0, 3, 2, "Diminuir bolinha")
        pm.menu.add(0, 4, 3, "Tamanho padrão")
        pm.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> open()
                2 -> stepBubble(1)
                3 -> stepBubble(-1)
                4 -> setBubbleLevel(3)
            }
            true
        }
        pm.show()
    }

    private fun setupBubble() {
        val id = context.resources.getIdentifier("ic_launcher_round", "mipmap", context.packageName)
        bubble.setImageResource(id)
        bubble.scaleType = ImageView.ScaleType.CENTER_CROP
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.OVAL
        bg.setColor(Color.WHITE)
        bubble.background = bg
        bubble.outlineProvider = ViewOutlineProvider.BACKGROUND
        bubble.clipToOutline = true
        bubble.elevation = context.dp(8).toFloat()
        bubble.alpha = 0.94f
        bubble.contentDescription = "Menu"
        bubble.setOnTouchListener { v, e ->
            bScale.onTouchEvent(e)
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    bDownX = e.rawX
                    bDownY = e.rawY
                    bStartX = bubble.translationX
                    bStartY = bubble.translationY
                    bMoved = false
                    bLong = false
                    bPinch = false
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    v.postDelayed(bLongRun, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    bPinch = true
                    v.removeCallbacks(bLongRun)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!bPinch && e.pointerCount == 1) {
                        val dx = e.rawX - bDownX
                        val dy = e.rawY - bDownY
                        if (!bMoved && Math.hypot(dx.toDouble(), dy.toDouble()) > slop) {
                            bMoved = true
                            v.removeCallbacks(bLongRun)
                        }
                        if (bMoved) placeBubble(bStartX + dx, bStartY + dy)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    v.removeCallbacks(bLongRun)
                    if (!bMoved && !bLong && !bPinch) open()
                    if (bMoved) saveBubble()
                    bPinch = false
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.removeCallbacks(bLongRun)
                    bPinch = false
                }
            }
            true
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyBubblePos()
    }

    init {
        setupBubble()
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
        // a bolinha fica por cima de tudo; some enquanto o menu está aberto
        if (bubble.parent == null) {
            val sz = bubblePx()
            addView(bubble, LayoutParams(sz, sz, Gravity.TOP or Gravity.START))
            bubble.post { applyBubblePos() }
        }
    }

    private fun apply(o: Float) {
        offset = o.coerceIn(0f, 1f)
        val p = panel ?: return
        p.visibility = if (offset > 0f) View.VISIBLE else View.INVISIBLE
        p.translationX = -panelW * (1f - offset)
        scrim.visibility = if (offset > 0f) View.VISIBLE else View.GONE
        scrim.alpha = 0.32f * offset
        bubble.visibility = if (offset > 0f) View.INVISIBLE else View.VISIBLE
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
                // aberto: qualquer toque pode arrastar para fechar; fechado: o menu só abre pela bolinha
                armed = isOpen
                startOffset = offset
            }
            MotionEvent.ACTION_MOVE -> {
                if (armed && !dragging) {
                    val dx = ev.x - downX
                    val dy = ev.y - downY
                    if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy) * 1.0f) {
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
