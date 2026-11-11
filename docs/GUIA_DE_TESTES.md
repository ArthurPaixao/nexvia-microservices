# NEXVIA — Guia de Testes

Passo a passo para **subir o ambiente do zero** e **testar todos os requisitos** da solução.
Para entender como o projeto funciona por dentro, veja a [Documentação Técnica](DOCUMENTACAO_TECNICA.md).

**Sumário**

1. [Pré-requisitos](#1-pré-requisitos)
2. [Subindo o ambiente](#2-subindo-o-ambiente)
3. [Conferindo se tudo subiu](#3-conferindo-se-tudo-subiu)
4. [Três formas de testar](#4-três-formas-de-testar)
5. [Roteiro 1 — CRUD do ms-apoio](#5-roteiro-1--crud-do-ms-apoio)
6. [Roteiro 2 — CRUD do ms-alertas](#6-roteiro-2--crud-do-ms-alertas)
7. [Roteiro 3 — A noite da Dona Maria (comunicação síncrona)](#7-roteiro-3--a-noite-da-dona-maria-comunicação-síncrona)
8. [Roteiro 4 — Regras de negócio e erros](#8-roteiro-4--regras-de-negócio-e-erros)
9. [Roteiro 5 — Múltiplas instâncias e Load Balancer](#9-roteiro-5--múltiplas-instâncias-e-load-balancer)
10. [Roteiro 6 — Circuit Breaker e Fallback](#10-roteiro-6--circuit-breaker-e-fallback)
11. [Roteiro 7 — Conferindo o banco e o Flyway](#11-roteiro-7--conferindo-o-banco-e-o-flyway)
12. [Testes automatizados (Maven)](#12-testes-automatizados-maven)
13. [Recomeçar do zero](#13-recomeçar-do-zero)
14. [Problemas comuns](#14-problemas-comuns)

---

## 1. Pré-requisitos

| Ferramenta | Versão | Para quê |
| --- | --- | --- |
| JDK | **25** | Executar os serviços (com `JAVA_HOME` configurado) |
| Docker Desktop | qualquer recente | MySQL via `docker compose` |
| Postman (opcional) | qualquer | Rodar a coleção de testes pronta |
| Git | qualquer | Clonar o repositório |

O Maven **não** precisa estar instalado: cada projeto tem o Maven Wrapper (`mvnw.cmd` no Windows, `./mvnw` no Linux/macOS).
As portas **3306**, **8761** e **8082** precisam estar livres.

---

## 2. Subindo o ambiente

```bash
git clone https://github.com/ArthurPaixao/nexvia-microservices.git
cd nexvia-microservices
```

**Passo 1. MySQL (Docker)**, na raiz do repositório:

```bash
docker compose up -d
```

**Passos 2 a 5. Serviços**, **nesta ordem**, cada um em um terminal (no Windows, `mvnw.cmd`; no Linux/macOS, `./mvnw`):

| Ordem | Terminal | Comando | Pronto quando o log mostrar |
| --- | --- | --- | --- |
| 2 | `eureka-server/` | `mvnw.cmd spring-boot:run` | `Started EurekaServerApplication` |
| 3 | `api-gateway/` | `mvnw.cmd spring-boot:run` | `Started ApiGatewayApplication` |
| 4 | `ms-apoio/` | `mvnw.cmd spring-boot:run` | `Successfully applied 2 migrations` e `Started MsApoioApplication` |
| 5 | `ms-alertas/` | `mvnw.cmd spring-boot:run` | `Successfully applied 4 migrations` e `Started MsAlertasApplication` |

> Pelo **IntelliJ IDEA**: abra cada pasta como projeto Maven e execute a classe `*Application` na mesma ordem.
> A primeira execução baixa as dependências do Maven e pode levar alguns minutos.

---

## 3. Conferindo se tudo subiu

1. **Eureka**: abra http://localhost:8761. Em *Instances currently registered with Eureka* devem aparecer `API-GATEWAY`, `MS-ALERTAS` e `MS-APOIO`, todos `UP`.
2. **Swagger**: abra http://localhost:8082/swagger-ui/index.html. No seletor **Select a definition** (canto superior direito) devem aparecer `ms-alertas` e `ms-apoio`.
3. **Teste rápido pelo Gateway**:
   ```bash
   curl http://localhost:8082/ms-apoio/api/v1/pessoas
   ```
   Deve retornar **200** com 5 pessoas.

> O Gateway leva cerca de 10 a 30 s para "enxergar" um serviço recém-registrado. Se receber **404** com `"error":"Not Found"` e sem a mensagem da NEXVIA, aguarde e tente de novo.

---

## 4. Três formas de testar

### A) Coleção Postman (recomendado: testes automáticos)

O arquivo [`postman/NEXVIA.postman_collection.json`](../postman/NEXVIA.postman_collection.json) tem **todas as requisições** dos roteiros abaixo, cada uma com **asserções automáticas** (código HTTP e campos da resposta).

1. No Postman: **Import** → selecione o arquivo `postman/NEXVIA.postman_collection.json`.
2. A coleção já traz as variáveis `baseUrl = http://localhost:8082` e `eurekaUrl = http://localhost:8761`.
3. Clique com o botão direito na coleção → **Run collection** → **Run NEXVIA**.
4. Resultado esperado nas pastas 1 a 5: **42 requisições e 91 asserções, todas verdes**.

| Pasta | Requisições | O que cobre |
| --- | --- | --- |
| 1 - Infraestrutura | 5 | Eureka, Swagger dinâmico, OpenAPI dos dois serviços, Load Balancer |
| 2 - ms-apoio (CRUD) | 6 | GET, GET por ID, POST (201 + `Location`), PUT, DELETE, 404 |
| 3 - ms-alertas (CRUD) | 7 | Tipos de risco + CRUD de acionamentos (o POST valida a pessoa via OpenFeign) |
| 4 - Fluxo Dona Maria (OpenFeign) | 12 | Acionar → `EM_ALERTA` → regras 409 → concluir → `SEGURA` |
| 5 - Regras de negocio e erros | 12 | 400, 404, 409 e 422 nos dois serviços |
| 6 - Circuit Breaker (MANUAL) | 5 | **6a** com o `ms-apoio` parado (fallback 503) · **6b** depois de subir de novo (recuperação) |

> A pasta **6** é **manual**: desmarque-a no Runner e siga o [Roteiro 6](#10-roteiro-6--circuit-breaker-e-fallback). Rode a subpasta **6a** com o `ms-apoio` parado e a **6b** depois de subir o `ms-apoio` e aguardar ~1 minuto.
> A coleção espera o **banco recém-criado** (carga inicial do Flyway). Para rodar de novo, veja [Recomeçar do zero](#13-recomeçar-do-zero).

Também dá para rodar pelo terminal com o **Newman** (precisa do Node.js):

```bash
npx newman run postman/NEXVIA.postman_collection.json --folder "1 - Infraestrutura" --folder "2 - ms-apoio (CRUD)" --folder "3 - ms-alertas (CRUD)" --folder "4 - Fluxo Dona Maria (OpenFeign)" --folder "5 - Regras de negocio e erros"
```

Circuit Breaker pelo Newman (uma fase de cada vez):

```bash
npx newman run postman/NEXVIA.postman_collection.json --folder "6a - com o ms-apoio PARADO"
npx newman run postman/NEXVIA.postman_collection.json --folder "6b - depois de SUBIR o ms-apoio"
```

### B) Swagger UI

Em http://localhost:8082/swagger-ui/index.html, escolha o microsserviço em **Select a definition**, abra um endpoint, clique em **Try it out** → **Execute**. Os exemplos de corpo já vêm preenchidos (`@Schema(example = ...)`). As chamadas saem pelo Gateway.

### C) curl

Todos os comandos dos roteiros abaixo podem ser colados no terminal. No **PowerShell**, use `curl.exe` (e não `curl`) ou rode no **Git Bash**.

---

## 5. Roteiro 1 — CRUD do ms-apoio

| # | Requisição | Esperado |
| --- | --- | --- |
| 1.1 | `GET /ms-apoio/api/v1/pessoas` | **200**, 5 pessoas, prioridade 1 primeiro |
| 1.2 | `GET /ms-apoio/api/v1/pessoas/1` | **200**, Maria Aparecida dos Santos, `CADASTRADA` |
| 1.3 | `POST /ms-apoio/api/v1/pessoas` (corpo abaixo) | **201**, header `Location` com `/api/v1/pessoas/6`, `statusSeguranca = CADASTRADA` |
| 1.4 | `PUT /ms-apoio/api/v1/pessoas/6` (mesmo corpo, `prioridade: 2`) | **200**, prioridade atualizada |
| 1.5 | `DELETE /ms-apoio/api/v1/pessoas/6` | **204** |
| 1.6 | `GET /ms-apoio/api/v1/pessoas/6` | **404** |

```bash
curl -i -X POST http://localhost:8082/ms-apoio/api/v1/pessoas \
  -H "Content-Type: application/json" \
  -d '{"nome":"José Ferreira","endereco":"Rua Nova, 10","bairro":"Vila Ribeirinha","necessidade":"Usa marca-passo, 90 anos","prioridade":1,"nomePadrinho":"Rita Ferreira","telefonePadrinho":"11912345678","consentimentoLgpd":true}'
```

Repare que o **status não é enviado** no corpo: o sistema define `CADASTRADA` sozinho.

---

## 6. Roteiro 2 — CRUD do ms-alertas

| # | Requisição | Esperado |
| --- | --- | --- |
| 2.1 | `GET /ms-alertas/api/v1/tipos-risco` | **200**, 5 tipos (Deslizamento, Alagamento, ...) |
| 2.2 | `GET /ms-alertas/api/v1/acionamentos` | **200**, 3 acionamentos |
| 2.3 | `GET /ms-alertas/api/v1/acionamentos/1` | **200**, pessoa 1, Deslizamento, `ABERTO` |
| 2.4 | `POST /ms-alertas/api/v1/acionamentos` (corpo abaixo) | **201**, header `Location`, `status = ABERTO` |
| 2.5 | `PUT /ms-alertas/api/v1/acionamentos/4` (`tipoRiscoId: 3`, `gravidade: MEDIA`) | **200**, tipo Enxurrada |
| 2.6 | `DELETE /ms-alertas/api/v1/acionamentos/4` | **204** |

```bash
curl -i -X POST http://localhost:8082/ms-alertas/api/v1/acionamentos \
  -H "Content-Type: application/json" \
  -d '{"pessoaId":2,"tipoRiscoId":2,"rua":"Travessa do Córrego","gravidade":"ALTA","descricao":"Água subindo rapidamente na Travessa do Córrego"}'
```

No POST, o `ms-alertas` já faz uma chamada **síncrona** ao `ms-apoio` para validar se a pessoa existe.

---

## 7. Roteiro 3 — A noite da Dona Maria (comunicação síncrona)

Este é o **fluxo principal**: o `ms-alertas` altera o status da pessoa no `ms-apoio` via **OpenFeign**.

```bash
# 3.1 Antes: Dona Maria está CADASTRADA e o acionamento 1 está ABERTO
curl http://localhost:8082/ms-apoio/api/v1/pessoas/1
curl http://localhost:8082/ms-alertas/api/v1/acionamentos/1

# 3.2 Aciona o padrinho (João)
curl -X PATCH http://localhost:8082/ms-alertas/api/v1/acionamentos/1/acionar

# 3.3 Status mudou nos DOIS serviços
curl http://localhost:8082/ms-apoio/api/v1/pessoas/1

# 3.4 Painel "quem ainda falta"
curl "http://localhost:8082/ms-apoio/api/v1/pessoas?status=EM_ALERTA"

# 3.5 João confirma "estou seguro"
curl -X PATCH http://localhost:8082/ms-alertas/api/v1/acionamentos/1/concluir

# 3.6 Dona Maria está SEGURA e o painel ficou vazio
curl http://localhost:8082/ms-apoio/api/v1/pessoas/1
curl "http://localhost:8082/ms-apoio/api/v1/pessoas?status=EM_ALERTA"
```

| Passo | Acionamento 1 (`ms-alertas`) | Pessoa 1 (`ms-apoio`) | HTTP |
| --- | --- | --- | --- |
| 3.1 | `ABERTO` | `CADASTRADA` | 200 |
| 3.2 / 3.3 | `PADRINHO_ACIONADO` | `EM_ALERTA` | 200 |
| 3.4 | — | lista só com a Dona Maria | 200 |
| 3.5 / 3.6 | `CONCLUIDO` | `SEGURA` | 200 |

---

## 8. Roteiro 4 — Regras de negócio e erros

Os itens marcados com ★ precisam ser executados **depois do 3.2** (com a Dona Maria `EM_ALERTA`) e **antes do 3.5**. Na coleção Postman, eles já estão nessa ordem, dentro da pasta 4 (requisições 4.5 a 4.9).

| # | Requisição | Esperado | O que comprova |
| --- | --- | --- | --- |
| 4.1 | `POST /ms-apoio/api/v1/pessoas` com `{"nome":"Jo","telefonePadrinho":"123","prioridade":7,"consentimentoLgpd":false}` | **422** + lista `errors` | Bean Validation e consentimento LGPD |
| 4.2 | `POST /ms-apoio/api/v1/pessoas` com corpo `{"nome":` | **400** | JSON malformado |
| 4.3 | `GET /ms-apoio/api/v1/pessoas/abc` | **400** | Tipo de parâmetro inválido |
| 4.4 | `GET /ms-apoio/api/v1/pessoas/999` | **404** | Recurso inexistente |
| 4.5 | `GET /ms-apoio/api/v1/pessoas?status=INVALIDO` | **400** | Enum inválido no filtro |
| 4.6 ★ | `PUT /ms-apoio/api/v1/pessoas/1` (qualquer corpo válido) | **409** | Pessoa em alerta não pode ser alterada |
| 4.7 ★ | `DELETE /ms-apoio/api/v1/pessoas/1` | **409** | Pessoa em alerta não pode ser excluída |
| 4.8 ★ | `PATCH /ms-alertas/api/v1/acionamentos/1/acionar` de novo | **409** | Transição de status inválida |
| 4.9 ★ | `PUT` ou `DELETE /ms-alertas/api/v1/acionamentos/1` | **409** | Acionamento em andamento é protegido |
| 4.10 | `PATCH /ms-alertas/api/v1/acionamentos/2/concluir` (está `ABERTO`) | **409** | Só conclui com `PADRINHO_ACIONADO` |
| 4.11 | `POST /ms-alertas/api/v1/acionamentos` com `pessoaId: 999` | **404** | Pessoa validada no ms-apoio via **OpenFeign** |
| 4.12 | `POST /ms-alertas/api/v1/acionamentos` com `tipoRiscoId: 99` | **409** | Tipo de risco inexistente |
| 4.13 | `POST /ms-alertas/api/v1/acionamentos` com `gravidade: "ENORME"` | **400** | Enum inválido no corpo |
| 4.14 | `POST /ms-alertas/api/v1/acionamentos` com `{}` | **422** | Bean Validation |
| 4.15 | `PATCH /ms-alertas/api/v1/acionamentos/999/acionar` | **404** | O erro de negócio atravessa o Circuit Breaker sem cair no fallback |

Formato do erro (todos os casos):

```json
{ "timestamp": "...", "status": 409, "error": "Pessoa id 1 está EM_ALERTA e não pode ser alterada", "path": "/api/v1/pessoas/1" }
```

---

## 9. Roteiro 5 — Múltiplas instâncias e Load Balancer

1. Com tudo rodando, abra **mais um terminal** em `ms-apoio/` e rode `mvnw.cmd spring-boot:run` de novo.
   A porta aleatória (`server.port=0`) evita conflito.
2. Em http://localhost:8761, `MS-APOIO` deve mostrar **2 instâncias** (ex.: `ms-apoio:36`, `ms-apoio:14`).
3. Chame várias vezes:
   ```bash
   curl http://localhost:8082/ms-apoio/api/v1/pessoas/port
   ```
   A porta da resposta **alterna** entre as duas instâncias:
   ```
   Instância do ms-apoio respondeu na porta 33607
   Instância do ms-apoio respondeu na porta 36757
   Instância do ms-apoio respondeu na porta 33607
   ```
4. O mesmo balanceamento vale para as chamadas **OpenFeign** do `ms-alertas` para o `ms-apoio`.

---

## 10. Roteiro 6 — Circuit Breaker e Fallback

Use o **acionamento 2** (Benedita, `ABERTO`).

1. **Pare todas as instâncias do `ms-apoio`** (Ctrl+C nos terminais).
2. Chame 3 vezes:
   ```bash
   curl -i -X PATCH http://localhost:8082/ms-alertas/api/v1/acionamentos/2/acionar
   ```

   | Chamada | HTTP | `status` no corpo | Log do `ms-alertas` |
   | --- | --- | --- | --- |
   | 1ª | **503** | `CONFIRMACAO_PENDENTE` | `Falha ao acionar padrinho do acionamento 2. Ativando fallback. Erro: Falha ao comunicar com ms-apoio` |
   | 2ª | **503** | `CONFIRMACAO_PENDENTE` | idem: o circuito **abre** (2 falhas = 100%) |
   | 3ª | **503** | `CONFIRMACAO_PENDENTE` | `CircuitBreaker 'alterarStatusPessoa' is OPEN and does not permit further calls` |

3. **Fallback do concluir**: com um acionamento em `PADRINHO_ACIONADO` (ex.: acionamento 1 depois do passo 3.2), `PATCH .../1/concluir` retorna **503** e o status **continua `PADRINHO_ACIONADO`**, para a confirmação poder ser reenviada.
4. **Recuperação**: suba o `ms-apoio` de novo e espere cerca de **1 minuto** (50 s do circuito aberto + registro no Eureka). Repita:
   ```bash
   curl -X PATCH http://localhost:8082/ms-alertas/api/v1/acionamentos/2/acionar
   curl http://localhost:8082/ms-apoio/api/v1/pessoas/4
   ```
   Agora: **200**, acionamento `PADRINHO_ACIONADO` e Benedita `EM_ALERTA`. O acionamento pendente foi reprocessado.

---

## 11. Roteiro 7 — Conferindo o banco e o Flyway

Comprove que cada microsserviço tem o **seu próprio banco** e que as tabelas vieram das **migrations**:

```bash
# Bancos criados (um por microsserviço)
docker exec nexvia_mysql mysql -uroot -proot --default-character-set=utf8mb4 -e "SHOW DATABASES;"

# Histórico do Flyway em cada banco
docker exec nexvia_mysql mysql -uroot -proot --default-character-set=utf8mb4 -e "SELECT version, description, success FROM nexvia_apoio.flyway_schema_history;"
docker exec nexvia_mysql mysql -uroot -proot --default-character-set=utf8mb4 -e "SELECT version, description, success FROM nexvia_alertas.flyway_schema_history;"

# Dados persistidos (ex.: status depois do Roteiro 3)
docker exec nexvia_mysql mysql -uroot -proot --default-character-set=utf8mb4 -e "SELECT id, nome, status_seguranca FROM nexvia_apoio.tb_pessoa_apoio;"
docker exec nexvia_mysql mysql -uroot -proot --default-character-set=utf8mb4 -e "SELECT id, pessoa_id, status FROM nexvia_alertas.tb_acionamento;"
```

Esperado: os bancos `nexvia_apoio` (migrations 1 e 2) e `nexvia_alertas` (migrations 1 a 4), cada um com as suas tabelas, sem nenhuma tabela compartilhada.

---

## 12. Testes automatizados (Maven)

Cada projeto tem um teste `contextLoads` que sobe o contexto Spring completo com **H2 em memória** (perfil `test`). Ele não precisa do MySQL nem do Eureka:

```bash
cd ms-apoio
mvnw.cmd test
```

Esperado: `Tests run: 1, Failures: 0, Errors: 0` e `BUILD SUCCESS` (o mesmo vale para `ms-alertas`, `api-gateway` e `eureka-server`).

---

## 13. Recomeçar do zero

Para voltar à carga inicial (por exemplo, para rodar a coleção Postman de novo):

1. Pare os quatro serviços (Ctrl+C).
2. Na raiz do repositório:
   ```bash
   docker compose down
   ```
3. Apague a pasta **`.data`** da raiz (dados do MySQL).
4. Suba de novo a partir do [passo 1](#2-subindo-o-ambiente). O Flyway recria tudo.

---

## 14. Problemas comuns

| Sintoma | Causa provável | Solução |
| --- | --- | --- |
| `Communications link failure` ao subir um microsserviço | MySQL não está rodando ou ainda inicializando | `docker compose up -d`, aguarde ~20 s e suba o serviço de novo |
| Gateway responde **404** sem a mensagem da NEXVIA (`"error":"Not Found"`, `requestId`) | Serviço ainda não registrado ou não descoberto pelo Gateway | Confira em http://localhost:8761 e aguarde 10 a 30 s |
| Swagger sem microsserviços no seletor | Os serviços subiram depois do Gateway | Aguarde ~10 s (atualização automática) e recarregue a página |
| `Port 8761/8082 already in use` | Outra aplicação usando a porta | Encerre o processo que ocupa a porta |
| `Port 3306 already in use` no `docker compose` | MySQL local instalado | Pare o MySQL local antes de subir o container |
| `JAVA_HOME is not set` ao rodar `mvnw` | JDK 25 não configurado | Instale o JDK 25 e configure `JAVA_HOME` |
| `Failed to execute goal ... maven-resources-plugin ... Input length = 1` | Encoding de arquivos no Windows | Já tratado: o `pom.xml` fixa o `maven-resources-plugin` 3.1.0 (padrão da disciplina) |
| Testes da coleção falham com 409 em "acionar" | Banco já usado em execuções anteriores | Siga [Recomeçar do zero](#13-recomeçar-do-zero) |
