# Especificação de Engenharia de Sistemas: Automação de Geração Musical em Lote (Google Flow Music / MusicFX)

**Versão do Documento:** 3.0  
**Papel:** Analista de Sistemas Sênior & Arquiteto Java  
**Stack Principal:** Java 21 + Spring Boot 3.3+ + Playwright for Java (ou Selenium 4 com CDP) + JUnit 5 / MockMvc  

---

## 1. Visão Geral do Sistema
Este documento especifica a arquitetura, as regras de negócio, o fluxo de automação (RPA), a estratégia de testes cirúrgicos e o **Master Prompt** para a construção de um sistema backend em **Java 21 com Spring Boot 3.3+**.

O objetivo do sistema é acessar de forma automatizada a plataforma **Google Flow Music (MusicFX / Google Labs)** e, a partir de um contexto textual (`baseText`), tags de estilo (`genreTags`) e meta de quantidade (`targetCount`, padrão: 30), gerar **30 músicas instrumentais** e realizar o **download automatizado de todas as 30 faixas diretamente para a pasta padrão de Downloads do sistema operacional** do usuário.

Além de aceitar o payload JSON diretamente na chamada HTTP, o sistema possui um arquivo interno **`src/main/resources/default.json`**. Caso o endpoint seja chamado **sem corpo na requisição (body vazio)**, o sistema carrega automaticamente os objetos definidos no `default.json` e, se houver múltiplos objetos JSON nesse arquivo, executa o ciclo completo de geração e download para cada um deles sequencialmente.

---

## 2. Arquitetura e Fluxo de Execução (End-to-End)

O sistema opera de forma assíncrona orientada a fila de lotes (*Sequential Batch Queue*), dividida em 6 fases:

1. **Ingestão Flexível (Payload HTTP ou Fallback para `default.json`):**
   - O cliente dispara um `POST /api/v1/music-batch/generate`.
   - **Cenário A (Com Request Body):** Se um JSON for enviado no corpo da requisição, o sistema valida e enfileira a execução daquele objeto.
   - **Cenário B (Sem Request Body / Body Vazio):** Se a chamada `POST` não tiver corpo (`null` ou vazio), o serviço `DefaultInputLoaderService` lê o arquivo `src/main/resources/default.json` embutido no projeto.
   - **Suporte a Múltiplos Objetos:** O arquivo `default.json` pode conter 1 único objeto ou uma lista (JSON Array) com $N$ objetos. Para cada objeto encontrado, o sistema dispara uma execução na fila de processamento, gerando um `batchId` individual agrupado sob um `executionGroupId`.

2. **Expansão Semântica e Engenharia de Prompts (`PromptVariationService`):**
   - Para cada lote em execução, o serviço recebe `baseText`, `targetCount` (ex: 30) e `genreTags`.
   - Gera 30 variações sutis e coesas do texto base (combinando o cenário principal com variações de andamento, dinâmica, ambiência e instrumentação).
   - Injeta automaticamente travas obrigatórias em cada prompt para garantir áudio 100% instrumental: `", strictly instrumental, no vocals, no singing, high fidelity studio recording"`.

3. **Conexão ao Navegador Autenticado via CDP (`BrowserSessionManager`):**
   - Para evitar bloqueios de autenticação do Google (OAuth2, 2FA, Captchas e detecção de `navigator.webdriver`), o sistema **não inicializa um navegador anônimo descartável**.
   - Ele se conecta via **Chrome DevTools Protocol (CDP)** na porta `9222` a uma instância do Google Chrome real do usuário já logada na conta Google.

4. **Ciclo de Geração das 30 Faixas por Objeto (`FlowMusicAutomationEngine`):**
   - Navega até a URL do Google Flow Music / MusicFX.
   - Executa o laço de geração inserindo cada variação de prompt no campo de texto, acionando o botão de geração e aguardando de forma reativa a renderização dos cards de áudio no DOM.
   - Como cada submissão pode gerar de 1 a 4 faixas simultâneas, o sistema contabiliza individualmente cada faixa gerada até atingir o `targetCount` (30 faixas).

5. **Download Automático na Pasta Padrão do Sistema (`OsDownloadDirectoryService`):**
   - Após a geração das faixas no navegador, o sistema realiza o download das 30 músicas diretamente para a **pasta padrão de Downloads do Sistema Operacional** (Windows, Linux ou macOS), resolvida dinamicamente:
     $$\text{DownloadPath} = \text{System.getProperty("user.home")} + \text{File.separator} + \text{"Downloads"}$$
   - O sistema intercepta o fluxo de download do navegador, aguarda a conclusão física do arquivo no disco (tamanho $> 0\text{ bytes}$ e sem extensão temporária `.crdownload`), e salva com nomenclatura padronizada: `batch_{indice}_track_{01..30}_{slug}_{timestamp}.mp3`.

6. **Iteração de Múltiplos Lotes (Quando `default.json` possui mais de um objeto):**
   - Caso o `default.json` possua $N$ objetos (ex: 3 temas musicais diferentes), assim que o download da 30ª música do Objeto 1 é finalizado e seu manifesto é salvo, o orquestrador inicia automaticamente a chamada de geração e download para o Objeto 2 na mesma sessão do navegador, repetindo o processo até concluir todos os $N$ objetos:
     $$\text{Total de Músicas Baixadas} = \sum_{i=1}^{N} \text{targetCount}_i$$

---

## 3. Especificação do Payload e do Arquivo `default.json`

### 3.1. Estrutura dos Campos de Entrada (`MusicGenerationRequest`)
* `baseText` (`String`, obrigatório): Texto base descrevendo o cenário, clima, narrativa ou estilo desejado.
* `targetCount` (`Integer`, opcional, padrão = `30`): Quantidade exata de músicas instrumentais a serem geradas e baixadas para aquele texto.
* `genreTags` (`String`, opcional): Tags musicais complementares e diretrizes de estilo instrumental.

### 3.2. Arquivo Interno `src/main/resources/default.json`
Este arquivo fica versionado dentro do próprio projeto (`src/main/resources/default.json`). O leitor Jackson deve ser configurado com `DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY` habilitado, permitindo que o desenvolvedor coloque tanto um único objeto `{...}` quanto múltiplos objetos `[ {...}, {...} ]`.

**Exemplo de `src/main/resources/default.json` com múltiplos objetos:**
```json
[
  {
    "baseText": "Uma noite chuvosa em uma metrópole cyberpunk, clima investigativo e melancólico, saxofone suave ao fundo com batida lo-fi constante",
    "targetCount": 30,
    "genreTags": "instrumental, lofi hip hop, jazz noir, no vocals"
  },
  {
    "baseText": "Jornada espacial silenciosa passando por nebulosas distantes, transmitindo sensação de mistério, descoberta científica e paz profunda",
    "targetCount": 30,
    "genreTags": "instrumental, ambient synthwave, cinematic space pads, slow tempo, no vocals"
  },
  {
    "baseText": "Taverna medieval aconchegante iluminada por lareira, clima festivo porém relaxante, alaúde, violão acústico e flauta doce",
    "targetCount": 30,
    "genreTags": "instrumental, medieval folk, acoustic tavern, fantasy rpg ambient, no vocals"
  }
]
```
> **Comportamento:** Se o usuário fizer um `POST /api/v1/music-batch/generate` sem enviar body, o sistema lerá os 3 objetos acima, executará a rotina de geração e download 3 vezes seguidas e salvará $3 \times 30 = 90$ músicas instrumentais na pasta `Downloads` do computador.

---

## 4. Requisitos Funcionais Detalhados

| ID | Requisito | Descrição Técnica |
| :--- | :--- | :--- |
| **RF-01** | Endpoint com Body Opcional | Expor `POST /api/v1/music-batch/generate` com `@RequestBody(required = false)`. Se o body estiver presente, processa o objeto enviado; se estiver ausente/vazio, carrega `classpath:default.json`. |
| **RF-02** | Processamento Multi-Objeto do `default.json` | Suportar múltiplos objetos JSON no arquivo `default.json`, disparando a execução de geração e download para cada item da lista sequencialmente para não causar colisão na aba do navegador. |
| **RF-03** | Garantia de Faixas Instrumentais | Sanitizar o `baseText` e `genreTags` e concatenar sufixos mandatórios de música puramente instrumental em todas as 30 variações de cada lote. |
| **RF-04** | Automação da Interface Web (Flow Music) | Preencher o campo de prompt da plataforma Google Flow Music, submeter a geração e monitorar o DOM até que as faixas de áudio estejam prontas para reprodução/download. |
| **RF-05** | Download das 30 Músicas na Pasta Padrão | Resolver automaticamente o diretório `~/Downloads` do usuário logado no SO e baixar todas as 30 faixas geradas por lote, validando integridade no disco ($> 10\text{ KB}$). |
| **RF-06** | Manifesto de Rastreabilidade | Ao concluir o 30º download de cada lote, salvar na pasta `Downloads` um arquivo `manifest_batch_<batchId>.json` relacionando cada arquivo `.mp3` ao prompt utilizado. |
| **RF-07** | Consulta de Progresso em Tempo Real | Expor `GET /api/v1/music-batch/status` e `GET /api/v1/music-batch/status/{batchId}` informando o lote atual, quantas músicas foram geradas/baixadas e o caminho da pasta `Downloads`. |

---

## 5. Requisitos Não-Funcionais e Resiliência

### 5.1. Humanização e Prevenção de Rate-Limit (Anti-Bot)
- **Jitter Temporal:** Entre cada submissão de prompt e clique de download, aplicar um atraso aleatório entre $4.000\text{ ms}$ e $9.000\text{ ms}$ via `ThreadLocalRandom`.
- **Pausa entre Lotes (Multi-Objeto):** Quando o `default.json` possuir mais de um objeto, aplicar um intervalo de resfriamento (*cooldown*) configurável (ex: $15.000\text{ ms}$) entre o término de um lote de 30 músicas e o início do próximo lote.
- **Inserção Reativa no DOM:** Disparar eventos nativos de teclado/input para que os componentes web do Google reconheçam a habilitação do botão de gerar.

### 5.2. Tolerância a Falhas (Retry & Self-Healing)
- **Retry com Backoff Exponencial:** Se uma geração ou download falhar por instabilidade de rede ou timeout de renderização, retentar até 3 vezes ($2^k \text{ segundos}$) antes de substituir a variação de prompt.
- **Isolamento de Lotes:** Se o `default.json` contiver 3 objetos e ocorrer um erro irrecuperável no objeto 2, o sistema registra a falha no relatório de status e avança para o objeto 3 sem abortar toda a fila.

---

## 6. Contratos da API REST

### 6.1. Modo 1: Chamada COM Corpo na Requisição (Execução Única)
**`POST /api/v1/music-batch/generate`**
```json
{
  "baseText": "Uma noite chuvosa em uma metrópole cyberpunk, clima investigativo e melancólico, saxofone suave ao fundo com batida lo-fi constante",
  "targetCount": 30,
  "genreTags": "instrumental, lofi hip hop, jazz noir, no vocals"
}
```

### 6.2. Modo 2: Chamada SEM Corpo na Requisição (Fallback para `default.json`)
**`POST /api/v1/music-batch/generate`** *(Sem Request Body ou Body vazio)*
```bash
curl -X POST http://localhost:8080/api/v1/music-batch/generate
```

**Resposta HTTP 202 (Accepted) para chamada sem body (ex: 3 objetos no `default.json`):**
```json
{
  "source": "DEFAULT_JSON_FILE",
  "totalBatchesQueued": 3,
  "totalTracksToGenerate": 90,
  "downloadDestinationPath": "C:\\Users\\Usuario\\Downloads",
  "batches": [
    {
      "batchId": "batch-101",
      "baseTextPreview": "Uma noite chuvosa em uma metrópole cyberpunk...",
      "targetCount": 30,
      "status": "RUNNING"
    },
    {
      "batchId": "batch-102",
      "baseTextPreview": "Jornada espacial silenciosa passando por nebulosas...",
      "targetCount": 30,
      "status": "QUEUED"
    },
    {
      "batchId": "batch-103",
      "baseTextPreview": "Taverna medieval aconchegante iluminada por lareira...",
      "targetCount": 30,
      "status": "QUEUED"
    }
  ]
}
```

---

## 7. Estratégia de Testes Automatizados (Apenas nos Pontos Críticos)

Para manter o código limpo, rápido de compilar e sem testes supérfluos (não testar getters/setters, DTOs simples ou abrir o navegador Chrome real em testes de build), **os testes automatizados devem ser implementados estritamente nos 4 pontos críticos abaixo**:

1. **`MusicBatchControllerTest` (`@WebMvcTest` + `MockMvc`):**
   - **Teste 1 (Com Body):** Envia um `POST /api/v1/music-batch/generate` com um JSON contendo `baseText`, `targetCount` e `genreTags` e verifica se o `BatchOrchestratorService` foi chamado exatamente 1 vez com os dados do payload (e que o `DefaultInputLoaderService` **não** foi acionado).
   - **Teste 2 (Sem Body -> Fallback Multi-Objeto):** Envia um `POST /api/v1/music-batch/generate` sem corpo (`content` vazio), simula o retorno de 2 objetos vindos do `DefaultInputLoaderService` e verifica se a API responde `202 Accepted` acionando a execução para os 2 lotes.
2. **`DefaultInputLoaderServiceTest` (Teste Unitário com `ObjectMapper`):**
   - Verifica se a leitura do arquivo `default.json` funciona tanto quando o JSON contém uma lista de múltiplos objetos (`[ {...}, {...} ]`) quanto quando contém apenas um único objeto (`{...}`), preenchendo `targetCount = 30` caso o campo seja omitido.
3. **`PromptVariationServiceTest` (Teste Unitário):**
   - Verifica se, ao passar um `baseText`, `targetCount = 30` e `genreTags`, o método retorna exatamente 30 prompts distintos e garante que todos os 30 contêm as tags `genreTags` e a restrição `"strictly instrumental, no vocals"`.
4. **`OsDownloadDirectoryServiceTest` (Teste Unitário):**
   - Verifica se o método de resolução da pasta de download aponta corretamente para `Paths.get(System.getProperty("user.home"), "Downloads")`.

---

## 8. Estrutura de Pacotes do Projeto Spring Boot

```text
src/
├── main/
│   ├── java/com/automation/flowmusic/
│   │   ├── FlowMusicAutomationApplication.java
│   │   ├── config/
│   │   │   ├── AsyncConfig.java                  # Virtual Threads (Java 21)
│   │   │   ├── JacksonConfig.java                # ACCEPT_SINGLE_VALUE_AS_ARRAY
│   │   │   └── AutomationProperties.java         # @ConfigurationProperties (seletores, URLs, timeouts)
│   │   ├── controller/
│   │   │   └── MusicBatchController.java         # POST /generate (body opcional) e GET /status
│   │   ├── domain/
│   │   │   ├── dto/
│   │   │   │   ├── MusicGenerationRequest.java   # Record: baseText, targetCount, genreTags
│   │   │   │   ├── BatchSubmissionResponse.java  # Resposta com lista de lotes enfileirados
│   │   │   │   └── BatchStatusResponse.java      # Progresso detalhado de cada lote
│   │   │   └── model/
│   │   │       ├── BatchJob.java                 # Estado de execução de cada lote de 30 músicas
│   │   │       └── TrackMetadata.java            # Metadados do arquivo MP3 salvo em Downloads
│   │   ├── service/
│   │   │   ├── DefaultInputLoaderService.java    # Lê src/main/resources/default.json (1 ou N objetos)
│   │   │   ├── BatchOrchestratorService.java     # Executa 1 ou N lotes em fila sequencial
│   │   │   ├── PromptVariationService.java       # Gera as 30 variações instrumentais por lote
│   │   │   ├── FlowMusicBrowserService.java      # Playwright/Selenium via CDP (geração + download)
│   │   │   └── OsDownloadDirectoryService.java   # Resolução de ~/Downloads e escrita do manifesto
│   │   └── exception/
│   │       ├── AutomationException.java
│   │       └── GlobalExceptionHandler.java
│   └── resources/
│       ├── application.yml                       # Seletores CSS/ARIA, timeouts, porta CDP 9222
│       └── default.json                          # Arquivo JSON padrão (com múltiplos objetos)
└── test/
    └── java/com/automation/flowmusic/
        ├── controller/
        │   └── MusicBatchControllerTest.java     # Teste de contrato (com body vs. sem body)
        └── service/
            ├── DefaultInputLoaderServiceTest.java# Teste de leitura de 1 ou N objetos do default.json
            ├── PromptVariationServiceTest.java   # Teste das 30 variações e travas instrumentais
            └── OsDownloadDirectoryServiceTest.java# Teste da resolução da pasta Downloads do SO
```

---

## 9. Master Prompt (Pronto para Gerar o Código Completo em IA)

Copie o bloco abaixo e utilize-o na IA codificadora para gerar todo o código do projeto seguindo rigorosamente esta especificação:

```text
Atue como um Arquiteto de Software Java Sênior e Especialista em Automação Web (RPA).
Desenvolva o código-fonte completo, funcional e pronto para produção de uma aplicação em Java 21 com Spring Boot 3.3+ que automatiza a geração e o download de 30 músicas instrumentais no site do Google Flow Music (MusicFX / Labs) a partir de um texto base.

### REQUISITOS OBRIGATÓRIOS DO SISTEMA:

1. STACK TECNOLÓGICA:
   - Java 21 (com Virtual Threads habilitadas para processamento assíncrono).
   - Spring Boot 3.3+ (Spring Web, Validation, Lombok, ConfigurationProperties, Jackson).
   - Automação Web: Playwright for Java conectando-se via Chrome DevTools Protocol (CDP na porta 9222) a uma instância já autenticada do Google Chrome.

2. ENDPOINT REST E FALLBACK PARA `default.json` (COM SUPORTE A MÚLTIPLOS OBJETOS):
   - Crie o endpoint `POST /api/v1/music-batch/generate` recebendo `@RequestBody(required = false) MusicGenerationRequest request`.
   - O payload `MusicGenerationRequest` deve conter:
     - `baseText` (String): texto base para inspiração das músicas.
     - `targetCount` (Integer): quantidade de músicas a gerar (se nulo ou <= 0, assumir 30 como padrão).
     - `genreTags` (String): tags de estilo musical (ex: "instrumental, lofi hip hop, jazz noir, no vocals").
   - Crie na pasta `src/main/resources/default.json` um arquivo JSON padrão contendo uma lista (array) com pelo menos 2 objetos de exemplo no formato `{ "baseText": "...", "targetCount": 30, "genreTags": "..." }`.
   - REGRA CRÍTICA DE FALLBACK:
     - Se a requisição HTTP `POST /api/v1/music-batch/generate` vier COM corpo válido (`request != null` e `baseText` preenchido), o sistema deve executar a geração para esse único objeto enviado.
     - Se a requisição HTTP vier SEM corpo (`request == null` ou `baseText` vazio), o sistema deve ler automaticamente o arquivo `src/main/resources/default.json` através do `DefaultInputLoaderService` (usando Jackson com `ACCEPT_SINGLE_VALUE_AS_ARRAY`).
     - Se o `default.json` tiver mais de um objeto JSON, o `BatchOrchestratorService` deve iterar sobre todos os objetos e executar o fluxo de geração e download para cada um deles sequencialmente (ou seja, se houver 3 objetos no `default.json`, ele executará 3 lotes de 30 músicas em sequência).

3. MOTOR DE VARIAÇÃO DE PROMPTS (30 MÚSICAS INSTRUMENTAIS POR OBJETO):
   - Crie o `PromptVariationService` que recebe o `MusicGenerationRequest` e gera `targetCount` (30) variações semânticas baseadas no `baseText` e combinadas com `genreTags`.
   - Garanta que todas as variações terminem com a trava obrigatória: `", strictly instrumental, no vocals, no lyrics, no singing, high quality studio audio"`.

4. AUTOMAÇÃO DO GOOGLE FLOW MUSIC E DOWNLOAD NA PASTA PADRÃO DO SO:
   - Conecte ao Chrome via CDP (`http://localhost:9222`) e acesse a URL do Google Flow Music configurada no `application.yml`.
   - Para cada lote, insira os prompts gerados, acione o botão de geração, aguarde o carregamento dos cards de áudio no DOM e contabilize as faixas geradas até atingir `targetCount` (30 músicas).
   - Após gerar as músicas, baixe todas as 30 faixas diretamente para a pasta padrão de Downloads do Sistema Operacional do usuário (`Paths.get(System.getProperty("user.home"), "Downloads")`), implementada na classe `OsDownloadDirectoryService`.
   - Utilize `page.waitForDownload()` e `download.saveAs(targetPath)` do Playwright para salvar cada música com o padrão `batch_<batchId>_track_<01..30>_<timestamp>.mp3`, validando que o arquivo existe e tem tamanho > 0 bytes.
   - Ao concluir as 30 músicas de cada objeto, gere também um arquivo `manifest_<batchId>.json` na pasta `Downloads` detalhando os arquivos baixados e seus prompts.
   - Implemente Jitter (pausas aleatórias de 4 a 9 segundos) entre ações e Retry (até 3 tentativas) em caso de falha em uma faixa.
   - Externalize todos os seletores CSS/ARIA no `application.yml` via `@ConfigurationProperties`.

5. TESTES AUTOMATIZADOS (APENAS NOS LUGARES NECESSÁRIOS):
   - NÃO crie testes para classes triviais, getters/setters ou testes que exijam abrir o Chrome real.
   - Crie APENAS os seguintes testes essenciais com JUnit 5 e Mockito/MockMvc:
     a) `MusicBatchControllerTest`: testa a chamada `POST /api/v1/music-batch/generate` enviando JSON no body (executa 1 lote) e enviando requisição SEM body (aciona o fallback do `default.json` com múltiplos objetos e enfileira múltiplas execuções).
     b) `DefaultInputLoaderServiceTest`: testa o carregamento e parsing do `default.json` contendo múltiplos objetos JSON e garante o valor padrão `targetCount = 30`.
     c) `PromptVariationServiceTest`: testa se o serviço gera exatamente 30 prompts contendo `genreTags` e a trava `"strictly instrumental, no vocals"`.
     d) `OsDownloadDirectoryServiceTest`: testa se o caminho resolvido corresponde à pasta `Downloads` dentro de `user.home`.

### ENTREGÁVEIS ESPERADOS:
1. `pom.xml` completo.
2. `src/main/resources/application.yml` e `src/main/resources/default.json` (com múltiplos objetos).
3. Todas as classes Java de produção completas e sem omissões.
4. As 4 classes de teste enxutas e funcionais nos pontos críticos listados.
5. Instruções de como iniciar o Chrome com `--remote-debugging-port=9222` e como disparar o `POST` com e sem corpo via `curl`.
```