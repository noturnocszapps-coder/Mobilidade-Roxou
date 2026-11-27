package br.com.roxou.mobilidade.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import br.com.roxou.mobilidade.domain.offer.OfferEvaluation
import br.com.roxou.mobilidade.domain.offer.OfferEvaluator
import br.com.roxou.mobilidade.domain.offer.Verdict
import java.util.Locale
import kotlin.math.abs

/**
 * Janela flutuante, pequena e escura. Na oferta mostra o veredito, um valor consolidado
 * (busca + viagem somadas) e o motivo. Depois encolhe para uma pílula com o botão do Waze.
 * Arrasta para mudar de lugar; um toque abre ou fecha os detalhes.
 * Usa a camada da acessibilidade (TYPE_ACCESSIBILITY_OVERLAY): não precisa de permissão extra.
 */
class FloatingPanel(
    private val context: Context,
    private val onWaze: (toDropoff: Boolean) -> Unit,
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs = context.getSharedPreferences("roxou_overlay", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private val density = context.resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()
    private val pt = Locale.forLanguageTag("pt-BR")

    private val green = Color.parseColor("#2ECC71")
    private val yellow = Color.parseColor("#F5C518")
    private val red = Color.parseColor("#FF4D4D")

    private val panelBg = GradientDrawable().apply {
        cornerRadius = dp(16).toFloat()
        setColor(Color.parseColor("#EE141414"))
    }

    private fun text(size: Float, color: Int = Color.WHITE, bold: Boolean = false) = TextView(context).apply {
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun TextView.oneLine() = apply {
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }

    private val verdictView = text(17f, bold = true).oneLine()
    private val mainView = text(24f, bold = true).oneLine()
    private val detailView = text(14f, Color.parseColor("#DDDDDD")).oneLine()
    private val moneyView = text(13f, Color.parseColor("#AAAAAA")).oneLine()
    private val reasonView = text(13f, Color.parseColor("#CCCCCC")).apply { maxLines = 2 }
    private val areaView = text(15f, bold = true).apply {
        setPadding(dp(8), dp(4), dp(8), dp(4))
        background = GradientDrawable().apply { cornerRadius = dp(8).toFloat(); setColor(red) }
    }
    private val wazeView = text(14f, Color.BLACK, bold = true).apply {
        setPadding(dp(12), dp(6), dp(12), dp(6))
        background = GradientDrawable().apply { cornerRadius = dp(20).toFloat(); setColor(Color.parseColor("#33CCFF")) }
    }
    private val hideView = text(18f, Color.parseColor("#888888"), bold = true).apply {
        text = "  ✕"
        setPadding(dp(8), dp(2), dp(2), dp(2))
    }
    private val buttons = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(wazeView)
        addView(hideView)
    }

    private val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(8), dp(12), dp(10))
        background = panelBg
        alpha = 0.95f
        listOf(verdictView, mainView, detailView, moneyView, reasonView, areaView).forEach { addView(it) }
        addView(buttons, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6) })
    }

    private val params = WindowManager.LayoutParams(
        dp(250), // largura fixa: o texto quebra ou encurta dentro dela, nunca some na borda
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = prefs.getInt("x", dp(12))
        // Padrão: canto de cima à esquerda, acima do cartão da oferta e longe do X da 99.
        y = prefs.getInt("y", dp(32))
    }

    private var attached = false
    private var expanded = false
    private var evaluation: OfferEvaluation? = null
    private var hasAddress = false
    private var driverAppOnScreen = false
    private var dismissed = false
    /** Próximo destino do botão: embarque primeiro, depois destino. */
    private var nextIsDropoff = false

    init {
        wazeView.setOnClickListener { onWaze(nextIsDropoff); nextIsDropoff = !nextIsDropoff; render() }
        wazeView.setOnLongClickListener { onWaze(!nextIsDropoff); true }
        hideView.setOnClickListener { dismissed = true; refreshVisibility() }
        setupDrag()
    }

    fun showOffer(e: OfferEvaluation) {
        val isNew = evaluation?.offer?.key != e.offer.key
        evaluation = e
        hasAddress = e.offer.pickupAddress != null || e.offer.dropoffAddress != null
        if (isNew) {
            nextIsDropoff = false
            dismissed = false
            expanded = true
            handler.removeCallbacks(collapse)
            handler.postDelayed(collapse, 20_000)
        }
        render()
        refreshVisibility()
    }

    /** O cartão sumiu (aceitou, recusou ou expirou): encolhe para a pílula. */
    fun offerGone() {
        if (!expanded) return
        handler.removeCallbacks(collapse)
        handler.postDelayed(collapse, 1_500)
    }

    fun setDriverAppOnScreen(onScreen: Boolean) {
        if (driverAppOnScreen == onScreen) return
        driverAppOnScreen = onScreen
        refreshVisibility()
    }

    /** Área da janela na tela, para o OCR ignorar o que é nosso. */
    fun screenRect(): Rect? {
        if (!attached) return null
        val loc = IntArray(2)
        root.getLocationOnScreen(loc)
        return Rect(loc[0], loc[1], loc[0] + root.width, loc[1] + root.height)
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        if (attached) runCatching { wm.removeView(root) }
        attached = false
    }

    private val collapse = Runnable { expanded = false; render() }

    private fun refreshVisibility() {
        val show = driverAppOnScreen && !dismissed && evaluation != null
        if (show && !attached) {
            runCatching { wm.addView(root, params) }.onSuccess { attached = true }
        } else if (!show && attached) {
            runCatching { wm.removeView(root) }
            attached = false
        }
    }

    private fun render() {
        val e = evaluation ?: return
        val color = if (e.blocked) red else when (e.verdict) {
            Verdict.GOOD -> green
            Verdict.BORDERLINE -> yellow
            Verdict.BAD -> red
        }
        panelBg.setStroke(dp(3), color)
        verdictView.setTextColor(color)
        verdictView.text = when {
            e.blocked -> "⛔ BAIRRO EVITADO"
            e.verdict == Verdict.GOOD -> "✔ COMPENSA"
            e.verdict == Verdict.BORDERLINE -> "≈ NO LIMITE"
            else -> "✖ NÃO COMPENSA"
        }
        val perKm = String.format(pt, "%.2f", e.perKmCents / 100.0)
        val perHour = String.format(pt, "%.0f", e.perHourCents / 100.0)
        mainView.text = "R$ $perKm/km"
        detailView.text = String.format(pt, "R$ %s/h · %.1f km · %d min", perHour, e.totalKm, e.totalMinutes)
        moneyView.text = "Valor ${OfferEvaluator.money(e.offer.fareCents)} · sobra ${OfferEvaluator.money(e.netCents)}"
        reasonView.text = e.reason

        val blockedArea = listOf(e.pickupArea, e.dropoffArea).firstOrNull { it.blocked }
        val unknown = listOf(e.pickupArea, e.dropoffArea).firstOrNull { it.unknownCode != null }
        when {
            blockedArea != null -> {
                areaView.text = "⛔ ${if (blockedArea === e.pickupArea) "EMBARQUE" else "DESTINO"}: ${blockedArea.area?.uppercase(pt)}"
                (areaView.background as GradientDrawable).setColor(red)
                areaView.setTextColor(Color.WHITE)
                areaView.visibility = View.VISIBLE
            }
            unknown != null -> {
                areaView.text = "? código ${unknown.unknownCode}: traduza em Config"
                (areaView.background as GradientDrawable).setColor(Color.parseColor("#444444"))
                areaView.setTextColor(Color.WHITE)
                areaView.visibility = View.VISIBLE
            }
            else -> areaView.visibility = View.GONE
        }

        wazeView.text = if (nextIsDropoff) "Waze ▸ destino" else "Waze ▸ embarque"
        wazeView.visibility = if (hasAddress) View.VISIBLE else View.GONE

        // Pílula: só a cor, o valor consolidado e o Waze. O alerta de bairro continua visível.
        verdictView.visibility = if (expanded || e.blocked) View.VISIBLE else View.GONE
        detailView.visibility = if (expanded) View.VISIBLE else View.GONE
        reasonView.visibility = if (expanded) View.VISIBLE else View.GONE
        moneyView.visibility = if (expanded) View.VISIBLE else View.GONE
        mainView.textSize = if (expanded) 24f else 18f
        params.width = if (expanded || e.blocked) dp(250) else WindowManager.LayoutParams.WRAP_CONTENT
        root.requestLayout()
        if (attached) runCatching { wm.updateViewLayout(root, params) }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDrag() {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        root.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    touchX = ev.rawX; touchY = ev.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - touchX
                    val dy = ev.rawY - touchY
                    if (abs(dx) > dp(8) || abs(dy) > dp(8)) moved = true
                    if (moved) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        if (attached) runCatching { wm.updateViewLayout(root, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        prefs.edit().putInt("x", params.x).putInt("y", params.y).apply()
                    } else {
                        expanded = !expanded
                        handler.removeCallbacks(collapse)
                        render()
                    }
                    true
                }
                else -> false
            }
        }
    }
}
