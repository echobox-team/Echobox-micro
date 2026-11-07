import json
import logging
import os
import socket
from datetime import datetime
from typing import List, Optional

import httpx
import jwt
import pika
from fastapi import Depends, FastAPI, HTTPException, Request, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field
from pymongo import MongoClient, ReturnDocument

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ms-feedback")

# Environment variables (Cloud Native - Aula 03b & Aula 04)
MONGO_HOST = os.getenv("MONGO_HOST", "localhost")
MONGO_PORT = int(os.getenv("MONGO_PORT", "27017"))
MONGO_DB = os.getenv("MONGO_DB", "echobox_feedback")

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
RABBITMQ_QUEUE = os.getenv("RABBITMQ_QUEUE", "echobox.notifications")

MS_COMPANY_URL = os.getenv("MS_COMPANY_URL", "http://ms-company:8082")
JWT_SECRET = os.getenv(
    "JWT_SECRET",
    "mySecretKey123456789012345678901234567890123456789012345678901234567890",
)

POD_NAME = os.getenv("HOSTNAME", socket.gethostname())

app = FastAPI(
    title="Echobox - ms-feedback API (Python + MongoDB)",
    version="1.0.0",
    description="Microsservico de Dominio de Feedbacks e Replies em Python (FastAPI) com persistencia NoSQL (MongoDB) e mensageria RabbitMQ",
    docs_url="/swagger-ui.html",
    openapi_url="/api-docs",
)


@app.middleware("http")
async def add_pod_header(request: Request, call_next):
    response = await call_next(request)
    response.headers["X-Served-By-Pod"] = POD_NAME
    return response


security = HTTPBearer(auto_error=False)

# MongoDB Connection
mongo_client = MongoClient(host=MONGO_HOST, port=MONGO_PORT)
db = mongo_client[MONGO_DB]
feedbacks_col = db["feedbacks"]
replies_col = db["replies"]
counters_col = db["counters"]


def get_next_sequence(name: str) -> int:
    ret = counters_col.find_one_and_update(
        {"_id": name},
        {"$inc": {"seq": 1}},
        upsert=True,
        return_document=ReturnDocument.AFTER,
    )
    return ret["seq"]


# JWT Validation Dependency
def verify_jwt(credentials: Optional[HTTPAuthorizationCredentials] = Depends(security)):
    if credentials is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={
                "errorCode": "ERR_9000",
                "message": "Unauthorized access - authentication required",
                "status": 401,
            },
        )
    token = credentials.credentials
    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=["HS256"])
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"errorCode": "ERR_9002", "message": "Token has expired", "status": 401},
        )
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"errorCode": "ERR_9001", "message": "Invalid or malformed token", "status": 401},
        )


# RabbitMQ Publisher (Aula 07 - Integracao por Mensageria)
def publish_notification_event(event_type: str, payload: dict):
    try:
        connection = pika.BlockingConnection(
            pika.ConnectionParameters(host=RABBITMQ_HOST, port=RABBITMQ_PORT, connection_attempts=2, retry_delay=1)
        )
        channel = connection.channel()
        channel.queue_declare(queue=RABBITMQ_QUEUE, durable=True)
        message = {
            "eventType": event_type,
            "timestamp": datetime.utcnow().isoformat(),
            "sourcePod": POD_NAME,
            "data": payload,
        }
        channel.basic_publish(
            exchange="",
            routing_key=RABBITMQ_QUEUE,
            body=json.dumps(message),
            properties=pika.BasicProperties(delivery_mode=2, content_type="application/json"),
        )
        connection.close()
        logger.info(f"Published event {event_type} to RabbitMQ queue '{RABBITMQ_QUEUE}'")
    except Exception as e:
        logger.warning(f"Could not publish event to RabbitMQ ({RABBITMQ_HOST}:{RABBITMQ_PORT}): {e}")


# Synchronous communication with ms-company via Service Discovery (Aula 02 & Aula 04)
def fetch_from_ms_company(path: str, fallback: dict) -> dict:
    try:
        with httpx.Client(timeout=2.0) as client:
            resp = client.get(f"{MS_COMPANY_URL}{path}")
            if resp.status_code == 200:
                return resp.json()
    except Exception as e:
        logger.warning(f"Could not reach ms-company at {MS_COMPANY_URL}{path}: {e}")
    return fallback


# Schemas matching original Java JSON format
class FeedbackIn(BaseModel):
    title: str = Field(..., min_length=1)
    review: str = Field(..., min_length=1)
    user: int
    company: int
    category: int
    status: int


class FeedbackOut(FeedbackIn):
    id: int


class ReplyIn(BaseModel):
    title: str = Field(..., min_length=1)
    review: str = Field(..., min_length=1)
    feedback: int
    user: int


class ReplyOut(ReplyIn):
    id: int
    createdDate: str


@app.on_event("startup")
def seed_initial_data():
    try:
        if feedbacks_col.count_documents({}) == 0:
            initial_feedbacks = [
                {"title": "Excelente Qualidade do Produto!", "review": "O produto superou minhas expectativas em termos de qualidade e funcionalidades. Muito satisfeito com a compra.", "user": 2, "company": 1, "category": 1, "status": 3},
                {"title": "Resposta Lenta do Atendimento", "review": "Levei vários dias para receber uma resposta do atendimento ao cliente. Pode ser melhorado.", "user": 3, "company": 2, "category": 2, "status": 2},
                {"title": "Problemas de Navegação no Site", "review": "Tive dificuldade para encontrar as informações que precisava no site. A navegação poderia ser mais intuitiva.", "user": 4, "company": 1, "category": 3, "status": 1},
                {"title": "Entrega Muito Rápida", "review": "Recebi meu pedido muito mais rápido do que esperava. Excelente logística!", "user": 5, "company": 3, "category": 4, "status": 3},
                {"title": "Preços Competitivos", "review": "Os preços são muito competitivos para as funcionalidades oferecidas. Bom custo-benefício.", "user": 6, "company": 4, "category": 5, "status": 3},
                {"title": "Bug no Aplicativo Mobile", "review": "Encontrei um bug crítico ao usar o aplicativo mobile no Android. Não consegui finalizar a compra.", "user": 7, "company": 1, "category": 6, "status": 2},
                {"title": "Solicitação: Modo Escuro", "review": "Adoraria ver uma opção de modo escuro no aplicativo. Melhoraria muito a experiência do usuário.", "user": 8, "company": 1, "category": 7, "status": 1},
                {"title": "Experiência Geral Excelente", "review": "Tive uma experiência maravilhosa com seu serviço. Continue com o bom trabalho!", "user": 1, "company": 1, "category": 8, "status": 3},
            ]
            for fb in initial_feedbacks:
                fb["id"] = get_next_sequence("feedback_id")
                feedbacks_col.insert_one(fb)

        if replies_col.count_documents({}) == 0:
            initial_replies = [
                {"title": "Obrigado pelo seu feedback!", "review": "Realmente apreciamos sua avaliação positiva. Ficamos felizes que você esteja satisfeito com a qualidade do nosso produto.", "feedback": 1, "user": 1},
                {"title": "Estamos trabalhando nisso", "review": "Obrigado por nos alertar sobre isso. Estamos trabalhando para melhorar nossos tempos de resposta.", "feedback": 2, "user": 1},
                {"title": "Melhorias na navegação em breve", "review": "Anotamos seu feedback e estamos planejando um redesign do site para melhorar a navegação.", "feedback": 3, "user": 1},
            ]
            for rp in initial_replies:
                rp["id"] = get_next_sequence("reply_id")
                rp["createdDate"] = datetime.utcnow().isoformat()
                replies_col.insert_one(rp)
    except Exception as e:
        logger.warning(f"MongoDB seed skipped or deferred: {e}")


# ==================== INSTANCE / LOAD BALANCING ENDPOINT ====================

@app.get("/feedbacks/instance", tags=["Feedback (ms-feedback)"])
def get_instance_info():
    """
    Endpoint publico para demonstrar Escalabilidade Horizontal e Balanceamento de Carga
    entre multiplas replicas (Pods) no Kubernetes / Amazon EKS.
    """
    return {
        "service": "ms-feedback",
        "language": "Python 3.11 (FastAPI)",
        "database": f"MongoDB ({MONGO_HOST}/{MONGO_DB})",
        "podName": POD_NAME,
    }


# ==================== FEEDBACK ENDPOINTS ====================

@app.get("/feedbacks", response_model=List[FeedbackOut], tags=["Feedback (ms-feedback)"])
def get_all_feedbacks(claims: dict = Depends(verify_jwt)):
    docs = list(feedbacks_col.find({}, {"_id": 0}))
    return docs


@app.get("/feedbacks/{id}", response_model=FeedbackOut, tags=["Feedback (ms-feedback)"])
def get_feedback_by_id(id: int, claims: dict = Depends(verify_jwt)):
    doc = feedbacks_col.find_one({"id": id}, {"_id": 0})
    if not doc:
        raise HTTPException(status_code=404, detail={"errorCode": "ERR_3000", "message": "Feedback not found", "status": 404})
    return doc


@app.get("/feedbacks/{id}/enriched", tags=["Feedback (ms-feedback)"])
def get_enriched_feedback(id: int, claims: dict = Depends(verify_jwt)):
    """
    Demonstrates synchronous REST communication via Kubernetes Service Discovery
    between ms-feedback (Python/MongoDB) and ms-company (Java/PostgreSQL).
    """
    doc = feedbacks_col.find_one({"id": id}, {"_id": 0})
    if not doc:
        raise HTTPException(status_code=404, detail={"errorCode": "ERR_3000", "message": "Feedback not found", "status": 404})

    company_info = fetch_from_ms_company(
        f"/companies/{doc['company']}",
        {"id": doc["company"], "name": f"Company #{doc['company']}", "email": "contato@echobox.com"},
    )
    category_info = fetch_from_ms_company(
        f"/categories/{doc['category']}",
        {"id": doc["category"], "type": f"Category #{doc['category']}"},
    )
    status_info = fetch_from_ms_company(
        f"/statuses/{doc['status']}",
        {"id": doc["status"], "type": f"Status #{doc['status']}"},
    )
    replies = list(replies_col.find({"feedback": id}, {"_id": 0}))
    return {
        **doc,
        "servedByPod": POD_NAME,
        "companyDetails": company_info,
        "categoryDetails": category_info,
        "statusDetails": status_info,
        "replies": replies,
    }


@app.post("/feedbacks", response_model=FeedbackOut, status_code=status.HTTP_201_CREATED, tags=["Feedback (ms-feedback)"])
def create_feedback(feedback: FeedbackIn, claims: dict = Depends(verify_jwt)):
    new_id = get_next_sequence("feedback_id")
    doc = {"id": new_id, **feedback.model_dump()}
    feedbacks_col.insert_one(doc.copy())

    # Fetch company email via REST Service Discovery (ms-company) and publish event to RabbitMQ (ms-notification)
    company_info = fetch_from_ms_company(
        f"/companies/{feedback.company}",
        {"id": feedback.company, "name": "Empresa", "email": "contato@echobox.com"},
    )
    publish_notification_event(
        "FEEDBACK_CREATED",
        {
            "feedbackId": new_id,
            "title": feedback.title,
            "review": feedback.review,
            "companyId": feedback.company,
            "companyName": company_info.get("name", "Empresa"),
            "recipientEmail": company_info.get("email", "contato@echobox.com"),
        },
    )
    return doc


@app.put("/feedbacks/{id}", response_model=FeedbackOut, tags=["Feedback (ms-feedback)"])
def update_feedback(id: int, feedback: FeedbackIn, claims: dict = Depends(verify_jwt)):
    updated_doc = {"id": id, **feedback.model_dump()}
    res = feedbacks_col.find_one_and_replace({"id": id}, updated_doc, projection={"_id": 0}, return_document=ReturnDocument.AFTER)
    if not res:
        raise HTTPException(status_code=404, detail={"errorCode": "ERR_3000", "message": "Feedback not found", "status": 404})
    return res


@app.delete("/feedbacks/{id}", status_code=status.HTTP_204_NO_CONTENT, tags=["Feedback (ms-feedback)"])
def delete_feedback(id: int, claims: dict = Depends(verify_jwt)):
    res = feedbacks_col.delete_one({"id": id})
    if res.deleted_count == 0:
        raise HTTPException(status_code=404, detail={"errorCode": "ERR_3000", "message": "Feedback not found", "status": 404})
    replies_col.delete_many({"feedback": id})
    return None


# ==================== REPLY ENDPOINTS ====================

@app.get("/replies", response_model=List[ReplyOut], tags=["Reply (ms-feedback)"])
def get_all_replies(claims: dict = Depends(verify_jwt)):
    return list(replies_col.find({}, {"_id": 0}))


@app.get("/replies/{id}", response_model=ReplyOut, tags=["Reply (ms-feedback)"])
def get_reply_by_id(id: int, claims: dict = Depends(verify_jwt)):
    doc = replies_col.find_one({"id": id}, {"_id": 0})
    if not doc:
        raise HTTPException(status_code=404, detail={"errorCode": "ERR_4000", "message": "Reply not found", "status": 404})
    return doc


@app.post("/replies", response_model=ReplyOut, status_code=status.HTTP_201_CREATED, tags=["Reply (ms-feedback)"])
def create_reply(reply: ReplyIn, claims: dict = Depends(verify_jwt)):
    fb = feedbacks_col.find_one({"id": reply.feedback}, {"_id": 0})
    if not fb:
        raise HTTPException(status_code=404, detail={"errorCode": "ERR_3000", "message": "Feedback not found for reply", "status": 404})

    new_id = get_next_sequence("reply_id")
    doc = {
        "id": new_id,
        **reply.model_dump(),
        "createdDate": datetime.utcnow().isoformat(),
    }
    replies_col.insert_one(doc.copy())

    # Publish event to RabbitMQ (Aula 07)
    publish_notification_event(
        "REPLY_CREATED",
        {
            "replyId": new_id,
            "feedbackId": reply.feedback,
            "feedbackTitle": fb.get("title", ""),
            "title": reply.title,
            "review": reply.review,
            "recipientEmail": "usuario@echobox.com",
        },
    )
    return doc


@app.put("/replies/{id}", response_model=ReplyOut, tags=["Reply (ms-feedback)"])
def update_reply(id: int, reply: ReplyIn, claims: dict = Depends(verify_jwt)):
    existing = replies_col.find_one({"id": id}, {"_id": 0})
    if not existing:
        raise HTTPException(status_code=404, detail={"errorCode": "ERR_4000", "message": "Reply not found", "status": 404})
    updated_doc = {
        "id": id,
        **reply.model_dump(),
        "createdDate": existing.get("createdDate", datetime.utcnow().isoformat()),
    }
    replies_col.replace_one({"id": id}, updated_doc)
    return updated_doc


@app.delete("/replies/{id}", status_code=status.HTTP_204_NO_CONTENT, tags=["Reply (ms-feedback)"])
def delete_reply(id: int, claims: dict = Depends(verify_jwt)):
    res = replies_col.delete_one({"id": id})
    if res.deleted_count == 0:
        raise HTTPException(status_code=404, detail={"errorCode": "ERR_4000", "message": "Reply not found", "status": 404})
    return None
