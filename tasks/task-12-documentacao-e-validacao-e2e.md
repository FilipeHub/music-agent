# Task 12 — Documentação e validação ponta a ponta

**Dependências:** Tasks 01–11
**Referência na especificação:** Seção 9 (Entregáveis esperados, item 5), Seção 2 fase 6, Seção 3.2

## Objetivo
Documentar o uso do sistema e validar o fluxo completo com o `default.json` contendo múltiplos objetos.

## Escopo
1. **`README.md`** na raiz do projeto, contendo:
   - Pré-requisitos (Java 21, Maven Wrapper, Google Chrome).
   - Como iniciar o Chrome com `--remote-debugging-port=9222` em Linux, macOS e Windows e fazer login.
   - Como rodar a aplicação (modo real e perfil `simulation`).
   - Exemplos de `curl`: POST com body, POST sem body (fallback `default.json`), GET status geral e por `batchId`.
   - Tabela de propriedades do `application.yml` (seletores, timeouts, jitter, cooldown, retry, sufixo instrumental).
   - Como ajustar os seletores caso a interface do Google mude.
   - Estrutura dos arquivos gerados (`batch_..._track_NN_....mp3` e `manifest_<batchId>.json`).
   - Limitações conhecidas (rate limit/quota da plataforma, mudanças de DOM, termos de uso).
2. Revisar que existem **apenas** os 4 testes previstos na Seção 7 (+ eventuais opcionais justificados).
3. Executar a validação E2E.

## Critérios de aceite
- [ ] `./mvnw clean verify` verde com os 4 testes: `MusicBatchControllerTest`, `DefaultInputLoaderServiceTest`,
      `PromptVariationServiceTest`, `OsDownloadDirectoryServiceTest`.
- [ ] Execução simulada do `default.json` (3 objetos) gera `Σ targetCount` arquivos + 3 manifestos.
- [ ] Execução real com `default.json` reduzido (ex.: 2 objetos × 3 faixas) gera 6 faixas reais + 2 manifestos,
      com cooldown entre os lotes e mesma aba do navegador.
- [ ] Uma pessoa sem contexto consegue rodar o projeto apenas seguindo o README.

## Resultado observável
```bash
./mvnw clean verify                                  # 4 testes, BUILD SUCCESS
./mvnw spring-boot:run                               # Chrome logado na porta 9222
curl -s -X POST http://localhost:8080/api/v1/music-batch/generate | jq
# { "source": "DEFAULT_JSON_FILE", "totalBatchesQueued": 3, "totalTracksToGenerate": 90, ... }
curl -s http://localhost:8080/api/v1/music-batch/status | jq '.batches[] | {batchId, status, downloadedCount}'
ls ~/Downloads | grep -c "_track_"                   # total de faixas = Σ targetCount
ls ~/Downloads/manifest_batch-*.json                 # 1 manifesto por lote
```

## Testes
Execução da suíte completa (`./mvnw clean verify`) + roteiro manual E2E descrito acima.
