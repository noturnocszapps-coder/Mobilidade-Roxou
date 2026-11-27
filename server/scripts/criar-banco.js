// Cria (ou completa) as tabelas do Roxou Mobilidade no NT Database: npm run banco
import fs from 'node:fs';
import pg from 'pg';
import './carregar-env.js';

const sql = fs.readFileSync(new URL('../schema.sql', import.meta.url), 'utf8');
const db = new pg.Client({ connectionString: process.env.DATABASE_URL });
await db.connect();
await db.query(sql);
await db.end();
console.log('Banco pronto (schema "mobilidade").');
