# MCP Tool: queryDocs

## Resumen

Búsqueda semántica restringida a cambios de documentación (.md, .rst, .adoc) y API specs (openapi.yaml, swagger). Excluye código y configuración.

---

## Parámetros

| Param | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `prompt` | String | Sí | Descripción en lenguaje natural de la documentación que se busca |
| `projectId` | String (UUID) | Sí | ID del proyecto |
| `limit` | int | Sí | Máximo de resultados (1–10 recomendado) |

## Retorno

Mismo schema que `queryMemory` — solo filtra por `kind = 'doc'`.

## Cuándo usar

- Buscar decisiones de diseño: "por qué se eligió Oracle sobre PostgreSQL"
- Encontrar guías: "cómo configurar el entorno de desarrollo"
- API contracts: "esquema del endpoint de batch indexing"
