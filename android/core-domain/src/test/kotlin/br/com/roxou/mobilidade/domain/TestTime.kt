package br.com.roxou.mobilidade.domain

import br.com.roxou.mobilidade.domain.time.BRASILIA
import java.time.Instant
import java.time.LocalDateTime

/** "2026-11-24T04:00" no horário de Brasília. */
fun br(local: String): Instant = LocalDateTime.parse(local).atZone(BRASILIA).toInstant()
