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
import br.com.roxou.mobilidade.data.conta.LicencaStore
import br.com.roxou.mobilidade.data.conta.RoxouApi
import br.com.roxou.mobilidade.licenca.Modulo
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.offer.AcceptDetector
import br.com.roxou.mobilidade.domain.offer.AreaFilter
import br.com.roxou.mobilidade.domain.offer.OcrLine
import br.com.roxou.mobilidade.domain.offer.OfferEvaluation
import br.com.roxou.mobilidade.domain.offer.OfferEvaluator
import br.com.roxou.mobilidade.domain.offer.OfferOutcome
import br.com.roxou.mobilidade.domain.offer.OfferRecord
import br.com.roxou.mobilidade.domain.offer.OfferParser
import br.com.roxou.mobilidade.domain.offer.ReadPlan
import br.com.roxou.mobilidade.domain.offer.ParsedOffer
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
    private var shotStartedAt = 0L
    private var triesThisOffer = 0
    private var offerVisible = false

    private var pickupAddress: String? = null
    private var dropoffAddress: String? = null
    private var addressesAt = 0L
    private var lastOffer: ParsedOffer? = null
    private var lastOfferShownAt = 0L
    private val loggedKeys = mutableSetOf<String>()
    /** Oferta atual, para o histórico de todas as ofertas (aceitas e recusadas). */
    private var lastEval: OfferEvaluation? = null
    private var lastOfferId = ""
    private var lastOfferAt: java.time.Instant? = null
    private var lastRecordedId = ""

    /** Sessão da oferta na tela: quantas leituras, qual oferta foi lida e quando. */
    private var readKey: String? = null
    private var readAt = 0L
    private var rereads = 0
    private var offerByScan = false
    private var lastScan = 0L
    private var lastDriverWindowChange = 0L
    private var activeApp = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        Diag.init(this)
        repo = RoxouRepository.get(this)
        licencas = LicencaStore.get(this)
        scope.launch { RoxouApi(licencas).renovarSePreciso() }
        if (licencas.testador) scope.launch { licencas.sincronizarHora() }
        instance = this
        panel = FloatingPanel(this, ::openWaze) { logAccepted(byButton = true) }
        scope.launch { repo.state.collect { state = it } }
        Diag.update { it.copy(serviceConnectedAt = System.currentTimeMillis(), serviceAlive = true) }
        Diag.log("Serviço de leitura conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Um erro aqui não pode derrubar o serviço: o Android desliga a acessibilidade de app que cai.
        try {
            handleEvent(event ?: return)
        } catch (t: Throwable) {
            shotPending = false
            Diag.error("evento", t)
        }
    }

    private fun handleEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (!Radar.isOn(this)) {
            panel?.setDriverAppOnScreen(false)
            return
        }
        // Leitura automática e janela flutuante são do plano Pro.
        if (!licencas.liberado(Modulo.LEITURA)) {
            panel?.setDriverAppOnScreen(false)
            if (pkg in DRIVER_APPS) avisarPro()
            return
        }
        val type = event.eventType
        // Toque no botão "Aceitar" da Uber/99 (quando o app entrega esse evento): registra o aceite na hora.
        if (type == AccessibilityEvent.TYPE_VIEW_CLICKED && pkg in DRIVER_APPS) {
            val t = (event.text.joinToString(" ") + " " + (event.contentDescription ?: "")).lowercase()
            val refuse = Regex("recusar|rejeitar|ignorar|nao aceitar|não aceitar|decline|reject").containsMatchIn(t)
            if (("aceitar" in t || "accept" in t) && !refuse && lastOffer != null && SystemClock.uptimeMillis() - lastOfferShownAt < 60_000) {
                Diag.log("Toque em Aceitar visto pela acessibilidade")
                logAccepted(byButton = false)
            }
            return
        }
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && pkg !in IGNORED && pkg != packageName) {
            panel?.setDriverAppOnScreen(pkg in DRIVER_APPS)
        }
        // TYPE_WINDOWS_CHANGED não traz pacote confiável: a oferta da Uber por cima da 99 (ou do Waze)
        // chega assim. Então qualquer mudança de janelas também dispara a procura.
        val windowsChanged = type == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        if (pkg !in DRIVER_APPS && !windowsChanged) return
        val now = SystemClock.uptimeMillis()
        if (pkg in DRIVER_APPS) {
            if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) lastDriverWindowChange = now
            Diag.update { it.copy(lastEventAt = System.currentTimeMillis(), lastEventApp = appName(pkg), eventsCount = it.eventsCount + 1) }
        } else {
            lastDriverWindowChange = now
        }
        if (now - lastCheck < 250) return
        lastCheck = now
        checkForOffer(now)
        checkRideStage(now)
    }

    /**
     * A Uber ou a 99 está ocupando a tela (e não só a bolhinha ou o cartão por cima de outro app)?
     * Aceite só vale lido na tela do app de motorista: lendo o chat, o navegador ou outro app,
     * palavras como "a caminho" ou o nome da rua apareciam e marcavam aceite falso (27/11).
     */
    private fun driverAppFullScreen(): Boolean {
        val screenH = resources.displayMetrics.heightPixels
        return runCatching {
            windows.any { w ->
                val pkg = w.root?.packageName?.toString() ?: return@any false
                pkg in DRIVER_APPS && Rect().also { w.getBoundsInScreen(it) }.height() > screenH * 0.6
            }
        }.getOrDefault(false)
    }

    private fun appName(pkg: String) = when (pkg) {
        UBER -> "Uber"
        NINETY_NINE -> "99"
        else -> pkg
    }

    private fun checkForOffer(now: Long) {
        // Trava de segurança: se uma leitura nunca terminou (erro do Android ou do OCR), libera de novo.
        // Antes, uma leitura presa deixava o Radar cego até reiniciar o serviço.
        if (shotPending && now - shotStartedAt > 8_000) {
            shotPending = false
            Diag.log("Leitura presa há mais de 8 s: liberada")
        }
        // A oferta pode vir numa janela por cima (a da Uber aparece por cima da 99 e do Waze),
        // então procura em todas as janelas visíveis, não só na janela ativa.
        val roots = runCatching { windows.mapNotNull { it.root } }.getOrDefault(emptyList())
            .ifEmpty { listOfNotNull(rootInActiveWindow) }
        val driverRoots = roots.filter { (it.packageName?.toString() ?: "") in DRIVER_APPS }
        if (driverRoots.isEmpty()) {
            if (offerVisible && now - lastOfferSeen > 2_000) endOffer("app de motorista saiu da tela")
            return
        }
        panel?.setDriverAppOnScreen(true)

        var found = ""
        for (r in driverRoots) {
            val pkg = r.packageName.toString()
            val hit = TRIGGERS.getValue(pkg).any { id -> r.findAccessibilityNodeInfosByViewId("$pkg:id/$id").isNotEmpty() }
            if (hit) { found = pkg; break }
        }

        if (found.isEmpty()) {
            if (offerVisible && !offerByScan && now - lastOfferSeen > 2_000) endOffer("cartão sumiu")
            // Plano B: cartão sem o ID conhecido (variantes novas da Uber/99). Lê a tela de tempos em
            // tempos enquanto o app de motorista está na frente; logo depois de uma janela nova, mais rápido.
            val interval = if (now - lastDriverWindowChange < 4_000) 1_500L else 4_000L
            // Só com o app de motorista ocupando a tela (a bolhinha da Uber por cima de outro app não conta).
            val screenH = resources.displayMetrics.heightPixels
            val big = driverRoots.firstOrNull { r -> Rect().also { r.getBoundsInScreen(it) }.height() > screenH / 4 }
            // Continua lendo durante a corrida: a 99/Uber mostram ofertas novas por cima da corrida atual.
            if (big != null && !shotPending && now - lastScan > interval) {
                lastScan = now
                shoot(0, big.packageName.toString(), byScan = true)
            }
            return
        }

        if (!offerVisible || activeApp != found) {
            startOffer(found, "ID do cartão")
        }
        lastOfferSeen = now
        if (shotPending) return
        val plan = ReadPlan.decide(
            offerRead = readKey != null, tries = triesThisOffer, msSinceRead = now - readAt, rereads = rereads,
            msSinceLastShot = now - lastShot, recentWindowChange = now - lastDriverWindowChange < 4_000,
        )
        when (plan) {
            // Ainda não leu: tenta até 8 vezes (o cartão anima ao entrar).
            ReadPlan.Action.FIRST_TRIES -> shoot(if (triesThisOffer == 0) 400L else 1_100L, found, byScan = false)
            ReadPlan.Action.GIVE_UP_NOTICE -> {
                triesThisOffer++
                Diag.discard("${appName(found)}: marca de cartão na tela sem oferta (tela da corrida?). Sigo lendo de tempos em tempos.")
            }
            // Já leu: confere a cada 3 s se a oferta mudou (a próxima pode vir sem o cartão sumir).
            ReadPlan.Action.REREAD -> { rereads++; shoot(0, found, byScan = false) }
            // Nunca desiste: a marca do cartão pode ficar na tela da corrida e a oferta nova vem por cima.
            ReadPlan.Action.KEEP_WATCHING -> { lastScan = now; shoot(0, found, byScan = true) }
            ReadPlan.Action.WAIT -> Unit
        }
    }

    private fun startOffer(pkg: String, how: String) {
        offerVisible = true
        offerByScan = how == "leitura da tela"
        activeApp = pkg
        triesThisOffer = 0
        rereads = 0
        readKey = null
        Diag.update { it.copy(lastOfferSeenAt = System.currentTimeMillis(), lastOfferApp = appName(pkg), lastOfferHow = how) }
        Diag.log("Cartão de oferta na tela: ${appName(pkg)} ($how)")
    }

    /** Depois que a oferta sai, toda leitura da tela por 60 s também procura sinais de aceite. */
    private var acceptWatchUntil = 0L

    private fun endOffer(why: String) {
        acceptWatchUntil = SystemClock.uptimeMillis() + 60_000
        offerVisible = false
        offerByScan = false
        readKey = null
        panel?.offerGone()
        Diag.update { it.copy(panel = "oferta saiu ($why)") }
        Diag.log("Oferta saiu da tela: $why")
        checkAcceptedSoon()
    }

    private fun shoot(delay: Long, pkg: String, byScan: Boolean) {
        shotPending = true
        shotStartedAt = SystemClock.uptimeMillis()
        handler.postDelayed({
            if (!byScan) triesThisOffer++
            // Tela inteira: o recorte pelo cartão cortava o valor em variantes da Uber
            // (Exclusivo, Adolescente) e saía vazio enquanto o cartão animava.
            screenshotText(null) { lines ->
                shotPending = false
                try {
                    onScreenText(lines, pkg, byScan)
                } catch (t: Throwable) {
                    Diag.error("leitura", t)
                }
            }
        }, delay)
    }

    private fun onScreenText(lines: List<OcrLine>?, pkg: String, byScan: Boolean) {
        if (lines == null) {
            if (!byScan) Diag.discard("${appName(pkg)}: o Android não entregou o print da tela")
            return
        }
        val platform = OfferParser.platformHint(lines) ?: if (pkg == UBER) Platform.UBER else Platform.NINETY_NINE
        val offer = OfferParser.parse(lines, platform)
        if (offer == null) {
            if (byScan) {
                // Oferta lida e agora sumiu (inclusive a que veio por cima da corrida, com a marca do cartão ainda na tela).
                if (offerVisible && (offerByScan || readKey != null)) endOffer("leitura não achou mais a oferta")
                else {
                    watchAccept(lines)
                    if (panel?.inRide == true) applyRideStage(lines)
                }
            } else {
                val legs = lines.count { Regex("""\d+\s*min""").containsMatchIn(it.text) && it.text.contains("(") }
                val money = lines.count { it.text.contains("R$") }
                Diag.discard("${appName(pkg)}: leitura ${triesThisOffer}: $legs linha(s) de tempo, $money de valor (precisa 2 de tempo e 1 de valor)")
                Diag.log("Linhas: " + lines.sortedBy { it.top }.joinToString(" | ") { Diag.mask(it.text) }.take(900))
            }
            return
        }
        if (byScan && !offerVisible) startOffer(if (platform == Platform.UBER) UBER else NINETY_NINE, "leitura da tela")
        if (byScan) lastOfferSeen = SystemClock.uptimeMillis()
        val changed = offer.key != readKey
        readKey = offer.key
        readAt = SystemClock.uptimeMillis()
        if (changed) handleOffer(offer)
    }

    private fun handleOffer(offer: ParsedOffer) {
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

        val app = if (offer.platform == Platform.UBER) "Uber" else "99"
        val parse = String.format(
            java.util.Locale.ROOT, "%s R$ %.2f · busca %d min %.1f km · viagem %d min %.1f km · nota %s%s",
            app, offer.fareCents / 100.0, offer.pickupMinutes, offer.pickupKm, offer.tripMinutes, offer.tripKm,
            offer.passengerRating?.toString() ?: "-", if (offer.timeSuspect) " · TEMPO DUVIDOSO" else "",
        )
        val result = String.format(
            java.util.Locale.ROOT, "%s · R$ %.2f/km · R$ %.2f/h · %d min no total%s",
            e.verdict.name, e.perKmCents / 100.0, e.perHourCents / 100.0, e.totalMinutes, if (e.timeEstimated) " (tempo estimado)" else "",
        )
        Diag.update { it.copy(lastParse = parse, lastResult = result, offersRead = it.offersRead + 1, panel = "mostrando a oferta") }
        Diag.log("LIDA: $parse")
        Diag.log("CÁLCULO: $result")

        listOfNotNull(e.pickupArea.unknownCode, e.dropoffArea.unknownCode).forEach { code ->
            scope.launch { repo.updateSettings { it.copy(offer = AreaFilter.learnCode(it.offer, code)) } }
        }
    }

    /**
     * O cartão sumiu: olha a tela para ver se virou "a caminho" (o motorista aceitou).
     * Olha duas vezes (1,5 s e 4 s depois). Sem aceite, a oferta entra no histórico como recusada.
     */
    /** Oferta que já teve a checagem de aceite: a mesma oferta não abre a pergunta "Aceitou?" de novo. */
    private var acceptCheckedKey: String? = null

    private fun checkAcceptedSoon(attempt: Int = 0) {
        val offer = lastOffer ?: return
        if (offer.key in loggedKeys) return
        if (attempt == 0) {
            if (acceptCheckedKey == offer.key) return
            acceptCheckedKey = offer.key
        }
        if (SystemClock.uptimeMillis() - lastOfferShownAt > 90_000) { record(OfferOutcome.REJECTED); return }
        handler.postDelayed({
            if (offer.key != lastOffer?.key || offer.key in loggedKeys) return@postDelayed
            screenshotText(null) { lines ->
                // Voltou para a tela de espera ("Você está online", "Procurando viagens", "Buscando"):
                // o motorista recusou ou a oferta expirou. Fica recusada e nada de pergunta nem botão.
                if (lines != null && driverAppFullScreen() && AcceptDetector.looksIdle(lines)) {
                    Diag.log("Oferta recusada/expirada: a tela voltou para a espera")
                    acceptWatchUntil = 0
                    record(OfferOutcome.REJECTED)
                    return@screenshotText
                }
                // Com outra corrida em andamento, "a caminho" e afins são da corrida atual: aí só vale a rua do embarque nova.
                val accepted = lines != null && driverAppFullScreen() && (
                    (panel?.inRide != true && AcceptDetector.looksAccepted(lines)) ||
                        // A navegação da 99 até o passageiro mostra a rua do embarque desta oferta.
                        AcceptDetector.mentionsStreet(lines, offer.pickupAddress)
                    )
                when {
                    accepted -> logAccepted(byButton = false)
                    attempt < 3 -> checkAcceptedSoon(attempt + 1)
                    else -> {
                        Diag.log("Aceite não detectado depois que a oferta saiu (4 leituras)")
                        record(OfferOutcome.REJECTED)
                        // Pode ter aceitado numa tela que o app não conhece: deixa Embarque/Destino e o ✔
                        // por alguns segundos. Tocar em um deles conta como aceite.
                        panel?.maybeAccepted()
                    }
                }
            }
        }, when (attempt) { 0 -> 1_200L; 1 -> 2_000L; 2 -> 3_000L; else -> 5_000L })
    }

    private fun watchAccept(lines: List<OcrLine>) {
        val o = lastOffer ?: return
        if (SystemClock.uptimeMillis() > acceptWatchUntil || o.key in loggedKeys) return
        if (!driverAppFullScreen()) return
        // Tela de espera: o mapa mostra ruas próximas (às vezes a do embarque), mas não é aceite.
        if (AcceptDetector.looksIdle(lines)) { acceptWatchUntil = 0; return }
        if ((panel?.inRide != true && AcceptDetector.looksAccepted(lines)) || AcceptDetector.mentionsStreet(lines, o.pickupAddress)) {
            Diag.log("Aceite visto numa leitura depois que a oferta saiu")
            logAccepted(byButton = false)
        }
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
        toast("Corrida aceita registrada")
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
        shotStartedAt = now
        screenshotText(null) { lines ->
            shotPending = false
            // A mesma leitura também procura oferta nova por cima da corrida (antes ela só via a etapa
            // da viagem e a oferta sobreposta podia passar sem avaliação).
            if (lines != null) {
                try {
                    onScreenText(lines, activeApp.ifEmpty { UBER }, byScan = true)
                } catch (t: Throwable) {
                    Diag.error("leitura", t)
                }
            }
        }
    }

    private fun applyRideStage(lines: List<OcrLine>) {
        val p = panel ?: return
        if (!driverAppFullScreen()) return
        run {
            when {
                AcceptDetector.looksIdle(lines) -> {
                    if (p.inRide) Diag.log("Tela de espera da Uber/99: botão e pergunta apagados")
                    p.idle()
                    RideNotification.hide(this)
                }
                AcceptDetector.looksEnded(lines) -> { if (p.rideEnded()) RideNotification.hide(this) }
                AcceptDetector.looksOnTrip(lines) -> p.passengerOnBoard()
                // Navegação mostrando a rua do destino (e não mais a do embarque): passageiro a bordo.
                AcceptDetector.mentionsStreet(lines, acceptedDropoff) && !AcceptDetector.mentionsStreet(lines, acceptedPickup) -> p.passengerOnBoard()
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
        val o = lastOffer
        if (o != null && o.key !in loggedKeys && panel?.isTentative == true) {
            logAccepted(byButton = true) // tocou em Embarque/Destino depois que a oferta saiu: aceitou
        }
        // Botão da corrida em andamento: usa os endereços da corrida aceita, não os da última oferta vista.
        val acc = if (toDropoff) acceptedDropoff else acceptedPickup
        if (panel?.inRide == true && !offerVisible && acc != null) {
            go(acc)
            return
        }
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
        Toast.makeText(this, "Leitura automática bloqueada. Abra o Roxou para ver o seu plano.", Toast.LENGTH_LONG).show()
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
                    try {
                        onShot(result)
                    } catch (t: Throwable) {
                        Diag.error("print", t)
                        done(null)
                    }
                }

                private fun onShot(result: ScreenshotResult) {
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

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Diag.update { it.copy(serviceAlive = false) }
        Diag.log("Serviço de leitura desligado pelo Android ou pelo usuário")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Diag.update { it.copy(serviceAlive = false) }
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
        /** IDs do cartão de oferta vistos na Etapa 1. Variante sem eles cai na leitura da tela (plano B). */
        private val TRIGGERS = mapOf(
            UBER to listOf("primary_touch_area", "ub_driver_offers_browse_pill_container"),
            NINETY_NINE to listOf("broad_order_container"),
        )
        /** Janelas que aparecem por cima sem tirar o app de motorista da frente. */
        private val IGNORED = setOf("com.android.systemui", "com.samsung.android.honeyboard", "com.google.android.inputmethod.latin")
    }
}
