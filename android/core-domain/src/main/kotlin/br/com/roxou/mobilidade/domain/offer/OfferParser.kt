package br.com.roxou.mobilidade.domain.offer

import br.com.roxou.mobilidade.domain.model.Cents
import br.com.roxou.mobilidade.domain.model.Platform

/** Uma linha lida pelo OCR, com a posição na tela (pixels). */
data class OcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val height: Int get() = bottom - top
}

/**
 * Oferta lida do cartão. Os endereços ficam só em memória (para o botão do Waze e o filtro
 * de bairros) e nunca são gravados no banco.
 */
data class ParsedOffer(
    val platform: Platform,
    val fareCents: Cents,
    val pickupKm: Double,
    val pickupMinutes: Int,
    val tripKm: Double,
    val tripMinutes: Int,
    val pickupAddress: String? = null,
    val dropoffAddress: String? = null,
    /** Nota do passageiro (ex.: 4,88), quando aparece no cartão. */
    val passengerRating: Double? = null,
) {
    /** Chave para não mostrar a mesma oferta duas vezes. */
    val key: String get() = "$platform|$fareCents|$pickupKm|$tripKm|$tripMinutes"
}

/**
 * Transforma as linhas do OCR do cartão de oferta em números.
 * Formatos vistos no teste da Etapa 1 (27/11/2026):
 * - 99: "R$13,80", "4 min (363 m)" (busca), endereço, "17 min (8,1 km)" (viagem), endereço.
 * - Uber: "R$ 15,05", "9 min (3.0 km)" (busca), endereço, "14 minutos (5.9 km)" (viagem), endereço.
 */
object OfferParser {
    private val timeDist = Regex(
        """(?:(\d{1,2})\s*h\s*)?(\d{1,3})\s*min(?:utos?)?\.?\s*\(\s*(\d+(?:[.,]\d+)?)\s*(km|m)\s*\)""",
        RegexOption.IGNORE_CASE,
    )
    private val money = Regex("""R\$\s?(\d{1,4})[.,](\d{2})(?!\d)(?!\s*/)""")
    private val notFare = listOf("/km", "por corrida", "ganhou", "taxa", "incl", "/h")
    private val notAddress = Regex(
        """(?i)tenho interesse|aceitar|recusar|selecionar|pgto|pagamento|corridas|verificado|perfil|\bR\$""",
    )

    private val ratingNumber = Regex("""(?<![\d.,])([1-5][.,]\d{2})(?![\d.,])""")
    private val ratingHint = Regex("""(?i)corrida|\(\s*\d+\s*\)|^\s*[*★☆tk+]\s""")

    /** Nota do passageiro: "t 4,88 986 corridas" (99), "4,87 · 31 corridas", "* 5,00 (48)" (Uber). */
    fun findRating(lines: List<OcrLine>): Double? = lines.asSequence()
        .filter { !it.text.contains("R$") && !it.text.contains("%") && ratingHint.containsMatchIn(it.text) }
        .mapNotNull { ratingNumber.find(it.text)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull() }
        .firstOrNull { it in 1.0..5.0 }

    fun parse(lines: List<OcrLine>, platform: Platform): ParsedOffer? {
        val sorted = lines.filter { it.text.isNotBlank() }.sortedBy { it.top }
        val legs = sorted.mapNotNull { line -> timeDist.find(line.text)?.let { line to it } }
        if (legs.size < 2) return null
        val (pickupLine, pickupMatch) = legs[0]
        val (tripLine, tripMatch) = legs[1]

        val fareLine = sorted
            .filter { it.top < pickupLine.top }
            .filter { l -> notFare.none { l.text.contains(it, ignoreCase = true) } }
            .filter { money.containsMatchIn(it.text) }
            .maxWithOrNull(compareBy<OcrLine> { it.height }.thenByDescending { it.top })
            ?: return null
        val m = money.find(fareLine.text) ?: return null
        val fare = m.groupValues[1].toLong() * 100 + m.groupValues[2].toLong()
        if (fare <= 0) return null

        val pickup = leg(pickupMatch)
        val trip = leg(tripMatch)

        val pickupAddr = sorted.filter {
            it.top >= pickupLine.bottom - 10 && it.bottom <= tripLine.top + 10 && sameColumn(it, pickupLine)
        }
        val gap = (tripLine.height * 2.6).toInt()
        val dropAddr = sorted.filter {
            it.top >= tripLine.bottom - 10 && it.top <= tripLine.bottom + gap && sameColumn(it, tripLine)
        }.takeWhile { !notAddress.containsMatchIn(it.text) && timeDist.find(it.text) == null }

        return ParsedOffer(
            platform = platform,
            fareCents = fare,
            pickupKm = pickup.first,
            pickupMinutes = pickup.second,
            tripKm = trip.first,
            tripMinutes = trip.second,
            pickupAddress = joinAddress(pickupAddr),
            dropoffAddress = joinAddress(dropAddr),
            passengerRating = findRating(sorted.filter { it.top < pickupLine.top }),
        )
    }

    /** Linhas de endereço soltas em qualquer tela (ex.: tela da corrida já aceita na 99). */
    fun findAddresses(lines: List<OcrLine>): List<String> {
        val street = Regex("""(?i)^\s*(rua|r\.|av\.?|avenida|al\.|alameda|travessa|tv\.|pra[çc]a|rod\.?|rodovia|estrada|est\.)\s""")
        val sorted = lines.filter { it.text.isNotBlank() }.sortedBy { it.top }
        return sorted.mapIndexedNotNull { i, line ->
            if (!street.containsMatchIn(line.text)) return@mapIndexedNotNull null
            val next = sorted.getOrNull(i + 1)
            val cont = next?.takeIf {
                it.top - line.bottom < line.height && sameColumn(it, line) &&
                    !notAddress.containsMatchIn(it.text) && !street.containsMatchIn(it.text) &&
                    timeDist.find(it.text) == null
            }
            joinAddress(listOfNotNull(line, cont))
        }
    }

    private fun sameColumn(a: OcrLine, ref: OcrLine) = a.left >= ref.left - 60 && a.left <= ref.left + 120

    private fun leg(m: MatchResult): Pair<Double, Int> {
        val hours = m.groupValues[1].toIntOrNull() ?: 0
        val minutes = m.groupValues[2].toInt() + hours * 60
        val value = m.groupValues[3].replace(',', '.').toDouble()
        val km = if (m.groupValues[4].equals("m", ignoreCase = true)) value / 1000.0 else value
        return km to minutes
    }

    private fun joinAddress(lines: List<OcrLine>): String? =
        lines.filter { !notAddress.containsMatchIn(it.text) }
            .joinToString(" ") { it.text.trim() }
            .replace(Regex("\\s+"), " ")
            .trim().trimEnd(',').trim()
            .takeIf { it.length >= 4 }
}
