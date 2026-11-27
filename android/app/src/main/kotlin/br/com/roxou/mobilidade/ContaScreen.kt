package br.com.roxou.mobilidade

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.roxou.mobilidade.data.conta.Cobranca
import br.com.roxou.mobilidade.data.conta.EstadoConta
import br.com.roxou.mobilidade.data.conta.LicencaStore
import br.com.roxou.mobilidade.data.conta.PlanoApi
import br.com.roxou.mobilidade.data.conta.RoxouApi
import br.com.roxou.mobilidade.licenca.Modulo
import br.com.roxou.mobilidade.licenca.Situacao
import br.com.roxou.mobilidade.ui.SectionCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Tela funcional de conta e planos. O visual final vem do AI Studio (design/Acesso.kt e design/Pagamento.kt).

internal enum class Passo { INICIO, CODIGO, PLANOS, DADOS, PIX }

internal class ContaViewModel(context: Context) : ViewModel() {
    val store = LicencaStore.get(context)
    private val api = RoxouApi(store)
    val estado = store.estado

    var passo by mutableStateOf(Passo.INICIO); private set
    var email by mutableStateOf(""); private set
    var carregando by mutableStateOf(false); private set
    var erro by mutableStateOf<String?>(null); private set
    var planos by mutableStateOf<List<PlanoApi>>(emptyList()); private set
    var diasTeste by mutableStateOf(0); private set
    var escolhido by mutableStateOf<PlanoApi?>(null); private set
    var cobranca by mutableStateOf<Cobranca?>(null); private set

    private fun rodar(acao: suspend () -> Unit) = viewModelScope.launch {
        carregando = true
        erro = null
        try { acao() } catch (e: Exception) { erro = e.message ?: "Erro. Tente de novo." }
        carregando = false
    }

    fun mudarEmail(v: String) { email = v }
    fun pedirCodigo() = rodar { api.pedirCodigo(email); passo = Passo.CODIGO }
    fun entrar(codigo: String) = rodar { api.entrar(email, codigo); passo = Passo.INICIO }
    fun voltar() { erro = null; passo = if (passo == Passo.DADOS) Passo.PLANOS else Passo.INICIO }
    fun verPlanos() = rodar {
        val (dias, lista) = api.planos()
        diasTeste = dias
        planos = lista
        passo = Passo.PLANOS
    }
    fun escolher(p: PlanoApi) { escolhido = p; passo = Passo.DADOS }
    fun pagar(nome: String, cpf: String, pix: Boolean) = rodar {
        val c = api.assinar(escolhido!!.id, pix, nome, cpf)
        cobranca = c
        passo = Passo.PIX
        if (!pix) c.link?.let { abrirLink = it }
        esperarPagamento()
    }
    var abrirLink by mutableStateOf<String?>(null)

    /** Confere sozinho a cada 10 s, por até 10 minutos, se o pagamento caiu. */
    private fun esperarPagamento() = viewModelScope.launch {
        val antes = store.estado.value.plano
        repeat(60) {
            delay(10_000)
            if (passo != Passo.PIX) return@launch
            if (api.renovarLicenca() && store.estado.value.plano != antes) { passo = Passo.INICIO; return@launch }
        }
    }
    fun jaPaguei() = rodar { api.renovarLicenca(); if (store.estado.value.plano != "gratis") passo = Passo.INICIO }
    fun cancelar() = rodar { api.cancelar(); api.renovarLicenca() }
    fun sair() = rodar { api.sair() }
    fun renovar() = viewModelScope.launch { api.renovarSePreciso() }
}

private fun reais(c: Long) = "R$ " + String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", c / 100.0)

private fun nomePlano(p: String) = when (p) {
    "gratis" -> "Grátis"; "teste" -> "Teste grátis"; "cortesia" -> "Cortesia"
    else -> p.replaceFirstChar { it.uppercase() }
}

/** Cartão no topo dos Ajustes. */
@Composable
fun ContaCard(onAbrir: () -> Unit) {
    val ctx = LocalContext.current
    val e by LicencaStore.get(ctx).estado.collectAsStateWithLifecycle()
    SectionCard("Minha conta e plano") {
        Text(resumo(e), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onAbrir, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(if (e.logado) "Ver meu plano" else "Entrar e testar o Pro grátis")
        }
    }
}

internal fun resumo(e: EstadoConta) = when {
    e.modoTeste -> "Versão de teste: tudo liberado"
    !e.contaDisponivel -> "Conta indisponível nesta versão"
    e.situacao == Situacao.EM_DIA && e.plano != "gratis" -> "${nomePlano(e.plano)} · faltam ${e.diasRestantes} dias"
    e.situacao == Situacao.EM_TOLERANCIA -> "Pagamento pendente: o Pro continua por ${e.diasRestantes} dias"
    e.situacao == Situacao.VENCIDA -> "Seu Pro venceu"
    else -> "Plano grátis"
}

@Composable
fun ContaScreen(onFechar: () -> Unit) {
    val ctx = LocalContext.current
    val vm: ContaViewModel = viewModel { ContaViewModel(ctx.applicationContext) }
    val e by vm.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.renovar() }
    vm.abrirLink?.let { link ->
        LaunchedEffect(link) {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            vm.abrirLink = null
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = if (vm.passo == Passo.INICIO) onFechar else vm::voltar) { Text("← Voltar") }
        vm.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when {
            !e.contaDisponivel -> Text("Conta indisponível nesta versão.")
            vm.passo == Passo.CODIGO -> Codigo(vm)
            vm.passo == Passo.PLANOS -> Planos(vm, e)
            vm.passo == Passo.DADOS -> Dados(vm)
            vm.passo == Passo.PIX -> Pix(vm, ctx)
            !e.logado -> Entrar(vm)
            else -> MeuPlano(vm, e)
        }
    }
}

@Composable
private fun Entrar(vm: ContaViewModel) {
    Text("Entre para liberar o Pro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("7 dias grátis, sem cartão.")
    OutlinedTextField(vm.email, vm::mudarEmail, label = { Text("E-mail") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
    Button(onClick = vm::pedirCodigo, enabled = !vm.carregando && "@" in vm.email,
        modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Receber código") }
}

@Composable
private fun Codigo(vm: ContaViewModel) {
    var codigo by remember { mutableStateOf("") }
    var espera by remember { mutableStateOf(60) }
    LaunchedEffect(espera) { if (espera > 0) { delay(1_000); espera-- } }
    Text("Mandamos um código para ${vm.email}")
    OutlinedTextField(codigo, { v ->
        codigo = v.filter(Char::isDigit).take(6)
        if (codigo.length == 6 && !vm.carregando) vm.entrar(codigo)
    }, label = { Text("Código de 6 dígitos") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
    Button(onClick = { vm.entrar(codigo) }, enabled = !vm.carregando && codigo.length == 6,
        modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Entrar") }
    if (espera > 0) Text("Reenviar código em 0:%02d".format(espera))
    else TextButton(onClick = { espera = 60; vm.pedirCodigo() }) { Text("Reenviar código") }
}

@Composable
private fun MeuPlano(vm: ContaViewModel, e: EstadoConta) {
    Text(nomePlano(e.plano), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text(resumo(e), fontSize = 20.sp)
    e.email?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    Modulo.entries.forEach { m -> Text((if (m in e.modulos) "✓  " else "🔒  ") + m.nome) }
    Button(onClick = vm::verPlanos, enabled = !vm.carregando, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Ver planos") }
    var confirmar by remember { mutableStateOf(false) }
    if (e.plano !in setOf("gratis", "teste", "cortesia")) {
        if (!confirmar) TextButton(onClick = { confirmar = true }) { Text("Cancelar assinatura") }
        else OutlinedButton(onClick = { confirmar = false; vm.cancelar() }, modifier = Modifier.fillMaxWidth()) {
            Text("Confirmar: você continua com o Pro até o fim do período pago")
        }
    }
    TextButton(onClick = vm::sair) { Text("Sair desta conta") }
}

@Composable
private fun Planos(vm: ContaViewModel, e: EstadoConta) {
    Text("Planos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    if (vm.diasTeste > 0 && !e.logado) Text("${vm.diasTeste} dias grátis para testar")
    vm.planos.forEach { p ->
        SectionCard(p.nome + if (p.meses == 12) "  ·  Mais barato" else "") {
            Text("${reais(p.precoCentavos / p.meses)}/mês", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            p.precoPrimeiraCentavos?.let { Text("1º mês ${reais(it)}, depois ${reais(p.precoCentavos)}/mês") }
            if (p.meses > 1) Text("${reais(p.precoCentavos)} " + if (p.meses == 12) "por ano" else "a cada ${p.meses} meses")
            Button(onClick = { vm.escolher(p) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Assinar") }
        }
    }
    Text("Cancele quando quiser. Sem fidelidade.", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun Dados(vm: ContaViewModel) {
    val p = vm.escolhido ?: return
    var nome by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var pix by remember { mutableStateOf(true) }
    Text(p.nome, style = MaterialTheme.typography.headlineSmall)
    Text(reais(p.precoPrimeiraCentavos ?: p.precoCentavos), fontSize = 32.sp, fontWeight = FontWeight.Bold)
    OutlinedTextField(nome, { nome = it }, label = { Text("Nome completo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(cpf, { cpf = it.filter(Char::isDigit).take(11) }, label = { Text("CPF (só números)") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = pix, onClick = { pix = true }, label = { Text("Pix") })
        FilterChip(selected = !pix, onClick = { pix = false }, label = { Text("Cartão") })
    }
    Button(onClick = { vm.pagar(nome, cpf, pix) }, enabled = !vm.carregando && nome.length >= 3 && cpf.length == 11,
        modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(if (pix) "Gerar Pix" else "Pagar com cartão") }
}

@Composable
private fun Pix(vm: ContaViewModel, ctx: Context) {
    val c = vm.cobranca ?: return
    Text(reais(c.valorCentavos), fontSize = 36.sp, fontWeight = FontWeight.Bold)
    val qr = remember(c.pixImagemBase64) {
        c.pixImagemBase64?.let { runCatching { Base64.decode(it, Base64.DEFAULT) }.getOrNull() }
            ?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    qr?.let { Image(it, contentDescription = "QR Code do Pix", modifier = Modifier.size(240.dp).background(Color.White)) }
    c.pixCopiaECola?.let { codigo ->
        var copiado by remember { mutableStateOf(false) }
        LaunchedEffect(copiado) { if (copiado) { delay(2_000); copiado = false } }
        Button(onClick = {
            (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Pix", codigo))
            copiado = true
        }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(if (copiado) "Copiado!" else "Copiar código Pix") }
        Text("Pague no app do seu banco. A liberação é automática.")
    }
    Text("Esperando o pagamento...", style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = vm::jaPaguei, enabled = !vm.carregando, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Já paguei") }
}
