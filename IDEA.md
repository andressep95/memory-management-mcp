# MCP Agent Server — Documento de Requerimientos

**Stack:** Spring Boot 3.4.x + Oracle 23ai + DJL + Spring AI
**Versión:** 0.2 | **Fecha:** 2026-05-14

---

## 1. Visión General

Servidor MCP (Model Context Protocol) autónomo que corre en una ThinkStation local. Provee memoria vectorial, catálogo de skills y trazabilidad de sesiones para agentes de IA (Claude Code). No depende de Python, ChromaDB ni APIs externas. Todo el estado reside en Oracle 23ai; los proyectos cliente solo mantienen hooks ejecutables locales.

### Objetivos

| # | Objetivo |
|---|---|
| 1 | Proveer búsqueda semántica de skills por similitud vectorial |
| 2 | Indexar automáticamente diffs de commits como memoria del proyecto |
| 3 | Gestionar preferencias de skills por usuario y proyecto |
| 4 | Registrar y auditar sesiones de trabajo del agente |
| 5 | Detectar el stack tecnológico del proyecto y aplicar reglas relevantes |
| 6 | Funcionar 100% offline una vez descargados los modelos DJL |

---

## 2. Catálogo de MCP Tools y Resources

### 2.1 Tools

| Tool | Descripción | Inputs requeridos | Inputs opcionales | Output | Disparado por |
|---|---|---|---|---|---|
| `search_skills` | Busca skills relevantes por similitud semántica | `git_username`, `project_name`, `session_id`, `query` | `limit` (default 5) | Lista de skills con nombre, file_path y score | UserPromptSubmit hook |
| `query_memory` | Consulta cambios previos indexados en Oracle Vector | `git_username`, `project_name`, `session_id`, `query` | `limit` (default 5) | Lista de cambios con file, intent, what, why, score | UserPromptSubmit hook |
| `index_commit` | Indexa un diff de commit en la memoria vectorial | `git_username`, `project_name`, `session_id`, `diff`, `commit_hash`, `branch`, `what`, `why` | — | Confirmación con ID generado | post-commit hook |
| `check_user_setup` | Verifica si el usuario tiene preferencias configuradas | `git_username`, `project_name` | — | `has_config: boolean` + lista de skills disponibles del proyecto | session-start hook (primera vez) |
| `save_user_preferences` | Persiste las preferencias de skills del usuario | `git_username`, `project_name`, `mode` (additive/restrictive), `skill_ids[]` | — | Confirmación con resumen de configuración | TUI post-setup |
| `update_user_preferences` | Agrega o quita skills de las preferencias del usuario | `git_username`, `project_name`, `action` (add/remove), `skill_ids[]` | — | Configuración actualizada | Comando explícito del usuario |
| `detect_stack` | Detecta el stack tecnológico y retorna reglas aplicables | `git_username`, `project_name`, `session_id`, `file_list[]` | — | Stack detectado + reglas del proyecto aplicables | session-start hook |
| `sync_skills` | Sincroniza el catálogo de skills hacia el MCP server | `git_username`, `project_name`, `skills[]` (name, content, file_path) | — | Skills indexados/actualizados con sus IDs | sync.sh script |
| `register_session` | Registra el inicio de una sesión de trabajo | `git_username`, `project_name`, `session_id` | — | Confirmación con timestamp de inicio | session-start hook |
| `close_session` | Cierra y registra el fin de una sesión | `session_id`, `git_username`, `project_name` | — | Resumen: skills usados, queries realizadas | post-session hook |

### 2.2 Resources

| Resource | Descripción | Inputs requeridos | Inputs opcionales | Output | Usado por |
|---|---|---|---|---|---|
| `get_skill_content` | Retorna el contenido completo de un skill por nombre | `skill_name`, `project_name` | — | Contenido del SKILL.md + metadata | Hook después de `search_skills` |
| `get_recent_memory` | Retorna los N cambios más recientes del proyecto | `project_name`, `git_username` | `limit` (default 10) | Lista ordenada por fecha de cambios recientes | session-start hook |

---

## 3. Requerimientos Funcionales por Módulo

### 3.1 Módulo de Embeddings (DJL Local)

- [ ] Cargar modelo `multilingual-e5-small` (384 dimensiones) desde disco al arrancar
- [ ] Exponer un `EmbeddingService` interno que convierte texto → `float[]`
- [ ] No realizar llamadas a internet en tiempo de ejecución
- [ ] Reutilizar el modelo cargado (singleton) durante toda la vida del servidor

### 3.2 Módulo de Skills

- [ ] CRUD de skills con nombre, contenido y file_path
- [ ] Generar embedding al crear/actualizar un skill vía `EmbeddingService`
- [ ] Asociar skills a proyectos mediante `PROJECT_SKILLS`
- [ ] `sync_skills`: recibir lista de skills por POST, hacer upsert por `(project_name, file_path)`, regenerar embedding si el contenido cambió
- [ ] `search_skills`: recibir query, generar embedding, ejecutar `vector_distance` en Oracle, retornar top-N con score
- [ ] `get_skill_content`: retornar contenido completo + metadata por nombre

### 3.3 Módulo de Memoria (Commits)

- [ ] `index_commit`: recibir diff + metadatos, generar embedding del diff, insertar en `MEMORY_CHANGES`
- [ ] `query_memory`: recibir query, generar embedding, ejecutar búsqueda vectorial, retornar top-N cambios
- [ ] `get_recent_memory`: retornar N registros más recientes ordenados por `created_at`
- [ ] Soporte a tags para clasificación de cambios (refactor, feat, fix, etc.)

### 3.4 Módulo de Preferencias de Usuario

- [ ] `check_user_setup`: verificar existencia de fila activa en `USER_SKILL_PREFERENCES` para `(git_username, project_name)`
- [ ] `save_user_preferences`: upsert de preferencias con modo aditivo o restrictivo
- [ ] `update_user_preferences`: agregar o quitar skills individuales sin sobrescribir toda la config
- [ ] Validar que los `skill_ids` existen en `SKILLS` antes de guardar

### 3.5 Módulo de Sesiones

- [ ] `register_session`: insertar en `SESSIONS` con `started_at = now()`
- [ ] `close_session`: actualizar `closed_at` y calcular resumen de uso
- [ ] `SESSION_SKILL_USAGE`: registrar cada llamada a `search_skills` con timestamp y query text
- [ ] `get_recent_memory` debe respetar el contexto `(git_username, project_name)` de la sesión activa

### 3.6 Módulo de Detección de Stack

- [ ] `detect_stack`: recibir lista de archivos del proyecto (`file_list[]`), inferir lenguajes/frameworks por extensión y nombre de archivos conocidos (pom.xml → Java/Maven, package.json → Node, etc.)
- [ ] Retornar lista de reglas aplicables según el stack (reglas almacenadas en BD o archivos de configuración)
- [ ] El resultado se inyecta como contexto adicional al agente vía hook

### 3.7 Transporte MCP (SSE)

- [ ] Implementar servidor MCP con Spring AI MCP Server starter
- [ ] Usar WebFlux para el transporte SSE (Server-Sent Events)
- [ ] Registrar todos los Tools y Resources del catálogo en el `McpServerBuilder`
- [ ] Endpoint de health en `/actuator/health` para verificar disponibilidad

---

## 4. Esquema Oracle 23ai

### 4.1 Tablas

| Tabla | Columnas clave | Propósito |
|---|---|---|
| `SKILLS` | `id`, `name`, `content`, `embedding VECTOR(384)`, `file_path`, `created_by`, `active`, `synced_at` | Catálogo global de skills con vectores para búsqueda semántica |
| `PROJECT_SKILLS` | `project_name`, `skill_id`, `enabled_by`, `enabled_at` | Skills habilitados por proyecto |
| `USER_SKILL_PREFERENCES` | `git_username`, `project_name`, `skill_id`, `mode` (additive/restrictive), `active`, `configured_at` | Preferencias personales por usuario y proyecto |
| `MEMORY_CHANGES` | `id`, `commit_hash`, `branch`, `author`, `file_path`, `lines_start`, `lines_end`, `intent`, `what`, `why`, `language`, `tags`, `embedding VECTOR(384)`, `created_at` | Historial de commits indexados vectorialmente |
| `SESSIONS` | `session_id` (UUID), `git_username`, `project_name`, `started_at`, `last_activity`, `closed_at` | Trazabilidad de sesiones de trabajo |
| `SESSION_SKILL_USAGE` | `session_id`, `skill_id`, `queried_at`, `query_text` | Auditoría de skills consultados por sesión |
| `USER_ROLES` | `git_username`, `project_name`, `role` (global_admin / project_admin / user) | Control de acceso (base para expansión futura) |

### 4.2 Índices requeridos

- [ ] Índice vectorial HNSW en `SKILLS.embedding` para búsqueda semántica rápida
- [ ] Índice vectorial HNSW en `MEMORY_CHANGES.embedding`
- [ ] Índice en `MEMORY_CHANGES(project_name, created_at DESC)` para `get_recent_memory`
- [ ] Índice único en `SKILLS(project_name, file_path)` para upsert de sync

---

## 5. Modelo de Identidad y Sesión

| Identificador | Rol | Origen local | Consideraciones |
|---|---|---|---|
| `git_username` | Identifica al usuario | `git config user.name` | Estrategia de autenticación pendiente — riesgo de suplantación |
| `project_name` | Identifica el repositorio | `git remote get-url origin` o `git rev-parse --show-toplevel` | Normalizar a snake_case al recibir |
| `session_id` | Trazabilidad de sesión | `uuidgen` en `session-start.sh`, exportado como env var | UUID v4, no expira automáticamente (TTL pendiente de definir) |

---

## 6. Hooks del Proyecto Cliente

> Los hooks residen en el repositorio del usuario, no en el servidor MCP.

| Hook | Evento Claude Code | MCP Tools invocados | Comportamiento esperado |
|---|---|---|---|
| `session-start.sh` | `SessionStart` | `check_user_setup` → `detect_stack` → `register_session` | Si primera vez: lanza TUI → `save_user_preferences`. Inyecta stack + reglas al agente como `additionalContext`. |
| `user-prompt-submit.sh` | `UserPromptSubmit` | `search_skills` → `query_memory` → `get_skill_content` | Inyecta contenido del skill relevante + memoria previa como `additionalContext`. El agente recibe texto directo. |
| `post-commit.sh` | `PostToolUse` (git commit) | `index_commit` | Extrae diff con `git diff HEAD~1 HEAD`, envía al MCP para indexar en Oracle. Sin ChromaDB ni Python local. |
| `validate-commit.sh` | `PreToolUse` (git commit) | Ninguna (lógica local) | Valida formato del commit body (`what:` / `why:` / `breaking:`). No requiere MCP. |
| `post-session.sh` | `PostToolUse` (después de commit) | `close_session` (opcional) | Puede cerrar sesión o mantenerla abierta. Recuerda al agente hacer `/clear`. |

---

## 7. Flujos de Preferencias de Usuario

| Escenario | Condición de entrada | UX | Tool + payload | Frecuencia |
|---|---|---|---|---|
| Primera configuración | `check_user_setup` retorna `false` | TUI interactivo en terminal | `save_user_preferences` con `mode + skill_ids[]` | Una vez por usuario/proyecto |
| Modo aditivo | Usuario quiere todos los skills + extras | Selección de skills adicionales | `mode: 'additive'`, `skill_ids: [extras]` | En setup o update posterior |
| Modo restrictivo | Usuario quiere solo un subset | Selección de subset | `mode: 'restrictive'`, `skill_ids: [seleccionados]` | En setup o update posterior |
| Actualización posterior | Usuario pide agregar/quitar skills | TUI on-demand | `update_user_preferences` con `action: add/remove` | A demanda |
| Re-configuración total | Usuario quiere resetear preferencias | TUI completo nuevamente | `save_user_preferences` sobrescribe config anterior | A demanda |

---

## 8. Casos Prácticos de Oracle 23ai (Aprendizaje)

> Estos tres casos son ejercicios de aprendizaje que también sirven de prueba de concepto para la arquitectura del servidor MCP.

### Caso 1 — JSON Relational Duality Views

**Enunciado:** API REST para un "E-commerce Product Catalog". Los datos se almacenan en tablas relacionales normalizadas pero la aplicación persiste y recupera usando JSON Duality Views de Oracle 23ai, eliminando mapeos complejos en JPA/Hibernate.

**Conceptos a dominar:**
- Oracle 23ai JSON Relational Duality Views
- Diferencia entre almacenamiento relacional vs. exposición documental
- Spring Data JDBC vs. JPA para vistas actualizables
- Optimistic Locking mediante columna ETag generada por Oracle

**Requerimientos a construir:**
- [ ] Tablas relacionales: `PRODUCTS`, `CATEGORIES`, `SUPPLIERS`
- [ ] Duality View `PRODUCT_CATALOG_DV` que exponga el catálogo como documentos JSON
- [ ] Endpoints REST: `GET /products`, `GET /products/{id}`, `POST /products`, `PUT /products/{id}`, `DELETE /products/{id}`
- [ ] Manejo de ETag para control de concurrencia optimista
- [ ] Tests de integración con base de datos real

---

### Caso 2 — Búsqueda Semántica con AI Vector Search

**Enunciado:** Módulo de búsqueda inteligente para el servidor MCP. Generación de embeddings con DJL (Java puro), almacenamiento en columnas `VECTOR(384)` de Oracle 23ai, búsqueda por similitud de coseno.

**Conceptos a dominar:**
- Tipo de dato `VECTOR` y funciones de distancia (`COSINE`, `DOT_PRODUCT`, `EUCLIDEAN`)
- Creación de índices vectoriales (`IVF`, `HNSW`) en Oracle
- DJL para generación de vectores en Java (sin Python)
- RAG (Retrieval-Augmented Generation) nativo en base de datos

**Requerimientos a construir:**
- [ ] `EmbeddingService`: carga el modelo `multilingual-e5-small` con DJL y expone `float[] embed(String text)`
- [ ] `VectorSearchRepository`: ejecuta `vector_distance` en Oracle con JDBC directo
- [ ] Índice HNSW en producción, IVF como alternativa para benchmarking
- [ ] Benchmarking de tiempos: sin índice vs. IVF vs. HNSW con dataset de al menos 10.000 registros
- [ ] Endpoint `POST /search` que recibe una query y retorna top-5 resultados con score

---

### Caso 3 — High-Performance Batch Processing

**Enunciado:** Servicio de carga masiva para indexar 1 millón de commits/skills. Dos versiones: inserts tradicionales vs. optimizados con JDBC Batch y Array Processing. Reporte de rendimiento comparativo.

**Conceptos a dominar:**
- Bind Variables y por qué evitan el "Hard Parsing" en la Shared Pool
- Mecanismos de Undo y Redo Log en Oracle
- JDBC Batching y su configuración en Spring Boot
- `DBMS_XPLAN` para analizar planes de ejecución

**Requerimientos a construir:**
- [ ] `BatchIngestionService`: versión naive con inserts uno a uno
- [ ] `OptimizedBatchIngestionService`: versión con `JdbcTemplate.batchUpdate()` y batch size configurable
- [ ] Medición de tiempo de ejecución, filas/segundo y consumo de SGA con `V$SYSSTAT`
- [ ] Reporte generado automáticamente en formato JSON comparando ambas versiones
- [ ] Endpoint `POST /batch/ingest` que acepta archivos de datos y retorna el reporte de rendimiento

---

## 9. Decisiones Pendientes

| Decisión | Prioridad | Estado | Detalle |
|---|---|---|---|
| Seguridad de `git_username` | Alta | Pendiente | Definir estrategia: JWT, token local firmado, o SSH key fingerprint para evitar suplantación |
| Embedding en DJL vs. Spring AI embedding API | Alta | Pendiente | DJL elimina dependencia Python. Evaluar rendimiento de `multilingual-e5-small` vs. modelos disponibles vía Spring AI |
| Multi-tenant isolation | Alta | Pendiente | ¿Schema compartido con `project_name` como discriminador, o schemas separados en Oracle por organización? |
| Expiración de `session_id` | Media | Pendiente | Definir TTL: fijo (ej. 8 horas), o cierre por inactividad (ej. 30 min sin actividad) |
| Sistema de roles completo | Media | Base diseñada | Expandir `USER_ROLES` para permisos granulares: crear skills, asignar a proyectos, ver memoria de otros |
| Actualización de skills eliminados | Baja | Pendiente | Definir comportamiento en `USER_SKILL_PREFERENCES` cuando un skill es eliminado del catálogo global (cascade delete vs. soft delete) |

---

## 10. Nota Arquitectural

> El proyecto usuario mantiene **únicamente** hooks ejecutables y scripts de extracción local.
> Todo el estado (skills, memoria, preferencias, sesiones) reside en el **MCP Server con Oracle 23ai**.
> No existe dependencia de ChromaDB, Python para embeddings ni archivos de skills locales en el cliente.
> El servidor es autónomo una vez descargados los modelos DJL.
