# Task 09 — Conexão CDP ao Chrome autenticado (`BrowserSessionManager`)

**Dependências:** Task 02 (pode ser feita em paralelo às Tasks 03–08)
**Referência na especificação:** Seção 2 fase 3, Seção 9 itens 1 e 4

## Objetivo
Conectar a aplicação, via Playwright `connectOverCDP`, a uma instância **real e já logada** do Google Chrome
na porta 9222, reutilizando a sessão do usuário (sem navegador anônimo, evitando OAuth2/2FA/Captcha).

## Escopo
1. **`service/BrowserSessionManager.java`** (perfil padrão, **não** ativo em `simulation`):
   - Criação lazy de `Playwright` + `Browser` via `playwright.chromium().connectOverCDP(cdpUrl)`.
   - Reutilizar o `BrowserContext` existente (`browser.contexts().get(0)`) para herdar cookies/sessão do Google.
   - `Page getFlowMusicPage()` → reutiliza uma aba já aberta na URL do Flow Music ou abre uma nova e navega
     até `automation.browser.flowMusicUrl`.
   - Reconexão automática caso o Chrome seja fechado/desconectado entre lotes.
   - `@PreDestroy`: encerrar o Playwright **sem fechar o Chrome do usuário** (apenas desconectar).
   - Garantir acesso single-thread ao Playwright (o objeto não é thread-safe).
2. Mensagem de erro clara (`AutomationException`) se o Chrome não estiver acessível em `cdpUrl`, incluindo o
   comando para iniciá-lo.
3. Verificação de sessão: detectar se a página redirecionou para login do Google (`accounts.google.com`) e
   falhar com mensagem "Faça login no Chrome antes de iniciar".
4. Smoke check manual: `ApplicationRunner` ativo com `automation.browser.smoke-check=true` que conecta, abre a
   URL, loga o título da página e desconecta.

## Pré-requisito manual (iniciar o Chrome)
```bash
# Linux
google-chrome --remote-debugging-port=9222 --user-data-dir="$HOME/.chrome-flow-music"
# macOS
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --remote-debugging-port=9222 --user-data-dir="$HOME/.chrome-flow-music"
# Windows
"C:\Program Files\Google\Chrome\Application\chrome.exe" --remote-debugging-port=9222 --user-data-dir="%USERPROFILE%\.chrome-flow-music"
```
Fazer login na conta Google nesse Chrome uma única vez (o perfil é persistido em `--user-data-dir`).

## Critérios de aceite
- [ ] Com o Chrome aberto e logado, a aplicação conecta e navega até o Flow Music na aba do usuário.
- [ ] Com o Chrome fechado, a aplicação retorna erro claro (não trava, não lança stacktrace genérico).
- [ ] Encerrar a aplicação **não** fecha o Chrome.

## Resultado observável
```bash
curl -s http://localhost:9222/json/version        # Chrome expõe o CDP
./mvnw spring-boot:run -Dspring-boot.run.arguments=--automation.browser.smoke-check=true
# Log: "Conectado ao Chrome via CDP (Chrome/1xx). Página aberta: <título do Flow Music>"
# Visual: a aba do Flow Music aparece no Chrome do usuário.
```

## Testes
Nenhum automatizado (a Seção 7 proíbe testes que abram o Chrome real no build). Validação manual pelo smoke check.

