package br.com.roxou.mobilidade.domain

import br.com.roxou.mobilidade.domain.goals.TierEngine
import br.com.roxou.mobilidade.domain.goals.TierTable
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.offer.AreaFilter
import br.com.roxou.mobilidade.domain.offer.OcrLine
import br.com.roxou.mobilidade.domain.offer.OfferEvaluator
import br.com.roxou.mobilidade.domain.offer.OfferParser
import br.com.roxou.mobilidade.domain.offer.OfferRules
import br.com.roxou.mobilidade.domain.offer.TierContext
import br.com.roxou.mobilidade.domain.offer.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Linhas reais do OCR do teste 3 da Etapa 1 (27/11/2026). */
class OfferTest {
    private fun l(b: String, t: String): OcrLine {
        val n = Regex("-?\\d+").findAll(b).map { it.value.toInt() }.toList()
        return OcrLine(t, n[0], n[1], n[2], n[3])
    }

    private val offer99 = listOf(
        l("[52,34][309,75]", "12:43 99"),
        l("[0,168][141,202]", "esidente"),
        l("[104,744][401,800]", "Pgto. no app"),
        l("[82,1043][333,1091]", "R$1,64/km"),
        l("[82,878][761,1003]", "R$13,80 41,1x"),
        l("[152,1195][705,1245]", "0% de Taxa99 semanal"),
        l("[14,1286][853,1344]", "ré R\$T1,61 por corrida longa incl."),
        l("[83,1454][605,1501]", "t 4,88 986 corridas"),
        l("[168,1526][501,1581]", "Perfil Premium"),
        l("[573,163][994,204]", "X Não afeta a TA"),
        l("[182,1688][482,1743]", "4 min (363 m)"),
        l("[631,350][880,392]", "MONTALVÄO"),
        l("[151,1904][503,1956]", "17 min (8,1 km)"),
        l("[167,1752][943,1816]", "Rua Junqueira Freire, 176, Álvares"),
        l("[152,1824][360,1866]", "Machado"),
        l("[153,2036][351,2077]", "Charlote"),
        l("[154,1969][883,2020]", "Rua Prof. Hugo Mielli, 362, Vila"),
        l("[932,1668][1033,1704]", "Uber"),
    )

    private val offerUber = listOf(
        l("[196,1069][332,1105]", "UberX"),
        l("[100,1165][589,1289]", "R$ 15,05"),
        l("[132,1327][390,1376]", "* 5,00 (48)"),
        l("[195,1486][511,1531]", "9 min (3.0 km)"),
        l("[539,1330][769,1372]", "Verificado"),
        l("[196,1621][396,1657]", "Prudente"),
        l("[196,1561][956,1603]", "R. Luiz Antonio Chelse, Presidente"),
        l("[203,1687][630,1733]", "14 minutos (5.9 km)"),
        l("[192,1765][758,1810]", "Av. Washington Luiz, 875,"),
        l("[196,1826][655,1862]", "Presidente Prudente"),
        l("[890,1091][932,1131]", "X"),
        l("[438,1993][834,2035]", "Tenho interesse"),
    )

    @Test fun le_oferta_da_99() {
        val o = OfferParser.parse(offer99, Platform.NINETY_NINE)!!
        assertEquals(13_80L, o.fareCents)
        assertEquals(0.363, o.pickupKm, 1e-9)
        assertEquals(4, o.pickupMinutes)
        assertEquals(8.1, o.tripKm, 1e-9)
        assertEquals(17, o.tripMinutes)
        assertEquals("Rua Junqueira Freire, 176, Álvares Machado", o.pickupAddress)
        assertEquals("Rua Prof. Hugo Mielli, 362, Vila Charlote", o.dropoffAddress)
    }

    @Test fun le_oferta_da_uber() {
        val o = OfferParser.parse(offerUber, Platform.UBER)!!
        assertEquals(15_05L, o.fareCents)
        assertEquals(3.0, o.pickupKm, 1e-9)
        assertEquals(9, o.pickupMinutes)
        assertEquals(5.9, o.tripKm, 1e-9)
        assertEquals(14, o.tripMinutes)
        assertEquals("R. Luiz Antonio Chelse, Presidente Prudente", o.pickupAddress)
        assertEquals("Av. Washington Luiz, 875, Presidente Prudente", o.dropoffAddress)
    }

    @Test fun tela_sem_oferta_nao_vira_oferta() {
        val lines = listOf(l("[399,176][618,241]", "R\$O,00"), l("[351,2034][731,2105]", "Buscando"))
        assertNull(OfferParser.parse(lines, Platform.NINETY_NINE))
    }

    @Test fun soma_busca_e_viagem() {
        val o = OfferParser.parse(offer99, Platform.NINETY_NINE)!!
        val e = OfferEvaluator.evaluate(o, OfferRules())
        assertEquals(8.463, e.totalKm, 1e-9)
        assertEquals(21, e.totalMinutes)
        assertEquals(163L, e.perKmCents) // R$ 13,80 / 8,463 km
        assertEquals(39_43L, e.perHourCents) // R$ 13,80 em 21 min
        assertEquals(13_80L - 381L, e.netCents) // 8,463 km x R$ 0,45
        assertEquals(Verdict.BORDERLINE, e.verdict)
    }

    @Test fun corrida_fraca_ajuda_a_fechar_faixa_com_carro_alugado() {
        val o = OfferParser.parse(offerUber, Platform.UBER)!!
        val rules = OfferRules(minPerKmCents = 180, goodPerKmCents = 250)
        val semMeta = OfferEvaluator.evaluate(o, rules)
        assertEquals(Verdict.BAD, semMeta.verdict)
        val ctx = TierContext(true, setOf(Platform.UBER), TierEngine.evaluate(54, TierTable.LOCALIZA_ZARP))
        val comMeta = OfferEvaluator.evaluate(o, rules, ctx)
        assertEquals(Verdict.BORDERLINE, comMeta.verdict)
        assertTrue(comMeta.reason.contains("faltam 6"))
    }

    @Test fun busca_longe_piora_a_cor() {
        val o = OfferParser.parse(offerUber, Platform.UBER)!!.copy(fareCents = 40_00, pickupKm = 6.5)
        val e = OfferEvaluator.evaluate(o, OfferRules())
        assertEquals(Verdict.BORDERLINE, e.verdict)
        assertTrue(e.reason.startsWith("Busca longe"))
    }

    @Test fun bairro_bloqueado_no_destino() {
        val o = OfferParser.parse(offer99, Platform.NINETY_NINE)!!
        val e = OfferEvaluator.evaluate(o, OfferRules(blockedAreas = listOf("Vila Charlote")))
        assertTrue(e.blocked)
        assertTrue(e.dropoffArea.blocked)
        assertFalse(e.pickupArea.blocked)
        assertTrue(e.reason.startsWith("Destino em Vila Charlote"))
    }

    @Test fun codigo_de_regiao_traduzido_e_bloqueado() {
        val rules = OfferRules(blockedAreas = listOf("Parque dos Pinheiros"), areaCodes = mapOf("P5S3" to "Parque dos Pinheiros"))
        val m = AreaFilter.match("Rua Tal, 10, Uep5-S.3", rules)
        assertTrue(m.blocked)
        assertEquals("Parque dos Pinheiros", m.area)
    }

    @Test fun codigo_desconhecido_aparece_para_traduzir() {
        val m = AreaFilter.match("Rua Tal, 10, Uep4-S.2", OfferRules())
        assertFalse(m.blocked)
        assertNotNull(m.unknownCode)
        assertEquals("p4s2", AreaFilter.normalizeCode(m.unknownCode!!))
    }

    @Test fun bloqueio_pelo_proprio_codigo() {
        val m = AreaFilter.match("Rua Tal, 10, UEP5 S2", OfferRules(blockedAreas = listOf("P5S2")))
        assertTrue(m.blocked)
    }

    @Test fun bloqueio_pelo_codigo_escrito_de_varios_jeitos() {
        val rules = OfferRules(blockedAreas = listOf("UP5S2"))
        assertTrue(AreaFilter.match("Rua Tal, 10, Uep5-S.2", rules).blocked)
        assertTrue(AreaFilter.match("Uep5-S2, Presidente Prudente", rules).blocked)
        assertFalse(AreaFilter.match("Rua Tal, 10, Uep5-S.3", rules).blocked)
    }

    @Test fun nome_sem_acento_bate() {
        val m = AreaFilter.match("Rua A, 1, Jardim Leonor", OfferRules(blockedAreas = listOf("jardim leonór")))
        assertTrue(m.blocked)
    }

    @Test fun acha_endereco_na_tela_da_corrida() {
        val lines = listOf(l("[167,1752][943,1816]", "Rua Junqueira Freire, 176, Álvares"), l("[152,1824][360,1866]", "Machado"))
        assertEquals(listOf("Rua Junqueira Freire, 176, Álvares Machado"), OfferParser.findAddresses(lines))
    }
}

class OfferSettingsTest {
    @Test fun regras_da_oferta_ida_e_volta() {
        val rules = br.com.roxou.mobilidade.domain.offer.OfferRules(
            minPerKmCents = 170, blockedAreas = listOf("Jardim Leonor", "Brasil Novo"),
            areaCodes = linkedMapOf("P5S3" to "Parque dos Pinheiros", "P4S2" to ""),
        )
        val s = br.com.roxou.mobilidade.domain.summary.AppSettings(offer = rules)
        val back = br.com.roxou.mobilidade.domain.settings.SettingsCodec.decode(
            br.com.roxou.mobilidade.domain.settings.SettingsCodec.encode(s),
        )
        assertEquals(rules, back.offer)
    }

    @Test fun aprende_codigo_novo_uma_vez() {
        val r0 = OfferRules()
        val r1 = AreaFilter.learnCode(r0, "Uep4-S.2")
        val r2 = AreaFilter.learnCode(r1, "UEP4 S2")
        assertEquals(mapOf("P4S2" to ""), r2.areaCodes)
    }
}

class AcceptDetectorTest {
    private fun line(t: String) = OcrLine(t, 0, 0, 100, 40)

    @Test fun tela_a_caminho_e_aceite() {
        assertTrue(br.com.roxou.mobilidade.domain.offer.AcceptDetector.looksAccepted(listOf(line("A caminho"), line("Rua Tal, 10"))))
        assertTrue(br.com.roxou.mobilidade.domain.offer.AcceptDetector.looksAccepted(listOf(line("Iniciar Viagem"))))
    }

    @Test fun voltou_a_procurar_nao_e_aceite() {
        assertFalse(br.com.roxou.mobilidade.domain.offer.AcceptDetector.looksAccepted(listOf(line("Procurando viagens"))))
        assertFalse(br.com.roxou.mobilidade.domain.offer.AcceptDetector.looksAccepted(listOf(line("Você está online"))))
    }

    @Test fun vira_corrida_a_confirmar() {
        val o = br.com.roxou.mobilidade.domain.offer.ParsedOffer(Platform.NINETY_NINE, 13_80, 0.363, 4, 8.1, 17)
        val t = br.com.roxou.mobilidade.domain.offer.AcceptDetector.toTrip("x", o, java.time.Instant.EPOCH, byButton = true)
        assertEquals(Platform.NINETY_NINE, t.platform)
        assertFalse(t.confirmed)
        assertEquals(13_80L, t.amountCents)
    }
}
