# Echobox — Arquitetura de Microsserviços

Sistema de gestão de feedbacks corporativos construído em arquitetura distribuída de microsserviços Cloud Native, conteinerizado com Docker e orquestrado em Kubernetes (Amazon EKS).

---

## Visão Geral dos Microsserviços

| Microsserviço | Responsabilidade | Linguagem / Framework | Banco de Dados (Database per Service) | Porta |
| :--- | :--- | :--- | :--- | :--- |
| **`ms-auth`** | Autenticação, controle de acesso JWT e gestão de usuários | Java 21 / Spring Boot 3.5 | PostgreSQL (`echobox_auth`) | `8081` |
| **`ms-company`** | Gestão de empresas, categorias e status de atendimento | Java 21 / Spring Boot 3.5 | PostgreSQL (`echobox_company`) | `8082` |
| **`ms-feedback`** | Registro de feedbacks, respostas (replies) e agregação de dados | Python 3.11 / FastAPI | MongoDB (`echobox_feedback`) | `8083` |
| **`ms-notification`** | Processamento assíncrono de eventos e envio de notificações SMTP | Java 21 / Spring Boot 3.5 | Mensageria RabbitMQ + MailHog | `8084` |

---

## Arquitetura do Sistema (Amazon EKS)

```
                               CLIENTE / HTTP
                                     |
                                     v
                          NGINX Ingress Controller
                            (k8s/03-ingress.yaml)
                                     |
        +-------------------+--------+--------+-------------------+
        | /users            | /companies      | /feedbacks        | /notifications
        |                   | /categories     | /replies          |
        |                   | /statuses       |                   |
        v                   v                 v                   v
  Service ms-auth   Service ms-company  Service ms-feedback  Service ms-notification
    (ClusterIP)        (ClusterIP)          (ClusterIP)          (ClusterIP)
        |                   |    ^            |         \             ^
        v                   v    | (REST)     v          \            |
   Pod ms-auth       Pods ms-company    Pods ms-feedback  \           |
   (1 réplica)        (2 réplicas)       (3 réplicas)      \          |
   Java / Spring      Java / Spring     Python / FastAPI    \         |
        |                   |                 |              v        |
        v                   v                 v             RabbitMQ -+
  postgres-auth      postgres-company   mongo-feedback    (echobox.notifications)
  (PostgreSQL)         (PostgreSQL)       (MongoDB)
```

### Padrões Arquiteturais Implementados

- **Database per Service**: Cada microsserviço possui sua própria instância isolada de banco de dados (`postgres-auth`, `postgres-company` e `mongo-feedback`), sem compartilhamento de tabelas ou conexões entre serviços.
- **Comunicação Síncrona (REST + Service Discovery)**: O `ms-feedback` consulta sincronamente o `ms-company` via HTTP REST utilizando o DNS interno do Kubernetes (`http://ms-company:8082/companies/{id}`, `/categories/{id}`, `/statuses/{id}`).
- **Comunicação Assíncrona (RabbitMQ)**: Na criação de um feedback ou resposta, o `ms-feedback` publica um evento JSON (`FEEDBACK_CREATED` / `REPLY_CREATED`) na fila `echobox.notifications` do RabbitMQ, consumido de forma assíncrona pelo `ms-notification` para disparo de e-mail via SMTP.
- **Escalabilidade Horizontal e Balanceamento de Carga**: Os serviços `ms-feedback` (3 réplicas) e `ms-company` (2 réplicas) executam múltiplos Pods simultâneos distribuídos pelo Service Kubernetes (`ClusterIP`).
- **API Gateway / Ingress**: O acesso externo ao cluster é centralizado pelo **NGINX Ingress Controller** (`k8s/03-ingress.yaml`).

---

## Estrutura do Repositório

```
.
├── docker-compose.yml       # Orquestração local de containers
├── k8s/
│   ├── 01-infrastructure.yaml   # PostgreSQL (x2), MongoDB, RabbitMQ e MailHog
│   ├── 02-microservices.yaml    # Deployments e Services (ClusterIP) dos 4 microsserviços
│   └── 03-ingress.yaml          # Regras de roteamento do NGINX Ingress Controller
├── ms-auth/                 # Microsserviço de Autenticação e Usuários (Java / Spring Boot)
├── ms-company/              # Microsserviço de Empresas, Categorias e Status (Java / Spring Boot)
├── ms-feedback/             # Microsserviço de Feedbacks e Replies (Python / FastAPI)
├── ms-notification/         # Microsserviço de Notificações (Java / Spring Boot)
├── nginx/                   # Configuração do gateway reverso para execução local
└── scripts/                 # Scripts de build de imagens e deploy no Amazon EKS
```

---

## Como Executar

### 1. Ambiente Local (Docker Compose)

```bash
docker compose up --build -d
```

- **Gateway NGINX**: `http://localhost:8080`
- **Swagger UI (`ms-auth`)**: `http://localhost:8081/swagger-ui.html`
- **Swagger UI (`ms-company`)**: `http://localhost:8082/swagger-ui.html`
- **Swagger UI (`ms-feedback`)**: `http://localhost:8083/swagger-ui.html`
- **Swagger UI (`ms-notification`)**: `http://localhost:8084/swagger-ui.html`
- **RabbitMQ Management**: `http://localhost:15672` (`guest` / `guest`)
- **MailHog Web UI**: `http://localhost:8025`

### 2. Ambiente Cloud (Amazon EKS / Kubernetes)

```bash
# Aplicar infraestrutura (Bancos de Dados e RabbitMQ)
kubectl apply -f k8s/01-infrastructure.yaml

# Aplicar Deployments e Services dos microsserviços
kubectl apply -f k8s/02-microservices.yaml

# Aplicar regras do NGINX Ingress Controller
kubectl apply -f k8s/03-ingress.yaml
```

#### Rotas Expostas pelo NGINX Ingress Controller

- **APIs REST**:
  - `/users/**` → `ms-auth:8081`
  - `/companies/**`, `/categories/**`, `/statuses/**` → `ms-company:8082`
  - `/feedbacks/**`, `/replies/**` → `ms-feedback:8083`
  - `/notifications/**` → `ms-notification:8084`
- **Documentação OpenAPI / Swagger UI no Cluster**:
  - `/ms-auth/swagger-ui/index.html`
  - `/ms-company/swagger-ui/index.html`
  - `/ms-feedback/swagger-ui.html`
  - `/ms-notification/swagger-ui/index.html`
