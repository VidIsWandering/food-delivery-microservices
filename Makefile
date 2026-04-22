# ============================================================================
# Food Delivery Microservices - Makefile
# Top-level commands for development, testing, and deployment
# ============================================================================

.PHONY: help dev test lint logs local-setup local-down seed health-check

# Default target
help: ## Show this help message
	@echo "Food Delivery Microservices - Available Commands"
	@echo "================================================"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ============================================================================
# Local Development
# ============================================================================

local-setup: ## Setup local K8s cluster (Kind) + install all infrastructure
	python3 scripts/local_setup.py setup

local-down: ## Tear down local K8s cluster
	python3 scripts/local_setup.py teardown

dev: ## Start dev mode for a service (usage: make dev svc=order-service)
	@if [ -z "$(svc)" ]; then echo "Usage: make dev svc=<service-name>"; exit 1; fi
	cd services/$(svc) && skaffold dev --port-forward

# ============================================================================
# Testing
# ============================================================================

test: ## Run tests for a service (usage: make test svc=order-service)
	@if [ -z "$(svc)" ]; then echo "Usage: make test svc=<service-name>"; exit 1; fi
	@echo "Running tests for $(svc)..."
	@if [ -f "services/$(svc)/pom.xml" ]; then \
		cd services/$(svc) && mvn test; \
	elif [ -f "services/$(svc)/go.mod" ]; then \
		cd services/$(svc) && go test ./...; \
	elif [ -f "services/$(svc)/package.json" ]; then \
		cd services/$(svc) && npm test; \
	fi

test-all: ## Run tests for all services
	@for dir in services/*/; do \
		svc=$$(basename $$dir); \
		echo "\n=== Testing $$svc ==="; \
		$(MAKE) test svc=$$svc || true; \
	done

# ============================================================================
# Linting
# ============================================================================

lint: ## Lint a service (usage: make lint svc=order-service)
	@if [ -z "$(svc)" ]; then echo "Usage: make lint svc=<service-name>"; exit 1; fi
	@echo "Linting $(svc)..."
	@if [ -f "services/$(svc)/pom.xml" ]; then \
		cd services/$(svc) && mvn checkstyle:check; \
	elif [ -f "services/$(svc)/go.mod" ]; then \
		cd services/$(svc) && golangci-lint run; \
	elif [ -f "services/$(svc)/package.json" ]; then \
		cd services/$(svc) && npm run lint; \
	fi

# ============================================================================
# Utilities
# ============================================================================

logs: ## Tail logs for a service (usage: make logs svc=order-service)
	@if [ -z "$(svc)" ]; then echo "Usage: make logs svc=<service-name>"; exit 1; fi
	kubectl logs -f -l app=$(svc) -n food-app --tail=100

seed: ## Load sample data into the system
	python3 scripts/seed_data.py

health-check: ## Check health of all services
	python3 scripts/health_check.py

# ============================================================================
# Docker
# ============================================================================

build: ## Build Docker image for a service (usage: make build svc=order-service)
	@if [ -z "$(svc)" ]; then echo "Usage: make build svc=<service-name>"; exit 1; fi
	docker build -t food-delivery/$(svc):latest services/$(svc)

# ============================================================================
# Helm / Deployment
# ============================================================================

helm-lint: ## Lint all Helm charts
	@for chart in deployments/helm/charts/*/; do \
		echo "Linting $$chart..."; \
		helm lint $$chart || true; \
	done

deploy-infra: ## Deploy infrastructure (Kafka, PostgreSQL, Redis) to K8s
	helm upgrade --install postgresql bitnami/postgresql -n databases --create-namespace -f deployments/infrastructure/postgresql/values.yaml
	helm upgrade --install redis bitnami/redis -n databases --create-namespace -f deployments/infrastructure/redis/values.yaml
	kubectl apply -f deployments/infrastructure/kafka/ -n kafka

deploy-apps: ## Deploy all application services via Helm
	helm upgrade --install food-delivery deployments/helm/ -n food-app --create-namespace
