PYTHON_BIN := $(shell pip3 show mike 2>/dev/null | awk '/^Location/{print $$2}' | sed 's|lib/python/site-packages|bin|')
export PATH := $(PYTHON_BIN):$(PATH)

.PHONY: help install serve build deploy clean version

help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Available targets:'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  %-15s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

install: ## Install Python dependencies for documentation
	pip3 install -r requirements.txt

serve: ## Serve documentation locally at http://127.0.0.1:8000
	mkdocs serve

build: ## Build documentation site
	mkdocs build

deploy: ## Deploy documentation to GitHub Pages (manual)
	@if [ -z "$(VERSION)" ]; then \
		VERSION=$$(grep "pluginVersion" gradle.properties | cut -d'=' -f2 | tr -d ' '); \
	else \
		VERSION=$(VERSION); \
	fi; \
	echo "Deploying version $$VERSION"; \
	mike deploy $$VERSION latest --push --update-aliases --allow-empty; \
	mike set-default latest --push

clean: ## Clean build artifacts
	rm -rf site .mkdocs_cache

version: ## Show current version from gradle.properties
	@grep "pluginVersion" gradle.properties | cut -d'=' -f2 | tr -d ' '
