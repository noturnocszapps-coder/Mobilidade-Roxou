package br.com.roxou.mobilidade.overlay

import android.content.Context
import br.com.roxou.mobilidade.domain.offer.OfferRecord
import br.com.roxou.mobilidade.domain.offer.OfferRecordCodec
import java.io.File

/**
 * Histórico de todas as ofertas vistas (aceitas e recusadas), num arquivo só do app.
 * Guarda só números e o nome do bairro. Base do resumo de ofertas e da IA da Etapa 7.
 */
object OfferLog {
    private const val FILE = "ofertas.txt"
    private const val MAX_LINES = 20_000

    private fun file(c: Context) = File(c.filesDir, FILE)

    @Synchronized
    fun append(c: Context, r: OfferRecord) {
        runCatching {
            val f = file(c)
            f.appendText(OfferRecordCodec.encode(r) + "\n")
            if (f.length() > 3_000_000) {
                val keep = f.readLines().takeLast(MAX_LINES / 2)
                f.writeText(keep.joinToString("\n", postfix = "\n"))
            }
        }
    }

    @Synchronized
    fun readAll(c: Context): List<OfferRecord> = runCatching {
        val f = file(c)
        if (!f.exists()) emptyList() else f.useLines { OfferRecordCodec.decodeAll(it) }
    }.getOrDefault(emptyList())
}
