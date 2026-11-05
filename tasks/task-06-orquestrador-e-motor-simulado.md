# Task 06 — Domínio de lotes e orquestrador com motor simulado

**Dependências:** Tasks 03, 04, 05
**Referência na especificação:** Seção 2 fases 1, 4, 5 e 6, RF-02, RF-06, Seção 8 (`domain/model`, `BatchOrchestratorService`)

## Objetivo
Implementar o estado dos lotes e o `BatchOrchestratorService`, que executa 1 ou N lotes em **fila sequencial**.
Para tornar o fluxo observável sem o Chrome real, introduzir uma abstração do motor de automação com uma
implementação **simulada**.

## Escopo
1. **Modelo de domínio** (`domain/model/`):
   - `BatchStatus` (enum): `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`.
   - `BatchJob`: `batchId`, `executionGroupId`, `request`, `prompts`, `status`, `generatedCount`,
     `downloadedCount`, `tracks` (`List<TrackMetadata>`), `errorMessage`, `startedAt`, `finishedAt`.
     Contadores thread-safe (ex.: `AtomicInteger`) pois são lidos pelo endpoint de status.
   - `TrackMetadata`: `trackNumber`, `fileName`, `absolutePath`, `prompt`, `sizeBytes`, `downloadedAt`.
2. **`service/BatchJobRegistry`** (ou dentro do orquestrador): armazenamento em memória
   (`ConcurrentHashMap`) dos `BatchJob`, com geração de `batchId` (`batch-101`, `batch-102`, ...) e
   `executionGroupId` (UUID).
3. **Abstração do motor**: interface `MusicGenerationEngine` com o contrato
   `List<TrackMetadata> generateAndDownload(BatchJob job)` (ou métodos separados de geração e download),
   atualizando os contadores do `BatchJob` a cada faixa.
   - `SimulatedMusicGenerationEngine` (`@Profile("simulation")`): para cada prompt, aguarda o jitter
     configurado e grava um arquivo fake (`> 10 KB`, bytes aleatórios) em Downloads usando
     `OsDownloadDirectoryService.buildTrackPath(...)`. Pode simular de 1 a 4 faixas por submissão, como a
     plataforma real, contabilizando até atingir o `targetCount`.
   - A implementação real (`FlowMusicBrowserService`) será criada nas Tasks 09–11.
4. **`service/BatchOrchestratorService.java`**:
   - `List<BatchJob> submit(List<MusicGenerationRequest> requests)` → cria os jobs como `QUEUED`,
     enfileira e retorna imediatamente.
   - Worker assíncrono single-consumer (executor da Task 02) processa os jobs **um por vez**:
     gera os prompts (`PromptVariationService`) → chama o motor → grava o manifesto
     `manifest_<batchId>.json` (lista de arquivos ↔ prompts) → marca `COMPLETED`.
   - Métodos de consulta: `findAll()`, `findById(batchId)`.
5. Disparo temporário para observação (até a Task 07): um `ApplicationRunner` ativo apenas com a propriedade
   `automation.simulation.run-on-startup=true`, que submete o conteúdo do `default.json`. Remover/desativar após a Task 07.

## Critérios de aceite
- [ ] Lotes nunca rodam em paralelo (logs mostram início do lote 2 somente após o fim do lote 1).
- [ ] Cada lote gera exatamente `targetCount` arquivos e 1 manifesto.
- [ ] Manifesto relaciona cada arquivo `.mp3` ao prompt usado.
- [ ] Nenhuma classe do orquestrador depende diretamente do Playwright (apenas da interface).

## Resultado observável
Com um `default.json` de teste com `targetCount` pequeno (ex.: 3):
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=simulation \
  -Dspring-boot.run.arguments=--automation.simulation.run-on-startup=true
ls -la ~/Downloads | grep -E "batch-10[0-9]_track_|manifest_batch-"
# batch_batch-101_track_01_uma-noite-chuvosa_..._.mp3 ... manifest_batch-101.json
# batch_batch-102_track_01_jornada-espacial_..._.mp3 ... manifest_batch-102.json
```

## Testes
Nenhum obrigatório pela Seção 7. A validação é o resultado observável acima
(o comportamento do orquestrador é coberto indiretamente pelo `MusicBatchControllerTest` da Task 07).

