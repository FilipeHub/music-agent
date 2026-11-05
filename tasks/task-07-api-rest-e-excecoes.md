# Task 07 — API REST (POST generate / GET status) e tratamento de exceções

**Dependências:** Task 06
**Referência na especificação:** RF-01, RF-02, RF-07, Seção 6, Seção 7 item 1

## Objetivo
Expor os endpoints REST com **body opcional** e fallback para o `default.json`, além dos endpoints de
progresso em tempo real e de um tratamento de erros padronizado.

## Escopo
1. **DTOs de resposta** (`domain/dto/`):
   - `BatchSubmissionResponse`: `source` (`REQUEST_BODY` | `DEFAULT_JSON_FILE`), `executionGroupId`,
     `totalBatchesQueued`, `totalTracksToGenerate`, `downloadDestinationPath`,
     `batches[]` (`batchId`, `baseTextPreview` — primeiros ~50 caracteres + `...`, `targetCount`, `status`).
   - `BatchStatusResponse`: `batchId`, `status`, `targetCount`, `generatedCount`, `downloadedCount`,
     `downloadDestinationPath`, `errorMessage`, `startedAt`, `finishedAt`, `tracks[]`.
2. **`controller/MusicBatchController.java`**:
   - `POST /api/v1/music-batch/generate` com `@RequestBody(required = false) MusicGenerationRequest request`:
     - `request != null` e `baseText` preenchido → processa somente esse objeto (`source = REQUEST_BODY`).
     - `request == null` ou `baseText` vazio → `DefaultInputLoaderService.loadDefaults()` (`source = DEFAULT_JSON_FILE`).
     - Responde **`202 Accepted`** no formato da Seção 6.2.
   - `GET /api/v1/music-batch/status` → objeto com `currentBatchId`, `downloadDestinationPath` e
     `batches[]` (lista de `BatchStatusResponse` de todos os lotes).
   - `GET /api/v1/music-batch/status/{batchId}` → detalhe de um lote; `404` se não existir.
3. **`exception/GlobalExceptionHandler.java`** (`@RestControllerAdvice`):
   - `AutomationException` → `500`/`503` com corpo `ProblemDetail` (RFC 7807).
   - JSON malformado (`HttpMessageNotReadableException`) → `400`.
   - Lote inexistente → `404`.
4. Remover/desativar o disparo temporário de startup da Task 06.

## Critérios de aceite
- [ ] POST com body → `202`, `totalBatchesQueued = 1`, `source = REQUEST_BODY`.
- [ ] POST sem body (ou `{}`/`baseText` vazio) → `202`, `source = DEFAULT_JSON_FILE`, N lotes enfileirados.
- [ ] O primeiro lote aparece como `RUNNING` e os demais como `QUEUED` (ou todos `QUEUED` no instante da resposta).
- [ ] GET status mostra `generatedCount`/`downloadedCount` crescendo durante a execução.
- [ ] Erros retornam `ProblemDetail` consistente.

## Resultado observável
Com o perfil `simulation`:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=simulation

# Modo 1 — com body
curl -s -X POST http://localhost:8080/api/v1/music-batch/generate \
  -H "Content-Type: application/json" \
  -d '{"baseText":"Noite chuvosa cyberpunk","targetCount":3,"genreTags":"lofi, jazz noir"}' | jq

# Modo 2 — sem body (fallback default.json)
curl -s -X POST http://localhost:8080/api/v1/music-batch/generate | jq

# Progresso
watch -n 1 'curl -s http://localhost:8080/api/v1/music-batch/status | jq'
curl -s http://localhost:8080/api/v1/music-batch/status/batch-101 | jq
```

## Testes
**`MusicBatchControllerTest`** (`@WebMvcTest(MusicBatchController.class)` + `MockMvc` + `@MockBean`) — obrigatório pela Seção 7:
- **Teste 1 (Com Body):** POST com JSON (`baseText`, `targetCount`, `genreTags`) → `202`; verifica que
  `BatchOrchestratorService` foi chamado **exatamente 1 vez** com os dados do payload (usar `ArgumentCaptor`)
  e que `DefaultInputLoaderService` **nunca** foi acionado (`verifyNoInteractions`).
- **Teste 2 (Sem Body → Fallback Multi-Objeto):** POST com `content` vazio; mock do `DefaultInputLoaderService`
  retorna 2 objetos → `202`, `totalBatchesQueued = 2` e orquestrador acionado com os 2 lotes.
