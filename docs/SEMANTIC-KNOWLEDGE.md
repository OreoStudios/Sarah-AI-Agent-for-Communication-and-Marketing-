# Semantic knowledge

Set `SARAH_MEMORY_ENABLED=true` and `OPENAI_API_KEY` for the backend. Only enter business facts approved for transfer to the embedding provider; do not submit secrets or personal data. `POST /api/v1/memory` with kind/refId/content; `GET /api/v1/memory/recall?query=...&topK=5`. Both require OWNER. No automatic ingestion. Existing pgvector schema is used. Automatic RAG, deletion and retention controls remain separate work.
