# Task 02 — Configurações externalizadas e infraestrutura

**Dependências:** Task 01
**Referência na especificação:** Seção 3.2, Seção 5.1, Seção 8 (`config/`), Seção 9 itens 1 e 4

## Objetivo
Centralizar todas as configurações (URLs, porta CDP, seletores CSS/ARIA, timeouts, jitter, cooldown,
retry, sufixo instrumental) em `application.yml`, configurar o Jackson e habilitar Virtual Threads.

## Escopo
1. **`config/AutomationProperties.java`** — `@ConfigurationProperties(prefix = "automation")` + `@Validated`, com grupos:
   - `browser`: `cdpUrl` (padrão `http://localhost:9222`), `flowMusicUrl`, `navigationTimeoutMs`.
   - `selectors`: `promptInput`, `generateButton`, `audioCard`, `downloadButton`, `loadingIndicator` (CSS/ARIA).
   - `timing`: `jitterMinMs` (4000), `jitterMaxMs` (9000), `batchCooldownMs` (15000), `generationTimeoutMs`.
   - `retry`: `maxAttempts` (3), `backoffBaseSeconds` (2).
   - `generation`: `defaultTargetCount` (30), `instrumentalSuffix`
     (`", strictly instrumental, no vocals, no lyrics, no singing, high quality studio audio"`).
   - `download`: `minFileSizeBytes` (10240), `fileWaitTimeoutMs`.
2. **`config/JacksonConfig.java`** — `ObjectMapper` com `DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY`
   habilitado e `FAIL_ON_UNKNOWN_PROPERTIES` desabilitado.
3. **`config/AsyncConfig.java`** — `@EnableAsync` + executor baseado em Virtual Threads
   (`Executors.newVirtualThreadPerTaskExecutor()`) e `spring.threads.virtual.enabled: true`.
   O executor de lotes deve ser **single-consumer** (fila sequencial) para não haver colisão na aba do navegador.
4. Preencher `application.yml` com todos os valores acima (seletores podem iniciar como placeholders a serem
   ajustados na Task 10).
5. Criar `application-simulation.yml` com jitter/cooldown baixos (ex.: 100–300 ms / 1000 ms) para uso na Task 06.
6. Log de startup (ex.: `ApplicationRunner`) imprimindo um resumo das propriedades carregadas.

## Critérios de aceite
- [ ] Alterar um valor no `application.yml` (ex.: `batchCooldownMs`) reflete no log de startup.
- [ ] Propriedade obrigatória ausente/inválida (ex.: `jitterMinMs > jitterMaxMs` ou `cdpUrl` vazio) impede o startup com mensagem clara.
- [ ] Nenhum seletor ou URL fica hardcoded em classes Java.

## Resultado observável
```bash
./mvnw spring-boot:run
# Log esperado:
# Automation config -> cdpUrl=http://localhost:9222, flowMusicUrl=..., jitter=4000-9000ms,
#                      cooldown=15000ms, retry=3x, defaultTargetCount=30, virtualThreads=true
```

## Testes
Nenhum (classes de configuração são triviais segundo a Seção 7).

