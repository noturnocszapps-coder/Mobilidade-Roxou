package br.com.roxou.mobilidade.overlay

import android.accessibilityservice.AccessibilityService
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
import br.com.roxou.mobilidade.data.conta.LicencaStore
import br.com.roxou.mobilidade.data.conta.RoxouApi
import br.com.roxou.mobilidade.licenca.Modulo
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.offer.AreaFilter
import br.com.roxou.mobilidade.domain.offer.OcrLine
import br.com.roxou.mobilidade.domain.offer.OfferEvaluator
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
    private lateinit var licencas: LicencaStore
    private var avisoProDia = -1L
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

    override fun onServiceConnected() {
        super.onServiceConnected()
        repo = RoxouRepository.get(this)
        licencas = LicencaStore.get(this)
        scope.launch { RoxouApi(licencas).renovarSePreciso() }
        panel = FloatingPanel(this, ::openWaze)
        scope.launch { repo.state.collect { state = it } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        // Leitura automática e janela flutuante são do plano Pro.
        if (!licencas.liberado(Modulo.LEITURA)) {
            panel?.setDriverAppOnScreen(false)
            if (pkg in DRIVER_APPS) avisarPro()
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
        val todas = s?.settings?.offer ?: br.com.roxou.mobilidade.domain.offer.OfferRules()
        // Filtro de bairros é do plano Pro.
        val rules = if (licencas.liberado(Modulo.BAIRROS)) todas else todas.copy(blockedAreas = emptyList())
        val tier = s?.let {
            TierContext(it.settings.ownership == VehicleOwnership.RENTED, it.settings.rental.tierPlatforms, it.snapshot.tiers)
        }
        val e = OfferEvaluator.evaluate(offer, rules, tier)
        pickupAddress = offer.pickupAddress
        dropoffAddress = offer.dropoffAddress
        addressesAt = SystemClock.uptimeMillis()
        panel?.showOffer(e)

        listOfNotNull(e.pickupArea.unknownCode, e.dropoffArea.unknownCode).forEach { code ->
            scope.launch { repo.updateSettings { it.copy(offer = AreaFilter.learnCode(it.offer, code)) } }
        }
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

    /** Uma vez por dia, para não incomodar. */
    private fun avisarPro() {
        val dia = System.currentTimeMillis() / 86_400_000
        if (dia == avisoProDia) return
        avisoProDia = dia
        Toast.makeText(this, "A leitura automática é do plano Pro. Abra o Roxou > Ajustes para testar grátis.", Toast.LENGTH_LONG).show()
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
        handler.removeCallbacksAndMessages(null)
        panel?.destroy()
        panel = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val UBER = "com.ubercab.driver"
        const val NINETY_NINE = "com.app99.driver"
        val DRIVER_APPS = setOf(UBER, NINETY_NINE)
        private val TRIGGERS = mapOf(UBER to "$UBER:id/primary_touch_area", NINETY_NINE to "$NINETY_NINE:id/broad_order_container")
        /** Janelas que aparecem por cima sem tirar o app de motorista da frente. */
        private val IGNORED = setOf("com.android.systemui", "com.samsung.android.honeyboard", "com.google.android.inputmethod.latin")
    }
}
