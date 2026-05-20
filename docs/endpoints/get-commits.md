# GET /internal/memory/commits

## Resumen

Retorna el set de commit hashes (short, 7 chars) ya indexados para un proyecto. Usado por `extract_changes.py` para calcular el delta y evitar re-indexar commits existentes.

---

## Contrato

### Request

```http
GET /internal/memory/commits?apiKey=<api-key>
```

| Param | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `apiKey` | String | Sí | API key del proyecto |

### Response — 200 OK

```json
["7545dd9", "2c91c2e", "3642534", "a98969d"]
```

Array de strings con los short hashes de commits ya indexados.

### Errores

| Status | Causa |
|--------|-------|
| 401 | API key inválida o no encontrada |

---

## Uso

```bash
curl -s "http://localhost:8080/internal/memory/commits?apiKey=66eed4bb..." | jq .
```

### Integración con `extract_changes.py`

El script usa este endpoint para evitar trabajo duplicado:

```python
oracle_indexed = oracle_get_indexed_commits(api_key, base_url)
# Solo procesa commits cuyo short hash NO está en el set
if commit_hash not in oracle_indexed:
    oracle_pending.extend(to_oracle_entry(e) for e in entries)
```
