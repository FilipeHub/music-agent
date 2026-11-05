# Task 01 — Configuração inicial do projeto

**Status:** ✅ Concluída (2026-11-05)
**Dependências:** nenhuma
**Referência na especificação:** Cabeçalho (Stack), Seção 8 (Estrutura de Pacotes), Seção 9 item 1

## Objetivo
Criar o esqueleto do projeto Java 21 + Spring Boot 3.3+ com Maven, pronto para compilar, rodar e testar.

## Escopo
1. Criar `pom.xml` com:
   - `spring-boot-starter-parent` 3.3+ (adotado **3.5.16** — a linha 3.3.x não tem mais suporte OSS)
   - `java.version` = 21
   - Dependências: `spring-boot-starter-web`, `spring-boot-starter-validation`, `lombok`,
     `spring-boot-configuration-processor`, `com.microsoft.playwright:playwright` (1.64.0),
     `spring-boot-starter-test` (escopo `test`).
   - Plugin `spring-boot-maven-plugin`.
2. Adicionar o Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/`).
3. Criar a classe `com.automation.flowmusic.FlowMusicAutomationApplication` (`@SpringBootApplication`).
4. Criar a estrutura de pacotes vazia conforme a Seção 8:
   `config/`, `controller/`, `domain/dto/`, `domain/model/`, `service/`, `exception/`.
5. Criar `src/main/resources/application.yml` mínimo (`server.port: 8080`, `spring.application.name: flow-music-automation`).
6. Criar `.gitignore` (target/, .idea/, *.iml, .vscode/, etc.) e inicializar o repositório git.

## Fora de escopo
- Qualquer regra de negócio, propriedades de automação ou testes.

## Critérios de aceite
- [x] `./mvnw clean verify` termina com `BUILD SUCCESS`.
- [x] `./mvnw spring-boot:run` sobe a aplicação sem erros na porta 8080 (ou na porta definida em `SERVER_PORT`).
- [x] Estrutura de pacotes igual à da Seção 8.

## Resultado observável
```bash
./mvnw clean verify          # BUILD SUCCESS
./mvnw spring-boot:run       # log: "Started FlowMusicAutomationApplication in X seconds"
curl -i http://localhost:8080/   # responde (404 Whitelabel é aceitável) => servidor no ar
```

## Testes
Nenhum (a especificação proíbe testes supérfluos; a validação é o build e o startup).

## Notas de implementação
- **Java 21 obrigatório.** O `java` padrão da máquina é o 11; o JDK 21 está no SDKMAN. Antes do Maven, use
  `sdk use java 21.0.2-amzn` ou `export JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.2-amzn`.
- **Porta configurável.** `server.port: ${SERVER_PORT:8080}`. Na máquina de desenvolvimento a porta 8080 está
  ocupada pelo container Docker `evolution_api`, então rode com `SERVER_PORT=8081 ./mvnw spring-boot:run`.
- Os pacotes vazios contêm `package-info.java` com a descrição de cada um (o git não versiona diretórios vazios).
- Maven Wrapper 3.3.4 (`distributionType=only-script`, Maven 3.9.9): não há `maven-wrapper.jar` versionado.

## Evidências (2026-11-05)
```text
./mvnw clean verify                     -> BUILD SUCCESS (Compiling 7 source files ... release 21)
SERVER_PORT=8081 ./mvnw spring-boot:run -> Tomcat started on port 8081
                                           Started FlowMusicAutomationApplication in 0.815 seconds
curl http://localhost:8081/             -> HTTP 404 (Whitelabel) => servidor no ar
```
