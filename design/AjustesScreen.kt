package br.com.roxou.mobilidade.design

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class AjustesUi(
    val recursos: Recursos,
    val leituraLigada: Boolean,
    val locadoraNome: String,
    val parcelaSemanalCentavos: Long,
    val faixas: List<Pair<Int, Long>>,
    val metaPorDia: List<Int>,
    val codigosParaTraduzir: List<String>,
    val codigos: List<Pair<String, String>>,
    val modulos: List<ModuloUi>,
    val planoAtual: String?,
    val emailConta: String?,
)

private enum class FolhaAjuste { NENHUMA, LOCADORA, METAS, CODIGOS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    ui: AjustesUi,
    onRecursos: (Recursos) -> Unit,
    onLigarLeitura: () -> Unit,
    onSalvarLocadora: (nome: String, parcelaCentavos: Long, faixas: List<Pair<Int, Long>>) -> Unit,
    onSalvarMetas: (List<Int>) -> Unit,
    onSalvarCodigos: (List<Pair<String, String>>) -> Unit,
    onDesbloquear: (moduloId: String) -> Unit,
    onVerPlanos: () -> Unit,
    onSair: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var folhaAberta by remember { mutableStateOf(FolhaAjuste.NENHUMA) }

    val mostrarLocadoraLinha = ui.recursos.metaLocadora || ui.recursos.locacao
    val mostrarMetasLinha = ui.recursos.metaDiaria
    val mostrarCodigosLinha = ui.recursos.janelaFlutuante
    val mostrarConfigCard = mostrarLocadoraLinha || mostrarMetasLinha || mostrarCodigosLinha

    Surface(
        modifier = modifier.fillMaxSize(),
        color = RoxouColors.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RoxouSpacing.lg, vertical = RoxouSpacing.xl)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(RoxouSpacing.lg),
        ) {
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineLarge,
                color = RoxouColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
            )

            // Aviso: leitura desligada quando janela flutuante está ligada
            if (ui.recursos.janelaFlutuante && !ui.leituraLigada) {
                RoxouCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = RoxouSpacing.lg,
                    borderColor = RoxouColors.Red.copy(alpha = 0.55f),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = RoxouColors.Red,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                text = "Leitura desligada",
                                style = MaterialTheme.typography.titleMedium,
                                color = RoxouColors.TextPrimary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Spacer(modifier = Modifier.width(RoxouSpacing.sm))

                        Button(
                            onClick = onLigarLeitura,
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = RoxouSpacing.lg),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoxouColors.Red,
                                contentColor = Color.White,
                            ),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text(
                                text = "Ligar",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }

            // Seção 1: Meus recursos
            Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm)) {
                Text(
                    text = "Meus recursos",
                    style = MaterialTheme.typography.labelMedium,
                    color = RoxouColors.TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = RoxouSpacing.xs),
                )

                RoxouCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = RoxouSpacing.md,
                ) {
                    Column {
                        ui.modulos.forEachIndexed { index, modulo ->
                            ModuloLinha(
                                modulo = modulo,
                                ligado = recursoAtivoParaModulo(modulo.id, ui.recursos),
                                onToggle = { novoValor ->
                                    onRecursos(atualizarRecurso(modulo.id, novoValor, ui.recursos))
                                },
                                onDesbloquear = { onDesbloquear(modulo.id) },
                            )
                            if (index < ui.modulos.lastIndex) {
                                HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)
                            }
                        }
                    }
                }
            }

            // Seção 2: Configurações dos recursos ligados
            if (mostrarConfigCard) {
                RoxouCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = RoxouSpacing.md,
                ) {
                    Column {
                        var jaTemLinha = false

                        if (mostrarLocadoraLinha) {
                            ConfiguracaoLinha(
                                titulo = "Locadora e parcela",
                                detalhe = "${ui.locadoraNome} · ${formatarReais(ui.parcelaSemanalCentavos)}",
                                mostrarAlerta = false,
                                onClick = { folhaAberta = FolhaAjuste.LOCADORA },
                            )
                            jaTemLinha = true
                        }

                        if (mostrarMetasLinha) {
                            if (jaTemLinha) {
                                HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)
                            }
                            val media = if (ui.metaPorDia.isEmpty()) 0 else ui.metaPorDia.sum() / ui.metaPorDia.size
                            ConfiguracaoLinha(
                                titulo = "Meta por dia",
                                detalhe = "Média $media/dia",
                                mostrarAlerta = false,
                                onClick = { folhaAberta = FolhaAjuste.METAS },
                            )
                            jaTemLinha = true
                        }

                        if (mostrarCodigosLinha) {
                            if (jaTemLinha) {
                                HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)
                            }
                            val pendentes = ui.codigosParaTraduzir.size
                            ConfiguracaoLinha(
                                titulo = "Códigos de região",
                                detalhe = if (pendentes > 0) "$pendentes pendente(s)" else "${ui.codigos.size} salvos",
                                mostrarAlerta = pendentes > 0,
                                onClick = { folhaAberta = FolhaAjuste.CODIGOS },
                            )
                        }
                    }
                }
            }

            // Seção 3: Conta e Plano
            RoxouCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = RoxouSpacing.lg,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.md)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(RoxouColors.PurpleSurface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = RoxouColors.PurpleSoft,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ui.emailConta ?: "Conta local",
                                style = MaterialTheme.typography.titleMedium,
                                color = RoxouColors.TextPrimary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = ui.planoAtual ?: "Sem plano ativo",
                                style = MaterialTheme.typography.labelMedium,
                                color = RoxouColors.PurpleSoft,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onVerPlanos,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, RoxouColors.Purple),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text(
                            text = "Ver planos",
                            style = MaterialTheme.typography.titleMedium,
                            color = RoxouColors.PurpleSoft,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }

            // Sair discreto
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TextButton(
                    onClick = onSair,
                    modifier = Modifier.height(48.dp),
                ) {
                    Text(
                        text = "Sair",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoxouColors.TextMuted,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }

    when (folhaAberta) {
        FolhaAjuste.LOCADORA -> {
            ModalBottomSheet(
                onDismissRequest = { folhaAberta = FolhaAjuste.NENHUMA },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = RoxouColors.SurfaceCard,
                dragHandle = null,
            ) {
                LocadoraFolhaConteudo(
                    nomeInicial = ui.locadoraNome,
                    parcelaInicialCentavos = ui.parcelaSemanalCentavos,
                    faixasIniciais = ui.faixas,
                    onSalvar = { nome, parcela, faixas ->
                        onSalvarLocadora(nome, parcela, faixas)
                        folhaAberta = FolhaAjuste.NENHUMA
                    },
                )
            }
        }

        FolhaAjuste.METAS -> {
            ModalBottomSheet(
                onDismissRequest = { folhaAberta = FolhaAjuste.NENHUMA },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = RoxouColors.SurfaceCard,
                dragHandle = null,
            ) {
                MetasFolhaConteudo(
                    metasIniciais = ui.metaPorDia,
                    onSalvar = { novasMetas ->
                        onSalvarMetas(novasMetas)
                        folhaAberta = FolhaAjuste.NENHUMA
                    },
                )
            }
        }

        FolhaAjuste.CODIGOS -> {
            ModalBottomSheet(
                onDismissRequest = { folhaAberta = FolhaAjuste.NENHUMA },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = RoxouColors.SurfaceCard,
                dragHandle = null,
            ) {
                CodigosFolhaConteudo(
                    pendentes = ui.codigosParaTraduzir,
                    codigosIniciais = ui.codigos,
                    onSalvar = { novosCodigos ->
                        onSalvarCodigos(novosCodigos)
                        folhaAberta = FolhaAjuste.NENHUMA
                    },
                )
            }
        }

        FolhaAjuste.NENHUMA -> Unit
    }
}

@Composable
private fun ModuloLinha(
    modulo: ModuloUi,
    ligado: Boolean,
    onToggle: (Boolean) -> Unit,
    onDesbloquear: () -> Unit,
) {
    val rowModifier = if (modulo.liberado) {
        Modifier
            .fillMaxWidth()
            .padding(vertical = RoxouSpacing.sm, horizontal = RoxouSpacing.xs)
    } else {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RoxouRadius.sm))
            .clickable(onClick = onDesbloquear)
            .padding(vertical = RoxouSpacing.md, horizontal = RoxouSpacing.xs)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
    ) {
        Icon(
            imageVector = iconeModulo(modulo.id),
            contentDescription = null,
            tint = when {
                !modulo.liberado -> RoxouColors.TextMuted
                ligado -> RoxouColors.PurpleSoft
                else -> RoxouColors.TextSecondary
            },
            modifier = Modifier.size(20.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = modulo.nome,
                style = MaterialTheme.typography.titleMedium,
                color = if (modulo.liberado) RoxouColors.TextPrimary else RoxouColors.TextSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = modulo.frase,
                style = MaterialTheme.typography.labelMedium,
                color = RoxouColors.TextMuted,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (modulo.liberado) {
            Switch(
                checked = ligado,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = RoxouColors.Purple,
                    uncheckedThumbColor = RoxouColors.TextSecondary,
                    uncheckedTrackColor = RoxouColors.Track,
                    uncheckedBorderColor = RoxouColors.CardBorder,
                ),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(RoxouColors.Track),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Desbloquear",
                    tint = RoxouColors.PurpleSoft,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ConfiguracaoLinha(
    titulo: String,
    detalhe: String,
    mostrarAlerta: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RoxouRadius.sm))
            .clickable(onClick = onClick)
            .padding(vertical = RoxouSpacing.md, horizontal = RoxouSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = RoxouColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            if (mostrarAlerta) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(RoxouColors.Red),
                )
            }
        }

        Spacer(modifier = Modifier.width(RoxouSpacing.sm))

        Text(
            text = detalhe,
            style = MaterialTheme.typography.labelMedium,
            color = RoxouColors.TextSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LocadoraFolhaConteudo(
    nomeInicial: String,
    parcelaInicialCentavos: Long,
    faixasIniciais: List<Pair<Int, Long>>,
    onSalvar: (String, Long, List<Pair<Int, Long>>) -> Unit,
) {
    var nome by remember { mutableStateOf(nomeInicial) }
    var parcelaTexto by remember {
        mutableStateOf((parcelaInicialCentavos / 100L).toString())
    }
    var faixas by remember {
        mutableStateOf(
            faixasIniciais.map { (corridas, desconto) ->
                corridas.toString() to (desconto / 100L).toString()
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(RoxouSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
    ) {
        Text(
            text = "Locadora e parcela",
            style = MaterialTheme.typography.titleLarge,
            color = RoxouColors.TextPrimary,
            maxLines = 1,
            softWrap = false,
        )

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Locadora", maxLines = 1, softWrap = false) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = campoAjusteColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = parcelaTexto,
            onValueChange = { parcelaTexto = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' } },
            label = { Text("Parcela semanal", maxLines = 1, softWrap = false) },
            prefix = { Text("R$ ", color = RoxouColors.TextSecondary, maxLines = 1, softWrap = false) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = MaterialTheme.shapes.medium,
            colors = campoAjusteColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Faixas de desconto",
                style = MaterialTheme.typography.labelMedium,
                color = RoxouColors.TextSecondary,
                maxLines = 1,
                softWrap = false,
            )
            TextButton(
                onClick = { faixas = faixas + ("" to "") },
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = RoxouColors.PurpleSoft,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "faixa",
                    color = RoxouColors.PurpleSoft,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }

        faixas.forEachIndexed { idx, par ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
            ) {
                OutlinedTextField(
                    value = par.first,
                    onValueChange = { novo ->
                        val copia = faixas.toMutableList()
                        copia[idx] = novo.filter { it.isDigit() } to par.second
                        faixas = copia
                    },
                    suffix = { Text("corr.", style = MaterialTheme.typography.labelMedium, color = RoxouColors.TextMuted, maxLines = 1) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.small,
                    colors = campoAjusteColors(),
                    modifier = Modifier.weight(1f),
                )

                Text(
                    text = "→",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoxouColors.TextMuted,
                    maxLines = 1,
                )

                OutlinedTextField(
                    value = par.second,
                    onValueChange = { novo ->
                        val copia = faixas.toMutableList()
                        copia[idx] = par.first to novo.filter { it.isDigit() }
                        faixas = copia
                    },
                    prefix = { Text("R$", style = MaterialTheme.typography.labelMedium, color = RoxouColors.TextMuted, maxLines = 1) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.small,
                    colors = campoAjusteColors(),
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = {
                        faixas = faixas.filterIndexed { i, _ -> i != idx }
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remover faixa",
                        tint = RoxouColors.TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(RoxouSpacing.xs))

        Button(
            onClick = {
                val faixasConvertidas = faixas.mapNotNull { (c, d) ->
                    val corridas = c.toIntOrNull()
                    val reais = d.toLongOrNull()
                    if (corridas != null && corridas > 0 && reais != null && reais >= 0L) {
                        corridas to (reais * 100L)
                    } else {
                        null
                    }
                }.sortedBy { it.first }
                onSalvar(nome.trim(), parseCentavos(parcelaTexto), faixasConvertidas)
            },
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = RoxouColors.Purple,
                contentColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(
                text = "Salvar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun MetasFolhaConteudo(
    metasIniciais: List<Int>,
    onSalvar: (List<Int>) -> Unit,
) {
    val dias = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
    var metas by remember {
        mutableStateOf(
            List(7) { idx -> metasIniciais.getOrElse(idx) { 18 } },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(RoxouSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
    ) {
        Text(
            text = "Meta por dia",
            style = MaterialTheme.typography.titleLarge,
            color = RoxouColors.TextPrimary,
            maxLines = 1,
            softWrap = false,
        )

        Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm)) {
            dias.forEachIndexed { idx, dia ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = dia,
                        style = MaterialTheme.typography.titleMedium,
                        color = RoxouColors.TextPrimary,
                        maxLines = 1,
                        softWrap = false,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
                    ) {
                        Surface(
                            onClick = {
                                val atual = metas[idx]
                                if (atual > 0) {
                                    metas = metas.toMutableList().also { it[idx] = atual - 1 }
                                }
                            },
                            shape = CircleShape,
                            color = RoxouColors.Track,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "−",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = RoxouColors.TextPrimary,
                                    maxLines = 1,
                                )
                            }
                        }

                        Text(
                            text = metas[idx].toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = RoxouColors.TextPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.width(36.dp),
                        )

                        Surface(
                            onClick = {
                                val atual = metas[idx]
                                metas = metas.toMutableList().also { it[idx] = atual + 1 }
                            },
                            shape = CircleShape,
                            color = RoxouColors.PurpleSurface,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "+",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = RoxouColors.PurpleSoft,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(RoxouSpacing.xs))

        Button(
            onClick = { onSalvar(metas) },
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = RoxouColors.Purple,
                contentColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(
                text = "Salvar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CodigosFolhaConteudo(
    pendentes: List<String>,
    codigosIniciais: List<Pair<String, String>>,
    onSalvar: (List<Pair<String, String>>) -> Unit,
) {
    var codigoInput by remember { mutableStateOf(pendentes.firstOrNull() ?: "") }
    var bairroInput by remember { mutableStateOf("") }
    var lista by remember { mutableStateOf(codigosIniciais) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(RoxouSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
    ) {
        Text(
            text = "Códigos de região",
            style = MaterialTheme.typography.titleLarge,
            color = RoxouColors.TextPrimary,
            maxLines = 1,
            softWrap = false,
        )

        if (pendentes.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
            ) {
                pendentes.forEach { cod ->
                    Surface(
                        onClick = { codigoInput = cod },
                        shape = CircleShape,
                        color = RoxouColors.Red.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, RoxouColors.Red.copy(alpha = 0.45f)),
                    ) {
                        Text(
                            text = cod,
                            style = MaterialTheme.typography.labelMedium,
                            color = RoxouColors.Red,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = codigoInput,
            onValueChange = { codigoInput = it.uppercase() },
            label = { Text("Código", maxLines = 1, softWrap = false) },
            placeholder = { Text("UP5S2", color = RoxouColors.TextMuted, maxLines = 1) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = campoAjusteColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = bairroInput,
            onValueChange = { bairroInput = it },
            label = { Text("Bairro", maxLines = 1, softWrap = false) },
            placeholder = { Text("Jardim Leonor", color = RoxouColors.TextMuted, maxLines = 1) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = campoAjusteColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedButton(
            onClick = {
                val c = codigoInput.trim()
                val b = bairroInput.trim()
                if (c.isNotEmpty() && b.isNotEmpty()) {
                    lista = lista.filterNot { it.first.equals(c, ignoreCase = true) } + (c to b)
                    codigoInput = ""
                    bairroInput = ""
                }
            },
            enabled = codigoInput.trim().isNotEmpty() && bairroInput.trim().isNotEmpty(),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, RoxouColors.CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(
                text = "Adicionar código",
                style = MaterialTheme.typography.bodyMedium,
                color = RoxouColors.PurpleSoft,
                maxLines = 1,
                softWrap = false,
            )
        }

        lista.forEach { par ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${par.first} → ${par.second}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoxouColors.TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { lista = lista.filterNot { it == par } },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remover",
                        tint = RoxouColors.TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Button(
            onClick = { onSalvar(lista) },
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = RoxouColors.Purple,
                contentColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(
                text = "Salvar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun campoAjusteColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = RoxouColors.Purple,
    unfocusedBorderColor = RoxouColors.CardBorder,
    focusedContainerColor = RoxouColors.Background,
    unfocusedContainerColor = RoxouColors.Background,
    focusedTextColor = RoxouColors.TextPrimary,
    unfocusedTextColor = RoxouColors.TextPrimary,
    focusedLabelColor = RoxouColors.PurpleSoft,
    unfocusedLabelColor = RoxouColors.TextSecondary,
    cursorColor = RoxouColors.Purple,
)

private fun iconeModulo(id: String): ImageVector = when (id) {
    "janela" -> Icons.Filled.Star
    "metaLocadora" -> Icons.Filled.DateRange
    "locacao" -> Icons.Filled.ShoppingCart
    "financas" -> Icons.Filled.CheckCircle
    "metaDiaria" -> Icons.Filled.Place
    else -> Icons.Filled.Build
}

private fun recursoAtivoParaModulo(id: String, recursos: Recursos): Boolean = when (id) {
    "janela" -> recursos.janelaFlutuante
    "metaLocadora" -> recursos.metaLocadora
    "locacao" -> recursos.locacao
    "financas" -> recursos.financas
    "metaDiaria" -> recursos.metaDiaria
    else -> false
}

private fun atualizarRecurso(id: String, ativo: Boolean, recursos: Recursos): Recursos = when (id) {
    "janela" -> recursos.copy(janelaFlutuante = ativo)
    "metaLocadora" -> recursos.copy(metaLocadora = ativo)
    "locacao" -> recursos.copy(locacao = ativo)
    "financas" -> recursos.copy(financas = ativo)
    "metaDiaria" -> recursos.copy(metaDiaria = ativo)
    else -> recursos
}

@Preview(
    name = "Ajustes",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 980,
)
@Composable
private fun AjustesScreenPreview() {
    RoxouDesignTheme {
        AjustesScreen(
            ui = AjustesUi(
                recursos = Recursos(
                    metaLocadora = true,
                    locacao = true,
                    financas = true,
                    metaDiaria = true,
                    janelaFlutuante = true,
                ),
                leituraLigada = false,
                locadoraNome = "Localiza",
                parcelaSemanalCentavos = 114900L,
                faixas = listOf(40 to 8000L, 60 to 16000L, 100 to 25000L),
                metaPorDia = listOf(18, 18, 18, 20, 22, 20, 12),
                codigosParaTraduzir = listOf("UP5S2"),
                codigos = listOf("BR2K1" to "Centro", "PQ8M4" to "Parque do Povo"),
                modulos = listOf(
                    ModuloUi("janela", "Janela flutuante", "Avalia ofertas sobre Uber e 99", true),
                    ModuloUi("metaLocadora", "Meta da locadora", "Conta corridas Uber para desconto", true),
                    ModuloUi("locacao", "Locação", "Controla parcela semanal do carro", true),
                    ModuloUi("financas", "Finanças", "Caixa, gastos e dinheiro livre", true),
                    ModuloUi("metaDiaria", "Meta diária", "Objetivo de corridas por dia", true),
                    ModuloUi("ia", "Resumo inteligente", "Analisa melhores horários da semana", false),
                ),
                planoAtual = "Teste grátis · faltam 5 dias",
                emailConta = "motorista@roxou.com.br",
            ),
            onRecursos = {},
            onLigarLeitura = {},
            onSalvarLocadora = { _, _, _ -> },
            onSalvarMetas = {},
            onSalvarCodigos = {},
            onDesbloquear = {},
            onVerPlanos = {},
            onSair = {},
        )
    }
}
