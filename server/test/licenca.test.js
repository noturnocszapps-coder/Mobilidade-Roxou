import { test } from 'node:test';
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import { assinarLicenca, conferirLicenca, decidirAcesso } from '../src/licenca.js';

const { privateKey, publicKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'P-256' });
const priv = privateKey.export({ type: 'pkcs8', format: 'pem' });
const pub = publicKey.export({ type: 'spki', format: 'pem' });

test('licença assinada confere e alterada não confere', () => {
  const token = assinarLicenca({ conta: 'c1', modulos: ['leitura', 'bairros'], valido_ate: 100 }, priv);
  assert.deepEqual(conferirLicenca(token, pub), { v: '1', conta: 'c1', modulos: 'leitura,bairros', valido_ate: '100' });
  const [t, a] = token.split('.');
  const falso = Buffer.from(Buffer.from(t, 'base64url').toString().replace('100', '999')).toString('base64url');
  assert.equal(conferirLicenca(`${falso}.${a}`, pub), null);
});

const cfg = { modulosPro: ['leitura'], validadeGratisDias: 30 };
const agora = new Date('2026-12-01T12:00:00Z');

test('assinatura paga vence teste e grátis', () => {
  const r = decidirAcesso({ agora, cfg, config: cfg,
    assinatura: { plano_id: 'anual', pago_ate: '2027-12-01T00:00:00Z', modulos: ['leitura', 'backup'] },
    conta: { teste_ate: '2026-12-05T00:00:00Z' } });
  assert.equal(r.plano, 'anual');
  assert.deepEqual(r.modulos, ['leitura', 'backup']);
});

test('assinatura vencida cai no teste, e depois no grátis', () => {
  const assinatura = { plano_id: 'mensal', pago_ate: '2026-11-01T00:00:00Z', modulos: ['leitura'] };
  assert.equal(decidirAcesso({ agora, assinatura, config: cfg, conta: { teste_ate: '2026-12-05T00:00:00Z' } }).plano, 'teste');
  const g = decidirAcesso({ agora, assinatura, config: cfg, conta: { teste_ate: '2026-11-20T00:00:00Z' } });
  assert.equal(g.plano, 'gratis');
  assert.deepEqual(g.modulos, []);
});
