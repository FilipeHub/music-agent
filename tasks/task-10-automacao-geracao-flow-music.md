# Task 10 — Automação da geração no Google Flow Music

**Dependências:** Tasks 08 e 09
**Referência na especificação:** Seção 2 fase 4, RF-04, Seção 5.1 (inserção reativa no DOM)

## Objetivo
Implementar a parte de **geração** do `FlowMusicBrowserService` (implementação real de `MusicGenerationEngine`):
inserir cada prompt, acionar a geração, aguardar reativamente os cards de áudio e contabilizar as faixas até o `targetCount`.

## Escopo
1. **Mapeamento de seletores**: inspecionar a interface real do Flow Music/MusicFX e preencher no
   `application.yml` os seletores (preferir ARIA/role/texto a classes CSS geradas):
   `promptInput`, `generateButton`, `audioCard`, `loadingIndicator`, `downloadButton`.
2. **`service/FlowMusicBrowserService.java`** (perfil padrão):
   - Obter a página via `BrowserSessionManager`.
   - Para cada prompt do lote:
     - Limpar o campo e digitar com eventos nativos (`locator.fill()` + `pressSequentially`/`keyboard.type`
       com pequeno delay) para que os web components reconheçam o input e habilitem o botão.
     - Aguardar o botão habilitado e clicar.
     - Aguardar reativamente (`waitFor`, `page.waitForFunction`) até que **novos** cards de áudio apareçam
       (comparar contagem antes/depois) ou o indicador de carregamento desapareça, com timeout `generationTimeoutMs`.
     - Contabilizar cada faixa nova (1 a 4 por submissão) em `BatchJob.generatedCount`, parando ao atingir `targetCount`.
     - Guardar a referência (índice/locator) de cada card e o prompt que o originou, para a Task 11.
     - Aplicar `jitter()` entre submissões.
   - Envolver cada submissão no `RetryExecutor` da Task 08 (timeout de renderização = falha retentável).
   - Detectar mensagens de erro/limite da plataforma (ex.: quota, conteúdo bloqueado) e tratá-las como falha
     do prompt (substituição) ou do lote (rate limit persistente).
3. Até a Task 11, o download pode ficar como `TODO`/no-op, mas a contagem de geração deve funcionar.

## Critérios de aceite
- [ ] Com `targetCount` pequeno (ex.: 4), os prompts são digitados e submetidos visivelmente no Chrome.
- [ ] O status mostra `generatedCount` atingindo exatamente o `targetCount` (faixas excedentes da última submissão são ignoradas).
- [ ] Timeout de renderização dispara retry conforme a Task 08.
- [ ] Nenhum seletor hardcoded no código Java.

## Resultado observável
```bash
# Chrome aberto com --remote-debugging-port=9222 e logado (ver Task 09)
./mvnw spring-boot:run
curl -s -X POST http://localhost:8080/api/v1/music-batch/generate \
  -H "Content-Type: application/json" \
  -d '{"baseText":"Taverna medieval aconchegante","targetCount":4,"genreTags":"medieval folk"}'
# Visual: o prompt é digitado no Flow Music, o botão é clicado e os cards de áudio surgem.
curl -s http://localhost:8080/api/v1/music-batch/status/batch-101 | jq '.generatedCount'   # 4
```

## Testes
Nenhum automatizado (depende do Chrome real). Validação manual pelo cenário acima.

