# Etapa 4 — Login, planos e liberação por módulos (prompt para o Google AI Studio)

**Como usar:** copie tudo que está dentro do bloco abaixo e cole no AI Studio, no projeto ligado ao repositório. No commit, **nunca aceite "substituir" pelos arquivos do GitHub**.

O servidor (API, banco e Asaas) já está pronto na pasta `server/` do repositório. Este prompt faz só a parte do app.

```
Você vai trabalhar no app Android "Roxou Mobilidade", que já existe na pasta android/ deste repositório.
Ele é Kotlin + Jetpack Compose, Gradle modular: app, core-domain (Kotlin puro), core-data, core-ui,
feature-trips, feature-goals, feature-rental, feature-finance e overlay (leitura das ofertas + janela flutuante).
NÃO apague, NÃO renomeie e NÃO reescreva arquivos que já existem, a não ser os poucos citados abaixo.
Mantenha o mesmo visual do app (arquivos em app/src/main/kotlin/br/com/roxou/mobilidade/design/ e core-ui/Theme.kt).

OBJETIVO
Criar login, a tela de planos e a liberação de cada função paga ("módulo") de acordo com o plano da pessoa.
O app continua funcionando 100% offline: a internet só é usada para entrar, assinar e renovar a licença.

REGRAS QUE NÃO PODEM MUDAR
- O app nunca aceita nem recusa corridas sozinho.
- O que é grátis continua grátis e nunca pede login: lançamento manual de corridas, metas, locação, caixa, finanças.
- Os dados do motorista nunca ficam presos: mesmo sem plano, ele vê e exporta tudo que já lançou.
- Nenhuma senha, chave do Asaas ou do banco dentro do APK.
- Textos todos em português do Brasil, simples e curtos.

1) NOVO MÓDULO GRADLE :core-licenca (Kotlin puro, plugin kotlin-jvm, igual ao core-domain)
   Pacote br.com.roxou.mobilidade.licenca. Sem dependências além de junit nos testes.
   a) enum class Modulo(val chave: String, val nome: String):
      LEITURA("leitura", "Leitura automática das ofertas e janela flutuante")
      BAIRROS("bairros", "Filtro de bairros")
      HISTORICO("historico", "Histórico completo")
      BACKUP("backup", "Backup na nuvem")
      RELATORIOS("relatorios", "Relatórios")
   b) data class Licenca(conta, email, aparelho, plano, modulos: Set<Modulo>, validoAteSeg: Long,
      toleranciaDias: Int, emitidoEmSeg: Long).
   c) object LicencaVerifier { fun verificar(token: String, chavePublicaBase64: String): Licenca? }
      FORMATO DO TOKEN (vem do servidor, não mude):
      token = base64url(texto) + "." + base64url(assinatura)   (base64url SEM padding "=")
      texto = linhas "chave=valor" separadas por "\n", por exemplo:
        v=1
        conta=6f1c...uuid
        email=fulano@gmail.com
        aparelho=a1b2c3
        plano=mensal            (pode ser: gratis, teste, cortesia, mensal, trimestral, anual)
        modulos=leitura,bairros,historico,backup,relatorios   (vazio no plano grátis)
        valido_ate=1767225600    (segundos desde 1970, UTC)
        tolerancia_dias=7
        emitido_em=1764260000
      assinatura = ECDSA P-256 com SHA-256, em DER. Verifique com java.security:
        KeyFactory.getInstance("EC") + X509EncodedKeySpec(Base64.decode(chavePublicaBase64))
        Signature.getInstance("SHA256withECDSA").
      Use java.util.Base64.getUrlDecoder() (aceita sem padding). Assinatura inválida, v diferente de 1
      ou campo faltando = retorna null. Módulo desconhecido na lista é ignorado (não quebra).
   d) object Acesso:
      fun agoraSeguro(agoraSeg: Long, maiorJaVistoSeg: Long) = max(agoraSeg, maiorJaVistoSeg)
        (proteção contra voltar o relógio do celular)
      fun liberado(lic: Licenca?, modulo: Modulo, agoraSeg: Long, aparelho: String): Boolean
        = lic != null && lic.aparelho == aparelho && modulo in lic.modulos
          && agoraSeg <= lic.validoAteSeg + lic.toleranciaDias * 86400
      fun situacao(lic, agoraSeg): enum EM_DIA / EM_TOLERANCIA (venceu mas ainda dentro da folga) / VENCIDA / SEM_LICENCA
      fun diasRestantes(lic, agoraSeg): Int
   e) Testes JUnit em core-licenca/src/test: gerar um par de chaves EC P-256 no próprio teste
      (KeyPairGenerator "EC" + ECGenParameterSpec("secp256r1")), assinar um texto, e testar:
      licença válida; texto alterado = null; aparelho diferente = bloqueado; vencida há 3 dias = liberada
      (tolerância 7); vencida há 8 dias = bloqueada; relógio voltado para trás usa o maior horário já visto.
   f) Adicione include(":core-licenca") no settings.gradle.kts e adicione :core-licenca:test no passo
      de testes do .github/workflows/build-apk.yml (junto do :core-domain:test).

2) EM core-data (Android)
   a) build.gradle.kts: api(project(":core-licenca")); buildFeatures { buildConfig = true } e dois campos:
        buildConfigField("String", "API_URL", "\"" + (System.getenv("ROXOU_API_URL") ?: "") + "\"")
        buildConfigField("String", "CHAVE_PUBLICA", "\"" + (System.getenv("ROXOU_LICENCA_CHAVE_PUBLICA") ?: "") + "\"")
      No .github/workflows/build-apk.yml, no passo "Gerar o APK debug", passe essas duas variáveis:
        env:
          ROXOU_API_URL: ${{ vars.ROXOU_API_URL }}
          ROXOU_LICENCA_CHAVE_PUBLICA: ${{ vars.ROXOU_LICENCA_CHAVE_PUBLICA }}
   b) class LicencaStore (singleton, LicencaStore.get(context)), em SharedPreferences "roxou_licenca":
      guarda token da licença, sessão, e-mail e o maior horário já visto.
      - aparelhoId = Settings.Secure.ANDROID_ID.
      - val estado: StateFlow<EstadoConta> (email, plano, modulos, validoAte, situacao, logado).
      - fun liberado(modulo: Modulo): Boolean — usa LicencaVerifier + Acesso, atualiza o "maior horário já visto".
      - MODO DESENVOLVEDOR: se BuildConfig.CHAVE_PUBLICA estiver vazia, liberado() devolve sempre true
        e a tela de conta mostra "Versão de teste: tudo liberado". Assim o APK atual do Fernando continua funcionando.
   c) class RoxouApi(baseUrl = BuildConfig.API_URL), usando só HttpURLConnection + org.json (sem bibliotecas novas),
      em Dispatchers.IO, timeout de 15 s. Erros do servidor vêm como {"erro": "mensagem"}: mostre essa mensagem.
      Rotas (todas JSON):
        GET  /v1/planos  -> { diasTeste, planos: [{ id, nome, descricao, preco_centavos, preco_primeira_centavos (pode ser null), ciclo ("MONTHLY"|"QUARTERLY"|"YEARLY"), modulos: [..] }] }
        POST /v1/auth/codigo  { email }  -> { ok }      (o servidor manda um código de 6 dígitos por e-mail)
        POST /v1/auth/entrar  { email, codigo, aparelho }  -> { sessao, email, licenca, plano, modulos, validoAte }
        GET  /v1/licenca   (header Authorization: Bearer <sessao>)  -> { email, licenca, plano, modulos, validoAte }
        POST /v1/assinar   (Bearer) { plano, forma: "PIX"|"CREDIT_CARD", nome, cpf }
             -> { link, valorCentavos, pix?: { copiaECola, imagemBase64 } }
        POST /v1/assinatura/cancelar (Bearer) -> { ok }
        POST /v1/sair (Bearer) -> { ok }
      Resposta 401 em /v1/licenca = sessão perdida: apaga a sessão (mantém a licença guardada até vencer).
   d) Renovação silenciosa: ao abrir o app (MainActivity) e ao conectar o serviço de leitura, se tiver sessão
      e a licença foi emitida há mais de 6 horas, chama GET /v1/licenca e salva. Sem internet, não faz nada e não mostra erro.
   e) Adicione <uses-permission android:name="android.permission.INTERNET" /> no AndroidManifest do app
      e atualize o comentário do topo (agora usa internet só para conta e licença).

3) TRAVAS NOS MÓDULOS (só estas, o resto do app não muda)
   - overlay/OfferReaderService: se !LicencaStore.get(this).liberado(Modulo.LEITURA), não lê ofertas e não
     mostra a janela. Mostre UMA vez por dia uma notificação simples (ou Toast): "A leitura automática é do plano
     Pro. Abra o Roxou > Conta para testar 7 dias grátis."
   - Filtro de bairros: se !liberado(Modulo.BAIRROS), avalie a oferta com rules.copy(blockedAreas = emptyList()).
   - Deixe uma função pronta para os módulos que ainda não existem (HISTORICO, BACKUP, RELATORIOS), sem travar nada hoje.

4) TELAS NOVAS (no módulo app, rota "conta", aberta por um cartão no topo da tela Config: "Minha conta e plano",
   mostrando o plano atual). Visual: clean, identidade roxa, pouca informação, números grandes, botões grandes,
   tema escuro igual ao resto do app. Pensado para usar com uma mão, no carro parado.
   a) ENTRAR (quando não está logado)
      - Título "Entre para liberar o Pro". Subtítulo: "7 dias grátis, sem cartão."
      - Campo de e-mail + botão "Receber código".
      - Depois: campo de 6 dígitos (teclado numérico, preenche sozinho se colar), botão "Entrar",
        link "Reenviar código" (libera depois de 60 s, com contador).
      - Sem senha. Mensagens de erro simples ("Código errado", "Sem internet. Tente de novo.").
   b) MEU PLANO (quando está logado)
      - Cartão grande no topo: nome do plano ("Teste grátis", "Grátis", "Mensal", "Trimestral", "Anual"),
        e em número grande "faltam X dias". Se estiver EM_TOLERANCIA: faixa amarela "Pagamento pendente:
        o Pro continua por mais X dias". Se VENCIDA: faixa vermelha "Seu Pro venceu".
      - Lista dos módulos com ✓ (liberado) ou cadeado (bloqueado), usando Modulo.nome.
      - Botão "Ver planos". Link pequeno "Sair desta conta". Se tiver assinatura, link "Cancelar assinatura"
        com confirmação ("Você continua com o Pro até o fim do período pago").
   c) PLANOS (dados vindos de GET /v1/planos; NUNCA escreva preço fixo no código)
      - Um cartão por plano, em coluna. Mostrar o preço por mês em número grande:
          MONTHLY: preco_centavos/100 por mês; se preco_primeira_centavos existir, mostrar
                   "1º mês R$ 5,99" em destaque e "depois R$ 9,90/mês" menor.
          QUARTERLY: (preco_centavos/3)/100 "por mês", e embaixo "R$ 23,97 a cada 3 meses".
          YEARLY: (preco_centavos/12)/100 "por mês", e embaixo "R$ 71,88 por ano".
        Formato de dinheiro brasileiro (R$ 5,99). Marque o Anual com selo "Mais barato".
      - Embaixo: "Cancele quando quiser. Sem fidelidade."
      - Tocar num plano abre PAGAR.
   d) PAGAR
      - Campos: nome completo e CPF (máscara 000.000.000-00, validar dígitos). Guardar para a próxima vez.
      - Escolha da forma: "Pix" (padrão) ou "Cartão".
      - Botão "Gerar Pix". Mostra o QR Code (imagemBase64 é um PNG), o valor grande, e botão grande
        "Copiar código Pix" (copia copiaECola, mostra "Copiado!"). Texto: "Pague no app do seu banco.
        A liberação é automática em até 1 minuto."
      - Cartão: abre o link no navegador (Intent ACTION_VIEW).
      - Botão "Já paguei": chama GET /v1/licenca; se o plano mudou, mostra "Pro liberado!" e volta para MEU PLANO.
        Enquanto a tela estiver aberta, confere sozinho a cada 10 s, no máximo por 10 minutos.

5) CONFIGURAÇÕES DO SERVIDOR NO APP
   Se BuildConfig.API_URL estiver vazia, a tela Conta mostra só: "Conta indisponível nesta versão." (sem travar nada).

6) NÃO FAÇA
   - Não use Firebase, Google Sign-In, Supabase, Retrofit ou bibliotecas novas.
   - Não mexa no avaliador de ofertas, nas metas, na locação, no OCR nem na janela flutuante além das travas do item 3.
   - Não crie cobrança pelo Google Play Billing (isso fica para quando o app for para a Play Store).

Ao terminar, liste os arquivos criados e alterados e confirme que os testes de :core-domain e :core-licenca passam.
```

## Depois que o AI Studio terminar

1. Na VPS, dentro da pasta `server/` do repositório: `npm install`, `cp .env.example .env` (preencher), `npm run banco`, `npm run gerar-chaves` e `npm start` (ou com PM2). O passo a passo completo está em `server/README.md`.
2. No GitHub, em Settings > Secrets and variables > Actions > **Variables**, criar:
   - `ROXOU_API_URL` = endereço da API (ex.: `https://mobilidade.seudominio.com.br`)
   - `ROXOU_LICENCA_CHAVE_PUBLICA` = a linha que o `npm run gerar-chaves` mostrou.
3. Rodar o "Gerar APK" de novo. Sem essas duas variáveis, o APK fica em modo de teste, com tudo liberado.
