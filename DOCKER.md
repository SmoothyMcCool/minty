# Minty Docker Deployment

This guide covers running Minty using Docker Compose.

## Prerequisites

- Docker and Docker Compose installed
- At least 8GB RAM available for containers
- For GPU acceleration (optional): NVIDIA Docker runtime

## Quick Start

### 1. Build and Start All Services

```bash
docker-compose up -d --build
```

This starts three containers:
- **minty-app** - The Minty application (Tomcat + Spring)
- **minty-mariadb** - MariaDB 11.8 database
- **minty-ollama** - Ollama LLM server

### 2. Pull Required Ollama Models

After the containers are running, pull the LLM models:

```bash
# Embedding model (required for document RAG)
docker exec -it minty-ollama ollama pull nomic-embed-text

# Chat models (pull at least one)
docker exec -it minty-ollama ollama pull gemma3:4b      # Small, fast
docker exec -it minty-ollama ollama pull gemma3:12b     # Medium
docker exec -it minty-ollama ollama pull llama3.2:3b    # Alternative small model
```

Note: The default model configured is `gpt-oss:20b`. If you don't have this model, either pull it or update the configuration to use a model you have.

### 3. Access Minty

Open your browser to: **http://localhost:8080/Minty/**

## Managing the Services

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f minty
docker-compose logs -f mariadb
docker-compose logs -f ollama
```

### Stop Services

```bash
docker-compose down
```

### Stop and Remove All Data

```bash
docker-compose down -v
```

### Restart a Service

```bash
docker-compose restart minty
```

## Configuration

### Environment Variables

The following environment variables can be set in `docker-compose.yml`:

| Variable | Default | Description |
|----------|---------|-------------|
| `MINTY_DATA_DIR` | `/app/data` | Data directory path |
| `MINTY_DB_URL` | `jdbc:mariadb://mariadb:3306/Minty` | Database JDBC URL |
| `MINTY_DB_USER` | `MintyUser` | Database username |
| `MINTY_DB_PASSWORD` | `hothamcakes` | Database password |
| `MINTY_OLLAMA_URI` | `http://ollama:11434` | Ollama server URL |

### Custom Configuration

To override the default `application.yaml`, uncomment the config volume mount in `docker-compose.yml`:

```yaml
volumes:
  - ./config/application.yaml:/usr/local/tomcat/conf/Minty/application.yaml:ro
```

## GPU Support (NVIDIA)

To enable GPU acceleration for Ollama, uncomment the deploy section in `docker-compose.yml`:

```yaml
ollama:
  # ... other config ...
  deploy:
    resources:
      reservations:
        devices:
          - driver: nvidia
            count: all
            capabilities: [gpu]
```

## Troubleshooting

### Database Tables Missing

If you see errors about missing tables (e.g., `SPRING_SESSION doesn't exist`), the database needs to be initialized:

```bash
# Option 1: Reset everything (loses data)
docker-compose down -v
docker-compose up -d

# Option 2: Manually run init script
docker exec -i minty-mariadb mariadb -u MintyUser -phothamcakes Minty < docker/init-db/01-schema.sql
```

### Application Won't Start

Check the logs for errors:

```bash
docker-compose logs minty
```

Common issues:
- Database not ready: Wait for MariaDB to fully start before Minty connects
- Ollama not running: Ensure the ollama container is healthy
- Missing directories: The entrypoint script should create these automatically

### Ollama Model Errors

If you get errors about missing models:

```bash
# List available models
docker exec -it minty-ollama ollama list

# Pull a missing model
docker exec -it minty-ollama ollama pull <model-name>
```

### Check Container Health

```bash
docker-compose ps
```

All services should show as "healthy" or "running".

## Ports

| Service | Port | Description |
|---------|------|-------------|
| Minty | 8080 | Web UI and API |
| MariaDB | 3306 | Database (exposed for debugging) |
| Ollama | 11434 | LLM API |

## Data Persistence

Data is persisted in Docker volumes:

- `minty-data` - Application data (documents, plugins, logs)
- `mariadb-data` - Database files
- `ollama-models` - Downloaded LLM models

To backup volumes:

```bash
docker run --rm -v minty_mariadb-data:/data -v $(pwd):/backup alpine tar czf /backup/mariadb-backup.tar.gz /data
```
