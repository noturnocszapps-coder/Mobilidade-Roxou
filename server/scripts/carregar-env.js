// Lê o arquivo .env (sem precisar de biblioteca).
import fs from 'node:fs';

const arquivo = new URL('../.env', import.meta.url);
if (fs.existsSync(arquivo)) {
  for (const linha of fs.readFileSync(arquivo, 'utf8').split('\n')) {
    const m = linha.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/);
    if (m && process.env[m[1]] === undefined) process.env[m[1]] = m[2];
  }
}
