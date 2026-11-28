package br.com.roxou.mobilidade.design

data class CompensaUi(
    val minimoPorKmCentavos: Long,
    val bomPorKmCentavos: Long,
    val minimoPorHoraCentavos: Long,
    val bomPorHoraCentavos: Long,
    val custoPorKmCentavos: Long,
    val buscaMaximaKm: Double,
    val bairrosEvitados: List<String>,
)
