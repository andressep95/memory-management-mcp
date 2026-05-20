# MCP Tool: getIndexedCommits

## Resumen

Retorna el set de commit hashes ya indexados para un proyecto. Permite al agente verificar el estado de la memoria sin hacer queries semánticas.

---

## Parámetros

| Param | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `projectId` | String (UUID) | Sí | ID del proyecto |

## Retorno

```json
["7545dd9", "2c91c2e", "3642534", "a98969d", "..."]
```

Set de short hashes (7 chars) de commits indexados.

## Cuándo usar

- Verificar si un commit específico ya fue indexado
- Diagnosticar gaps en la memoria
- Antes de ejecutar re-indexación manual
