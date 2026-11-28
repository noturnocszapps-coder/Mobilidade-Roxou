# Etapa 4 — Telas de conta (prompt curto para o AI Studio)

Cole o bloco abaixo no AI Studio. No commit, **não aceite "substituir"** pelos arquivos do GitHub.
A parte de licença, segurança e servidor fica com o Claude, direto no repositório.

```
Trabalhe só na pasta design/. Mude apenas design/Acesso.kt e crie design/Pagamento.kt.
Não mexa nos outros arquivos. Use RoxouColors, RoxouSpacing, RoxouCard e RoxouDesignTheme como já existem.
Todas as telas são só visuais: recebem dados prontos e devolvem ações por callbacks (sem rede, sem ViewModel).
Textos em português, curtos, números grandes, botões de 52 dp. Faça @Preview de cada estado.

1) LoginScreen (em Acesso.kt): troque senha por código no e-mail. Sem senha, sem "Criar conta", sem "Esqueci a senha".
   fun LoginScreen(etapa: EtapaLogin, email: String, carregando: Boolean, erro: String?,
                   segundosParaReenviar: Int, onEmail: (String) -> Unit, onPedirCodigo: () -> Unit,
                   onEntrar: (codigo: String) -> Unit, onReenviar: () -> Unit, onVoltar: () -> Unit)
   enum class EtapaLogin { EMAIL, CODIGO }
   - EMAIL: título "Entre para liberar o Pro", subtítulo "7 dias grátis, sem cartão.", campo de e-mail,
     botão "Receber código".
   - CODIGO: texto "Mandamos um código para {email}", 6 caixinhas de dígito (teclado numérico,
     aceita colar os 6 de uma vez, chama onEntrar sozinho ao completar), botão "Entrar",
     "Reenviar código em 0:{segundos}" e, quando chegar a 0, link "Reenviar código". Link "Trocar e-mail" chama onVoltar.

2) PlanosScreen: mantenha como está. Só troque o texto do botão: se diasDeTeste > 0 e o usuário ainda não
   testou, "Começar teste grátis"; senão "Assinar". Adicione no fim, pequeno: "Cancele quando quiser. Sem fidelidade."

3) Novo design/Pagamento.kt:
   enum class FormaPagamento { PIX, CARTAO }
   a) fun DadosPagamentoScreen(plano: PlanoUi, nome: String, cpf: String, forma: FormaPagamento,
        carregando: Boolean, erro: String?, onNome: (String) -> Unit, onCpf: (String) -> Unit,
        onForma: (FormaPagamento) -> Unit, onContinuar: () -> Unit, onVoltar: () -> Unit)
      Resumo do plano no topo (nome e valor grande), campos Nome completo e CPF (máscara 000.000.000-00,
      teclado numérico), escolha Pix (padrão) ou Cartão em dois botões grandes lado a lado,
      botão "Gerar Pix" ou "Pagar com cartão".
   b) fun PixScreen(valorCentavos: Long, qrCode: ImageBitmap?, copiaECola: String, aguardando: Boolean,
        onCopiar: () -> Unit, onJaPaguei: () -> Unit, onVoltar: () -> Unit)
      Valor bem grande, QR Code em fundo branco (240 dp), botão principal "Copiar código Pix"
      (depois de tocar, mostra "Copiado!" por 2 s), texto "Pague no app do seu banco. A liberação é automática.",
      um indicador "Esperando o pagamento..." quando aguardando = true, e botão secundário "Já paguei".
   c) fun ProLiberadoScreen(nomePlano: String, validoAte: String, onContinuar: () -> Unit)
      Ícone de check verde grande, "Pro liberado!", "Plano {nomePlano} até {validoAte}", botão "Começar".

4) Componente de cadeado (em Acesso.kt):
   fun ModuloBloqueadoCard(titulo: String, frase: String, onVerPlanos: () -> Unit)
   Cartão com ícone de cadeado roxo, título do módulo, frase curta (ex.: "Leitura automática das ofertas é do Pro")
   e botão "Ver planos". Também uma faixa:
   fun FaixaSituacaoPlano(situacao: SituacaoPlano, dias: Int)
   enum class SituacaoPlano { EM_DIA, TOLERANCIA, VENCIDO, GRATIS }
   EM_DIA: roxa "Pro · faltam {dias} dias"; TOLERANCIA: amarela "Pagamento pendente: o Pro continua por {dias} dias";
   VENCIDO: vermelha "Seu Pro venceu"; GRATIS: cinza "Plano grátis".

Não crie lógica de licença, login de verdade, rede ou cobrança. Só as telas.
```
