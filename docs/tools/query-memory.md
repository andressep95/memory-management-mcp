# MCP Tool: queryMemory

## Resumen

Búsqueda semántica sobre todo el historial git indexado (código + docs + config). Retorna los commits más relevantes por similitud vectorial con el prompt.

---

## Parámetros

| Param | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `prompt` | String | Sí | Descripción en lenguaje natural de lo que se busca |
| `projectId` | String (UUID) | Sí | ID del proyecto |
| `limit` | int | Sí | Máximo de resultados (1–10 recomendado) |

## Retorno

```json
[
  {
    "commitHash": "7545dd9",
    "filePath": "src/main/java/.../SetupMcpTools.java",
    "branch": "main",
    "author": "Andres Sepulveda",
    "intent": "feat",
    "what": "Adds SetupValidationMcpTools that verifies blueprint hash",
    "why": "Agents need a server-side gate to confirm scaffold was applied",
    "kind": "code",
    "language": "java",
    "tags": ["feat", "addition", "source", "setup"],
    "score": 0.82
  }
]
```

## Variantes

| Tool | Filtro `kind` |
|------|---------------|
| `queryMemory` | Todos (code + doc + config) |
| `queryCode` | Solo `kind = 'code'` |
| `queryDocs` | Solo `kind = 'doc'` |

## Cuándo usar cada uno

- **queryMemory** — Búsqueda amplia: "¿cómo se implementó X?", "¿por qué se cambió Y?"
- **queryCode** — Buscar implementaciones específicas: "batch processing de commits"
- **queryDocs** — Buscar decisiones documentadas: "arquitectura del sistema de embeddings"
