// Teste do fluxo completo com um PostgreSQL de verdade.
// Rode com: TEST_DATABASE_URL=postgres://... npm test  (sem a variável, o teste é pulado).
import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import fs from 'node:fs';
import pg from 'pg';
import { criarApp } from '../src/app.js';
import { conferirLicenca } from '../src/licenca.js';

const url = process.env.TEST_DATABASE_URL;
const { privateKey, publicKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'P-256' });
const pub = publicKey.export({ type: 'spki', format: 'pem' });
let db, server, base;
const codigos = {};
const asaasChamadas = [];

const asaas = {
  criarCliente: async () => ({ id: 'cus_1' }),
  criarAssinatura: async (d) => { asaasChamadas.push(['assinatura', d]); return { id: `sub_${asaasChamadas.length}` }; },
  cancelarAssinatura: async (id) => { asaasChamadas.push(['cancelar', id]); },
  pagamentosDaAssinatura: async (id) => ({ data: [{ id: `pay_${id}`, invoiceUrl: 'https://asaas/x' }] }),
  alterarValorPagamento: async (id, v) => { asaasChamadas.push(['valor', id, v]); },
  qrCodePix: async () => ({ payload: '000201PIX', encodedImage: 'iVBOR' }),
};

before(async () => {
  if (!url) return;
  db = new pg.Pool({ connectionString: url });
  await db.query('DROP SCHEMA IF EXISTS mobilidade CASCADE');
  await db.query(fs.readFileSync(new URL('../schema.sql', import.meta.url), 'utf8'));
  const app = criarApp({
    db, asaas, webhookToken: 'segredo',
    email: { enviarCodigo: async (e, c) => { codigos[e] = c; } },
    chavePrivadaPem: privateKey.export({ type: 'pkcs8', format: 'pem' }),
  });
  await new Promise((r) => { server = app.listen(0, r); });
  base = `http://127.0.0.1:${server.address().port}`;
});
after(async () => { server?.close(); await db?.end(); });

const chamar = async (metodo, caminho, corpo, sessao, extra = {}) => {
  const r = await fetch(base + caminho, {
    method: metodo,
    headers: { 'content-type': 'application/json', ...(sessao ? { authorization: `Bearer ${sessao}` } : {}), ...extra },
    body: corpo ? JSON.stringify(corpo) : undefined,
  });
  return { status: r.status, corpo: await r.json() };
};

async function entrar(email, aparelho) {
  await chamar('POST', '/v1/auth/codigo', { email });
  return (await chamar('POST', '/v1/auth/entrar', { email, codigo: codigos[email.toLowerCase()], aparelho })).corpo;
}

test('fluxo: planos, login com teste grátis, Pix, webhook e licença paga', { skip: !url }, async () => {
  const planos = (await chamar('GET', '/v1/planos')).corpo;
  assert.deepEqual(planos.planos.map((p) => p.id), ['mensal', 'trimestral', 'anual']);
  assert.equal(planos.diasTeste, 7);

  assert.equal((await chamar('POST', '/v1/auth/entrar', { email: 'a@b.com', codigo: '000000', aparelho: 'x' })).status, 400);
  const login = await entrar('Motorista@Teste.com', 'cel-1');
  assert.equal(login.plano, 'teste');
  const lic = conferirLicenca(login.licenca, pub);
  assert.equal(lic.aparelho, 'cel-1');
  assert.equal(lic.tolerancia_dias, '7');
  assert.match(lic.modulos, /leitura/);

  const cobranca = await chamar('POST', '/v1/assinar', { plano: 'mensal', forma: 'PIX', nome: 'Fulano de Tal', cpf: '123.456.789-09' }, login.sessao);
  assert.equal(cobranca.status, 200);
  assert.equal(cobranca.corpo.pix.copiaECola, '000201PIX');
  assert.equal(cobranca.corpo.valorCentavos, 599);
  assert.deepEqual(asaasChamadas.find((c) => c[0] === 'valor'), ['valor', 'pay_sub_1', 5.99]);
  assert.equal(asaasChamadas[0][1].valor, 9.9);

  assert.equal((await chamar('POST', '/v1/webhooks/asaas', { event: 'PAYMENT_RECEIVED' }, null, { 'asaas-access-token': 'errado' })).status, 401);
  const aviso = { event: 'PAYMENT_RECEIVED', payment: { id: 'pay_sub_1', subscription: 'sub_1', value: 5.99 } };
  for (let i = 0; i < 2; i++) { // aviso repetido não pode dar 2 meses
    assert.equal((await chamar('POST', '/v1/webhooks/asaas', aviso, null, { 'asaas-access-token': 'segredo' })).status, 200);
  }
  const paga = (await chamar('GET', '/v1/licenca', null, login.sessao)).corpo;
  assert.equal(paga.plano, 'mensal');
  const dias = (new Date(paga.validoAte) - Date.now()) / 86400_000;
  assert.ok(dias > 27 && dias < 32, `validade de ${dias} dias`);
});

test('teste grátis só uma vez por aparelho, e no máximo 2 celulares', { skip: !url }, async () => {
  await entrar('primeiro@x.com', 'cel-9');
  const outra = await entrar('segundo@x.com', 'cel-9');
  assert.equal(outra.plano, 'gratis');

  const s1 = (await entrar('tres@x.com', 'a')).sessao;
  await entrar('tres@x.com', 'b');
  await entrar('tres@x.com', 'c');
  assert.equal((await chamar('GET', '/v1/licenca', null, s1)).status, 401);
});
