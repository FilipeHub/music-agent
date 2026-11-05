# Task 08 — Resiliência: jitter, retry com backoff, cooldown e isolamento de lotes

**Dependências:** Task 07
**Referência na especificação:** Seção 5.1 e 5.2

## Objetivo
Adicionar humanização (anti-bot) e tolerância a falhas ao fluxo de execução, validando tudo com o motor
simulado antes de integrar o navegador real.

## Escopo
1. **`service/HumanDelayService`** (ou utilitário equivalente):
   - `void jitter()` → `Thread.sleep(ThreadLocalRandom.current().nextLong(jitterMinMs, jitterMaxMs + 1))`.
   - Aplicado entre cada submissão de prompt e entre cada clique de download.
2. **Retry com backoff exponencial** (`RetryExecutor` ou similar):
   - Até `automation.retry.maxAttempts` (3) tentativas, aguardando `2^k` segundos entre elas.
   - Esgotadas as tentativas, **substituir a variação de prompt** (gerar uma nova variação via
     `PromptVariationService`) e continuar o lote; registrar a substituição no log e no manifesto.
   - Definir um limite máximo de substituições por lote; ultrapassado, o lote é marcado `FAILED`.
3. **Cooldown entre lotes**: aguardar `automation.timing.batchCooldownMs` entre o fim de um lote e o início do próximo
   (não aplicar após o último lote).
4. **Isolamento de lotes**: exceção irrecuperável em um lote → `status = FAILED`, `errorMessage` preenchido,
   e o orquestrador **avança para o próximo lote** sem abortar a fila.
5. **Injeção de falhas no simulador**: propriedade `automation.simulation.failure-rate` (0.0–1.0) e
   `automation.simulation.fail-batch-index` (força falha irrecuperável em um lote específico) para observar o comportamento.

## Critérios de aceite
- [ ] Logs mostram atrasos aleatórios dentro da faixa configurada.
- [ ] Falha transitória → logs `tentativa 1/3`, `2/3` com esperas de 2s, 4s...; depois sucesso ou substituição de prompt.
- [ ] Com 3 lotes e falha forçada no lote 2: lote 1 `COMPLETED`, lote 2 `FAILED`, lote 3 `COMPLETED`.
- [ ] Cooldown visível no log entre os lotes.

## Resultado observável
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=simulation \
  -Dspring-boot.run.arguments="--automation.simulation.failure-rate=0.3 --automation.simulation.fail-batch-index=2"
curl -s -X POST http://localhost:8080/api/v1/music-batch/generate | jq
curl -s http://localhost:8080/api/v1/music-batch/status | jq '.batches[] | {batchId, status, errorMessage}'
# batch-101 COMPLETED | batch-102 FAILED (mensagem) | batch-103 COMPLETED
```

## Testes
Nenhum obrigatório pela Seção 7. A validação é feita pelo cenário de falhas injetadas acima.
(Opcional, se a equipe julgar crítico: um teste unitário do `RetryExecutor` com `backoffBaseSeconds` zerado.)
