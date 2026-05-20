# MCP Tool: queryCode

## Resumen

Búsqueda semántica restringida a cambios de código fuente (.java, .py, .ts, .go, .rs, .sql, .sh, etc). Excluye documentación y configuración.

---

## Parámetros

| Param | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `prompt` | String | Sí | Descripción en lenguaje natural del código que se busca |
| `projectId` | String (UUID) | Sí | ID del proyecto |
| `limit` | int | Sí | Máximo de resultados (1–10 recomendado) |

## Retorno

Mismo schema que `queryMemory` — solo filtra por `kind = 'code'`.

## Cuándo usar

- Buscar implementaciones: "cómo se genera el embedding"
- Encontrar cambios en un módulo: "cambios recientes en el repositorio de proyectos"
- Entender patrones: "cómo se manejan las transacciones JPA"
