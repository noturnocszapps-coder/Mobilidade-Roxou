# Roxou Mobilidade

App Android nativo (Kotlin + Jetpack Compose) para motoristas de Uber e 99. O código do app fica na pasta [`android/`](android/).

> A pasta `src/` e os arquivos na raiz (Vite/React) são o protótipo web gerado no Google AI Studio. Eles não entram no APK.

## Como baixar e instalar o APK pelo celular

O GitHub Actions gera um APK novo a cada push na branch `main`. Não precisa de computador nem de cabo.

### Jeito mais fácil: release "ultima-versao"

1. No celular, entre logado no GitHub e abra o repositório.
2. Toque em **Releases** (ou abra direto `https://github.com/noturnocszapps-coder/Mobilidade-Roxou/releases/tag/ultima-versao`).
3. Em **Assets**, toque no arquivo `roxou-mobilidade-NUMERO.apk`. O download começa direto, sem ZIP.
4. Abra o arquivo baixado e toque em **Instalar**. Na primeira vez, o Android pede para **permitir instalar apps desta fonte** (o Chrome ou o gerenciador de arquivos). Permita e volte.

A release é substituída a cada build, então esse link sempre aponta para a versão mais recente.

### Alternativa: artifact da execução

1. No repositório, toque em **Actions** e depois em **Gerar APK**.
2. Abra a execução mais recente com o sinal verde ✅.
3. Role até **Artifacts** e toque em **roxou-mobilidade-apk**. Vem um ZIP.
4. Abra o ZIP no gerenciador de arquivos, extraia o `.apk` e instale como no passo 4 acima.

Os artifacts ficam guardados por 30 dias. Dica: no Chrome do celular, use **Site para computador** se a aba Actions não mostrar tudo.

### Atualizar o app

Basta instalar o APK novo por cima do anterior. Todas as versões usam a mesma assinatura de debug, então o Android aceita a atualização e os dados do app são mantidos.

Se aparecer **"App não instalado"**, desinstale a versão antiga (isso acontece com APKs gerados antes deste workflow, que tinham outra assinatura).

### Gerar um APK sem fazer push

Em **Actions → Gerar APK**, toque em **Run workflow** e escolha a branch.

### Aviso sobre a acessibilidade (Android 13 ou mais novo)

Um app instalado fora da Play Store só libera a acessibilidade depois de **Permitir configurações restritas**: Configurações → Apps → Roxou Mobilidade → menu ⋮ → Permitir configurações restritas.

## Para quem for mexer no código

- Projeto Gradle em `android/`, com o wrapper do Gradle (`./gradlew assembleDebug`).
- O workflow está em [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml).
- O número da versão vem do número da execução no GitHub Actions, então cada build é uma versão maior que a anterior.
- `android/keystore/debug.keystore` é uma chave de debug pública, só para manter a mesma assinatura entre builds. Nunca usar para a Play Store.
