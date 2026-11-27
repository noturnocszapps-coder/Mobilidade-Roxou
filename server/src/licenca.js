// Licença assinada que o app confere sozinho, sem internet.
// Formato: base64url(texto) + "." + base64url(assinatura ECDSA P-256 / SHA-256, DER).
// O texto tem uma informação por linha ("chave=valor"), fácil de ler no celular.
import crypto from 'node:crypto';

const b64url = (buf) => Buffer.from(buf).toString('base64url');

export function montarTexto(campos) {
  return Object.entries(campos)
    .map(([k, v]) => `${k}=${Array.isArray(v) ? v.join(',') : v}`)
    .join('\n');
}

export function assinarLicenca(campos, chavePrivadaPem) {
  const texto = Buffer.from(montarTexto({ v: 1, ...campos }), 'utf8');
  const assinatura = crypto.sign('sha256', texto, { key: chavePrivadaPem, dsaEncoding: 'der' });
  return `${b64url(texto)}.${b64url(assinatura)}`;
}

/** Usada nos testes: confere a licença como o app faz. */
export function conferirLicenca(token, chavePublicaPem) {
  const [t, a] = token.split('.');
  const texto = Buffer.from(t, 'base64url');
  const ok = crypto.verify('sha256', texto, { key: chavePublicaPem, dsaEncoding: 'der' }, Buffer.from(a, 'base64url'));
  if (!ok) return null;
  return Object.fromEntries(texto.toString('utf8').split('\n').map((l) => {
    const i = l.indexOf('=');
    return [l.slice(0, i), l.slice(i + 1)];
  }));
}

const seg = (d) => Math.floor(d.getTime() / 1000);

/**
 * Decide o que a conta tem liberado agora.
 * Ordem: assinatura paga em dia > cortesia manual > teste grátis > plano grátis.
 */
export function decidirAcesso({ agora, assinatura, conta, config }) {
  const pagoAte = assinatura?.pago_ate ? new Date(assinatura.pago_ate) : null;
  if (pagoAte && pagoAte > agora) {
    return { plano: assinatura.plano_id, modulos: assinatura.modulos, validoAte: pagoAte };
  }
  if (conta.liberado_ate && new Date(conta.liberado_ate) > agora) {
    return { plano: 'cortesia', modulos: config.modulosPro, validoAte: new Date(conta.liberado_ate) };
  }
  if (conta.teste_ate && new Date(conta.teste_ate) > agora) {
    return { plano: 'teste', modulos: config.modulosPro, validoAte: new Date(conta.teste_ate) };
  }
  const dias = config.validadeGratisDias;
  return { plano: 'gratis', modulos: [], validoAte: new Date(agora.getTime() + dias * 86400_000) };
}

export function emitirLicenca({ agora, conta, aparelhoId, acesso, toleranciaDias, chavePrivadaPem }) {
  return assinarLicenca({
    conta: conta.id,
    email: conta.email,
    aparelho: aparelhoId,
    plano: acesso.plano,
    modulos: acesso.modulos,
    valido_ate: seg(acesso.validoAte),
    tolerancia_dias: toleranciaDias,
    emitido_em: seg(agora),
  }, chavePrivadaPem);
}
