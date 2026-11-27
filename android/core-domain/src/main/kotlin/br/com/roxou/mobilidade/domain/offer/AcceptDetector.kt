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
        // 99 (navegação própria até o embarque, visto em 27/11): "Chegue antes de 19:48".
        "chegue antes de", "a caminho do embarque", "local de embarque", "passageiro te espera",
    )
    private val stillLooking = listOf("procurando", "buscando viagens", "buscando corridas", "voce esta online", "ficar offline")

    /** Passageiro a bordo: a tela já mostra o fim da viagem (hora de ir ao destino). */
    private val onTripWords = listOf(
        "finalizar viagem", "finalizar corrida", "encerrar viagem", "encerrar corrida", "concluir viagem",
        "concluir corrida", "deixar passageiro", "deixe o passageiro", "a caminho do destino", "rumo ao destino",
        "deslize para finalizar", "deslize para encerrar", "passageiro a bordo",
    )
    /** Viagem terminou: tela de avaliação ou de ganhos da corrida. */
    private val endedWords = listOf("avalie o passageiro", "avaliar passageiro", "como foi a viagem", "viagem concluida", "corrida finalizada", "voce ganhou")

    fun looksOnTrip(lines: List<OcrLine>): Boolean {
        val text = " " + lines.joinToString(" ") { AreaFilter.normalize(it.text) } + " "
        return onTripWords.any { text.contains(it) }
    }

    fun looksEnded(lines: List<OcrLine>): Boolean {
        val text = " " + lines.joinToString(" ") { AreaFilter.normalize(it.text) } + " "
        return endedWords.any { text.contains(it) } || stillLooking.any { text.contains(it) }
    }

    /**
     * A tela fala da rua deste endereço? Ex.: depois do aceite, a navegação da 99 mostra a rua do embarque;
     * com o passageiro a bordo, mostra a rua do destino. Compara só o nome da rua (antes da 1ª vírgula).
     */
    fun mentionsStreet(lines: List<OcrLine>, address: String?): Boolean {
        val street = address?.substringBefore(',')?.let(AreaFilter::normalize)
            ?.replace(Regex("^(rua|r|av|avenida|al|alameda|travessa|tv|praca|rod|rodovia|estrada)\\s+"), "")
            ?.trim().orEmpty()
        if (street.length < 6) return false
        val text = " " + lines.joinToString(" ") { AreaFilter.normalize(it.text) } + " "
        return text.contains(street)
    }

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
