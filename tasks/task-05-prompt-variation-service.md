# Task 05 — Motor de variação de prompts instrumentais

**Dependências:** Task 04
**Referência na especificação:** Seção 2 fase 2, RF-03, Seção 7 item 3, Seção 9 item 3

## Objetivo
Implementar o `PromptVariationService`, que gera `targetCount` variações sutis, coesas e **distintas** do
`baseText`, combinadas com `genreTags` e sempre terminando com a trava instrumental.

## Escopo
1. **`service/PromptVariationService.java`**:
   - `List<String> generateVariations(MusicGenerationRequest request)`.
   - Sanitizar `baseText` e `genreTags` (trim, colapsar espaços, remover quebras de linha e caracteres de controle).
   - Combinar o cenário base com dimensões de variação: **andamento** (slow, mid-tempo, upbeat...),
     **dinâmica** (soft, building, punchy...), **ambiência** (rainy room, wide reverb, intimate...) e
     **instrumentação** (piano lead, warm pads, plucked strings...).
   - Garantir unicidade: combinar dimensões de forma determinística (ex.: produto cartesiano rotacionado)
     para que N prompts nunca se repitam, mesmo para N > 30.
   - Formato final: `<baseText>, <variação>, <genreTags><instrumentalSuffix>`.
   - Sufixo lido de `automation.generation.instrumentalSuffix`
     (`", strictly instrumental, no vocals, no lyrics, no singing, high quality studio audio"`).
   - Respeitar limite de tamanho do prompt (configurável), truncando o `baseText` se necessário — nunca o sufixo.
2. As listas de variação podem ficar em constantes ou no `application.yml`.

## Critérios de aceite
- [ ] `targetCount = 30` → exatamente 30 prompts, todos distintos.
- [ ] Todos contêm o `genreTags` e terminam com a trava instrumental.
- [ ] `genreTags` nulo/vazio não gera vírgulas duplicadas nem `"null"` no texto.

## Resultado observável
- `./mvnw test -Dtest=PromptVariationServiceTest` verde.
- (Opcional) Log em DEBUG listando os prompts gerados ao iniciar um lote (visível a partir da Task 06).

## Testes
**`PromptVariationServiceTest`** (unitário) — obrigatório pela Seção 7:
- Dado `baseText`, `targetCount = 30` e `genreTags`, retorna **exatamente 30** prompts **distintos**
  (`new HashSet<>(prompts).size() == 30`).
- Todos os 30 contêm `genreTags` e a string `"strictly instrumental, no vocals"`.

