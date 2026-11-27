package br.com.roxou.mobilidade.domain

import br.com.roxou.mobilidade.domain.goals.TierTable
import br.com.roxou.mobilidade.domain.rental.RentalPlan
import br.com.roxou.mobilidade.domain.rental.VehicleOwnership
import br.com.roxou.mobilidade.domain.settings.SettingsCodec
import br.com.roxou.mobilidade.domain.summary.AppSettings
import br.com.roxou.mobilidade.domain.time.TurnoverConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class SettingsCodecTest {
    @Test fun `ida e volta preserva tudo`() {
        val s = AppSettings(
            ownership = VehicleOwnership.OWN,
            rental = RentalPlan.MOVIDA_FX,
            carryShortfall = true,
            turnover = TurnoverConfig(rideWeekResetTime = LocalTime.of(5, 30)),
        )
        assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)))
        assertEquals(AppSettings(), SettingsCodec.decode(SettingsCodec.encode(AppSettings())))
    }

    @Test fun `vazio ou invalido cai no padrao`() {
        assertEquals(AppSettings(), SettingsCodec.decode(emptyMap()))
        val bad = mapOf(SettingsCodec.RENTAL_TIERS to "40:x", SettingsCodec.RIDE_RESET_TIME to "25:99")
        val d = SettingsCodec.decode(bad)
        assertEquals(TierTable.LOCALIZA_ZARP, d.rental.tiers)
        assertEquals(LocalTime.of(4, 0), d.turnover.rideWeekResetTime)
    }

    @Test fun `faixas em texto`() {
        assertEquals("40:8000,60:16000,80:25000", SettingsCodec.encodeTiers(TierTable.LOCALIZA_ZARP))
        assertEquals(TierTable.LOCALIZA_ZARP, SettingsCodec.decodeTiers(" 80:25000, 40:8000 ,60:16000"))
    }
}
