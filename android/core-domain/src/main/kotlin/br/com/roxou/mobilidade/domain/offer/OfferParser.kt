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
    /**
     * Algum tempo lido não combina com a distância (ex.: 12 km em 3 min = 240 km/h):
     * o OCR provavelmente perdeu um dígito. O avaliador estima o tempo pela distância.
     */
    val timeSuspect: Boolean = false,
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
        val sorted = lines.filter { it.text.isNotBlank() }.map { it.copy(text = fixOcr(it.text)) }.sortedBy { it.top }
        // O OCR às vezes devolve a mesma linha duas vezes (cartão animando): a viagem saía igual à busca
        // ("9 min (7.6 km)" duas vezes, R$ 300/h no registro de 28/11). Linha repetida no mesmo lugar não conta.
        val legs = sorted.mapNotNull { line -> timeDist.find(line.text)?.let { line to it } }
            .fold(mutableListOf<Pair<OcrLine, MatchResult>>()) { acc, cur ->
                val prev = acc.lastOrNull()
                val dup = prev != null && prev.second.value == cur.second.value && kotlin.math.abs(prev.first.top - cur.first.top) < prev.first.height * 2
                if (!dup) acc.add(cur)
                acc
            }
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

        var pickup = leg(pickupMatch)
        var trip = leg(tripMatch)
        pickup = lostDecimal(pickupMatch, pickup, fare, trip.first)
        trip = lostDecimal(tripMatch, trip, fare, pickup.first)

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
            timeSuspect = implausible(pickup.first, pickup.second) || implausible(trip.first, trip.second),
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

    /** Mais de 100 km/h na cidade, ou tempo zero com distância de verdade: leitura errada. */
    fun implausible(km: Double, minutes: Int): Boolean =
        (minutes <= 0 && km > 0.3) || (minutes > 0 && km / (minutes / 60.0) > 100.0)

    /** Qual app mostrou a oferta, pelo texto do cartão (quando a janela não diz). */
    fun platformHint(lines: List<OcrLine>): Platform? {
        val t = lines.joinToString(" ") { it.text }
        return when {
            Regex("""(?i)\buber\s?(x|black|comfort|business|flash|moto|bag|exclusivo|adolescente|teens?|planet|juntos)\b|\buberx\b|\bbusiness\s+comfort\b""").containsMatchIn(t) -> Platform.UBER
            // Jeito de escrever da Uber: "5 minutos (1.6 km)", com ponto. A 99 escreve "17 min (8,1 km)".
            !Regex("""(?i)taxa99|\b99\s?(pop|comfort)""").containsMatchIn(t) &&
                Regex("""(?i)\d\s*minutos\s*\(|\(\s*\d+\.\d+\s*km\s*\)""").containsMatchIn(t) -> Platform.UBER
            Regex("""(?i)\b99\s?(pop|top|comfort|moto|entrega)\b|taxa99|\bnegocia\b""").containsMatchIn(t) -> Platform.NINETY_NINE
            else -> null
        }
    }

    /**
     * O OCR às vezes perde o ponto da Uber ("4 min (1.5 km)" vira "15 km"). Só corrige quando a leitura
     * é impossível (mais de 100 km/h), fica normal com o ponto de volta, e o valor por km ficaria abaixo
     * de R$ 1,00, o que quase nunca acontece. Se não, o tempo é que foi mal lido e vira estimativa.
     */
    private fun lostDecimal(m: MatchResult, leg: Pair<Double, Int>, fare: Long, otherKm: Double): Pair<Double, Int> {
        val raw = m.groupValues[3]
        val (km, minutes) = leg
        val digitsOnly = raw.length >= 2 && raw.all { it.isDigit() } && m.groupValues[4].equals("km", ignoreCase = true)
        if (!digitsOnly || !implausible(km, minutes) || implausible(km / 10.0, minutes)) return leg
        val perKm = fare / (km + otherKm).coerceAtLeast(0.1)
        return if (perKm < 100.0) (km / 10.0) to minutes else leg
    }

    private fun sameColumn(a: OcrLine, ref: OcrLine) = a.left >= ref.left - 60 && a.left <= ref.left + 120

    /** Letras que o OCR troca por números: "R$ 12,0l" → "R$ 12,01", "4.1l km" → "4.11 km", "1O min" → "10 min". */
    internal fun fixOcr(t: String): String = t
        .replace(Regex("(?<=\\d)[lI|](?=[\\d\\s,.)]|$|\\s*km)"), "1")
        .replace(Regex("(?<=\\d)[oO](?=\\d|\\s*min)"), "0")

    private fun leg(m: MatchResult): Pair<Double, Int> {
        val hours = m.groupValues[1].toIntOrNull() ?: 0
        var raw = m.groupValues[2].toInt()
        // Ícone lido como número na frente: "910 min" era "10 min" (registro de 28/11). Corrida de cidade
        // não leva 100+ min; com 3 dígitos, fica com os 2 últimos.
        if (hours == 0 && m.groupValues[2].length == 3 && raw >= 100) raw %= 100
        val minutes = raw + hours * 60
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
