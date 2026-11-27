package br.com.roxou.mobilidade.domain.offer

import br.com.roxou.mobilidade.domain.model.RecordSource
import br.com.roxou.mobilidade.domain.model.Trip
import java.time.Instant

/**
 * Descobre, pelo texto da tela logo depois que o cartão de oferta some, se o motorista aceitou.
 * O app só observa: quem aceita é sempre o motorista.
 * As palavras são um primeiro palpite; serão ajustadas com as telas reais da Uber e da 99.
 */
object AcceptDetector {
    private val acceptWords = listOf(
        "a caminho", "iniciar viagem", "iniciar corrida", "cheguei", "chegou ao local",
        "buscar passageiro", "busque o passageiro", "ir ate", "aguardando passageiro",
        "passageiro aguardando", "embarque em", "pegar ", "deslize para iniciar",
    )
    private val stillLooking = listOf("procurando", "buscando viagens", "buscando corridas", "voce esta online", "ficar offline")

    fun looksAccepted(lines: List<OcrLine>): Boolean {
        val text = " " + lines.joinToString(" ") { AreaFilter.normalize(it.text) } + " "
        if (stillLooking.any { text.contains(it) }) return false
        return acceptWords.any { text.contains(it) }
    }

    /** Corrida aceita vira registro "a confirmar": só conta na meta depois de confirmada. */
    fun toTrip(id: String, offer: ParsedOffer, at: Instant, byButton: Boolean) = Trip(
        id = id,
        platform = offer.platform,
        endedAt = at,
        amountCents = offer.fareCents,
        pickupKm = offer.pickupKm,
        tripKm = offer.tripKm,
        pickupMinutes = offer.pickupMinutes,
        tripMinutes = offer.tripMinutes,
        source = RecordSource.SCREEN_READER,
        confirmed = false,
        notes = if (byButton) "Aceite marcado na janela" else "Aceite lido pela tela",
    )
}
