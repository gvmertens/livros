# Personal Library Manager — Product Overview

A full-stack web application for tracking personal reading activity. Users manage their reading records (status, ratings, reviews) against a book catalog maintained by administrators.

## Modules

- **Identity** — user registration, login (JWT), role management, profile CRUD
- **Catalog** — admin-managed books, authors, publishers
- **Reading** — user reading records with status, rating, review
- **Recommendation** — stub only; future LLM-based suggestions via event consumption

## Roles

- `USER` — browse catalog, manage own reading records
- `ADMIN` — all USER permissions + full catalog CRUD, update any reading, promote user roles

## Key Business Rules

- Passwords stored as bcrypt hash (cost 12); never returned in responses
- JWT (RS256, 1-hour expiry) required for all endpoints except `/auth/register` and `/auth/login`
- One reading record per user per book (unique constraint)
- Rating range: 0.0–10.0; status FINISHED requires `startedAt` to be set
- Pagination defaults: page=0, size=20, max size=100
- Domain events published for: `user.created`, `book.*`, `reading.created`, `rating.updated`, `review.submitted`

## Architecture Intent

Modular monolith in Phase 1. Module boundaries are designed for future microservice extraction. The `EventBus` interface is swappable (in-process CDI now → Kafka/SQS later) without changing any service code.
