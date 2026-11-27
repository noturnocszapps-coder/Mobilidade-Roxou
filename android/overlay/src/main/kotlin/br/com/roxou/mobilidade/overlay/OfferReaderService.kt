package br.com.roxou.mobilidade.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import br.com.roxou.mobilidade.data.AppState
import br.com.roxou.mobilidade.data.RoxouRepository
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.offer.AcceptDetector
import br.com.roxou.mobilidade.domain.offer.AreaFilter
import br.com.roxou.mobilidade.domain.offer.OcrLine
import br.com.roxou.mobilidade.domain.offer.OfferEvaluation
import br.com.roxou.mobilidade.domain.offer.OfferEvaluator
import br.com.roxou.mobilidade.domain.offer.OfferOutcome
import br.com.roxou.mobilidade.domain.offer.OfferRecord
import br.com.roxou.mobilidade.domain.offer.OfferParser
import br.com.roxou.mobilidade.domain.offer.TierContext
import br.com.roxou.mobilidade.domain.rental.VehicleOwnership
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Lê o cartão de oferta da Uber e da 99 e mostra a avaliação na janela flutuante.
 *
 * - A 99 (Flutter) não entrega texto à acessibilidade, então o cartão é lido por imagem:
 *   print só em memória, OCR no celular, imagem apagada na hora (decisão de 27/11/2026).
 * - Guarda só os números da oferta. Os endereços ficam só na memória, para o Waze e o filtro de bairros.
 * - Nunca clica, nunca aceita nem recusa corrida.
 */
class OfferReaderService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private lateinit var repo: RoxouRepository
    private var panel: FloatingPanel? = null
    @Volatile private var state: AppState? = null

    private var lastCheck = 0L
    private var lastOfferSeen = 0L
    private var lastShot = 0L
    private var shotPending = false
    private var triesThisOffer = 0
    private var offerVisible = false

    private var pickupAddress: String? = null
    private var dropoffAddress: String? = null
    private var addressesAt = 0L
    private var lastOffer: br.com.roxou.mobilidade.domain.offer.ParsedOffer? = null
    private var lastOfferShownAt = 0L
    private val loggedKeys = mutableSetOf<String>()
    /** Oferta atual, para o histórico de todas as ofertas (aceitas e recusadas). */
    private var lastEval: OfferEvaluation? = null
    private var lastOfferId = ""
    private var lastOfferAt: java.time.Instant? = null
    private var lastRecordedId = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        repo = RoxouRepository.get(this)
        instance = this
        panel = FloatingPanel(this, ::openWaze) { logAccepted(byButton = true) }
        scope.launch { repo.state.collect { state = it } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (!Radar.isOn(this)) {
            panel?.setDriverAppOnScreen(false)
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && pkg !in IGNORED && pkg != packageName) {
            panel?.setDriverAppOnScreen(pkg in DRIVER_APPS)
        }
        if (pkg !in DRIVER_APPS) return
        val now = SystemClock.uptimeMillis()
        if (now - lastCheck < 300) return
        lastCheck = now
        checkForOffer(now)
        checkRideStage(now)
    }

    private fun checkForOffer(now: Long) {
        // A oferta pode vir numa janela por cima (a da 99 é assim), então procura em todas as janelas
        // visíveis. Janela que não está na tela (ex.: o Roxou na frente) não entra nesta lista.
        val roots = runCatching { windows.mapNotNull { it.root } }.getOrDefault(emptyList())
            .ifEmpty { listOfNotNull(rootInActiveWindow) }
        val driverRoots = roots.filter { (it.packageName?.toString() ?: "") in DRIVER_APPS }
        if (driverRoots.isEmpty()) return
        panel?.setDriverAppOnScreen(true)

        var card: android.view.accessibility.AccessibilityNodeInfo? = null
        var active = ""
        for (r in driverRoots) {
            val pkg = r.packageName.toString()
            val found = r.findAccessibilityNodeInfosByViewId(TRIGGERS.getValue(pkg)).firstOrNull()
            if (found != null) { card = found; active = pkg; break }
        }
        if (card == null) {
            if (offerVisible && now - lastOfferSeen > 2_500) {
                offerVisible = false
                panel?.offerGone()
                checkAcceptedSoon()
            }
            return
        }
        val cardNode = card
        if (!offerVisible) {
            offerVisible = true
            triesThisOffer = 0
        }
        lastOfferSeen = now
        if (triesThisOffer >= 4 || shotPending) return

        // Na Uber o cartão tem limites conhecidos; na 99 a tela inteira é lida.
        val crop = if (active == UBER) Rect().also { cardNode.getBoundsInScreen(it) } else null
        val platform = if (active == UBER) Platform.UBER else Platform.NINETY_NINE
        // Espera o cartão terminar de animar (o primeiro print da Uber saía vazio).
        val delay = if (triesThisOffer == 0) 700L else 1_800L
        shotPending = true
        handler.postDelayed({
            triesThisOffer++
            screenshotText(crop) { lines ->
                shotPending = false
                if (lines != null) handleOffer(lines, platform)
            }
        }, delay)
    }

    private fun handleOffer(lines: List<OcrLine>, platform: Platform) {
        val offer = OfferParser.parse(lines, platform) ?: return
        triesThisOffer = 99 // já leu esta oferta
        val s = state
        val rules = s?.settings?.offer ?: br.com.roxou.mobilidade.domain.offer.OfferRules()
        val tier = s?.let {
            TierContext(it.settings.ownership == VehicleOwnership.RENTED, it.settings.rental.tierPlatforms, it.snapshot.tiers)
        }
        val e = OfferEvaluator.evaluate(offer, rules, tier)
        pickupAddress = offer.pickupAddress
        dropoffAddress = offer.dropoffAddress
        addressesAt = SystemClock.uptimeMillis()
        panel?.showOffer(e)
        if (offer.key != lastOffer?.key) {
            // Oferta anterior que não chegou a ser fechada: entra como recusada.
            if (lastEval != null && lastOffer?.key?.let { it !in loggedKeys } == true && lastRecordedId != lastOfferId) record(OfferOutcome.REJECTED)
            lastOfferId = repo.newId()
            lastOfferAt = repo.now()
        }
        lastEval = e
        lastOffer = offer
        lastOfferShownAt = SystemClock.uptimeMillis()
        if (offer.key in loggedKeys) panel?.markLogged()

        listOfNotNull(e.pickupArea.unknownCode, e.dropoffArea.unknownCode).forEach { code ->
            scope.launch { repo.updateSettings { it.copy(offer = AreaFilter.learnCode(it.offer, code)) } }
        }
    }

    /**
     * O cartão sumiu: olha a tela para ver se virou "a caminho" (o motorista aceitou).
     * Olha duas vezes (1,5 s e 4 s depois). Sem aceite, a oferta entra no histórico como recusada.
     */
    private fun checkAcceptedSoon(attempt: Int = 0) {
        val offer = lastOffer ?: return
        if (offer.key in loggedKeys) return
        if (SystemClock.uptimeMillis() - lastOfferShownAt > 90_000) { record(OfferOutcome.REJECTED); return }
        handler.postDelayed({
            if (offer.key != lastOffer?.key || offer.key in loggedKeys) return@postDelayed
            screenshotText(null) { lines ->
                when {
                    lines != null && AcceptDetector.looksAccepted(lines) -> logAccepted(byButton = false)
                    attempt == 0 -> checkAcceptedSoon(1)
                    else -> record(OfferOutcome.REJECTED)
                }
            }
        }, if (attempt == 0) 1_500 else 2_500)
    }

    private fun record(outcome: OfferOutcome) {
        val e = lastEval ?: return
        lastRecordedId = lastOfferId
        val rec = OfferRecord.from(lastOfferId, lastOfferAt ?: repo.now(), e, outcome)
        scope.launch(Dispatchers.IO) { OfferLog.append(this@OfferReaderService, rec) }
    }

    /** Grava a corrida aceita no histórico como "a confirmar". Só conta na meta depois de confirmada. */
    private fun logAccepted(byButton: Boolean) {
        val offer = lastOffer ?: return
        if (!loggedKeys.add(offer.key)) return
        if (loggedKeys.size > 200) loggedKeys.clear()
        val trip = AcceptDetector.toTrip(repo.newId(), offer, repo.now(), byButton)
        scope.launch { repo.saveTrip(trip) }
        record(OfferOutcome.ACCEPTED)
        panel?.markLogged()
        acceptedPickup = offer.pickupAddress
        acceptedDropoff = offer.dropoffAddress
        if (PanelPrefs.rideNotification(this) && (offer.pickupAddress != null || offer.dropoffAddress != null)) {
            RideNotification.show(this, listOfNotNull(offer.pickupAddress, offer.dropoffAddress).joinToString(" → "))
        }
        toast(if (byButton) "Corrida registrada (a confirmar)" else "Aceite detectado: corrida registrada (a confirmar)")
    }

    private var lastStageCheck = 0L

    /**
     * Corrida aceita: de tempos em tempos olha a tela da Uber/99.
     * Passageiro a bordo ("finalizar viagem") → o botão vira "Ir ao destino". Viagem acabou → a janelinha some.
     */
    private fun checkRideStage(now: Long) {
        val p = panel ?: return
        if (!p.inRide || offerVisible || shotPending || now - lastStageCheck < 8_000) return
        lastStageCheck = now
        shotPending = true
        screenshotText(null) { lines ->
            shotPending = false
            if (lines == null) return@screenshotText
            when {
                AcceptDetector.looksEnded(lines) -> p.rideEnded()
                AcceptDetector.looksOnTrip(lines) -> p.passengerOnBoard()
            }
        }
    }

    /** Endereços da corrida aceita (para a notificação), só na memória. */
    private var acceptedPickup: String? = null
    private var acceptedDropoff: String? = null

    private fun openAccepted(toDropoff: Boolean) {
        val addr = if (toDropoff) acceptedDropoff else acceptedPickup
        if (addr != null) go(addr) else openWaze(toDropoff)
    }

    private fun openWaze(toDropoff: Boolean) {
        val fresh = SystemClock.uptimeMillis() - addressesAt < 60 * 60_000
        val remembered = if (!fresh) null else if (toDropoff) dropoffAddress else pickupAddress
        if (remembered != null) {
            go(remembered)
            return
        }
        // Sem endereço guardado: lê a tela da corrida agora.
        screenshotText(null) { lines ->
            val found = lines?.let(OfferParser::findAddresses).orEmpty()
            val addr = if (toDropoff) found.lastOrNull() else found.firstOrNull()
            if (addr != null) go(addr) else toast("Não achei o endereço na tela")
        }
    }

    private fun go(address: String) {
        if (!Waze.navigate(this, address)) toast("Waze não instalado")
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    /** Fila única de prints: o Android recusa prints muito seguidos (erro 3). */
    private fun screenshotText(crop: Rect?, done: (List<OcrLine>?) -> Unit) {
        if (Build.VERSION.SDK_INT < 30) { done(null); return }
        val wait = (lastShot + 1_100 - SystemClock.uptimeMillis()).coerceAtLeast(0)
        handler.postDelayed({
            lastShot = SystemClock.uptimeMillis()
            shot(crop, done)
        }, wait)
    }

    @RequiresApi(30)
    private fun shot(crop: Rect?, done: (List<OcrLine>?) -> Unit) {
        val exclude = panel?.screenRect()
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                override fun onSuccess(result: ScreenshotResult) {
                    val hb = result.hardwareBuffer
                    val hw = Bitmap.wrapHardwareBuffer(hb, result.colorSpace)
                    hb.close()
                    if (hw == null) { done(null); return }
                    val full = hw.copy(Bitmap.Config.ARGB_8888, false)
                    hw.recycle()
                    var offX = 0
                    var offY = 0
                    val bmp = if (crop != null && !crop.isEmpty) {
                        val l = crop.left.coerceIn(0, full.width - 1)
                        val t = crop.top.coerceIn(0, full.height - 1)
                        val w = (crop.right.coerceAtMost(full.width) - l).coerceAtLeast(1)
                        val h = (crop.bottom.coerceAtMost(full.height) - t).coerceAtLeast(1)
                        offX = l; offY = t
                        Bitmap.createBitmap(full, l, t, w, h).also { if (it != full) full.recycle() }
                    } else full
                    recognizer.process(InputImage.fromBitmap(bmp, 0))
                        .addOnSuccessListener { text ->
                            val lines = text.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                                val r = line.boundingBox ?: return@mapNotNull null
                                val abs = Rect(r.left + offX, r.top + offY, r.right + offX, r.bottom + offY)
                                if (exclude != null && Rect.intersects(exclude, abs)) return@mapNotNull null
                                OcrLine(line.text, abs.left, abs.top, abs.right, abs.bottom)
                            }
                            bmp.recycle() // a imagem nunca é gravada
                            done(lines)
                        }
                        .addOnFailureListener { bmp.recycle(); done(null) }
                }

                override fun onFailure(errorCode: Int) = done(null)
            })
        } catch (_: Exception) {
            done(null)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacksAndMessages(null)
        panel?.destroy()
        panel = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile private var instance: OfferReaderService? = null

        /** Chamado pelos botões da notificação. Sem o serviço ligado, os endereços não existem mais. */
        fun openMapFromOutside(context: Context, toDropoff: Boolean) {
            val s = instance
            if (s == null) {
                Toast.makeText(context, "Ligue o Radar para abrir o mapa", Toast.LENGTH_SHORT).show()
                RideNotification.hide(context)
            } else {
                s.handler.post { s.openAccepted(toDropoff) }
            }
        }

        const val UBER = "com.ubercab.driver"
        const val NINETY_NINE = "com.app99.driver"
        val DRIVER_APPS = setOf(UBER, NINETY_NINE)
        private val TRIGGERS = mapOf(UBER to "$UBER:id/primary_touch_area", NINETY_NINE to "$NINETY_NINE:id/broad_order_container")
        /** Janelas que aparecem por cima sem tirar o app de motorista da frente. */
        private val IGNORED = setOf("com.android.systemui", "com.samsung.android.honeyboard", "com.google.android.inputmethod.latin")
    }
}
