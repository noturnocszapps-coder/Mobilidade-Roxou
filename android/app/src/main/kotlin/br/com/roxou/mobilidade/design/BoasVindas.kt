package br.com.roxou.mobilidade.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BoasVindasScreen(
    onConcluir: (Recursos) -> Unit,
    onLigarLeitura: () -> Unit,
) {
    BoasVindasConteudo(
        passoInicial = 0,
        onConcluir = onConcluir,
        onLigarLeitura = onLigarLeitura,
    )
}

@Composable
private fun BoasVindasConteudo(
    passoInicial: Int,
    onConcluir: (Recursos) -> Unit,
    onLigarLeitura: () -> Unit,
) {
    var passo by remember { mutableIntStateOf(passoInicial.coerceIn(0, 2)) }
    var recursos by remember {
        mutableStateOf(
            Recursos(
                metaLocadora = true,
                locacao = true,
                financas = true,
                metaDiaria = true,
                janelaFlutuante = true,
            ),
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RoxouColors.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = RoxouSpacing.xl, vertical = RoxouSpacing.xl)
                .animateContentSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Indicador de bolinhas (3 passos)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RoxouSpacing.sm),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) { idx ->
                    val ativo = idx == passo
                    val cor by animateColorAsState(
                        targetValue = if (ativo) RoxouColors.Purple else RoxouColors.Track,
                        label = "bolinha_passo",
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = RoxouSpacing.xs)
                            .height(8.dp)
                            .width(if (ativo) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(cor),
                    )
                }
            }

            // Conteúdo do passo atual
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                when (passo) {
                    0 -> PassoApresentacao()
                    1 -> PassoRecursos(
                        recursos = recursos,
                        onRecursos = { recursos = it },
                    )
                    else -> PassoLeitura()
                }
            }

            // Ações de rodapé
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (passo) {
                    0 -> {
                        Button(
                            onClick = { passo = 1 },
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
                                text = "Continuar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }

                    1 -> {
                        Button(
                            onClick = { passo = 2 },
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
                                text = "Continuar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }

                    else -> {
                        Button(
                            onClick = onLigarLeitura,
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
                                text = "Ligar agora",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }

                        TextButton(
                            onClick = { onConcluir(recursos) },
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text(
                                text = "Depois",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RoxouColors.TextSecondary,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PassoApresentacao() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RoxouSpacing.xl),
    ) {
        // Ilustração da janelinha verde sobre um cartão de oferta
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RoxouSpacing.sm),
            contentAlignment = Alignment.TopEnd,
        ) {
            // Cartão simulado de oferta ao fundo
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                shape = MaterialTheme.shapes.large,
                color = RoxouColors.SurfaceCard,
                border = BorderStroke(1.dp, RoxouColors.CardBorder),
            ) {
                Column(
                    modifier = Modifier.padding(RoxouSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppMarcaSelo(app = "Uber")
                        Text(
                            text = "4 min · 1,2 km",
                            style = MaterialTheme.typography.labelMedium,
                            color = RoxouColors.TextMuted,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }

                    Text(
                        text = "R$ 19,20",
                        style = MaterialTheme.typography.headlineLarge,
                        color = RoxouColors.TextPrimary,
                        maxLines = 1,
                        softWrap = false,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(RoxouColors.Track),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(RoxouColors.Track),
                        )
                    }
                }
            }

            // Janelinha verde flutuante por cima
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = RoxouColors.Background,
                border = BorderStroke(1.5.dp, RoxouColors.Green),
                shadowElevation = 8.dp,
                modifier = Modifier.padding(end = RoxouSpacing.sm),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = RoxouSpacing.md, vertical = RoxouSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(RoxouColors.Green),
                    )
                    Text(
                        text = "R$ 2,40/km · R$ 48/h",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = RoxouColors.Green,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RoxouSpacing.xs),
        ) {
            Text(
                text = "Saiba na hora se a corrida compensa",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                color = RoxouColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Olhe de relance antes de aceitar",
                style = MaterialTheme.typography.bodyMedium,
                color = RoxouColors.TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PassoRecursos(
    recursos: Recursos,
    onRecursos: (Recursos) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RoxouSpacing.lg),
    ) {
        Text(
            text = "O que você quer usar?",
            style = MaterialTheme.typography.headlineLarge,
            color = RoxouColors.TextPrimary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )

        RoxouCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = RoxouSpacing.md,
        ) {
            Column {
                RecursoToggleLinha(
                    icone = Icons.Filled.Star,
                    nome = "Janela flutuante",
                    frase = "Avalia ofertas sobre Uber e 99",
                    ligado = recursos.janelaFlutuante,
                    onTrocar = { onRecursos(recursos.copy(janelaFlutuante = it)) },
                )
                HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)
                RecursoToggleLinha(
                    icone = Icons.Filled.DateRange,
                    nome = "Meta da locadora",
                    frase = "Conta corridas Uber para desconto",
                    ligado = recursos.metaLocadora,
                    onTrocar = { onRecursos(recursos.copy(metaLocadora = it)) },
                )
                HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)
                RecursoToggleLinha(
                    icone = Icons.Filled.ShoppingCart,
                    nome = "Locação",
                    frase = "Acompanha a parcela semanal",
                    ligado = recursos.locacao,
                    onTrocar = { onRecursos(recursos.copy(locacao = it)) },
                )
                HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)
                RecursoToggleLinha(
                    icone = Icons.Filled.CheckCircle,
                    nome = "Finanças",
                    frase = "Mostra caixa e dinheiro livre",
                    ligado = recursos.financas,
                    onTrocar = { onRecursos(recursos.copy(financas = it)) },
                )
                HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)
                RecursoToggleLinha(
                    icone = Icons.Filled.Place,
                    nome = "Meta diária",
                    frase = "Define objetivo de corridas por dia",
                    ligado = recursos.metaDiaria,
                    onTrocar = { onRecursos(recursos.copy(metaDiaria = it)) },
                )
            }
        }
    }
}

@Composable
private fun RecursoToggleLinha(
    icone: ImageVector,
    nome: String,
    frase: String,
    ligado: Boolean,
    onTrocar: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = RoxouSpacing.sm, horizontal = RoxouSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = if (ligado) RoxouColors.PurpleSoft else RoxouColors.TextMuted,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nome,
                style = MaterialTheme.typography.titleMedium,
                color = RoxouColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = frase,
                style = MaterialTheme.typography.labelMedium,
                color = RoxouColors.TextSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(
            checked = ligado,
            onCheckedChange = onTrocar,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RoxouColors.Purple,
                uncheckedThumbColor = RoxouColors.TextSecondary,
                uncheckedTrackColor = RoxouColors.Track,
                uncheckedBorderColor = RoxouColors.CardBorder,
            ),
        )
    }
}

@Composable
private fun PassoLeitura() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RoxouSpacing.lg),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(RoxouColors.PurpleSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = RoxouColors.PurpleSoft,
                modifier = Modifier.size(30.dp),
            )
        }

        Text(
            text = "Ligar a leitura das ofertas",
            style = MaterialTheme.typography.titleLarge,
            color = RoxouColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )

        RoxouCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = RoxouSpacing.lg,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm)) {
                Text(
                    text = "O app só lê os valores na tela.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoxouColors.TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Nunca aceita, recusa ou salva fotos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoxouColors.TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(
    name = "Boas-vindas - Passo 1",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 760,
)
@Composable
private fun BoasVindasPasso1Preview() {
    RoxouDesignTheme {
        BoasVindasConteudo(passoInicial = 0, onConcluir = {}, onLigarLeitura = {})
    }
}

@Preview(
    name = "Boas-vindas - Passo 2",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 760,
)
@Composable
private fun BoasVindasPasso2Preview() {
    RoxouDesignTheme {
        BoasVindasConteudo(passoInicial = 1, onConcluir = {}, onLigarLeitura = {})
    }
}

@Preview(
    name = "Boas-vindas - Passo 3",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 760,
)
@Composable
private fun BoasVindasPasso3Preview() {
    RoxouDesignTheme {
        BoasVindasConteudo(passoInicial = 2, onConcluir = {}, onLigarLeitura = {})
    }
}
