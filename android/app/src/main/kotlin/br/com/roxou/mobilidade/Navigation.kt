package br.com.roxou.mobilidade

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.roxou.mobilidade.data.RoxouRepository
import br.com.roxou.mobilidade.design.Aba
import br.com.roxou.mobilidade.design.AjustesScreen
import br.com.roxou.mobilidade.design.AjustesUi
import br.com.roxou.mobilidade.design.AppShell
import br.com.roxou.mobilidade.design.BoasVindasScreen
import br.com.roxou.mobilidade.design.CorridaItem
import br.com.roxou.mobilidade.design.CorridasScreen
import br.com.roxou.mobilidade.design.ModuloUi
import br.com.roxou.mobilidade.design.NovaCorridaSheet
import br.com.roxou.mobilidade.design.PlanoUi
import br.com.roxou.mobilidade.design.PlanosScreen
import br.com.roxou.mobilidade.data.conta.LicencaStore
import br.com.roxou.mobilidade.data.conta.RoxouApi
import br.com.roxou.mobilidade.design.Recursos
import br.com.roxou.mobilidade.design.RoxouColors
import br.com.roxou.mobilidade.design.RoxouDesignTheme
import br.com.roxou.mobilidade.domain.goals.DailyGoalPlan
import br.com.roxou.mobilidade.domain.goals.DiscountTier
import br.com.roxou.mobilidade.domain.goals.TierTable
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.model.RecordSource
import br.com.roxou.mobilidade.domain.model.Trip
import br.com.roxou.mobilidade.domain.time.BRASILIA
import br.com.roxou.mobilidade.domain.time.Turnover
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

/** Recursos ligados e "já viu as boas-vindas", guardados só no celular. */
class AppPrefs(context: Context) {
    private val sp = context.getSharedPreferences("roxou_app", Context.MODE_PRIVATE)
    val recursos = MutableStateFlow(
        Recursos(
            metaLocadora = sp.getBoolean("r_meta_locadora", true),
            locacao = sp.getBoolean("r_locacao", true),
            financas = sp.getBoolean("r_financas", true),
            metaDiaria = sp.getBoolean("r_meta_diaria", true),
            janelaFlutuante = sp.getBoolean("r_janela", true),
        ),
    )
    val boasVindasFeitas = MutableStateFlow(sp.getBoolean("boas_vindas", false))

    fun setRecursos(r: Recursos) {
        recursos.value = r
        sp.edit().putBoolean("r_meta_locadora", r.metaLocadora).putBoolean("r_locacao", r.locacao)
            .putBoolean("r_financas", r.financas).putBoolean("r_meta_diaria", r.metaDiaria)
            .putBoolean("r_janela", r.janelaFlutuante).apply()
    }

    fun concluirBoasVindas() {
        boasVindasFeitas.value = true
        sp.edit().putBoolean("boas_vindas", true).apply()
    }
}

/**
 * Modo de teste: todos os módulos liberados, sem login.
 * Os planos aparecem para conferir o visual; a cobrança entra com o backend de assinatura.
 */
private val MODULOS = listOf(
    ModuloUi("janela", "Janela flutuante", "Veja se a corrida compensa", true),
    ModuloUi("metaLocadora", "Meta da locadora", "Conta corridas Uber para desconto", true),
    ModuloUi("locacao", "Locação", "Parcela semanal do carro", true),
    ModuloUi("financas", "Finanças", "Caixa livre da semana", true),
    ModuloUi("metaDiaria", "Meta diária", "Corridas por dia", true),
)
private val BENEFICIOS = listOf("Janela que avalia cada oferta", "Histórico Uber e 99", "Metas e locação", "Waze no embarque e destino")
private val PLANOS = listOf(
    PlanoUi("mensal", "Mensal", 990, 1, 599, "1º mês por R$ 5,99", null, BENEFICIOS),
    PlanoUi("trimestral", "Trimestral", 2397, 3, null, null, null, BENEFICIOS),
    PlanoUi("anual", "Anual", 7188, 12, null, null, "Melhor preço", BENEFICIOS),
)

private val horaFmt = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun RoxouNavHost(repo: RoxouRepository) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context.applicationContext) }
    val recursos by prefs.recursos.collectAsStateWithLifecycle()
    val boasVindas by prefs.boasVindasFeitas.collectAsStateWithLifecycle()
    val state by repo.state.collectAsStateWithLifecycle<br.com.roxou.mobilidade.data.AppState?>(initialValue = null)
    val scope = rememberCoroutineScope()
    var aba by rememberSaveable { mutableStateOf(Aba.INICIO) }
    var planos by rememberSaveable { mutableStateOf(false) }
    val licencas = remember { LicencaStore.get(context.applicationContext) }
    val conta by licencas.estado.collectAsStateWithLifecycle()
    var novaCorrida by rememberSaveable { mutableStateOf(false) }
    var leituraLigada by remember { mutableStateOf(isReaderOn(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) leituraLigada = isReaderOn(context) }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    val ligarLeitura = {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    RoxouDesignTheme {
        if (conta.testador && conta.situacao == br.com.roxou.mobilidade.licenca.Situacao.VENCIDA) {
            // APK de testador vencido: o app inteiro fica travado. Só um APK novo libera.
            Box(Modifier.fillMaxSize().background(RoxouColors.Background).safeDrawingPadding()) {
                TesteEncerradoScreen()
            }
            return@RoxouDesignTheme
        }
        if (!boasVindas) {
            // Margem das barras do sistema: o "Depois" ficava atrás da barra de navegação.
            Box(Modifier.fillMaxSize().background(RoxouColors.Background).safeDrawingPadding()) {
                BoasVindasScreen(
                    onConcluir = { prefs.setRecursos(it); prefs.concluirBoasVindas() },
                    onLigarLeitura = ligarLeitura,
                    leituraLigada = leituraLigada,
                )
            }
            return@RoxouDesignTheme
        }
        if (planos && conta.contaDisponivel) {
            // Com o servidor configurado: login por código, planos de verdade e Pix.
            Box(Modifier.fillMaxSize().background(RoxouColors.Background).safeDrawingPadding()) {
                ContaScreen(onFechar = { planos = false })
            }
            return@RoxouDesignTheme
        }
        if (planos) {
            Box(Modifier.fillMaxSize().background(RoxouColors.Background).safeDrawingPadding()) {
                PlanosScreen(
                    planos = PLANOS, diasDeTeste = 7, planoAtualId = null,
                    onEscolher = { toast("Pagamento entra em breve. Por enquanto está tudo liberado.") },
                    onFechar = { planos = false },
                )
            }
            return@RoxouDesignTheme
        }
        val mostrarCompensa = recursos.janelaFlutuante
        val abaVisivel = if (aba == Aba.COMPENSA && !mostrarCompensa) Aba.INICIO else aba
        AppShell(abaAtual = abaVisivel, mostrarCompensa = mostrarCompensa, onAba = { aba = it }) {
            when (abaVisivel) {
                Aba.INICIO -> DashboardScreen(repo, recursos)
                Aba.COMPENSA -> CompensaRoute(repo)
                Aba.CORRIDAS -> {
                    val st = state ?: return@AppShell
                    val turnover = Turnover(st.settings.turnover)
                    val hoje = st.snapshot.workday
                    val itens = st.records.trips
                        .filter { turnover.workday(it.endedAt) == hoje }
                        .sortedByDescending { it.endedAt }
                        .map { t ->
                            CorridaItem(
                                id = t.id,
                                app = if (t.platform == Platform.NINETY_NINE) "99" else if (t.platform == Platform.UBER) "Uber" else "Outro",
                                hora = horaFmt.format(t.endedAt.atZone(BRASILIA)),
                                valorCentavos = t.amountCents,
                                km = t.totalKm,
                                confirmada = t.confirmed,
                                origem = when {
                                    t.source == RecordSource.MANUAL -> "Manual"
                                    t.notes?.contains("janela") == true -> "Botão Aceitei"
                                    else -> "Lida pela tela"
                                },
                            )
                        }
                    CorridasScreen(
                        itens = itens,
                        onAdicionar = { novaCorrida = true },
                        onConfirmar = { id -> scope.launch { repo.setTripConfirmed(id, true) } },
                        onApagar = { id -> scope.launch { repo.deleteTrip(id) } },
                    )
                }
                Aba.AJUSTES -> {
                    val st = state ?: return@AppShell
                    val s = st.settings
                    AjustesScreen(
                        ui = AjustesUi(
                            recursos = recursos,
                            leituraLigada = leituraLigada,
                            locadoraNome = s.rental.companyName,
                            parcelaSemanalCentavos = s.rental.weeklyInstallmentCents,
                            faixas = s.rental.tiers.sorted.map { it.minRides to it.discountCents },
                            metaPorDia = DayOfWeek.entries.map { s.goals.target(it) },
                            codigosParaTraduzir = s.offer.areaCodes.filterValues { it.isBlank() }.keys.toList(),
                            codigos = s.offer.areaCodes.map { it.key to it.value },
                            modulos = MODULOS,
                            planoAtual = resumo(conta),
                            emailConta = conta.email,
                        ),
                        onRecursos = prefs::setRecursos,
                        onLigarLeitura = ligarLeitura,
                        onSalvarLocadora = { nome, parcela, faixas ->
                            scope.launch {
                                runCatching {
                                    val table = TierTable(faixas.filter { it.first > 0 }.map { DiscountTier(it.first, it.second.coerceAtLeast(0)) })
                                    repo.updateSettings {
                                        it.copy(rental = it.rental.copy(companyName = nome.ifBlank { "Locadora" }, weeklyInstallmentCents = parcela.coerceAtLeast(0), tiers = table))
                                    }
                                }.onFailure { toast("Faixas inválidas (corridas repetidas?)") }
                            }
                        },
                        onSalvarMetas = { metas ->
                            scope.launch {
                                val plan = DailyGoalPlan(DayOfWeek.entries.mapIndexed { i, d -> d to (metas.getOrNull(i) ?: 0).coerceAtLeast(0) }.toMap())
                                repo.updateSettings { it.copy(goals = plan) }
                            }
                        },
                        onSalvarCodigos = { codigos ->
                            scope.launch {
                                val map = LinkedHashMap<String, String>()
                                codigos.filter { it.first.isNotBlank() }.forEach { map[it.first.trim()] = it.second.trim() }
                                repo.updateSettings { it.copy(offer = it.offer.copy(areaCodes = map)) }
                            }
                        },
                        onDesbloquear = { planos = true },
                        onVerPlanos = { planos = true },
                        onSair = {
                            if (conta.logado) scope.launch { RoxouApi(licencas).sair() }
                            else toast("Você não entrou em nenhuma conta.")
                        },
                    )
                }
            }
        }
        if (novaCorrida) {
            NovaCorridaSheet(
                onSalvar = { n ->
                    scope.launch {
                        repo.saveTrip(
                            Trip(
                                id = repo.newId(),
                                platform = if (n.app.trim() == "99") Platform.NINETY_NINE else Platform.UBER,
                                endedAt = repo.now(),
                                amountCents = n.valorCentavos,
                                tripKm = n.km,
                                source = RecordSource.MANUAL,
                                confirmed = true,
                            ),
                        )
                    }
                    novaCorrida = false
                },
                onFechar = { novaCorrida = false },
            )
        }
    }
}
