# Task 04 — Leitor do `default.json` (1 ou N objetos)

**Dependências:** Task 02
**Referência na especificação:** Seção 2 fase 1, Seção 3, RF-01, RF-02, Seção 7 item 2

## Objetivo
Criar o DTO de entrada, o arquivo `default.json` e o `DefaultInputLoaderService`, capaz de ler tanto um
único objeto `{...}` quanto uma lista `[ {...}, {...} ]`, aplicando `targetCount = 30` quando omitido.

## Escopo
1. **`domain/dto/MusicGenerationRequest.java`** — `record` com `baseText` (`@NotBlank` quando usado no body),
   `targetCount` (`Integer`), `genreTags` (`String`). Método utilitário para normalizar:
   `targetCount == null || targetCount <= 0` → `30` (valor de `automation.generation.defaultTargetCount`).
2. **`src/main/resources/default.json`** — array com os 3 objetos de exemplo da Seção 3.2
   (cyberpunk lo-fi, jornada espacial, taverna medieval).
3. **`service/DefaultInputLoaderService.java`**:
   - `List<MusicGenerationRequest> loadDefaults()` lendo `classpath:default.json` com o `ObjectMapper`
     configurado (`ACCEPT_SINGLE_VALUE_AS_ARRAY`).
   - Aplicar normalização do `targetCount`.
   - Ignorar (com log de aviso) objetos com `baseText` vazio.
   - Lançar `AutomationException` (criar a classe em `exception/` nesta task) se o arquivo não existir,
     estiver vazio ou for JSON inválido.
   - Sobrecarga/construtor que permita ler de um `Resource` arbitrário (facilita o teste).
4. Logar no startup: `default.json carregado: N objeto(s), total de M faixas`.

## Critérios de aceite
- [ ] Arquivo com array de 3 objetos → lista com 3 itens.
- [ ] Arquivo com um único objeto → lista com 1 item.
- [ ] Objeto sem `targetCount` → `targetCount = 30`.
- [ ] JSON inválido → `AutomationException` com mensagem clara.

## Resultado observável
- `./mvnw test -Dtest=DefaultInputLoaderServiceTest` verde.
- Startup exibe: `default.json carregado: 3 objeto(s), total de 90 faixas`.

## Testes
**`DefaultInputLoaderServiceTest`** (unitário com `ObjectMapper` real) — obrigatório pela Seção 7:
- Cenário lista: JSON `[ {...}, {...} ]` → 2 objetos com campos corretos.
- Cenário objeto único: JSON `{...}` → 1 objeto.
- Cenário sem `targetCount` → valor `30`.
- Usar `ByteArrayResource`/arquivos em `src/test/resources` para os cenários.

