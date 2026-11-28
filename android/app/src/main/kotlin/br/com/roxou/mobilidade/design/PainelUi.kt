package br.com.roxou.mobilidade.design

data class PainelUi(
    val diaTexto: String,
    val corridasHoje: Int,
    val metaHoje: Int,
    val corridasUberHoje: Int,
    val corridas99Hoje: Int,
    val faturadoHojeCentavos: Long,
    val corridasSemanaUber: Int,
    val proximaFaixaCorridas: Int?,
    val proximaFaixaDescontoCentavos: Long?,
    val descontoGarantidoCentavos: Long,
    val parcelaFaltaCentavos: Long?,
    val dinheiroLivreCentavos: Long,
    val recursos: Recursos,
)
