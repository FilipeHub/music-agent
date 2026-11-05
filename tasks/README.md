# Plano de Tasks — Automação Google Flow Music (MusicFX)

Fonte: [especifica_o_t_cnica_e_prompt_automa_o_google_flow_music.md](../especifica_o_t_cnica_e_prompt_automa_o_google_flow_music.md)

Cada task entrega um **resultado observável** (algo que pode ser executado/visto) e, quando a
especificação exige, **testes automatizados**. As tasks são incrementais e seguem a ordem numérica; a única
exceção é a Task 09, que depende apenas da Task 02 e pode ser feita em paralelo às Tasks 03–08.

| # | Task | Status | Resultado observável | Testes |
| :-- | :--- | :--- | :--- | :--- |
| 01 | [Configuração inicial do projeto](task-01-configuracao-inicial.md) | ✅ Concluída | App Spring Boot sobe na porta 8080 e `mvn verify` passa | — |
| 02 | [Configurações externalizadas e infraestrutura](task-02-configuracoes-e-infraestrutura.md) | ⏳ Pendente | Log de startup exibe propriedades carregadas e virtual threads ativas | — |
| 03 | [Serviço de diretório de Downloads do SO](task-03-os-download-directory-service.md) | ⏳ Pendente | Teste verde + caminho de Downloads logado no startup | `OsDownloadDirectoryServiceTest` |
| 04 | [Leitor do `default.json` (1 ou N objetos)](task-04-default-input-loader.md) | ⏳ Pendente | Teste verde + log "N objetos carregados do default.json" | `DefaultInputLoaderServiceTest` |
| 05 | [Motor de variação de prompts instrumentais](task-05-prompt-variation-service.md) | ⏳ Pendente | Teste verde gerando 30 prompts distintos e com travas | `PromptVariationServiceTest` |
| 06 | [Domínio de lotes e orquestrador com motor simulado](task-06-orquestrador-e-motor-simulado.md) | ⏳ Pendente | Com perfil `simulation`, arquivos fake `.mp3` + manifesto aparecem em `~/Downloads` | — |
| 07 | [API REST (POST generate / GET status) e exceções](task-07-api-rest-e-excecoes.md) | ⏳ Pendente | `curl` com e sem body retorna `202` e o status evolui em tempo real | `MusicBatchControllerTest` |
| 08 | [Resiliência: jitter, retry, cooldown e isolamento](task-08-resiliencia.md) | ⏳ Pendente | Com falhas injetadas no simulador, logs mostram retries e o lote seguinte continua | — |
| 09 | [Conexão CDP ao Chrome autenticado](task-09-conexao-cdp-chrome.md) | ⏳ Pendente | App conecta ao Chrome na porta 9222 e abre a URL do Flow Music | — (manual) |
| 10 | [Automação da geração no Flow Music](task-10-automacao-geracao-flow-music.md) | ⏳ Pendente | Prompts são digitados/submetidos no Chrome real e faixas são contabilizadas | — (manual) |
| 11 | [Download real das faixas e manifesto](task-11-download-e-manifesto.md) | ⏳ Pendente | `.mp3` reais + `manifest_<batchId>.json` em `~/Downloads` | — (manual) |
| 12 | [Documentação e validação ponta a ponta](task-12-documentacao-e-validacao-e2e.md) | ⏳ Pendente | README completo e execução E2E com `default.json` (N lotes) | Suíte completa |

## Ambiente de desenvolvimento

| Item | Valor |
| :--- | :--- |
| JDK | Java 21 (`~/.sdkman/candidates/java/21.0.2-amzn`). O `java` padrão do sistema é o 11, então use `sdk use java 21.0.2-amzn` ou exporte `JAVA_HOME` antes do `./mvnw` |
| Build | Maven Wrapper (`./mvnw`) → Maven 3.9.9 |
| Spring Boot | 3.5.16 |
| Playwright for Java | 1.64.0 |
| Porta HTTP | `${SERVER_PORT:8080}`. A porta 8080 local está ocupada pelo container Docker `evolution_api`, então use `SERVER_PORT=8081` |

> Nos exemplos de `curl` das tasks, troque `8080` pela porta efetivamente usada (ex.: `8081`).

## Decisões sobre inconsistências da especificação

A especificação possui alguns pontos divergentes entre as seções. As tasks adotam:

| Tema | Divergência | Decisão adotada |
| :--- | :--- | :--- |
| Sufixo instrumental | Seção 2: `strictly instrumental, no vocals, no singing, high fidelity studio recording` / Seção 9: `strictly instrumental, no vocals, no lyrics, no singing, high quality studio audio` | Usar o da **Seção 9** (Master Prompt), configurável em `application.yml` |
| Nome do arquivo | `batch_{indice}_track_{NN}_{slug}_{timestamp}.mp3` vs `batch_<batchId>_track_<NN>_<timestamp>.mp3` | `batch_<batchId>_track_<NN>_<slug>_<timestamp>.mp3` |
| Nome do manifesto | `manifest_batch_<batchId>.json` vs `manifest_<batchId>.json` | `manifest_<batchId>.json` |
| Integridade do arquivo | `> 0 bytes` vs `> 10 KB` | `> 10 KB` (RF-05, mais restritivo; configurável) |
| Ferramenta de automação | Playwright ou Selenium 4 | **Playwright for Java** |
| Versão do Spring Boot | "3.3+" | **3.5.16** (atende "3.3+"; a linha 3.3.x não tem mais suporte OSS) |

> O **motor simulado** (perfil `simulation`, Task 06) não está na especificação original; ele foi
> incluído para que o fluxo completo (API → orquestrador → arquivos em Downloads) seja observável
> e testável sem depender do Chrome real ou da conta Google.
