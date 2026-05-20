# POST /api/projects

## Resumen

Crea un nuevo proyecto y retorna su `projectId` (UUID) y `apiKey`. Estos valores son necesarios para todas las operaciones MCP y REST posteriores.

---

## Contrato

### Request

```http
POST /api/projects
Content-Type: application/json
```

```json
{
  "name": "my-project"
}
```

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `name` | String | Sí | Nombre único del proyecto |

### Response — 201 Created

```json
{
  "projectId": "fdb6167d-44e6-4f7e-b521-79e4c72018b7",
  "apiKey": "66eed4bbef7a10613afe2be4c55704b884b40599c6a5737c4960fcbc0ea01274",
  "name": "my-project"
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `projectId` | UUID | Identificador del proyecto para MCP tools |
| `apiKey` | String (64 hex) | Clave para autenticar requests REST e identificar el proyecto |
| `name` | String | Nombre del proyecto creado |

### Errores

| Status | Causa |
|--------|-------|
| 409 | Ya existe un proyecto con ese nombre |

---

## Uso

```bash
curl -s -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{"name":"my-project"}' | jq .
```

El `apiKey` retornado se usa en:
- `setupProject(apiKey, serverUrl)` — MCP tool
- `POST /internal/memory/batch` — campo `apiKey` en body
- `GET /internal/memory/commits?apiKey=` — query param
