# Memory Management MCP — API & Tools Reference

Índice global de endpoints REST y MCP tools expuestos por el servidor.

## MCP Tools

| Tool | Descripción | Doc |
|------|-------------|-----|
| `queryMemory` | Búsqueda semántica en todo el historial git indexado | [→ doc](./tools/query-memory.md) |
| `queryCode` | Búsqueda semántica solo en cambios de código fuente | [→ doc](./tools/query-code.md) |
| `queryDocs` | Búsqueda semántica solo en cambios de documentación | [→ doc](./tools/query-docs.md) |
| `querySkills` | Búsqueda semántica en el catálogo de skills del proyecto | [→ doc](./tools/query-skills.md) |
| `getIndexedCommits` | Retorna hashes de commits ya indexados | [→ doc](./tools/get-indexed-commits.md) |
| `setupProject` | Inicializa entorno del agente en un proyecto | [→ doc](./tools/setup-project.md) |
| `confirmSetup` | Valida hash del blueprint y marca setup como completado | [→ doc](./tools/setup-project.md#confirmsetup) |

## REST Endpoints

| Método | Path | Descripción | Doc |
|--------|------|-------------|-----|
| `POST` | `/api/projects` | Crear proyecto (retorna projectId + apiKey) | [→ doc](./endpoints/create-project.md) |
| `POST` | `/internal/memory/batch` | Indexar commits en batch + enrichment automático | [→ doc](./endpoints/batch-index.md) |
| `GET` | `/internal/memory/commits` | Obtener commits ya indexados | [→ doc](./endpoints/get-commits.md) |

---

## Estándar de Documentación

Cada archivo de documentación sigue esta estructura:

1. **Resumen** — Qué resuelve (2-3 líneas)
2. **Diagrama de Flujo** — Paso a paso con bifurcaciones (mermaid)
3. **Arquitectura** — Estructura de carpetas mostrando solo archivos involucrados, con `(n)` indicando orden de ejecución
4. **Contrato** — Request/Response (endpoints) o Params/Return (tools)
