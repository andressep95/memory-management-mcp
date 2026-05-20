# POST /internal/memory/batch

## Resumen

Único punto de entrada para indexar commits en la memoria semántica del proyecto. Recibe un batch de cambios por archivo (1 entry = 1 archivo en 1 commit), genera embeddings locales con DJL, y los persiste en Oracle 23ai. Si detecta commits con campos semánticos pobres (legacy, sin `intent`/`why`), los encola automáticamente para enriquecimiento asíncrono via LLM.

---

## Diagrama de Flujo — Extracción y Agrupación de Datos (Cliente)

Antes de llegar al endpoint, `extract_changes.py` extrae y agrupa la data desde git:

```mermaid
flowchart TD
    A[extract_changes.py] --> B{Modo?}
    B -->|--all| C["git log --reverse --format=%H<br/>→ lista de todos los commits"]
    B -->|HEAD default| D[Solo procesa último commit]
    
    C --> E[GET /internal/memory/commits<br/>→ set de hashes ya indexados]
    E --> F[Filtrar: solo commits no indexados]
    F --> G[Para cada commit]
    D --> G

    G --> H["git log -1 → metadata<br/>(hash, author, branch, subject, body)"]
    H --> I["parse_body(body)<br/>→ what, why, breaking"]
    I --> J["parse_commit_parts(subject)<br/>→ type, scope"]
    J --> K["git diff-tree --name-only<br/>→ lista de archivos modificados"]

    K --> L[Para cada archivo del commit]
    L --> M["git diff parent..ref -- file<br/>→ diff completo"]
    M --> N["parse_hunks(diff)<br/>→ bloques @@ con líneas"]
    N --> O["Clasificar archivo:<br/>memory_kind → code/doc/config<br/>language → java/python/etc"]
    O --> P["Construir entry:<br/>{commit, author, branch, file,<br/>intent, what, why, kind, lang,<br/>tags, hunks}"]

    P --> Q{¿Acumulados >= 25?}
    Q -->|Sí| R["POST /internal/memory/batch<br/>flush batch de 25 entries"]
    Q -->|No| S[Siguiente archivo/commit]
    R --> S

    S --> T{¿Más commits?}
    T -->|Sí| G
    T -->|No| U[Flush entries restantes]
    U --> V[Fin — reporte de totales]
```

**Granularidad:** 1 entry = 1 archivo modificado en 1 commit. Un commit que toca 5 archivos genera 5 entries.

**Agrupación por batch:** Se acumulan entries hasta llegar a 25 (`MCP_BATCH_SIZE`), luego se envían al endpoint. Al terminar todos los commits, se hace un flush final con los restantes.

**Campos clave por entry:**

| Campo | Origen | Ejemplo |
|-------|--------|---------|
| `commitHash` | `git log -1 --format=%h` | `a1b2c3d` |
| `branch` | `git branch --contains` | `main` |
| `author` | `git log -1 --format=%an` | `dev@example.com` |
| `filePath` | `git diff-tree --name-only` | `src/auth/middleware.go` |
| `intent` | `parse_commit_parts(subject)` → type | `fix` (null si legacy) |
| `what` | `parse_body(body)` → línea `what:` | Fallback: subject completo |
| `why` | `parse_body(body)` → línea `why:` | null si legacy |
| `kind` | `memory_kind(filePath)` | `code` / `doc` / `config` |
| `language` | extensión del archivo | `go` / `java` / `python` |
| `tags` | `[type, changeType, fileKind, scope]` | `["fix", "modification", "source"]` |
| `hunks` | `parse_hunks(diff)` → bloques @@ | `[{linesStart, linesEnd, symbol, changeType, hunkDiff}]` |

---

## Diagrama de Flujo — Procesamiento en el Endpoint (Server)

```mermaid
flowchart TD
    A["POST /internal/memory/batch<br/>{apiKey, entries[]}"] --> B[Resolver projectId via apiKey]
    B --> C{apiKey válida?}
    C -->|No| D[HTTP 401 Unauthorized]
    C -->|Sí| E[Filtrar entries ya indexados<br/>por commit_hash:file_path]
    E --> F[Procesar en chunks de 50]
    F --> G[buildEmbedText<br/>intent + what + why + filePath]
    G --> H[DJL embedBatch → vectors 384 dims]
    H --> I[MemoryChange.index → crear entidades]
    I --> J[BATCH INSERT memory_changes]
    J --> K[BATCH UPDATE embedding]
    K --> L[Evaluar needsEnrichment por entry]
    L --> M{¿Hay entries pobres?}
    M -->|No| N["Response {inserted, skipped, enrichmentQueued: 0}"]
    M -->|Sí| O[INSERT enrichment_queue<br/>status=PENDING]
    O --> P["trigger() → EnrichmentProcessor"]
    P --> Q["Response {inserted, skipped, enrichmentQueued: N}"]

    P --> R[Thread async — drainQueue]
    R --> S[SELECT 5 PENDING<br/>FOR UPDATE SKIP LOCKED]
    S --> T{¿Cola vacía?}
    T -->|Sí| U[Thread muere]
    T -->|No| V[Lee MemoryChange]
    V --> W[OpenAI API<br/>commitMsg + filePath + diff]
    W --> X{¿OK?}
    X -->|Sí| Y[UPDATE intent, what, why<br/>+ re-genera embedding]
    Y --> Z[status = DONE]
    X -->|No| AA{attempts >= 3?}
    AA -->|No| AB[status = PENDING — retry]
    AA -->|Sí| AC[status = FAILED]
    Z --> S
    AB --> S
    AC --> S
```

---

## Arquitectura — Archivos Involucrados

```
src/main/java/com/cloudcentinel/memory_management_mcp/
├── infrastructure/
│   ├── rest/
│   │   └── (1) MemoryRestController.java              ← Recibe HTTP, resuelve projectId
│   ├── embedding/
│   │   └── (4) DjlEmbeddingService.java               ← Genera embeddings locales
│   ├── persistence/memory/
│   │   ├── (6) MemoryChangeRepositoryAdapter.java      ← INSERT + UPDATE Oracle
│   │   └── (8) EnrichmentTaskRepositoryAdapter.java    ← Cola persistente
│   └── enrichment/
│       ├──     EnrichmentLlmClient.java                ← Interface intercambiable
│       └── (10) OpenAiEnrichmentClient.java            ← Llama OpenAI API
├── application/memory/
│   ├── (2) BatchIndexMemoryHandler.java                ← Orquesta: dedup, embed, persist, detect, enqueue
│   ├── (3) KindClassifier.java                        ← Clasifica archivo → code/doc/config
│   └── (9) EnrichmentProcessor.java                   ← Drain loop async
├── domain/memory/
│   ├── entity/
│   │   ├── (5) MemoryChange.java                      ← Entidad de dominio
│   │   └── (7) EnrichmentTask.java                    ← Entidad de cola
│   └── repository/
│       ├──     MemoryChangeRepository.java            ← Port
│       └──     EnrichmentTaskRepository.java          ← Port
docker/oracle/init/
│   └──     01-schema.sql                              ← Tabla enrichment_queue
src/main/resources/
│   └──     application.yaml                           ← Config enrichment.*
```

---

## Contrato

### Request

```http
POST /internal/memory/batch
Content-Type: application/json
```

```json
{
  "apiKey": "project-api-key",
  "entries": [
    {
      "commitHash": "a1b2c3d",
      "branch": "main",
      "author": "dev@example.com",
      "filePath": "src/auth/middleware.go",
      "intent": "fix",
      "what": "Rejects expired JWT tokens",
      "why": "Expired tokens allowed unauthorized access",
      "kind": "code",
      "language": "go",
      "tags": ["fix", "modification", "source"],
      "rawDiff": "+if token.Expired() { return 401 }",
      "contentBefore": null,
      "contentAfter": null,
      "hunks": [
        {
          "linesStart": 42,
          "linesEnd": 45,
          "symbol": "func AuthMiddleware",
          "changeType": "modification",
          "hunkDiff": "+if token.Expired() {\n+  return 401\n+}"
        }
      ]
    }
  ]
}
```

### Response

```json
{
  "inserted": 47,
  "skipped": 3,
  "enrichmentQueued": 32
}
```

| Campo | Descripción |
|-------|-------------|
| `inserted` | Entries indexados exitosamente |
| `skipped` | Entries que ya existían (dedup por commit_hash:file_path) |
| `enrichmentQueued` | Entries detectados como pobres, encolados para LLM |

### Detección de campos pobres

| Condición | Se encola |
|-----------|-----------|
| `intent` es null o vacío | ✅ |
| `why` es null o vacío | ✅ |
| `what` contiene `:` y > 50 chars (parece subject crudo) | ✅ |

### Configuración del enrichment

```yaml
enrichment:
  enabled: true|false
  llm:
    provider: openai
    openai:
      api-key: ${OPENAI_API_KEY}
      model: gpt-4o-mini
      base-url: https://api.openai.com/v1
```

### Errores

| HTTP | Causa |
|------|-------|
| 401 | apiKey no reconocida |
| 500 | Error de Oracle o embedding |
