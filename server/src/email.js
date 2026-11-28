import nodemailer from 'nodemailer';

export function criarEmail(env) {
  if (!env.SMTP_HOST) {
    // Sem SMTP (teste local): mostra o código no log do servidor.
    return { enviarCodigo: async (email, codigo) => console.log(`[sem SMTP] código de ${email}: ${codigo}`) };
  }
  const transporte = nodemailer.createTransport({
    host: env.SMTP_HOST,
    port: Number(env.SMTP_PORTA || 587),
    secure: Number(env.SMTP_PORTA) === 465,
    auth: { user: env.SMTP_USUARIO, pass: env.SMTP_SENHA },
  });
  return {
    enviarCodigo: (email, codigo) => transporte.sendMail({
      from: env.EMAIL_REMETENTE,
      to: email,
      subject: `Seu código do Roxou Mobilidade: ${codigo}`,
      text: `Seu código para entrar no Roxou Mobilidade é ${codigo}.\n\nEle vale por 10 minutos. Se não foi você, ignore este e-mail.`,
    }),
  };
}
