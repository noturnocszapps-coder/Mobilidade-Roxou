package br.com.roxou.mobilidade.domain

import br.com.roxou.mobilidade.domain.goals.DiscountTier
import br.com.roxou.mobilidade.domain.goals.TierEngine
import br.com.roxou.mobilidade.domain.goals.TierTable
import br.com.roxou.mobilidade.domain.goals.WeeklyRideCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TierTest {
    private val zarp = TierTable.LOCALIZA_ZARP

    @Test fun `abaixo de 40 nao ha desconto`() {
        assertEquals(0L, TierEngine.discountEarned(0, zarp))
        assertEquals(0L, TierEngine.discountEarned(39, zarp))
        assertNull(TierEngine.evaluate(39, zarp).achieved)
    }

    @Test fun `faixas exatas 40 60 80 valem 80 160 250`() {
        assertEquals(80_00L, TierEngine.discountEarned(40, zarp))
        assertEquals(160_00L, TierEngine.discountEarned(60, zarp))
        assertEquals(250_00L, TierEngine.discountEarned(80, zarp))
    }

    @Test fun `nao existe faixa intermediaria`() {
        assertEquals(80_00L, TierEngine.discountEarned(50, zarp)) // o plano antigo dizia 50; está errado
        assertEquals(80_00L, TierEngine.discountEarned(59, zarp))
        assertEquals(160_00L, TierEngine.discountEarned(79, zarp))
        assertEquals(250_00L, TierEngine.discountEarned(140, zarp))
    }

    @Test fun `exemplo do documento - 54 corridas faltam 6 para 160 e 26 para 250`() {
        val p = TierEngine.evaluate(54, zarp)
        assertEquals(80_00L, p.achievedDiscountCents)
        assertEquals(60, p.next?.minRides)
        assertEquals(listOf(6 to 160_00L, 26 to 250_00L), p.upcoming.map { it.ridesRemaining to it.tier.discountCents })
        assertEquals(6, p.ridesToNext)
    }

    @Test fun `acima da ultima faixa nao ha proxima`() {
        val p = TierEngine.evaluate(85, zarp)
        assertNull(p.next)
        assertTrue(p.upcoming.isEmpty())
        assertNull(p.ridesToNext)
    }

    @Test fun `tabela configuravel e ordenada`() {
        val custom = TierTable(listOf(DiscountTier(70, 200_00), DiscountTier(30, 50_00)))
        assertEquals(50_00L, TierEngine.discountEarned(45, custom))
        assertEquals(40, TierEngine.evaluate(30, custom).upcoming.single().ridesRemaining)
        assertEquals(0L, TierEngine.discountEarned(100, TierTable.NONE))
    }

    @Test fun `contagem negativa nao quebra`() {
        assertEquals(40, TierEngine.evaluate(-3, zarp).ridesToNext)
    }

    @Test fun `numero do painel da Uber substitui a contagem do app`() {
        assertEquals(54, WeeklyRideCount(54).official)
        val c = WeeklyRideCount(countedByApp = 58, uberPanelOverride = 61)
        assertEquals(61, c.official)
        assertTrue(c.isCorrected)
        assertFalse(WeeklyRideCount(61, 61).isCorrected)
        assertEquals(160_00L, TierEngine.discountEarned(c.official, zarp))
    }
}
