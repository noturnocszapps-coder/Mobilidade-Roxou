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
import br.com.roxou.mobilidade.domain.offer.Verdict
import java.util.Locale
import kotlin.math.abs

/**
 * Janela flutuante pequena. Na oferta mostra só: cor/veredito, R$/km total (busca + viagem),
 * R$/h e a nota do passageiro, mais um alerta quando há bairro evitado ou nota baixa.
 * Some sozinha depois do tempo escolhido (2, 3, 5 ou 10 s). Depois do aceite pode ficar
 * só um botãozinho do Waze (opcional). Arrasta para cima/baixo; a posição na largura
 * (esquerda, centro, direita) vem dos ajustes.
 * Usa a camada da acessibilidade (TYPE_ACCESSIBILITY_OVERLAY): não precisa de permissão extra.
 */
class FloatingPanel(
    private val context: Context,
    private val onWaze: (toDropoff: Boolean) -> Unit,
    private val onAccepted: () -> Unit,
) {
    private enum class Mode { NONE, OFFER, WAZE }

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
        cornerRadius = dp(14).toFloat()
        setColor(Color.parseColor("#EE141414"))
    }

    private fun text(size: Float, color: Int = Color.WHITE, bold: Boolean = false) = TextView(context).apply {
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        maxLines = 1
    }

    private val verdictView = text(13f, bold = true)
    private val mainView = text(22f, bold = true)
    private val detailView = text(13f, Color.parseColor("#DDDDDD"))
    private val alertView = text(13f, Color.WHITE, bold = true).apply {
        setPadding(dp(6), dp(2), dp(6), dp(2))
        background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(red) }
    }
    private fun pill(label: String, bg: Int, fg: Int = Color.BLACK) = text(12f, fg, bold = true).apply {
        text = label
        setPadding(dp(8), dp(4), dp(8), dp(4))
        background = GradientDrawable().apply { cornerRadius = dp(20).toFloat(); setColor(bg) }
    }
    /** Etapa 1: até o passageiro (ponto A). Etapa 2: do ponto A até o destino (ponto B). */
    private val pickupButton = pill("Embarque", Color.parseColor("#33CCFF"))
    private val dropoffButton = pill("Destino", Color.parseColor("#33CCFF"))
    /** Plano B do aceite automático: um toque grava a corrida no histórico. Não aceita nada na Uber/99. */
    private val acceptButton = pill("✔", green)
    private val hideView = text(16f, Color.parseColor("#888888"), bold = true).apply {
        text = "✕"
        setPadding(dp(8), dp(0), dp(2), dp(0))
    }
    private val buttons = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(pickupButton)
        addView(dropoffButton, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(4) })
        addView(acceptButton, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(4) })
        addView(hideView)
    }

    private val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(10), dp(6), dp(10), dp(8))
        background = panelBg
        alpha = 0.95f
        addView(verdictView)
        addView(mainView)
        addView(detailView)
        addView(alertView, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(3) })
        addView(buttons, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(5) })
    }

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply { y = prefs.getInt("y", dp(32)) }

    private var attached = false
    private var mode = Mode.NONE
    private var pinned = false
    private var evaluation: OfferEvaluation? = null
    private var hasAddress = false
    private var driverAppOnScreen = false
    private var logged = false
    /** Qual botão do Waze fica aceso: embarque primeiro, depois destino. */
    private var nextIsDropoff = false

    init {
        acceptButton.setOnClickListener { onAccepted() }
        pickupButton.setOnClickListener { onWaze(false); nextIsDropoff = true; render() }
        dropoffButton.setOnClickListener { onWaze(true); nextIsDropoff = false; render() }
        hideView.setOnClickListener { setMode(Mode.NONE) }
        setupDrag()
    }

    fun showOffer(e: OfferEvaluation) {
        if (tentative) { tentative = false; handler.removeCallbacks(endTentative) }
        val isNew = evaluation?.offer?.key != e.offer.key
        evaluation = e
        hasAddress = e.offer.pickupAddress != null || e.offer.dropoffAddress != null
        if (isNew) {
            nextIsDropoff = false
            logged = false
            pinned = false
            handler.removeCallbacks(hide)
            handler.postDelayed(hide, PanelPrefs.seconds(context) * 1_000L)
        }
        setMode(Mode.OFFER)
    }

    /** O cartão sumiu (aceitou, recusou ou expirou): a oferta sai da tela. */
    fun offerGone() {
        if (mode != Mode.OFFER) return
        handler.removeCallbacks(hide)
        handler.postDelayed(hide, 1_000)
    }

    private var tentative = false
    private val endTentative = Runnable { if (tentative) { tentative = false; setMode(Mode.NONE) } }

    /** A oferta saiu e o app não viu o aceite: mostra Embarque/Destino e ✔ por 15 s, caso tenha aceitado. */
    fun maybeAccepted() {
        if (!hasAddress || mode == Mode.WAZE) return
        tentative = true
        nextIsDropoff = false
        handler.removeCallbacks(hide)
        handler.removeCallbacks(endTentative)
        handler.postDelayed(endTentative, 15_000)
        setMode(Mode.WAZE)
    }

    /** A corrida foi aceita e gravada: some o ✔ e, se o motorista quiser, fica só o Waze. */
    fun markLogged() {
        tentative = false
        handler.removeCallbacks(endTentative)
        logged = true
        if (PanelPrefs.wazeAfterAccept(context) && hasAddress) {
            handler.removeCallbacks(hide)
            setMode(Mode.WAZE)
        } else {
            render()
        }
    }

    /** Corrida aceita em andamento: a janelinha mostra o próximo passo (embarque ou destino). */
    val inRide: Boolean get() = mode == Mode.WAZE

    /** O passageiro entrou: o botão passa a levar ao destino. */
    fun passengerOnBoard() {
        if (mode != Mode.WAZE || nextIsDropoff) return
        nextIsDropoff = true
        render()
    }

    /** Viagem terminou: a janelinha some. */
    fun rideEnded() {
        if (mode == Mode.WAZE) setMode(Mode.NONE)
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

    private val hide = Runnable { if (mode == Mode.OFFER && !pinned) setMode(Mode.NONE) }

    private fun setMode(m: Mode) {
        mode = m
        Diag.update { it.copy(panel = when (m) { Mode.NONE -> "escondida"; Mode.OFFER -> "mostrando a oferta"; Mode.WAZE -> "botão do mapa (corrida aceita)" }) }
        render()
        refreshVisibility()
    }

    private fun applyPosition() {
        when (PanelPrefs.position(context)) {
            PanelPrefs.Position.LEFT -> { params.gravity = Gravity.TOP or Gravity.START; params.x = dp(8) }
            PanelPrefs.Position.CENTER -> { params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; params.x = 0 }
            PanelPrefs.Position.RIGHT -> { params.gravity = Gravity.TOP or Gravity.END; params.x = dp(8) }
        }
    }

    private fun refreshVisibility() {
        val show = driverAppOnScreen && mode != Mode.NONE && evaluation != null
        if (mode == Mode.OFFER && !driverAppOnScreen) Diag.update { it.copy(panel = "oferta lida, mas escondida: Uber/99 não estava na frente") }
        if (show && !attached) {
            applyPosition()
            runCatching { wm.addView(root, params) }
                .onSuccess { attached = true }
                .onFailure { Diag.error("janela", it) }
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
            e.verdict == Verdict.GOOD -> "COMPENSA"
            e.verdict == Verdict.BORDERLINE -> "NO LIMITE"
            else -> "NÃO COMPENSA"
        }
        mainView.text = String.format(pt, "R$ %.2f/km", e.perKmCents / 100.0)
        val rating = e.offer.passengerRating?.let { String.format(pt, " · ★ %.2f", it) }.orEmpty()
        // R$/h já conta a busca + a viagem; o total de minutos fica à vista para conferir.
        val approx = if (e.timeEstimated) "≈" else ""
        detailView.text = String.format(pt, "%sR$ %.0f/h · %s%d min", approx, e.perHourCents / 100.0, approx, e.totalMinutes) + rating

        val blockedArea = listOf(e.pickupArea, e.dropoffArea).firstOrNull { it.blocked }
        val unknown = listOf(e.pickupArea, e.dropoffArea).firstOrNull { it.unknownCode != null }
        val alertBg = alertView.background as GradientDrawable
        when {
            blockedArea != null -> {
                alertView.text = "${if (blockedArea === e.pickupArea) "Embarque" else "Destino"}: ${blockedArea.area}"
                alertBg.setColor(red)
                alertView.visibility = View.VISIBLE
            }
            e.lowRating -> {
                alertView.text = "⚠ Atenção: nota do passageiro"
                alertBg.setColor(Color.parseColor("#C0392B"))
                alertView.visibility = View.VISIBLE
            }
            unknown != null -> {
                alertView.text = "Código ${unknown.unknownCode}: traduza nos Ajustes"
                alertBg.setColor(Color.parseColor("#444444"))
                alertView.visibility = View.VISIBLE
            }
            else -> alertView.visibility = View.GONE
        }

        val offerMode = mode == Mode.OFFER
        verdictView.visibility = if (offerMode) View.VISIBLE else View.GONE
        mainView.visibility = if (offerMode) View.VISIBLE else View.GONE
        detailView.visibility = if (offerMode) View.VISIBLE else View.GONE
        if (!offerMode) alertView.visibility = View.GONE
        acceptButton.visibility = if (logged || !(offerMode || tentative)) View.GONE else View.VISIBLE
        // O próximo passo fica aceso; o outro fica apagado (mas continua funcionando).
        pickupButton.alpha = if (nextIsDropoff) 0.45f else 1f
        dropoffButton.alpha = if (nextIsDropoff) 1f else 0.45f
        val map = PanelPrefs.mapApp(context).label
        if (mode == Mode.WAZE) {
            // Corrida aceita: só o botão do passo atual, grande o bastante para tocar com o carro parado.
            pickupButton.text = "➜ Ir ao embarque · $map"
            dropoffButton.text = "➜ Ir ao destino · $map"
            pickupButton.textSize = 14f
            dropoffButton.textSize = 14f
            pickupButton.alpha = 1f
            dropoffButton.alpha = 1f
            pickupButton.visibility = if (hasAddress && !nextIsDropoff) View.VISIBLE else View.GONE
            dropoffButton.visibility = if (hasAddress && nextIsDropoff) View.VISIBLE else View.GONE
        } else {
            pickupButton.text = "Embarque"
            dropoffButton.text = "Destino"
            pickupButton.textSize = 12f
            dropoffButton.textSize = 12f
            val wazeVis = if (hasAddress) View.VISIBLE else View.GONE
            pickupButton.visibility = wazeVis
            dropoffButton.visibility = wazeVis
        }
        root.requestLayout()
        if (attached) runCatching { wm.updateViewLayout(root, params) }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDrag() {
        var startY = 0
        var touchY = 0f
        var moved = false
        root.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = params.y
                    touchY = ev.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = ev.rawY - touchY
                    if (abs(dy) > dp(8)) moved = true
                    if (moved) {
                        params.y = (startY + dy.toInt()).coerceAtLeast(0)
                        if (attached) runCatching { wm.updateViewLayout(root, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        prefs.edit().putInt("y", params.y).apply()
                    } else if (mode == Mode.OFFER) {
                        // Um toque segura a oferta na tela até ela sumir do app.
                        pinned = true
                        handler.removeCallbacks(hide)
                    }
                    true
                }
                else -> false
            }
        }
    }
}
