# =============================================================================
# RAG Platform Makefile
# =============================================================================

.PHONY: help up down build logs ps clean backend frontend db-migrate \
        backend-test frontend-test lint format install seed

# Default target
help: ## Show this help message
	@echo ""
	@echo "  RAG Platform - Available Commands"
	@echo "  ================================="
	@echo ""
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  \033[36m%-22s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo ""

# =============================================================================
# Docker Compose
# =============================================================================

up: ## Start all services with Docker Compose
	docker compose up --build -d

up-logs: ## Start all services and follow logs
	docker compose up --build

down: ## Stop all services
	docker compose down

down-clean: ## Stop all services and remove volumes
	docker compose down -v --remove-orphans

build: ## Build all Docker images
	docker compose build

logs: ## Follow logs for all services
	docker compose logs -f

logs-backend: ## Follow backend logs
	docker compose logs -f backend

logs-frontend: ## Follow frontend logs
	docker compose logs -f frontend

ps: ## Show running containers
	docker compose ps

restart-backend: ## Restart only the backend service
	docker compose restart backend

restart-frontend: ## Restart only the frontend service
	docker compose restart frontend

# =============================================================================
# Backend
# =============================================================================

backend-run: ## Run backend locally (requires Java 21 + Maven)
	cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

backend-build: ## Build backend JAR
	cd backend && ./mvnw clean package -DskipTests

backend-test: ## Run backend tests
	cd backend && ./mvnw test

backend-lint: ## Run backend static analysis
	cd backend && ./mvnw checkstyle:check

backend-clean: ## Clean backend build artifacts
	cd backend && ./mvnw clean

# =============================================================================
# Frontend
# =============================================================================

frontend-install: ## Install frontend dependencies
	cd frontend && npm install

frontend-dev: ## Run frontend in development mode
	cd frontend && npm run dev

frontend-build: ## Build frontend for production
	cd frontend && npm run build

frontend-test: ## Run frontend tests
	cd frontend && npm test

frontend-lint: ## Run frontend linting
	cd frontend && npm run lint

frontend-type-check: ## Run TypeScript type checking
	cd frontend && npm run type-check

# =============================================================================
# Database
# =============================================================================

db-connect: ## Connect to PostgreSQL via psql
	docker compose exec postgres psql -U $${POSTGRES_USER:-raguser} -d $${POSTGRES_DB:-ragplatform}

db-migrate: ## Run Flyway migrations manually
	cd backend && ./mvnw flyway:migrate

db-seed: ## Seed the database with sample data
	docker compose exec postgres psql -U $${POSTGRES_USER:-raguser} -d $${POSTGRES_DB:-ragplatform} -f /docker-entrypoint-initdb.d/seed.sql

db-reset: ## Drop and recreate the database (WARNING: destroys data)
	docker compose exec postgres psql -U $${POSTGRES_USER:-raguser} -c "DROP DATABASE IF EXISTS $${POSTGRES_DB:-ragplatform};"
	docker compose exec postgres psql -U $${POSTGRES_USER:-raguser} -c "CREATE DATABASE $${POSTGRES_DB:-ragplatform};"

# =============================================================================
# Setup & Installation
# =============================================================================

install: ## Install all dependencies
	cd frontend && npm install

setup: install ## Full project setup
	@cp -n .env.example .env || true
	@echo "✓ Copied .env.example to .env"
	@echo "✓ Please edit .env and fill in your credentials"

# =============================================================================
# Health checks
# =============================================================================

health: ## Check health of all services
	@echo "Checking backend..."
	@curl -sf http://localhost:8080/actuator/health | jq .status || echo "Backend: DOWN"
	@echo "Checking frontend..."
	@curl -sf http://localhost:3000 > /dev/null && echo "Frontend: UP" || echo "Frontend: DOWN"
	@echo "Checking Langfuse..."
	@curl -sf http://localhost:3001/api/public/health | jq .status || echo "Langfuse: DOWN"

# =============================================================================
# Cleanup
# =============================================================================

clean: down-clean ## Full cleanup (removes containers, volumes, build artifacts)
	cd backend && ./mvnw clean || true
	rm -rf frontend/node_modules frontend/.next frontend/out || true
	@echo "✓ Cleanup complete"
