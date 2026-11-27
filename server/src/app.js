// Rotas da API do Roxou Mobilidade.
// O app só fala com esta API. Nenhuma senha de banco ou chave do Asaas vai no APK.
import crypto from 'node:crypto';
import express from 'express';
import { decidirAcesso, emitirLicenca } from './licenca.js';

const MODULOS_PRO = ['leitura', 'bairros', 'historico', 'backup', 'relatorios'];
const INTERVALO = { MONTHLY: '1 month', QUARTERLY: '3 months', YEARLY: '1 year' };
const hash = (t) => crypto.createHash('sha256').update(t).digest('hex');
const emailValido = (e) => typeof e === 'string' && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e) && e.length <= 200;
const soDigitos = (t) => String(t ?? '').replace(/\D/g, '');
const reais = (centavos) => Math.round(centavos) / 100;

class Erro extends Error {
  constructor(status, mensagem) { super(mensagem); this.status = status; }
}

/** Monta a API. `db` é um pg.Pool (ou algo com .query). */
export function criarApp({ db, asaas, email, chavePrivadaPem, webhookToken, agora = () => new Date() }) {
  const app = express();
  app.use(express.json({ limit: '100kb' }));
  const q = async (sql, params) => (await db.query(sql, params)).rows;

  async function config() {
    const linhas = await q('SELECT chave, valor FROM mobilidade.config');
    const c = Object.fromEntries(linhas.map((l) => [l.chave, Number(l.valor)]));
    return {
      diasTeste: c.dias_teste ?? 7,
      toleranciaDias: c.tolerancia_dias ?? 7,
      maxAparelhos: c.max_aparelhos ?? 2,
      validadeGratisDias: c.validade_licenca_gratis_dias ?? 30,
      modulosPro: MODULOS_PRO,
    };
  }

  const rota = (fn) => (req, res) => fn(req, res).catch((e) => {
    if (!e.status) console.error(e);
    res.status(e.status || 500).json({ erro: e.status ? e.message : 'Erro no servidor. Tente de novo.' });
  });

  async function autenticar(req) {
    const token = (req.get('authorization') || '').replace(/^Bearer\s+/i, '');
    if (!token) throw new Erro(401, 'Entre na sua conta de novo.');
    const [s] = await q(
      `UPDATE mobilidade.sessoes SET ultimo_uso = now() WHERE token_hash = $1
       RETURNING conta_id, aparelho_id`, [hash(token)]);
    if (!s) throw new Erro(401, 'Entre na sua conta de novo.');
    const [conta] = await q('SELECT * FROM mobilidade.contas WHERE id = $1', [s.conta_id]);
    return { conta, aparelhoId: s.aparelho_id, tokenHash: hash(token) };
  }

  /** Começa o teste grátis na primeira vez (uma vez por conta e por aparelho). */
  async function talvezComecarTeste(conta, aparelhoId, cfg) {
    if (conta.teste_ate || cfg.diasTeste <= 0) return conta;
    const novo = await q(
      'INSERT INTO mobilidade.aparelhos_teste (aparelho_id) VALUES ($1) ON CONFLICT DO NOTHING RETURNING aparelho_id',
      [aparelhoId]);
    // Aparelho que já fez teste em outra conta: marca a conta como "teste usado" (data no passado).
    const ate = novo.length ? new Date(agora().getTime() + cfg.diasTeste * 86400_000) : new Date(0);
    const [c] = await q('UPDATE mobilidade.contas SET teste_ate = $2 WHERE id = $1 RETURNING *', [conta.id, ate]);
    return c;
  }

  async function licencaDa(conta, aparelhoId) {
    const cfg = await config();
    conta = await talvezComecarTeste(conta, aparelhoId, cfg);
    const [assinatura] = await q(
      `SELECT a.plano_id, a.pago_ate, p.modulos FROM mobilidade.assinaturas a
       JOIN mobilidade.planos p ON p.id = a.plano_id
       WHERE a.conta_id = $1 AND a.pago_ate IS NOT NULL
       ORDER BY a.pago_ate DESC LIMIT 1`, [conta.id]);
    const acesso = decidirAcesso({ agora: agora(), assinatura, conta, config: cfg });
    return {
      licenca: emitirLicenca({ agora: agora(), conta, aparelhoId, acesso, toleranciaDias: cfg.toleranciaDias, chavePrivadaPem }),
      plano: acesso.plano,
      modulos: acesso.modulos,
      validoAte: acesso.validoAte.toISOString(),
    };
  }

  app.get('/v1/saude', (_req, res) => res.json({ ok: true }));

  app.get('/v1/planos', rota(async (_req, res) => {
    const planos = await q(
      `SELECT p.id, p.nome, p.descricao, p.preco_centavos, p.preco_primeira_centavos, p.ciclo, p.modulos, p.vagas,
              (SELECT count(*)::int FROM mobilidade.assinaturas a
                 WHERE a.plano_id = p.id AND a.status = 'ativa') AS assinantes
       FROM mobilidade.planos p WHERE p.ativo ORDER BY p.ordem`);
    const cfg = await config();
    res.json({
      diasTeste: cfg.diasTeste,
      planos: planos
        .filter((p) => p.vagas == null || p.assinantes < p.vagas)
        .map(({ assinantes, vagas, ...p }) => p),
    });
  }));

  app.post('/v1/auth/codigo', rota(async (req, res) => {
    const emailLimpo = String(req.body?.email || '').trim().toLowerCase();
    if (!emailValido(emailLimpo)) throw new Erro(400, 'E-mail inválido.');
    const codigo = String(crypto.randomInt(0, 1_000_000)).padStart(6, '0');
    // No máximo 5 códigos por hora para o mesmo e-mail.
    const [linha] = await q(
      `INSERT INTO mobilidade.codigos_login (email, codigo_hash, expira_em)
       VALUES ($1, $2, now() + interval '10 minutes')
       ON CONFLICT (email) DO UPDATE SET
         codigo_hash = EXCLUDED.codigo_hash, expira_em = EXCLUDED.expira_em, tentativas = 0,
         enviados_na_hora = CASE WHEN codigos_login.hora_inicio < now() - interval '1 hour' THEN 1
                                 ELSE codigos_login.enviados_na_hora + 1 END,
         hora_inicio = CASE WHEN codigos_login.hora_inicio < now() - interval '1 hour' THEN now()
                            ELSE codigos_login.hora_inicio END
       RETURNING enviados_na_hora`, [emailLimpo, hash(emailLimpo + codigo)]);
    if (linha.enviados_na_hora > 5) throw new Erro(429, 'Muitos códigos pedidos. Espere uma hora.');
    await email.enviarCodigo(emailLimpo, codigo);
    res.json({ ok: true });
  }));

  app.post('/v1/auth/entrar', rota(async (req, res) => {
    const emailLimpo = String(req.body?.email || '').trim().toLowerCase();
    const codigo = soDigitos(req.body?.codigo);
    const aparelhoId = String(req.body?.aparelho || '').slice(0, 100);
    if (!emailValido(emailLimpo) || codigo.length !== 6 || !aparelhoId) throw new Erro(400, 'Dados incompletos.');

    const [c] = await q(
      `UPDATE mobilidade.codigos_login SET tentativas = tentativas + 1
       WHERE email = $1 RETURNING codigo_hash, expira_em, tentativas`, [emailLimpo]);
    if (!c || new Date(c.expira_em) < agora() || c.tentativas > 5) throw new Erro(400, 'Código vencido. Peça outro.');
    const ok = crypto.timingSafeEqual(Buffer.from(c.codigo_hash), Buffer.from(hash(emailLimpo + codigo)));
    if (!ok) throw new Erro(400, 'Código errado.');
    await q('DELETE FROM mobilidade.codigos_login WHERE email = $1', [emailLimpo]);

    const [conta] = await q(
      `INSERT INTO mobilidade.contas (email) VALUES ($1)
       ON CONFLICT (email) DO UPDATE SET email = EXCLUDED.email RETURNING *`, [emailLimpo]);

    // Limite de aparelhos: entrar num celular novo tira o mais antigo.
    const cfg = await config();
    await q(
      `INSERT INTO mobilidade.aparelhos (conta_id, aparelho_id) VALUES ($1, $2)
       ON CONFLICT (conta_id, aparelho_id) DO UPDATE SET ultimo_uso = now()`, [conta.id, aparelhoId]);
    const antigos = await q(
      `SELECT aparelho_id FROM mobilidade.aparelhos WHERE conta_id = $1
       ORDER BY ultimo_uso DESC OFFSET $2`, [conta.id, cfg.maxAparelhos]);
    for (const a of antigos) {
      await q('DELETE FROM mobilidade.aparelhos WHERE conta_id = $1 AND aparelho_id = $2', [conta.id, a.aparelho_id]);
      await q('DELETE FROM mobilidade.sessoes WHERE conta_id = $1 AND aparelho_id = $2', [conta.id, a.aparelho_id]);
    }

    const sessao = crypto.randomBytes(32).toString('base64url');
    await q('INSERT INTO mobilidade.sessoes (token_hash, conta_id, aparelho_id) VALUES ($1, $2, $3)',
      [hash(sessao), conta.id, aparelhoId]);
    res.json({ sessao, email: conta.email, ...(await licencaDa(conta, aparelhoId)) });
  }));

  app.get('/v1/licenca', rota(async (req, res) => {
    const { conta, aparelhoId } = await autenticar(req);
    const [ainda] = await q('SELECT 1 FROM mobilidade.aparelhos WHERE conta_id = $1 AND aparelho_id = $2',
      [conta.id, aparelhoId]);
    if (!ainda) throw new Erro(401, 'Este celular saiu da conta. Entre de novo.');
    await q('UPDATE mobilidade.aparelhos SET ultimo_uso = now() WHERE conta_id = $1 AND aparelho_id = $2',
      [conta.id, aparelhoId]);
    res.json({ email: conta.email, ...(await licencaDa(conta, aparelhoId)) });
  }));

  app.post('/v1/assinar', rota(async (req, res) => {
    const { conta } = await autenticar(req);
    const forma = req.body?.forma === 'CREDIT_CARD' ? 'CREDIT_CARD' : 'PIX';
    const [plano] = await q('SELECT * FROM mobilidade.planos WHERE id = $1 AND ativo', [req.body?.plano]);
    if (!plano) throw new Erro(400, 'Plano não encontrado.');
    const nome = String(req.body?.nome || conta.nome || '').trim().slice(0, 120);
    const cpf = soDigitos(req.body?.cpf || conta.cpf);
    if (nome.length < 3 || (cpf.length !== 11 && cpf.length !== 14)) throw new Erro(400, 'Informe nome e CPF.');

    let clienteId = conta.asaas_cliente_id;
    if (!clienteId) {
      clienteId = (await asaas.criarCliente({ nome, cpf, email: conta.email, contaId: conta.id })).id;
    }
    await q('UPDATE mobilidade.contas SET nome = $2, cpf = $3, asaas_cliente_id = $4 WHERE id = $1',
      [conta.id, nome, cpf, clienteId]);

    // Assinatura ainda não paga: cancela a antiga antes de criar outra (evita cobrança dobrada).
    const pendentes = await q(
      `SELECT id, asaas_assinatura_id FROM mobilidade.assinaturas WHERE conta_id = $1 AND status = 'pendente'`, [conta.id]);
    for (const p of pendentes) {
      if (p.asaas_assinatura_id) await asaas.cancelarAssinatura(p.asaas_assinatura_id).catch(() => {});
      await q(`UPDATE mobilidade.assinaturas SET status = 'cancelada' WHERE id = $1`, [p.id]);
    }

    const [nova] = await q(
      `INSERT INTO mobilidade.assinaturas (conta_id, plano_id, forma) VALUES ($1, $2, $3) RETURNING id`,
      [conta.id, plano.id, forma]);
    const hoje = agora().toISOString().slice(0, 10);
    const sub = await asaas.criarAssinatura({
      clienteId, forma, valor: reais(plano.preco_centavos), ciclo: plano.ciclo,
      descricao: `Roxou Mobilidade ${plano.nome}`, referencia: nova.id, vencimento: hoje,
    });
    await q('UPDATE mobilidade.assinaturas SET asaas_assinatura_id = $2 WHERE id = $1', [nova.id, sub.id]);

    const [primeira] = (await asaas.pagamentosDaAssinatura(sub.id)).data || [];
    if (!primeira) throw new Erro(502, 'O Asaas não gerou a cobrança. Tente de novo.');
    if (plano.preco_primeira_centavos != null) {
      await asaas.alterarValorPagamento(primeira.id, reais(plano.preco_primeira_centavos));
    }
    const resposta = {
      link: primeira.invoiceUrl,
      valorCentavos: plano.preco_primeira_centavos ?? plano.preco_centavos,
    };
    if (forma === 'PIX') {
      const qr = await asaas.qrCodePix(primeira.id);
      resposta.pix = { copiaECola: qr.payload, imagemBase64: qr.encodedImage };
    }
    res.json(resposta);
  }));

  app.post('/v1/assinatura/cancelar', rota(async (req, res) => {
    const { conta } = await autenticar(req);
    const ativas = await q(
      `SELECT id, asaas_assinatura_id FROM mobilidade.assinaturas
       WHERE conta_id = $1 AND status IN ('ativa', 'pendente')`, [conta.id]);
    for (const a of ativas) {
      if (a.asaas_assinatura_id) await asaas.cancelarAssinatura(a.asaas_assinatura_id);
      await q(`UPDATE mobilidade.assinaturas SET status = 'cancelada' WHERE id = $1`, [a.id]);
    }
    // O que já foi pago continua valendo até o fim do período.
    res.json({ ok: true });
  }));

  app.post('/v1/sair', rota(async (req, res) => {
    const { tokenHash } = await autenticar(req);
    await q('DELETE FROM mobilidade.sessoes WHERE token_hash = $1', [tokenHash]);
    res.json({ ok: true });
  }));

  // Avisos do Asaas (cadastre em Integrações > Webhooks, com o token de ASAAS_WEBHOOK_TOKEN).
  app.post('/v1/webhooks/asaas', rota(async (req, res) => {
    const recebido = req.get('asaas-access-token') || '';
    if (!webhookToken || recebido.length !== webhookToken.length
        || !crypto.timingSafeEqual(Buffer.from(recebido), Buffer.from(webhookToken))) {
      throw new Erro(401, 'Token inválido.');
    }
    const { event: evento, payment: pag, subscription: subEvento } = req.body || {};
    const subId = pag?.subscription || subEvento?.id;
    if (!subId) return res.json({ ok: true });
    const [a] = await q(
      `SELECT a.id, p.ciclo FROM mobilidade.assinaturas a JOIN mobilidade.planos p ON p.id = a.plano_id
       WHERE a.asaas_assinatura_id = $1`, [subId]);
    if (!a) return res.json({ ok: true });

    if (evento === 'PAYMENT_RECEIVED' || evento === 'PAYMENT_CONFIRMED') {
      const novo = await q(
        `INSERT INTO mobilidade.pagamentos (asaas_pagamento_id, assinatura_id, valor_centavos, evento)
         VALUES ($1, $2, $3, $4) ON CONFLICT DO NOTHING RETURNING asaas_pagamento_id`,
        [pag.id, a.id, Math.round(Number(pag.value || 0) * 100), evento]);
      if (novo.length) {
        await q(
          `UPDATE mobilidade.assinaturas SET status = CASE WHEN status = 'cancelada' THEN status ELSE 'ativa' END,
             pago_ate = GREATEST(COALESCE(pago_ate, now()), now()) + $2::interval
           WHERE id = $1`, [a.id, INTERVALO[a.ciclo]]);
      }
    } else if (evento === 'PAYMENT_REFUNDED' || evento === 'PAYMENT_CHARGEBACK_REQUESTED') {
      await q('UPDATE mobilidade.assinaturas SET pago_ate = now() WHERE id = $1', [a.id]);
    } else if (evento === 'SUBSCRIPTION_DELETED' || evento === 'SUBSCRIPTION_INACTIVATED') {
      await q(`UPDATE mobilidade.assinaturas SET status = 'cancelada' WHERE id = $1`, [a.id]);
    }
    res.json({ ok: true });
  }));

  return app;
}
