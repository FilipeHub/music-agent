# Task 03 — Serviço de diretório de Downloads do SO

**Dependências:** Task 02
**Referência na especificação:** Seção 2 fase 5, RF-05, RF-06, Seção 7 item 4

## Objetivo
Implementar o `OsDownloadDirectoryService`, responsável por resolver a pasta padrão de Downloads do
usuário (Windows, Linux, macOS), montar nomes de arquivos padronizados e gravar o manifesto do lote.

## Escopo
1. **`service/OsDownloadDirectoryService.java`**:
   - `Path resolveDownloadDirectory()` → `Paths.get(System.getProperty("user.home"), "Downloads")`;
     cria o diretório caso não exista.
   - `Path buildTrackPath(String batchId, int trackNumber, String slug)` → nome no padrão
     `batch_<batchId>_track_<NN>_<slug>_<timestamp>.mp3` (NN com 2 dígitos, slug sanitizado: minúsculas,
     sem acentos, apenas `[a-z0-9-]`, máx. ~40 caracteres).
   - `Path writeManifest(String batchId, Object manifest)` → grava `manifest_<batchId>.json`
     (JSON identado via `ObjectMapper`) na pasta de Downloads.
   - `boolean isFileComplete(Path file, long minBytes)` → existe, não termina em `.crdownload` e tamanho `>= minBytes`.
2. Logar no startup o caminho de Downloads resolvido.

## Critérios de aceite
- [ ] O caminho resolvido é exatamente `user.home/Downloads`, independente do SO.
- [ ] Nomes gerados seguem o padrão e não contêm caracteres inválidos para Windows/Linux/macOS.
- [ ] Manifesto é gravado como JSON válido.

## Resultado observável
- `./mvnw test -Dtest=OsDownloadDirectoryServiceTest` verde.
- No startup: `Download directory resolved to: /home/<user>/Downloads`.

## Testes
**`OsDownloadDirectoryServiceTest`** (unitário, JUnit 5) — obrigatório pela Seção 7:
- Verifica que `resolveDownloadDirectory()` é igual a `Paths.get(System.getProperty("user.home"), "Downloads")`.
- (Recomendado) Sobrescrever `user.home` para um `@TempDir` durante o teste, para não tocar na pasta real
  do usuário, e restaurar o valor original ao final.

