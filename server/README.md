# API do Roxou Mobilidade

Login por código no e-mail, planos, assinatura por Pix ou cartão (Asaas) e licença assinada que o app confere offline.
Roda na VPS Contabo, com o banco no NT Database (schema `mobilidade`, separado do guia de eventos).

## Instalar na VPS (uma vez)

```bash
cd server
npm install
cp .env.example .env      # preencha banco, Asaas e SMTP
npm run banco             # cria as tabelas e os planos
npm run gerar-chaves      # cria a chave da licença e mostra a chave pública
pm2 start src/index.js --name roxou-mobilidade-api
```

- Guarde uma cópia de `chaves/licenca-privada.pem` fora da VPS. Se ela for perdida, todas as licenças precisam ser reemitidas.
- Cole a chave pública no GitHub como variável `ROXOU_LICENCA_CHAVE_PUBLICA`.
- No Nginx, aponte o domínio da API para `127.0.0.1:3105` (com HTTPS).
- No Asaas (Integrações > Webhooks), cadastre `https://SEU-DOMINIO/v1/webhooks/asaas` com o mesmo token de `ASAAS_WEBHOOK_TOKEN`, e ligue os eventos de cobrança e de assinatura.

## Mudar preços e regras sem gerar APK

```sql
UPDATE mobilidade.planos SET preco_centavos = 990, preco_primeira_centavos = 599 WHERE id = 'mensal';
UPDATE mobilidade.config SET valor = '7' WHERE chave = 'dias_teste';
-- Dar acesso de cortesia a alguém por 30 dias:
UPDATE mobilidade.contas SET liberado_ate = now() + interval '30 days' WHERE email = 'fulano@gmail.com';
```

Quem já assina continua no valor antigo (o Asaas guarda o valor de cada assinatura).

## Testes

```bash
npm test                                          # testes da licença
TEST_DATABASE_URL=postgres://... npm test          # também o fluxo completo num banco de teste vazio
```
