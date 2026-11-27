package br.com.roxou.mobilidade.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PlanoUi(
    val id: String,
    val nome: String,
    val precoTotalCentavos: Long,
    val meses: Int,
    val precoPromocionalCentavos: Long?,
    val textoPromocao: String?,
    val selo: String?,
    val beneficios: List<String>,
)

data class ModuloUi(
    val id: String,
    val nome: String,
    val frase: String,
    val liberado: Boolean,
)

@Composable
fun LoginScreen(
    carregando: Boolean,
    erro: String?,
    onEntrar: (email: String, senha: String) -> Unit,
    onCriarConta: () -> Unit,
    onEsqueciSenha: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RoxouColors.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = RoxouSpacing.xl, vertical = RoxouSpacing.xxl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(RoxouColors.Purple),
                )
                Text(
                    text = "ROXOU",
                    style = MaterialTheme.typography.headlineLarge,
                    letterSpacing = 3.sp,
                    color = RoxouColors.TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            Spacer(modifier = Modifier.height(RoxouSpacing.xxl))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = {
                    Text(
                        text = "E-mail",
                        maxLines = 1,
                        softWrap = false,
                    )
                },
                singleLine = true,
                enabled = !carregando,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoxouColors.Purple,
                    unfocusedBorderColor = RoxouColors.CardBorder,
                    focusedContainerColor = RoxouColors.SurfaceCard,
                    unfocusedContainerColor = RoxouColors.SurfaceCard,
                    focusedTextColor = RoxouColors.TextPrimary,
                    unfocusedTextColor = RoxouColors.TextPrimary,
                    focusedLabelColor = RoxouColors.PurpleSoft,
                    unfocusedLabelColor = RoxouColors.TextSecondary,
                    cursorColor = RoxouColors.Purple,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(RoxouSpacing.md))

            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = {
                    Text(
                        text = "Senha",
                        maxLines = 1,
                        softWrap = false,
                    )
                },
                singleLine = true,
                enabled = !carregando,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoxouColors.Purple,
                    unfocusedBorderColor = RoxouColors.CardBorder,
                    focusedContainerColor = RoxouColors.SurfaceCard,
                    unfocusedContainerColor = RoxouColors.SurfaceCard,
                    focusedTextColor = RoxouColors.TextPrimary,
                    unfocusedTextColor = RoxouColors.TextPrimary,
                    focusedLabelColor = RoxouColors.PurpleSoft,
                    unfocusedLabelColor = RoxouColors.TextSecondary,
                    cursorColor = RoxouColors.Purple,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (!erro.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(RoxouSpacing.md))
                Text(
                    text = erro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoxouColors.Red,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(RoxouSpacing.xl))

            Button(
                onClick = { onEntrar(email.trim(), senha) },
                enabled = !carregando && email.isNotBlank() && senha.isNotBlank(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoxouColors.Purple,
                    contentColor = Color.White,
                    disabledContainerColor = RoxouColors.Track,
                    disabledContentColor = RoxouColors.TextMuted,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (carregando) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Text(
                        text = "Entrar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            Spacer(modifier = Modifier.height(RoxouSpacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onCriarConta,
                    enabled = !carregando,
                ) {
                    Text(
                        text = "Criar conta",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoxouColors.PurpleSoft,
                        maxLines = 1,
                        softWrap = false,
                    )
                }

                TextButton(
                    onClick = onEsqueciSenha,
                    enabled = !carregando,
                ) {
                    Text(
                        text = "Esqueci a senha",
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

@Composable
fun PlanosScreen(
    planos: List<PlanoUi>,
    diasDeTeste: Int,
    planoAtualId: String?,
    onEscolher: (String) -> Unit,
    onFechar: () -> Unit,
) {
    val inicialId = remember(planos) {
        planos.firstOrNull { it.id.equals("anual", ignoreCase = true) }?.id
            ?: planos.firstOrNull()?.id
            ?: ""
    }
    var selecionadoId by remember(planos) { mutableStateOf(inicialId) }

    val planoSelecionado = planos.firstOrNull { it.id == selecionadoId } ?: planos.firstOrNull()
    val beneficiosLista = planoSelecionado?.beneficios ?: emptyList()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RoxouColors.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RoxouSpacing.lg, vertical = RoxouSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RoxouSpacing.lg),
        ) {
            // Topo com fechar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Planos",
                    style = MaterialTheme.typography.headlineLarge,
                    color = RoxouColors.TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                )

                IconButton(
                    onClick = onFechar,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Fechar",
                        tint = RoxouColors.TextSecondary,
                    )
                }
            }

            if (planos.isEmpty()) {
                RoxouEstadoCarregando(mensagem = "Carregando planos...")
            } else {
                // Faixa no topo: dias grátis
                if (diasDeTeste > 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = RoxouColors.PurpleSurface,
                        border = BorderStroke(1.dp, RoxouColors.Purple.copy(alpha = 0.45f)),
                    ) {
                        Text(
                            text = "$diasDeTeste dias grátis para testar",
                            style = MaterialTheme.typography.titleMedium,
                            color = RoxouColors.PurpleSoft,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = RoxouSpacing.lg, vertical = RoxouSpacing.md),
                        )
                    }
                }

                // Cartões empilhados dos planos
                Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.md)) {
                    planos.forEach { plano ->
                        PlanoCard(
                            plano = plano,
                            selecionado = plano.id == selecionadoId,
                            atual = plano.id == planoAtualId,
                            onClick = { selecionadoId = plano.id },
                        )
                    }
                }

                // Lista curta de benefícios
                if (beneficiosLista.isNotEmpty()) {
                    RoxouCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = RoxouSpacing.lg,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm)) {
                            beneficiosLista.forEach { beneficio ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = RoxouColors.Green,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = beneficio,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = RoxouColors.TextPrimary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                val jaEhAtual = selecionadoId == planoAtualId
                Button(
                    onClick = {
                        if (selecionadoId.isNotEmpty()) {
                            onEscolher(selecionadoId)
                        }
                    },
                    enabled = selecionadoId.isNotEmpty() && !jaEhAtual,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoxouColors.Purple,
                        contentColor = Color.White,
                        disabledContainerColor = RoxouColors.Track,
                        disabledContentColor = RoxouColors.TextMuted,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(
                        text = when {
                            jaEhAtual -> "Seu plano atual"
                            diasDeTeste > 0 -> "Começar teste grátis"
                            else -> "Escolher plano"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanoCard(
    plano: PlanoUi,
    selecionado: Boolean,
    atual: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selecionado) RoxouColors.Purple else RoxouColors.CardBorder,
        label = "plano_borda",
    )
    val bgColor by animateColorAsState(
        targetValue = if (selecionado) RoxouColors.SurfaceElevated else RoxouColors.SurfaceCard,
        label = "plano_bg",
    )

    val mesesSeguro = plano.meses.coerceAtLeast(1)
    val precoPorMesCentavos = plano.precoTotalCentavos / mesesSeguro
    val periodoTexto = when (mesesSeguro) {
        1 -> "por mês"
        3 -> "a cada 3 meses"
        12 -> "por ano"
        else -> "a cada $mesesSeguro meses"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = bgColor,
        border = BorderStroke(if (selecionado) 2.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RoxouSpacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = plano.nome,
                    style = MaterialTheme.typography.titleMedium,
                    color = RoxouColors.TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.xs)) {
                    if (atual) {
                        PlanoSelo(
                            texto = "Seu plano",
                            corFundo = RoxouColors.Green.copy(alpha = 0.16f),
                            corTexto = RoxouColors.Green,
                        )
                    } else if (!plano.selo.isNullOrBlank()) {
                        PlanoSelo(
                            texto = plano.selo,
                            corFundo = RoxouColors.Purple,
                            corTexto = Color.White,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RoxouSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${formatarReaisPreciso(precoPorMesCentavos)}/mês",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp),
                        color = RoxouColors.TextPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${formatarReaisPreciso(plano.precoTotalCentavos)} $periodoTexto",
                        style = MaterialTheme.typography.labelMedium,
                        color = RoxouColors.TextSecondary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (!plano.textoPromocao.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(RoxouSpacing.sm))
                    PlanoSelo(
                        texto = plano.textoPromocao,
                        corFundo = RoxouColors.PurpleSurface,
                        corTexto = RoxouColors.PurpleSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanoSelo(
    texto: String,
    corFundo: Color,
    corTexto: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(corFundo)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = corTexto,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(
    name = "Login",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 760,
)
@Composable
private fun LoginScreenPreview() {
    RoxouDesignTheme {
        LoginScreen(
            carregando = false,
            erro = null,
            onEntrar = { _, _ -> },
            onCriarConta = {},
            onEsqueciSenha = {},
        )
    }
}

@Preview(
    name = "Login - Erro",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 760,
)
@Composable
private fun LoginScreenErroPreview() {
    RoxouDesignTheme {
        LoginScreen(
            carregando = false,
            erro = "E-mail ou senha incorretos",
            onEntrar = { _, _ -> },
            onCriarConta = {},
            onEsqueciSenha = {},
        )
    }
}

@Preview(
    name = "Planos",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 820,
)
@Composable
private fun PlanosScreenPreview() {
    val beneficios = listOf(
        "Avaliação na tela da Uber e 99",
        "Meta da locadora e faixas de desconto",
        "Controle de parcela e dinheiro livre",
    )
    RoxouDesignTheme {
        PlanosScreen(
            planos = listOf(
                PlanoUi(
                    id = "mensal",
                    nome = "Mensal",
                    precoTotalCentavos = 990L,
                    meses = 1,
                    precoPromocionalCentavos = 599L,
                    textoPromocao = "1º mês por R$ 5,99",
                    selo = null,
                    beneficios = beneficios,
                ),
                PlanoUi(
                    id = "trimestral",
                    nome = "Trimestral",
                    precoTotalCentavos = 2397L,
                    meses = 3,
                    precoPromocionalCentavos = null,
                    textoPromocao = null,
                    selo = null,
                    beneficios = beneficios,
                ),
                PlanoUi(
                    id = "anual",
                    nome = "Anual",
                    precoTotalCentavos = 7188L,
                    meses = 12,
                    precoPromocionalCentavos = null,
                    textoPromocao = null,
                    selo = "Melhor preço",
                    beneficios = beneficios,
                ),
            ),
            diasDeTeste = 7,
            planoAtualId = null,
            onEscolher = {},
            onFechar = {},
        )
    }
}
