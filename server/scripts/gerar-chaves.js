// Cria o par de chaves da licença. Rode UMA vez na VPS: npm run gerar-chaves
// - A chave privada fica em chaves/ (nunca vai para o GitHub).
// - A chave pública aparece na tela: cole no GitHub como variável ROXOU_LICENCA_CHAVE_PUBLICA.
import crypto from 'node:crypto';
import fs from 'node:fs';

const pasta = new URL('../chaves/', import.meta.url);
const arquivo = new URL('licenca-privada.pem', pasta);
if (fs.existsSync(arquivo)) {
  console.error('Já existe uma chave em chaves/licenca-privada.pem. Não gerei outra, para não invalidar as licenças.');
  process.exit(1);
}
const { privateKey, publicKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'P-256' });
fs.mkdirSync(pasta, { recursive: true });
fs.writeFileSync(arquivo, privateKey.export({ type: 'pkcs8', format: 'pem' }), { mode: 0o600 });
const publica = publicKey.export({ type: 'spki', format: 'der' }).toString('base64');
console.log('Chave privada salva em chaves/licenca-privada.pem (guarde uma cópia fora da VPS).');
console.log('\nChave pública (copie a linha abaixo para o GitHub):\n');
console.log(publica);
