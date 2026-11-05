# Task 11 — Download real das faixas e manifesto de rastreabilidade

**Dependências:** Task 10
**Referência na especificação:** Seção 2 fases 5 e 6, RF-05, RF-06

## Objetivo
Completar o `FlowMusicBrowserService` com o **download** de cada faixa gerada para `~/Downloads`, validando a
integridade física do arquivo e gerando o manifesto do lote.

## Escopo
1. Para cada card de áudio registrado na Task 10:
   - Abrir o menu/botão de download do card (seletor `downloadButton`).
   - Capturar o download com `page.waitForDownload(() -> locator.click())`.
   - Salvar com `download.saveAs(targetPath)`, onde `targetPath = OsDownloadDirectoryService.buildTrackPath(batchId, NN, slug)`
     (`batch_<batchId>_track_<NN>_<slug>_<timestamp>.mp3`).
   - Aguardar a conclusão física: arquivo existe, sem `.crdownload`, tamanho `>= minFileSizeBytes` (10 KB),
     com timeout `fileWaitTimeoutMs`; caso contrário, falha retentável (Task 08).
   - Se a plataforma entregar outro formato (ex.: `.wav`/`.m4a`), manter a extensão real ou converter — documentar a decisão.
   - Atualizar `BatchJob.downloadedCount` e registrar `TrackMetadata` (arquivo, prompt, tamanho, horário).
   - Aplicar `jitter()` entre cliques de download.
2. Ao concluir o último download do lote, gravar `manifest_<batchId>.json` via `OsDownloadDirectoryService.writeManifest`, contendo:
   `batchId`, `executionGroupId`, `baseText`, `genreTags`, `targetCount`, `startedAt`, `finishedAt` e a lista
   `tracks[]` (`trackNumber`, `fileName`, `prompt`, `sizeBytes`, prompts substituídos).
3. Garantir que o próximo lote (multi-objeto) reutilize a mesma sessão/aba, após o cooldown.

## Critérios de aceite
- [ ] Para `targetCount = N`, exatamente N arquivos de áudio válidos (`> 10 KB`) em `~/Downloads`.
- [ ] Nenhum arquivo `.crdownload` residual.
- [ ] Manifesto lista os N arquivos com seus prompts.
- [ ] `downloadedCount` no endpoint de status acompanha os downloads em tempo real.

## Resultado observável
```bash
curl -s -X POST http://localhost:8080/api/v1/music-batch/generate \
  -H "Content-Type: application/json" \
  -d '{"baseText":"Jornada espacial silenciosa","targetCount":4,"genreTags":"ambient synthwave"}'
ls -lh ~/Downloads | grep batch-101
cat ~/Downloads/manifest_batch-101.json | jq '.tracks | length'   # 4
# Os arquivos tocam normalmente em um player de áudio.
```

## Testes
Nenhum automatizado (depende do Chrome real). As regras de nome/caminho/integridade já são cobertas pela Task 03.

