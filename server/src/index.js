import fs from 'node:fs';
import pg from 'pg';
import '../scripts/carregar-env.js';
import { criarApp } from './app.js';
import { criarAsaas } from './asaas.js';
import { criarEmail } from './email.js';

const env = process.env;
const chavePrivadaPem = fs.readFileSync(new URL(`../${env.CHAVE_PRIVADA_ARQUIVO || 'chaves/licenca-privada.pem'}`, import.meta.url), 'utf8');
const app = criarApp({
  db: new pg.Pool({ connectionString: env.DATABASE_URL, max: 5 }),
  asaas: criarAsaas({ url: env.ASAAS_URL || 'https://api-sandbox.asaas.com/v3', chave: env.ASAAS_CHAVE }),
  email: criarEmail(env),
  chavePrivadaPem,
  webhookToken: env.ASAAS_WEBHOOK_TOKEN,
});
const porta = Number(env.PORTA || 3105);
app.listen(porta, '127.0.0.1', () => console.log(`API do Roxou Mobilidade na porta ${porta}`));
