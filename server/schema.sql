-- Roxou Mobilidade: contas, planos, assinaturas e aparelhos.
-- Fica num schema próprio no NT Database, separado das tabelas do guia de eventos.
-- Pode rodar de novo sem estragar nada (só cria o que falta).

CREATE SCHEMA IF NOT EXISTS mobilidade;
SET search_path TO mobilidade;

-- Valores que mudam sem gerar APK novo.
CREATE TABLE IF NOT EXISTS config (
  chave TEXT PRIMARY KEY,
  valor TEXT NOT NULL
);
INSERT INTO config (chave, valor) VALUES
  ('dias_teste', '7'),          -- teste grátis do Pro
  ('tolerancia_dias', '7'),     -- dias que o app funciona sem internet depois do vencimento
  ('max_aparelhos', '2'),       -- aparelhos por conta
  ('validade_licenca_gratis_dias', '30')
ON CONFLICT (chave) DO NOTHING;

-- Planos e os módulos que cada um libera.
-- Módulos: leitura (leitura automática + janela flutuante), bairros, historico, backup, relatorios.
CREATE TABLE IF NOT EXISTS planos (
  id TEXT PRIMARY KEY,
  nome TEXT NOT NULL,
  descricao TEXT NOT NULL DEFAULT '',
  preco_centavos INTEGER NOT NULL,           -- valor de cada cobrança
  preco_primeira_centavos INTEGER,           -- promoção na 1ª cobrança; NULL = sem promoção
  ciclo TEXT NOT NULL CHECK (ciclo IN ('MONTHLY', 'QUARTERLY', 'YEARLY')),
  modulos TEXT[] NOT NULL,
  vagas INTEGER,                             -- limite de assinantes; NULL = sem limite
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  ordem INTEGER NOT NULL DEFAULT 0
);
-- Preços iniciais (27/11/2026). Para mudar: UPDATE mobilidade.planos SET preco_centavos = ... WHERE id = ...;
-- Quem já assina continua no preço antigo até cancelar.
INSERT INTO planos (id, nome, descricao, preco_centavos, preco_primeira_centavos, ciclo, modulos, ordem) VALUES
  ('mensal', 'Mensal', 'Primeiro mês por R$ 5,99', 990, 599, 'MONTHLY',
     ARRAY['leitura','bairros','historico','backup','relatorios'], 1),
  ('trimestral', 'Trimestral', 'Sai R$ 7,99 por mês', 2397, NULL, 'QUARTERLY',
     ARRAY['leitura','bairros','historico','backup','relatorios'], 2),
  ('anual', 'Anual', 'Sai R$ 5,99 por mês', 7188, NULL, 'YEARLY',
     ARRAY['leitura','bairros','historico','backup','relatorios'], 3)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS contas (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email TEXT NOT NULL UNIQUE,
  nome TEXT,
  cpf TEXT,
  asaas_cliente_id TEXT,
  teste_ate TIMESTAMPTZ,          -- fim do teste grátis (uma vez por conta)
  liberado_ate TIMESTAMPTZ,       -- liberação manual (cortesia), se quiser dar acesso a alguém
  criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS codigos_login (
  email TEXT PRIMARY KEY,
  codigo_hash TEXT NOT NULL,
  expira_em TIMESTAMPTZ NOT NULL,
  tentativas INTEGER NOT NULL DEFAULT 0,
  enviados_na_hora INTEGER NOT NULL DEFAULT 1,
  hora_inicio TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS sessoes (
  token_hash TEXT PRIMARY KEY,
  conta_id UUID NOT NULL REFERENCES contas(id) ON DELETE CASCADE,
  aparelho_id TEXT NOT NULL,
  criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
  ultimo_uso TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS aparelhos (
  conta_id UUID NOT NULL REFERENCES contas(id) ON DELETE CASCADE,
  aparelho_id TEXT NOT NULL,
  criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
  ultimo_uso TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (conta_id, aparelho_id)
);

-- Aparelhos que já usaram o teste grátis (para não repetir criando outra conta).
CREATE TABLE IF NOT EXISTS aparelhos_teste (
  aparelho_id TEXT PRIMARY KEY,
  usado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS assinaturas (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conta_id UUID NOT NULL REFERENCES contas(id) ON DELETE CASCADE,
  plano_id TEXT NOT NULL REFERENCES planos(id),
  forma TEXT NOT NULL,                       -- PIX ou CREDIT_CARD
  asaas_assinatura_id TEXT UNIQUE,
  status TEXT NOT NULL DEFAULT 'pendente',   -- pendente, ativa, cancelada
  pago_ate TIMESTAMPTZ,
  criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS assinaturas_conta ON assinaturas (conta_id);

-- Cada pagamento confirmado, uma vez só (o Asaas pode mandar o mesmo aviso de novo).
CREATE TABLE IF NOT EXISTS pagamentos (
  asaas_pagamento_id TEXT PRIMARY KEY,
  assinatura_id UUID NOT NULL REFERENCES assinaturas(id) ON DELETE CASCADE,
  valor_centavos INTEGER NOT NULL,
  evento TEXT NOT NULL,
  recebido_em TIMESTAMPTZ NOT NULL DEFAULT now()
);
