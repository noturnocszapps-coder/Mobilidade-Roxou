package br.com.roxou.mobilidade.domain.offer

import br.com.roxou.mobilidade.domain.goals.TierProgress
import br.com.roxou.mobilidade.domain.model.Cents
import br.com.roxou.mobilidade.domain.model.Platform
import java.util.Locale
import kotlin.math.roundToLong

/** Limites do avaliador. Todos configuráveis na tela de Configurações. */
data class OfferRules(
    val minPerKmCents: Cents = 150,
    val goodPerKmCents: Cents = 200,
    val minPerHourCents: Cents = 30_00,
    val goodPerHourCents: Cents = 40_00,
    /** Custo de rodar 1 km (combustível + desgaste). Usado para mostrar quanto sobra. */
    val costPerKmCents: Cents = 45,
    /** Busca acima disso piora a cor em um nível. 0 desliga. */
    val maxPickupKm: Double = 5.0,
    /** Com carro alugado, corrida um pouco fraca vira amarela se faltar até isso para a próxima faixa. */
    val tierHelpRides: Int = 10,
    /** Bairros que o motorista não quer (embarque ou destino). */
    val blockedAreas: List<String> = emptyList(),
    /**
     * Códigos de região do endereço (ex.: "UEP5-S.3") e o bairro de cada um.
     * Código com bairro vazio = visto numa oferta, esperando o motorista traduzir.
     */
    val areaCodes: Map<String, String> = emptyMap(),
)

enum class Verdict { GOOD, BORDERLINE, BAD }

data class OfferEvaluation(
    val offer: ParsedOffer,
    val verdict: Verdict,
    val totalKm: Double,
    val totalMinutes: Int,
    val perKmCents: Cents,
    val perHourCents: Cents,
    /** Valor da oferta menos o custo de rodar o km total. */
    val netCents: Cents,
    /** Frase curta com o motivo real do veredito. */
    val reason: String,
    val pickupArea: AreaMatch,
    val dropoffArea: AreaMatch,
) {
    val blocked: Boolean get() = pickupArea.blocked || dropoffArea.blocked
}

/** Contexto da meta, para carro alugado. */
data class TierContext(val rented: Boolean, val tierPlatforms: Set<Platform>, val progress: TierProgress?)

object OfferEvaluator {
    private val pt: Locale = Locale.forLanguageTag("pt-BR")

    fun evaluate(offer: ParsedOffer, rules: OfferRules, tier: TierContext? = null): OfferEvaluation {
        val totalKm = offer.pickupKm + offer.tripKm
        val totalMin = (offer.pickupMinutes + offer.tripMinutes).coerceAtLeast(1)
        val perKm = if (totalKm > 0.05) (offer.fareCents / totalKm).roundToLong() else offer.fareCents
        val perHour = (offer.fareCents * 60.0 / totalMin).roundToLong()
        val net = offer.fareCents - (totalKm * rules.costPerKmCents).roundToLong()

        fun score(v: Long, min: Long, good: Long) = when {
            v >= good -> 2
            v >= min -> 1
            else -> 0
        }
        val kmScore = score(perKm, rules.minPerKmCents, rules.goodPerKmCents)
        val hourScore = score(perHour, rules.minPerHourCents, rules.goodPerHourCents)
        var level = minOf(kmScore, hourScore)
        var reason = when {
            level == 2 -> "Paga bem no km e na hora, já com a busca"
            kmScore <= hourScore && kmScore < 2 ->
                "Km total paga ${money(perKm)}, ${if (kmScore == 0) "abaixo do mínimo" else "abaixo do bom"} (${money(if (kmScore == 0) rules.minPerKmCents else rules.goodPerKmCents)})"
            else ->
                "Hora total paga ${money(perHour)}, ${if (hourScore == 0) "abaixo do mínimo" else "abaixo do bom"} (${money(if (hourScore == 0) rules.minPerHourCents else rules.goodPerHourCents)})"
        }

        if (rules.maxPickupKm > 0 && offer.pickupKm > rules.maxPickupKm && level > 0) {
            level -= 1
            reason = "Busca longe: ${km(offer.pickupKm)} até o passageiro"
        }

        // Carro alugado: corrida um pouco fraca ajuda a fechar a próxima faixa da locadora.
        val p = tier?.progress
        if (level == 0 && tier != null && tier.rented && offer.platform in tier.tierPlatforms && p?.next != null) {
            val toNext = p.ridesToNext ?: Int.MAX_VALUE
            val close = perKm >= rules.minPerKmCents * 0.85 && perHour >= rules.minPerHourCents * 0.85
            if (close && toNext <= rules.tierHelpRides) {
                level = 1
                reason = "Fraca, mas ajuda a fechar ${p.next.minRides} corridas (faltam $toNext, vale ${money(p.next.discountCents)})"
            }
        }

        val pickupArea = AreaFilter.match(offer.pickupAddress, rules)
        val dropoffArea = AreaFilter.match(offer.dropoffAddress, rules)
        val blockedName = listOf(pickupArea, dropoffArea).firstOrNull { it.blocked }
        if (blockedName != null) {
            reason = "${if (blockedName === pickupArea) "Embarque" else "Destino"} em ${blockedName.area}, bairro que você não quer"
        }

        val verdict = when (level) {
            2 -> Verdict.GOOD
            1 -> Verdict.BORDERLINE
            else -> Verdict.BAD
        }
        return OfferEvaluation(offer, verdict, totalKm, totalMin, perKm, perHour, net, reason, pickupArea, dropoffArea)
    }

    fun money(c: Cents): String = String.format(pt, "R$ %.2f", c / 100.0)
    fun km(v: Double): String = String.format(pt, "%.1f km", v)
}

/** Resultado do filtro de bairros para um endereço. */
data class AreaMatch(
    /** Nome do bairro encontrado (ou traduzido do código). */
    val area: String? = null,
    val blocked: Boolean = false,
    /** Código de região que apareceu no endereço e ainda não está na tabela. */
    val unknownCode: String? = null,
)

object AreaFilter {
    private val codePattern = Regex("""(?i)\b(?:U?E)?P\s?\d{1,2}\s?[-.]?\s?S\s?[.\-]?\s?\d{1,2}\b""")

    fun normalize(s: String): String =
        java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    /** "Uep5-S.3", "UEP5S3", "P5S2" viram "p5s3"/"p5s2", para comparar sem se importar com pontos e hifens. */
    fun normalizeCode(s: String): String = normalize(s).replace(" ", "").removePrefix("u").removePrefix("e")
        .let { if (it.startsWith("p")) it else "p$it" }

    fun match(address: String?, rules: OfferRules): AreaMatch {
        if (address.isNullOrBlank()) return AreaMatch()
        val norm = " ${normalize(address)} "

        // 1. Código de região no endereço: traduz pela tabela.
        val code = codePattern.find(address)?.value
        var translated: String? = null
        var unknown: String? = null
        if (code != null) {
            val key = normalizeCode(code)
            translated = rules.areaCodes.entries.firstOrNull { normalizeCode(it.key) == key }?.value?.takeIf { it.isNotBlank() }
            if (translated == null) unknown = code.uppercase().replace(" ", "")
        }

        // 2. Bairro bloqueado: pelo nome no endereço ou pelo código traduzido.
        val blockedHit = rules.blockedAreas.map { it.trim() }.filter { it.isNotEmpty() }.firstOrNull { b ->
            val nb = normalize(b)
            norm.contains(" $nb ") ||
                (translated != null && normalize(translated) == nb) ||
                (code != null && looksLikeCode(b) && normalizeCode(b) == normalizeCode(code))
        }
        return AreaMatch(
            area = blockedHit ?: translated ?: unknown,
            blocked = blockedHit != null,
            unknownCode = unknown,
        )
    }

    /** Junta um código novo à tabela (com bairro vazio), para aparecer em "códigos para traduzir". */
    fun learnCode(rules: OfferRules, code: String): OfferRules {
        val key = normalizeCode(code)
        if (rules.areaCodes.keys.any { normalizeCode(it) == key }) return rules
        return rules.copy(areaCodes = rules.areaCodes + (displayCode(code) to ""))
    }

    /** "Uep5-S.3" -> "P5S3". */
    fun displayCode(code: String): String = normalizeCode(code).uppercase()

    private fun looksLikeCode(s: String) = codePattern.containsMatchIn(s) || Regex("(?i)^p\\d+s\\d+$").matches(s.trim())
}
