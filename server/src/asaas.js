// Cliente mínimo da API do Asaas (https://docs.asaas.com).
export function criarAsaas({ url, chave, fetchFn = fetch }) {
  async function chamar(metodo, caminho, corpo) {
    if (!chave) throw new Error('ASAAS_CHAVE não configurada');
    const r = await fetchFn(`${url}${caminho}`, {
      method: metodo,
      headers: { 'Content-Type': 'application/json', access_token: chave, 'User-Agent': 'roxou-mobilidade' },
      body: corpo ? JSON.stringify(corpo) : undefined,
    });
    const dados = await r.json().catch(() => ({}));
    if (!r.ok) {
      const msg = dados?.errors?.map((e) => e.description).join('; ') || `Asaas respondeu ${r.status}`;
      throw Object.assign(new Error(msg), { status: 502 });
    }
    return dados;
  }

  return {
    criarCliente: ({ nome, cpf, email, contaId }) =>
      chamar('POST', '/customers', { name: nome, cpfCnpj: cpf, email, externalReference: contaId, notificationDisabled: false }),
    criarAssinatura: ({ clienteId, forma, valor, ciclo, descricao, referencia, vencimento }) =>
      chamar('POST', '/subscriptions', {
        customer: clienteId, billingType: forma, value: valor, cycle: ciclo,
        nextDueDate: vencimento, description: descricao, externalReference: referencia,
      }),
    cancelarAssinatura: (id) => chamar('DELETE', `/subscriptions/${id}`),
    pagamentosDaAssinatura: (id) => chamar('GET', `/subscriptions/${id}/payments`),
    alterarValorPagamento: (id, valor) => chamar('POST', `/payments/${id}`, { value: valor }),
    qrCodePix: (id) => chamar('GET', `/payments/${id}/pixQrCode`),
  };
}
