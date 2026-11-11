# NEXVIA — Microsserviços

> **O alerta avisa. O bairro garante que ninguém fique para trás.**

Global Solution 2026.2 · **Microservice and Web Engineering & IT Services** · Prof.ª Aparecida F. Castello Rosa · 3º SIS

## Integrantes — Equipe RiskLess

| Nome | RM |
| --- | --- |
| Arthur Paixão | RM 551739 |
| Vinicius Fagundes da Silva | RM 555270 |

---

## O problema

O alerta oficial da Defesa Civil já avisa os celulares da área de risco, mas para aí: **não sabe quem conseguiu sair**. Acamados, idosos que moram sozinhos e pessoas com deficiência dependem de alguém para agir.

- 8,27 milhões de pessoas viviam em áreas de risco nos 872 municípios monitorados pelo Cemaden (IBGE, Censo 2010).
- No terremoto de 2011 no Japão, 56,5% dos mortos tinham mais de 65 anos, e a mortalidade de pessoas com deficiência foi o dobro da população geral.
- Na enchente do RS em 2024, os alertas chegaram a toda a população em só 42,9% das cidades atingidas.

## A solução

A **NEXVIA** começa onde o alerta oficial termina. Cada pessoa que não consegue evacuar sozinha tem um cadastro **com consentimento (LGPD)** e um **padrinho**, um vizinho ou familiar de referência. Quando um alerta é confirmado, o sistema **aciona o padrinho**, acompanha o **status de segurança** de cada pessoa ("quem ainda falta") e registra o **"estou seguro"**. Se a comunicação falhar, o acionamento fica **pendente** para escalonamento, e ninguém se perde no caminho.

**Persona (fictícia):** Dona Maria, 82 anos, acamada, usa concentrador de oxigênio. Às 22h, moradores relatam rachaduras na encosta. O acionamento é aberto, João (o padrinho) é acionado, Dona Maria passa para `EM_ALERTA` e, quando João confirma que ela está a salvo, passa para `SEGURA`.

---

## Arquitetura

```
                        ┌──────────────────────┐
                        │    Eureka Server     │  :8761  (service discovery)
                        └──────────▲───────────┘
                 registro/consulta │
 Cliente (Postman / Swagger)       │
        │                          │
        ▼                          │
 ┌──────────────┐   rota pelo nome do serviço (Discovery Locator + Load Balancer)
 │ API Gateway  │ :8082 ──────────────────────────────┐
 └──────────────┘                                      │
        │ /ms-alertas/**                               │ /ms-apoio/**
        ▼                                              ▼
 ┌─────────────────────┐   OpenFeign (síncrono)  ┌─────────────────────┐
 │     ms-alertas      │  ─────────────────────▶ │      ms-apoio       │  (N instâncias,
 │ porta aleatória     │  Load Balancer +        │ porta aleatória     │   porta aleatória)
 │ Circuit Breaker +   │  Circuit Breaker        │                     │
 │ Fallback            │                         │                     │
 └─────────┬───────────┘                         └─────────┬───────────┘
           │                                               │
           ▼                                               ▼
   MySQL: nexvia_alertas                            MySQL: nexvia_apoio
   (Flyway V1..V4)                                  (Flyway V1..V2)
```

| Componente | Responsabilidade |
| --- | --- |
| `eureka-server` | Registro e descoberta dos serviços (porta **8761**) |
| `api-gateway` | Ponto único de entrada, roteamento dinâmico via Eureka, Load Balancer e Swagger unificado (porta **8082**) |
| `ms-apoio` | Pessoas que precisam de ajuda: cadastro com consentimento, necessidade, prioridade, padrinho e **status de segurança** |
| `ms-alertas` | Acionamentos: tipo de risco, rua, gravidade, **acionamento do padrinho** e **"estou seguro"**. Altera o status da pessoa no `ms-apoio` via **OpenFeign** |

### Comunicação síncrona e mudança de status

| Ação (ms-alertas) | Acionamento | Pessoa (ms-apoio, via OpenFeign) |
| --- | --- | --- |
| `POST /api/v1/acionamentos` | `ABERTO` (status inicial) | valida se a pessoa existe (`GET /api/v1/pessoas/{id}`) |
| `PATCH /api/v1/acionamentos/{id}/acionar` | `ABERTO` / `CONFIRMACAO_PENDENTE` → `PADRINHO_ACIONADO` | `CADASTRADA` / `SEGURA` → `EM_ALERTA` (`PUT /api/v1/pessoas/{id}/alertar`) |
| `PATCH /api/v1/acionamentos/{id}/concluir` | `PADRINHO_ACIONADO` → `CONCLUIDO` | `EM_ALERTA` → `SEGURA` (`PUT /api/v1/pessoas/{id}/segura`) |
| **ms-apoio fora do ar** (fallback de `acionar`) | → `CONFIRMACAO_PENDENTE` + **HTTP 503** | sem alteração |
| **ms-apoio fora do ar** (fallback de `concluir`) | mantém `PADRINHO_ACIONADO` + **HTTP 503** | sem alteração |

Regras de negócio (retornam **409 Conflict**):
- Uma pessoa `EM_ALERTA` não pode ser alterada nem excluída: o cadastro fica congelado durante o alerta.
- Só é possível acionar o padrinho com o acionamento `ABERTO` ou `CONFIRMACAO_PENDENTE`, e concluir com `PADRINHO_ACIONADO`.
- Um acionamento só pode ser editado com status `ABERTO` e não pode ser excluído com status `PADRINHO_ACIONADO`.
- A pessoa só pode ser marcada como `SEGURA` se estiver `EM_ALERTA`.

O status **nunca** é recebido nos DTOs de entrada: o sistema define o status inicial no cadastro, e ele só muda pelos endpoints de comunicação síncrona.

---

## Tecnologias

| Item | Versão / uso |
| --- | --- |
| Java | **25** |
| Spring Boot | **4.0.9** (todos os projetos) |
| Spring Cloud | 2025.1.4 — Netflix Eureka, Gateway Server WebFlux, LoadBalancer, OpenFeign, Circuit Breaker Resilience4j |
| Persistência | Spring Data JPA, **MySQL 8.4.6** (Docker), **Flyway** (migrations de tabelas e carga inicial) |
| Validação | Bean Validation (Jakarta Validation) |
| Documentação | springdoc-openapi 3.1.0 (Swagger UI dinâmico no Gateway) |
| Outros | Lombok, Maven Wrapper, Docker Compose |

## Organização do código (camadas)

Os dois microsserviços seguem a mesma estrutura:

```
ms-apoio/src/main/java/com/github/arthurpaixao/ms/apoio
├── config/          OpenApiConfig (Swagger)
├── controller/      PessoaApoioController  -> só acessa o Service; ResponseEntity + códigos HTTP
├── dto/             PessoaApoioRequestDTO (entrada + validações) | PessoaApoioResponseDTO (saída)
├── entities/        PessoaApoio, StatusSeguranca (enum)
├── exceptions/      exceções customizadas + dto/ (CustomErrorDTO, ValidationErrorDTO) + handler/ (GlobalExceptionHandler)
├── repositories/    PessoaApoioRepository (JpaRepository)
└── service/         PessoaApoioService (regras de negócio, CRUD, status)

ms-alertas/src/main/java/com/github/arthurpaixao/ms/alertas
├── client/          PessoaApoioClient (@FeignClient("ms-apoio"), sem URL fixa)
├── config/          OpenApiConfig
├── controller/      AcionamentoController (@CircuitBreaker + fallback), TipoRiscoController
├── dto/             AcionamentoRequestDTO | AcionamentoResponseDTO | TipoRiscoResponseDTO | PessoaApoioDTO
├── entities/        Acionamento, TipoRisco (@ManyToOne), StatusAcionamento, Gravidade
├── exceptions/      ResourceNotFound, StatusInvalido, Database, Comunicacao + dto/ + handler/
├── repositories/    AcionamentoRepository, TipoRiscoRepository
└── service/         AcionamentoService, TipoRiscoService
```

Migrations Flyway (`src/main/resources/db/migration`):

| Microsserviço | Banco (independente) | Migrations |
| --- | --- | --- |
| ms-apoio | `nexvia_apoio` | `V1__create_table_tb_pessoa_apoio.sql`, `V2__insert_tb_pessoa_apoio.sql` |
| ms-alertas | `nexvia_alertas` | `V1__create_table_tb_tipo_risco.sql`, `V2__insert_tb_tipo_risco.sql`, `V3__create_table_tb_acionamento.sql`, `V4__insert_tb_acionamento.sql` |

Os bancos são criados automaticamente na primeira execução (`createDatabaseIfNotExist=true`). Cada microsserviço tem o seu próprio banco, sem compartilhar schema nem tabelas. O `ms-alertas` guarda só o `pessoaId`, e a pessoa é consultada no `ms-apoio` via OpenFeign.

---

## Como executar (ambiente limpo)

### Pré-requisitos

- **Java 25** (JDK) instalado, com `JAVA_HOME` configurado
- **Docker** e **Docker Compose** (Docker Desktop no Windows)
- Maven **não** é obrigatório: cada projeto tem o **Maven Wrapper** (`mvnw` / `mvnw.cmd`)
- Portas livres: **3306** (MySQL), **8761** (Eureka) e **8082** (Gateway)

### 1. Clonar o repositório

```bash
git clone https://github.com/ArthurPaixao/nexvia-microservices.git
cd nexvia-microservices
```

### 2. Subir o MySQL com Docker

Na raiz do repositório:

```bash
docker compose up -d
```

Isso cria o container `nexvia_mysql` (MySQL 8.4.6, usuário `root`, senha `root`, porta `3306`). Para conferir:

```bash
docker ps
```

### 3. Subir os serviços — **nesta ordem**, um terminal para cada

> No **Windows** use `mvnw.cmd`. No **Linux/macOS** use `./mvnw`.
> Também é possível abrir cada pasta no **IntelliJ IDEA** e executar a classe `*Application`.

```bash
# Terminal 1 - Eureka Server (aguarde subir: http://localhost:8761)
cd eureka-server
mvnw.cmd spring-boot:run

# Terminal 2 - API Gateway (porta 8082)
cd api-gateway
mvnw.cmd spring-boot:run

# Terminal 3 - ms-apoio (porta aleatória)
cd ms-apoio
mvnw.cmd spring-boot:run

# Terminal 4 - ms-alertas (porta aleatória)
cd ms-alertas
mvnw.cmd spring-boot:run
```

**Várias instâncias (Load Balancer):** abra outro terminal e rode `mvnw.cmd spring-boot:run` de novo na pasta `ms-apoio`. Como a porta é aleatória (`server.port=0`), a segunda instância sobe sem conflito e se registra no Eureka.

Na primeira execução, o Flyway cria as tabelas e faz a carga inicial dos dois bancos automaticamente.

### 4. Conferir

| O quê | Endereço |
| --- | --- |
| Painel do Eureka (serviços registrados) | http://localhost:8761 |
| **Swagger UI** (os dois microsserviços, via Gateway) | http://localhost:8082/swagger-ui/index.html |
| OpenAPI JSON do ms-apoio | http://localhost:8082/ms-apoio/v3/api-docs |
| OpenAPI JSON do ms-alertas | http://localhost:8082/ms-alertas/v3/api-docs |

> No Swagger, escolha o microsserviço no seletor **"Select a definition"** (canto superior direito). As requisições já saem pelo Gateway.
> O Gateway leva alguns segundos para descobrir os serviços no Eureka. Se o seletor estiver vazio, aguarde ~10 s e recarregue a página.

---

## Endpoints (todos via Gateway: `http://localhost:8082/<microsserviço>/...`)

### ms-apoio — `/ms-apoio/api/v1/pessoas`

| Método | Endpoint | Descrição | Sucesso |
| --- | --- | --- | --- |
| GET | `/api/v1/pessoas` | Lista por prioridade. Filtro opcional `?status=EM_ALERTA` (painel "quem ainda falta") | 200 |
| GET | `/api/v1/pessoas/{id}` | Busca por ID | 200 |
| POST | `/api/v1/pessoas` | Cadastra (status inicial `CADASTRADA`), devolve a URI no header `Location` | 201 |
| PUT | `/api/v1/pessoas/{id}` | Atualiza (bloqueado se `EM_ALERTA`) | 200 |
| DELETE | `/api/v1/pessoas/{id}` | Exclui (bloqueado se `EM_ALERTA`) | 204 |
| PUT | `/api/v1/pessoas/{id}/alertar` | → `EM_ALERTA` (**chamado pelo ms-alertas via OpenFeign**) | 200 |
| PUT | `/api/v1/pessoas/{id}/segura` | → `SEGURA` (**chamado pelo ms-alertas via OpenFeign**) | 200 |
| GET | `/api/v1/pessoas/port` | Porta da instância que respondeu (teste de Load Balancer) | 200 |

### ms-alertas — `/ms-alertas/api/v1/acionamentos`

| Método | Endpoint | Descrição | Sucesso |
| --- | --- | --- | --- |
| GET | `/api/v1/acionamentos` | Lista todos | 200 |
| GET | `/api/v1/acionamentos/{id}` | Busca por ID | 200 |
| POST | `/api/v1/acionamentos` | Abre acionamento (status `ABERTO`), valida a pessoa no ms-apoio | 201 |
| PUT | `/api/v1/acionamentos/{id}` | Atualiza (somente `ABERTO`) | 200 |
| DELETE | `/api/v1/acionamentos/{id}` | Exclui (bloqueado se `PADRINHO_ACIONADO`) | 204 |
| PATCH | `/api/v1/acionamentos/{id}/acionar` | Aciona o padrinho e coloca a pessoa `EM_ALERTA` (**OpenFeign + Circuit Breaker**) | 200 (503 no fallback) |
| PATCH | `/api/v1/acionamentos/{id}/concluir` | "Estou seguro": conclui e marca a pessoa `SEGURA` (**OpenFeign + Circuit Breaker**) | 200 (503 no fallback) |
| GET | `/api/v1/tipos-risco` | Tipos de risco cadastrados no banco | 200 |
| GET | `/api/v1/tipos-risco/port` | Porta da instância que respondeu | 200 |

### Códigos de erro (GlobalExceptionHandler)

| HTTP | Quando |
| --- | --- |
| 400 | JSON malformado, enum inválido (ex.: `"gravidade": "ENORME"`) ou ID não numérico (`/pessoas/abc`) |
| 404 | Recurso não encontrado (inclusive pessoa inexistente no ms-apoio, detectada via OpenFeign) |
| 409 | Regra de negócio violada (status inválido, pessoa em alerta, tipo de risco inexistente) |
| 422 | Falha de Bean Validation, com a lista de campos e mensagens |
| 503 | Falha de comunicação com o ms-apoio (fallback do Circuit Breaker) |

---

## Roteiro de teste — a noite da Dona Maria

Dados iniciais (Flyway): 5 pessoas no `ms-apoio` (id **1** = Dona Maria) e 3 acionamentos no `ms-alertas` (id **1** = encosta rachando na Rua das Palmeiras, para a Dona Maria, `ABERTO`).

Pode ser feito pelo **Swagger** ou pelo Postman/curl:

```bash
# 1. Lista por prioridade: Dona Maria aparece no topo (prioridade 1), status CADASTRADA
curl http://localhost:8082/ms-apoio/api/v1/pessoas

# 2. Acionamento 1 está ABERTO
curl http://localhost:8082/ms-alertas/api/v1/acionamentos/1

# 3. Aciona o padrinho (João): acionamento -> PADRINHO_ACIONADO
curl -X PATCH http://localhost:8082/ms-alertas/api/v1/acionamentos/1/acionar

# 4. A comunicação síncrona alterou a pessoa no ms-apoio: EM_ALERTA
curl http://localhost:8082/ms-apoio/api/v1/pessoas/1

# 5. Painel "quem ainda falta"
curl "http://localhost:8082/ms-apoio/api/v1/pessoas?status=EM_ALERTA"

# 6. João confirma "estou seguro": acionamento -> CONCLUIDO, pessoa -> SEGURA
curl -X PATCH http://localhost:8082/ms-alertas/api/v1/acionamentos/1/concluir
curl http://localhost:8082/ms-apoio/api/v1/pessoas/1
```

Cadastro de uma nova pessoa (retorna **201** e o header `Location`):

```bash
curl -i -X POST http://localhost:8082/ms-apoio/api/v1/pessoas \
  -H "Content-Type: application/json" \
  -d '{"nome":"José Ferreira","endereco":"Rua Nova, 10","bairro":"Vila Ribeirinha","necessidade":"Usa marca-passo, 90 anos","prioridade":1,"nomePadrinho":"Rita Ferreira","telefonePadrinho":"11912345678","consentimentoLgpd":true}'
```

Abertura de um acionamento (os tipos de risco estão em `GET /ms-alertas/api/v1/tipos-risco`):

```bash
curl -i -X POST http://localhost:8082/ms-alertas/api/v1/acionamentos \
  -H "Content-Type: application/json" \
  -d '{"pessoaId":4,"tipoRiscoId":2,"rua":"Rua Beira-Rio","gravidade":"ALTA","descricao":"Água entrando nas casas da Rua Beira-Rio"}'
```

### Testando o Load Balancer

Com **duas instâncias** do `ms-apoio` rodando, chame várias vezes. A porta da resposta alterna entre as instâncias:

```bash
curl http://localhost:8082/ms-apoio/api/v1/pessoas/port
```

### Testando o Circuit Breaker e o Fallback

1. Pare **todas** as instâncias do `ms-apoio` (Ctrl+C nos terminais).
2. Acione o padrinho do acionamento 2:
   ```bash
   curl -i -X PATCH http://localhost:8082/ms-alertas/api/v1/acionamentos/2/acionar
   ```
3. Resultado: **HTTP 503** com o acionamento em `CONFIRMACAO_PENDENTE` (fallback). Depois de 2 falhas, o circuito abre, e o log do `ms-alertas` mostra `CircuitBreaker 'alterarStatusPessoa' is OPEN`.
4. Suba o `ms-apoio` de novo, espere ~50 s (`waitDurationInOpenState`) e repita o passo 2: o acionamento vai para `PADRINHO_ACIONADO` e a pessoa para `EM_ALERTA`.

Configuração (`ms-alertas/src/main/resources/application.properties`): janela `COUNT_BASED` das últimas 3 chamadas, mínimo de 2 chamadas, abre com 50% de falhas, fica 50 s aberto e permite 2 chamadas em `HALF_OPEN`. Erros de negócio (404/409) não contam como falha de comunicação.

---

## Parar o ambiente

```bash
# Ctrl+C em cada terminal dos serviços, depois:
docker compose down
```

Para apagar também os dados do MySQL (recomeçar do zero), remova a pasta `.data` criada na raiz.

---

## Observações técnicas

- **Ordem de inicialização:** Eureka Server → API Gateway → microsserviços (em qualquer ordem).
- **Sem URLs fixas:** o OpenFeign usa o nome do serviço (`@FeignClient("ms-apoio")`), resolvido pelo Eureka e distribuído pelo Spring Cloud LoadBalancer.
- **Profiles:** `dev` (padrão, MySQL) em `application-dev.properties`. Os testes (`mvnw test`) usam o profile `test`, com H2 em memória e sem Eureka.
- **Base:** Eureka Server, API Gateway, Swagger dinâmico, `GlobalExceptionHandler` e a organização em camadas seguem os projetos e materiais trabalhados em aula, atualizados para Spring Boot 4.0.9 / Spring Cloud 2025.1.4.
