# Echobox — Arquitetura de Microsserviços Cloud Native (Amazon EKS)

Projeto da Avaliação Somativa N2 da disciplina de **Backend Cloud Computing** (Prof. Geucimar Briatore).
Conversão completa do sistema monolítico **Echobox** para uma arquitetura distribuída de **Microsserviços Cloud Native** pronta para execução local (`Docker Compose`) e em nuvem na **Amazon AWS — Amazon EKS (Elastic Kubernetes Service)**.

---

## Atendimento aos Requisitos da Avaliação Somativa N2

| Requisito Oficial da Avaliação | Implementação no Projeto Echobox Micro |
| :--- | :--- |
| **1. Desenvolvimento individual** | **4 microsserviços independentes** (`ms-auth`, `ms-company`, `ms-feedback`, `ms-notification`), permitindo 1 integrante responsável por cada serviço. |
| **2. Diversidade tecnológica** | **2 Linguagens distintas**: Java 21 (Spring Boot 3.5) + Python 3.11 (FastAPI).<br>**2 SGBDs distintos**: PostgreSQL (Relacional/SQL) + MongoDB ( Orientado a Documentos/NoSQL). |
| **3. Conteinerização** | Todos os 4 microsserviços possuem `Dockerfile` próprio e script automatizado de build/push para o Docker Hub ([`scripts/build-and-push.sh`](./scripts/build-and-push.sh)). |
| **4. Amazon EKS** | Manifestos Kubernetes completos em [`k8s/`](./k8s) com `Deployments`, `Pods` e `Services`, mais script de deploy no EKS ([`scripts/deploy-eks.sh`](./scripts/deploy-eks.sh)). |
| **5. Database per Service** | Persistência 100% isolada:<br>• `ms-auth` → `postgres-auth` (`echobox_auth`)<br>• `ms-company` → `postgres-company` (`echobox_company`)<br>• `ms-feedback` → `mongo-feedback` (`echobox_feedback`) |
| **6. Comunicação síncrona (REST)** | O `ms-feedback` (Python) consome a API REST do `ms-company` (Java) em `http://ms-company:8082/companies/{id}` para enriquecer feedbacks e obter o e-mail da empresa destinatária. |
| **7. Comunicação assíncrona (RabbitMQ)** | Ao criar um Feedback ou Reply, o `ms-feedback` publica um evento na fila `echobox.notifications` do **RabbitMQ**, que é consumido pelo `ms-notification` para envio de e-mail via SMTP. |
| **8. Service Discovery** | Toda comunicação interna usa exclusivamente os nomes estáveis dos Services Kubernetes (`postgres-auth`, `postgres-company`, `mongo-feedback`, `rabbitmq`, `ms-company`), sem depender de IPs de Pods. |
| **9. Escalabilidade horizontal e balanceamento** | `ms-feedback` configurado com **3 réplicas** e `ms-company` com **2 réplicas** em [`k8s/02-microservices.yaml`](./k8s/02-microservices.yaml). Todos os serviços retornam o header `X-Served-By-Pod` e possuem endpoint `/instance` que exibe o nome do Pod que atendeu a requisição. |
| **10. NGINX Ingress Controller** | Configurado em [`k8s/03-ingress.yaml`](./k8s/03-ingress.yaml) (`ingressClassName: nginx`), expondo externamente `/users`, `/companies`, `/categories`, `/statuses`, `/feedbacks`, `/replies` e `/notifications`. |

---

## Arquitetura no Amazon EKS

```
                       CLIENTE / BROWSER / INSOMNIA
                                    |
                                    v
                                AMAZON AWS
                                    |
                                    v
                                AMAZON EKS
                                    |
                                    v
                         NGINX Ingress Controller
                      (k8s/03-ingress.yaml - Regras)
                                    |
       +-------------------+--------+--------+-------------------+
       | /users            | /companies      | /feedbacks        | /notifications
       |                   | /categories     | /replies          |
       |                   | /statuses       |                   |
       v                   v                 v                   v
 Service ms-auth   Service ms-company  Service ms-feedback  Service ms-notification
    ClusterIP          ClusterIP           ClusterIP             ClusterIP
       |                   |    ^            |         \             ^
       v                   v    | (REST)     v          \            |
  Pod ms-auth       Pods ms-company    Pods ms-feedback  \           |
  (1 réplica)        (2 réplicas)       (3 réplicas)      \          |
   Java/Spring        Java/Spring      Python/FastAPI      \         |
       |                   |                 |              v        |
       v                   v                 v             RabbitMQ -+
Service postgres-auth  Service postgres-  Service mongo-   (Fila AMQP)
       |                 company            feedback
       v                   |                 |
  PostgreSQL A             v                 v
 (echobox_auth)      PostgreSQL B         MongoDB
                   (echobox_company) (echobox_feedback)
```

---

## Execução Local com Docker Compose

Para testar toda a arquitetura localmente antes de subir na AWS:

```bash
docker compose up --build -d
```

- **NGINX Ingress Local (Ponto único de entrada)**: `http://localhost:8080`
- **MailHog (Visualizador de E-mails)**: `http://localhost:8025`
- **RabbitMQ Management**: `http://localhost:15672` (`guest` / `guest`)
- **Swagger UI (`ms-auth`)**: `http://localhost:8081/swagger-ui.html`
- **Swagger UI (`ms-company`)**: `http://localhost:8082/swagger-ui.html`
- **Swagger UI (`ms-feedback`)**: `http://localhost:8083/swagger-ui.html`
- **Swagger UI (`ms-notification`)**: `http://localhost:8084/swagger-ui.html`

---

## Implantação no Amazon EKS e Roteiro de Apresentação

Consulte o guia completo passo a passo em **[`GUIA_AWS_EKS.md`](./GUIA_AWS_EKS.md)** para:
1. Construir e enviar as imagens para o Docker Hub (`./scripts/build-and-push.sh <usuario>`).
2. Subir o cluster Amazon EKS e o NGINX Ingress Controller (`./scripts/deploy-eks.sh`).
3. Executar os comandos de demonstração para o professor em sala de aula.
