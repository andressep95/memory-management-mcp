# MCP Tool: querySkills

## Resumen

Búsqueda semántica sobre los chunks de skills habilitados para el proyecto. Retorna instrucciones y patrones relevantes para la tarea actual.

---

## Parámetros

| Param | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `prompt` | String | Sí | Descripción de la tarea o tema |
| `projectId` | String (UUID) | Sí | ID del proyecto (scopes a skills habilitados) |
| `limit` | int | Sí | Máximo de resultados (1–10 recomendado) |

## Retorno

```json
[
  {
    "skillName": "commit",
    "chunkTitle": "Commit Format",
    "content": "type(scope): concise description in imperative mood\n\nwhat: ...\nwhy: ...\nbreaking: ...",
    "score": 0.91
  }
]
```

## Cuándo usar

- **Siempre antes de cualquier tarea** — es obligatorio según el protocolo MCP
- Buscar instrucciones: "cómo formatear un commit"
- Encontrar patrones del proyecto: "convenciones de naming"
