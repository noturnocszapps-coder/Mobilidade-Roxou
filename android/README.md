# Roxou Mobilidade · app Android

App Android nativo (Kotlin + Jetpack Compose), Etapa 2 do documento-mestre: base modular, banco local, motores de cálculo, metas, locação e lançamento manual de corridas.

O app **nunca aceita nem recusa corridas**. Ele só registra e calcula.

## Módulos

| Módulo | O que tem |
|---|---|
| `core-domain` | Kotlin puro, sem Android. Viradas de horário, faixas da locadora, parcela, caixa, cascata do dia, franquia de km, combustível, jornada, melhor dia de folga. Todos os testes. |
| `core-data` | Room (banco local offline) e DataStore (configurações). Repositório único. |
| `core-ui` | Tema escuro, formatação em R$ e componentes. |
| `feature-trips` | Lançamento manual e histórico de corridas. |
| `feature-goals` | Meta semanal e diária com redistribuição, faixas 40/60/80, correção pelo painel da Uber. |
| `feature-rental` | Parcela, abatimentos (retido pela Uber ou pago), franquia de km. |
| `feature-finance` | Faturamento por categoria, caixa, ganhos extras, combustível e despesas. |
| `app` | Painel do turno, configurações e navegação. |

## Regras de horário (configuráveis, America/Sao_Paulo)

- Franquia de km: zera segunda 00h.
- Corridas e parcela: viram segunda 04h (fechamento da Uber).
- Dia de trabalho: termina às 04h.

## Compilar

O GitHub Actions (`.github/workflows/build-apk.yml`) roda os testes e gera o APK. Na `main`, o APK sai solto na release **ultima-versao**; em PRs, fica em **Actions → execução → Artifacts → roxou-mobilidade-apk**.

Localmente: `./gradlew :core-domain:test assembleDebug` (precisa do Android SDK).
