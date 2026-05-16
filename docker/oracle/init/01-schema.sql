-- ============================================================
-- MCP Agent Server — Oracle 23ai Schema
-- Generado: 15/5/2026, 0:17:15
-- ============================================================

-- ============================================================
-- USERS  —  Entidad fuerte: usuarios identificados por git_username
-- ============================================================
CREATE TABLE users (
    id                   RAW(16)                             DEFAULT SYS_GUID() NOT NULL,
    git_username         VARCHAR2(255)                       NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE            DEFAULT SYSTIMESTAMP NOT NULL,
    active               NUMBER(1)                           DEFAULT 1 NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_git_username UNIQUE (git_username),
    CONSTRAINT ck_users_0 CHECK (active IN (0, 1))
);


-- ============================================================
-- PROJECTS  —  Entidad fuerte: agrupa skills y memoria de un proyecto
-- ============================================================
CREATE TABLE projects (
    id                   RAW(16)                             DEFAULT SYS_GUID() NOT NULL,
    name                 VARCHAR2(255)                       NOT NULL,
    description          VARCHAR2(1000)                     ,
    created_by           RAW(16)                             NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE            DEFAULT SYSTIMESTAMP NOT NULL,
    active               NUMBER(1)                           DEFAULT 1 NOT NULL,
    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT fk_projects_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT uk_projects_name UNIQUE (name),
    CONSTRAINT ck_projects_0 CHECK (active IN (0, 1))
);


-- ============================================================
-- SKILLS  —  Entidad fuerte: catalogo global de skills
-- ============================================================
CREATE TABLE skills (
    id                   RAW(16)                             DEFAULT SYS_GUID() NOT NULL,
    name                 VARCHAR2(255)                       NOT NULL,
    content              CLOB                                NOT NULL,
    embedding            VECTOR(384, FLOAT32)               ,
    created_by           RAW(16)                             NOT NULL,
    active               NUMBER(1)                           DEFAULT 1 NOT NULL,
    synced_at            TIMESTAMP WITH TIME ZONE            DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_skills PRIMARY KEY (id),
    CONSTRAINT fk_skills_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT uk_skills_name UNIQUE (name),
    CONSTRAINT ck_skills_0 CHECK (active IN (0, 1))
);


-- ============================================================
-- SKILL_CHUNKS  —  Sub-archivos de un skill con embedding propio
-- ============================================================
CREATE TABLE skill_chunks (
    id                   RAW(16)                             DEFAULT SYS_GUID() NOT NULL,
    skill_id             RAW(16)                             NOT NULL,
    chunk_name           VARCHAR2(255)                       NOT NULL,
    content              CLOB                                NOT NULL,
    embedding            VECTOR(384, FLOAT32)               ,
    position             NUMBER(5)                           NOT NULL,
    synced_at            TIMESTAMP WITH TIME ZONE            DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_skill_chunks PRIMARY KEY (id),
    CONSTRAINT fk_skill_chunks_skill_id FOREIGN KEY (skill_id) REFERENCES skills (id),
    CONSTRAINT uk_skill_chunks_skill_chunk UNIQUE (skill_id, chunk_name)
);


-- ============================================================
-- MEMORY_CHANGES  —  Entidad fuerte: historial vectorial de commits del proyecto
-- Granularidad: 1 fila por archivo por commit.
-- Los hunks individuales (bloques @@) se almacenan en MEMORY_CHANGE_HUNKS.
-- ============================================================
CREATE TABLE memory_changes (
    id                   RAW(16)                             DEFAULT SYS_GUID() NOT NULL,
    project_id           RAW(16)                             NOT NULL,
    commit_hash          VARCHAR2(64)                        NOT NULL,
    branch               VARCHAR2(255)                       NOT NULL,
    author               VARCHAR2(255)                       NOT NULL,
    file_path            VARCHAR2(1000)                      NOT NULL,
    intent               VARCHAR2(50)                       ,
    what                 CLOB                                NOT NULL,
    why                  CLOB                               ,
    language             VARCHAR2(50)                       ,
    tags                 VARCHAR2(500)                      ,
    raw_diff             CLOB                               ,
    content_before       CLOB                               ,
    content_after        CLOB                               ,
    embedding            VECTOR(384, FLOAT32)               ,
    created_at           TIMESTAMP WITH TIME ZONE            DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_memory_changes PRIMARY KEY (id),
    CONSTRAINT fk_memory_changes_project_id FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT uk_memory_changes_commit_file UNIQUE (project_id, commit_hash, file_path)
);


-- ============================================================
-- MEMORY_CHANGE_HUNKS  —  Bloques @@ individuales de un archivo modificado
-- Permite auditoría granular y navegación de cambios por rango de líneas.
-- ============================================================
CREATE TABLE memory_change_hunks (
    id                   RAW(16)                             DEFAULT SYS_GUID() NOT NULL,
    memory_change_id     RAW(16)                             NOT NULL,
    lines_start          NUMBER(10)                          NOT NULL,
    lines_end            NUMBER(10)                          NOT NULL,
    symbol               VARCHAR2(500)                      ,
    change_type          VARCHAR2(20)                        NOT NULL,
    hunk_diff            CLOB                                NOT NULL,
    CONSTRAINT pk_memory_change_hunks PRIMARY KEY (id),
    CONSTRAINT fk_memory_change_hunks_mc FOREIGN KEY (memory_change_id) REFERENCES memory_changes (id) ON DELETE CASCADE,
    CONSTRAINT ck_memory_change_hunks_type CHECK (change_type IN ('addition', 'deletion', 'modification'))
);


-- ============================================================
-- PROJECT_SKILLS  —  Bateria oficial de skills por proyecto — admin-controlled
-- ============================================================
CREATE TABLE project_skills (
    id                   RAW(16)                             DEFAULT SYS_GUID() NOT NULL,
    project_id           RAW(16)                             NOT NULL,
    skill_id             RAW(16)                             NOT NULL,
    enabled_by           RAW(16)                             NOT NULL,
    enabled_at           TIMESTAMP WITH TIME ZONE            DEFAULT SYSTIMESTAMP NOT NULL,
    active               NUMBER(1)                           DEFAULT 1 NOT NULL,
    CONSTRAINT pk_project_skills PRIMARY KEY (id),
    CONSTRAINT fk_project_skills_project_id FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_skills_skill_id FOREIGN KEY (skill_id) REFERENCES skills (id),
    CONSTRAINT fk_project_skills_enabled_by FOREIGN KEY (enabled_by) REFERENCES users (id),
    CONSTRAINT uk_project_skills_project_id_skill_id UNIQUE (project_id, skill_id),
    CONSTRAINT ck_project_skills_0 CHECK (active IN (0, 1))
);


-- ============================================================
-- USER_PRIVATE_SKILLS  —  Skills personales del usuario — invisibles para otros usuarios del proyecto
-- ============================================================
CREATE TABLE user_private_skills (
    id                   RAW(16)                             DEFAULT SYS_GUID() NOT NULL,
    user_id              RAW(16)                             NOT NULL,
    project_id           RAW(16)                             NOT NULL,
    skill_id             RAW(16)                             NOT NULL,
    added_at             TIMESTAMP WITH TIME ZONE            DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_user_private_skills PRIMARY KEY (id),
    CONSTRAINT fk_user_private_skills_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_private_skills_project_id FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_user_private_skills_skill_id FOREIGN KEY (skill_id) REFERENCES skills (id),
    CONSTRAINT uk_user_private_skills_user_id_project_id_skill_id UNIQUE (user_id, project_id, skill_id)
);


-- ============================================================
-- USER_PROJECT_ROLES  —  Control de acceso: rol del usuario en cada proyecto
-- ============================================================
CREATE TABLE user_project_roles (
    id                   RAW(16)                             DEFAULT SYS_GUID() NOT NULL,
    user_id              RAW(16)                             NOT NULL,
    project_id           RAW(16)                             NOT NULL,
    role                 VARCHAR2(20)                        NOT NULL,
    granted_at           TIMESTAMP WITH TIME ZONE            DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_user_project_roles PRIMARY KEY (id),
    CONSTRAINT fk_user_project_roles_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_project_roles_project_id FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT uk_user_project_roles_user_id_project_id UNIQUE (user_id, project_id),
    CONSTRAINT ck_user_project_roles_0 CHECK (role IN ('global_admin', 'project_admin', 'user'))
);


-- ============================================================
-- USER_PREFERENCES  —  additive: bateria completa + extras | restrictive: solo los seleccionados
-- ============================================================
CREATE TABLE user_preferences (
    id                   RAW(16)                             DEFAULT SYS_GUID() NOT NULL,
    user_id              RAW(16)                             NOT NULL,
    project_id           RAW(16)                             NOT NULL,
    selection_mode       VARCHAR2(20)                        NOT NULL,
    active               NUMBER(1)                           DEFAULT 1 NOT NULL,
    configured_at        TIMESTAMP WITH TIME ZONE            DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_user_preferences PRIMARY KEY (id),
    CONSTRAINT fk_user_preferences_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_preferences_project_id FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT uk_user_preferences_user_id_project_id UNIQUE (user_id, project_id),
    CONSTRAINT ck_user_preferences_0 CHECK (selection_mode IN ('additive', 'restrictive')),
    CONSTRAINT ck_user_preferences_1 CHECK (active IN (0, 1))
);


-- ============================================================
-- USER_PREFERENCE_SKILLS  —  Skills de la bateria seleccionados por el usuario (depende del mode)
-- ============================================================
CREATE TABLE user_preference_skills (
    id                   RAW(16)                             DEFAULT SYS_GUID() NOT NULL,
    preference_id        RAW(16)                             NOT NULL,
    skill_id             RAW(16)                             NOT NULL,
    CONSTRAINT pk_user_preference_skills PRIMARY KEY (id),
    CONSTRAINT fk_user_preference_skills_preference_id FOREIGN KEY (preference_id) REFERENCES user_preferences (id),
    CONSTRAINT fk_user_preference_skills_skill_id FOREIGN KEY (skill_id) REFERENCES skills (id),
    CONSTRAINT uk_user_preference_skills_preference_id_skill_id UNIQUE (preference_id, skill_id)
);


-- ============================================================
-- SESSIONS  —  Una sesion por conversacion — closed_at NULL = sesion activa
-- ============================================================
CREATE TABLE sessions (
    id                   RAW(16)                             DEFAULT SYS_GUID() NOT NULL,
    user_id              RAW(16)                             NOT NULL,
    project_id           RAW(16)                             NOT NULL,
    started_at           TIMESTAMP WITH TIME ZONE            DEFAULT SYSTIMESTAMP NOT NULL,
    last_activity        TIMESTAMP WITH TIME ZONE            DEFAULT SYSTIMESTAMP NOT NULL,
    closed_at            TIMESTAMP WITH TIME ZONE           ,
    CONSTRAINT pk_sessions PRIMARY KEY (id),
    CONSTRAINT fk_sessions_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_sessions_project_id FOREIGN KEY (project_id) REFERENCES projects (id)
);


-- ============================================================
-- SESSION_SKILL_USAGE  —  Auditoria de skills usados por sesion — incluye privados y de bateria
-- ============================================================
CREATE TABLE session_skill_usage (
    id                   RAW(16)                             DEFAULT SYS_GUID() NOT NULL,
    session_id           RAW(16)                             NOT NULL,
    skill_id             RAW(16)                             NOT NULL,
    queried_at           TIMESTAMP WITH TIME ZONE            DEFAULT SYSTIMESTAMP NOT NULL,
    query_text           VARCHAR2(1000)                     ,
    CONSTRAINT pk_session_skill_usage PRIMARY KEY (id),
    CONSTRAINT fk_session_skill_usage_session_id FOREIGN KEY (session_id) REFERENCES sessions (id),
    CONSTRAINT fk_session_skill_usage_skill_id FOREIGN KEY (skill_id) REFERENCES skills (id)
);


-- ============================================================
-- COMENTARIOS DE TABLAS Y COLUMNAS
-- ============================================================

-- USERS
COMMENT ON TABLE  users                IS 'Entidad fuerte. Cada usuario del sistema identificado por su git_username. Es la raiz de acceso, preferencias y sesiones.';
COMMENT ON COLUMN users.id             IS 'PK generado por Oracle (SYS_GUID). RAW(16) equivale a UUID.';
COMMENT ON COLUMN users.git_username   IS 'Identificador unico del usuario proveniente de git. Se usa como clave de lookup desde el agente MCP.';
COMMENT ON COLUMN users.created_at     IS 'Timestamp de registro del usuario en el sistema.';
COMMENT ON COLUMN users.active         IS '1 = activo, 0 = desactivado. Soft delete.';

-- PROJECTS
COMMENT ON TABLE  projects             IS 'Entidad fuerte. Unidad logica que agrupa skills y memoria de un proyecto (backend, frontend, CLI, etc). Un proyecto puede tener N usuarios con distintos roles.';
COMMENT ON COLUMN projects.id          IS 'PK generado por Oracle (SYS_GUID).';
COMMENT ON COLUMN projects.name        IS 'Nombre unico del proyecto. Es el discriminador que usa el agente para resolver contexto.';
COMMENT ON COLUMN projects.description IS 'Descripcion opcional del proposito del proyecto.';
COMMENT ON COLUMN projects.created_by  IS 'FK a USERS.id. Usuario que creo el proyecto (debe tener rol global_admin).';
COMMENT ON COLUMN projects.created_at  IS 'Timestamp de creacion del proyecto.';
COMMENT ON COLUMN projects.active      IS '1 = activo, 0 = archivado. Soft delete.';

-- SKILLS
COMMENT ON TABLE  skills               IS 'Entidad fuerte. Catalogo global de skills del agente. Un skill es un bloque de conocimiento (markdown/texto) con embedding vectorial. La pertenencia a proyectos se gestiona via PROJECT_SKILLS.';
COMMENT ON COLUMN skills.id            IS 'PK generado por Oracle (SYS_GUID).';
COMMENT ON COLUMN skills.name          IS 'Nombre unico del skill. Es el identificador semantico (ej: clean-ddd-hexagonal, openapi, commit).';
COMMENT ON COLUMN skills.content       IS 'Contenido completo del skill en texto plano o markdown. Es lo que el agente lee cuando selecciona el skill.';
COMMENT ON COLUMN skills.embedding     IS 'Vector de 384 dimensiones (FLOAT32) generado por DJL con multilingual-e5-small. Usado para busqueda semantica por coseno.';
COMMENT ON COLUMN skills.created_by    IS 'FK a USERS.id. Usuario que registro el skill en el catalogo global.';
COMMENT ON COLUMN skills.active        IS '1 = activo, 0 = desactivado. Un skill inactivo no aparece en busquedas.';
COMMENT ON COLUMN skills.synced_at     IS 'Ultima vez que el contenido fue sincronizado. Se actualiza cuando el hash del contenido cambia.';

-- SKILL_CHUNKS
COMMENT ON TABLE  skill_chunks              IS 'Sub-archivos referenciados por un skill principal. Cada chunk representa un archivo de la carpeta references/ u otro sub-documento. Tiene su propio embedding para busqueda semantica precisa (arquitectura RAG). La busqueda devuelve el chunk relevante; el agente sube al skill padre para cargar el contexto completo.';
COMMENT ON COLUMN skill_chunks.id           IS 'PK generado por Oracle (SYS_GUID).';
COMMENT ON COLUMN skill_chunks.skill_id     IS 'FK a SKILLS.id. Skill principal al que pertenece este chunk.';
COMMENT ON COLUMN skill_chunks.chunk_name   IS 'Nombre o ruta relativa del sub-archivo dentro del skill (ej: references/DDD-TACTICAL.md). Unico dentro del skill.';
COMMENT ON COLUMN skill_chunks.content      IS 'Contenido completo del sub-archivo. Es lo que el agente lee cuando el chunk es seleccionado.';
COMMENT ON COLUMN skill_chunks.embedding    IS 'Vector de 384 dimensiones del contenido del chunk. Permite encontrar el sub-documento exacto que responde a la query del agente.';
COMMENT ON COLUMN skill_chunks.position     IS 'Orden del chunk dentro del skill. Permite reconstruir el skill completo concatenando chunks en orden ascendente.';
COMMENT ON COLUMN skill_chunks.synced_at    IS 'Ultima sincronizacion del chunk. Se actualiza cuando el contenido del sub-archivo cambia.';

-- MEMORY_CHANGES
COMMENT ON TABLE  memory_changes               IS 'Entidad fuerte. Historial vectorial de commits del proyecto. Granularidad: 1 fila por archivo por commit. Los hunks (@@ bloques) se almacenan en MEMORY_CHANGE_HUNKS. Permite busqueda semantica por intencion y reconstruccion de archivos para auditoria.';
COMMENT ON COLUMN memory_changes.id            IS 'PK generado por Oracle (SYS_GUID).';
COMMENT ON COLUMN memory_changes.project_id    IS 'FK a PROJECTS.id. Proyecto al que pertenece este cambio.';
COMMENT ON COLUMN memory_changes.commit_hash   IS 'Hash SHA corto del commit de git. Combinado con file_path forma clave unica por proyecto.';
COMMENT ON COLUMN memory_changes.branch        IS 'Rama en la que ocurrio el commit.';
COMMENT ON COLUMN memory_changes.author        IS 'Autor del commit segun git log (nombre).';
COMMENT ON COLUMN memory_changes.file_path     IS 'Ruta relativa del archivo modificado dentro del repositorio. Unica por (project_id, commit_hash).';
COMMENT ON COLUMN memory_changes.intent        IS 'Tipo de cambio del commit: feat, fix, refactor, docs, test, chore, etc.';
COMMENT ON COLUMN memory_changes.what          IS 'Descripcion del QUE cambio. Parseada del cuerpo del commit (linea what:).';
COMMENT ON COLUMN memory_changes.why           IS 'Descripcion del POR QUE cambio. Parseada del cuerpo del commit (linea why:).';
COMMENT ON COLUMN memory_changes.language      IS 'Lenguaje de programacion del archivo modificado, detectado por extension.';
COMMENT ON COLUMN memory_changes.tags          IS 'Etiquetas comma-separated: commit_type, change_type, file_kind, scope.';
COMMENT ON COLUMN memory_changes.raw_diff      IS 'Diff completo del archivo en este commit (todos los hunks unidos). Permite mostrar que cambio sin acceso a git.';
COMMENT ON COLUMN memory_changes.content_before IS 'Contenido completo del archivo ANTES del commit. Vacio si el archivo es nuevo. Permite reconstruccion historica para auditoria.';
COMMENT ON COLUMN memory_changes.content_after  IS 'Contenido completo del archivo DESPUES del commit. Vacio si el archivo fue eliminado. Permite reconstruccion historica para auditoria.';
COMMENT ON COLUMN memory_changes.embedding     IS 'Vector de 384 dimensiones (multilingual-e5-small) del texto semantico del cambio (what/why/intent). Permite busqueda por similitud de intencion.';
COMMENT ON COLUMN memory_changes.created_at    IS 'Timestamp de indexacion del commit en el sistema.';

-- MEMORY_CHANGE_HUNKS
COMMENT ON TABLE  memory_change_hunks              IS 'Bloques @@ individuales de un archivo modificado en un commit. Hijos de MEMORY_CHANGES (ON DELETE CASCADE). Permiten navegacion granular de cambios por rango de lineas.';
COMMENT ON COLUMN memory_change_hunks.id           IS 'PK generado por Oracle (SYS_GUID).';
COMMENT ON COLUMN memory_change_hunks.memory_change_id IS 'FK a MEMORY_CHANGES.id. CASCADE DELETE: se eliminan junto con el padre.';
COMMENT ON COLUMN memory_change_hunks.lines_start  IS 'Linea de inicio del hunk en el archivo DESPUES del commit (numero de linea en content_after).';
COMMENT ON COLUMN memory_change_hunks.lines_end    IS 'Linea de fin del hunk en el archivo DESPUES del commit.';
COMMENT ON COLUMN memory_change_hunks.symbol       IS 'Nombre del simbolo (funcion, clase, metodo) mas cercano al hunk, extraido del encabezado @@.';
COMMENT ON COLUMN memory_change_hunks.change_type  IS 'Tipo de cambio del hunk: addition (solo +), deletion (solo -), modification (ambos).';
COMMENT ON COLUMN memory_change_hunks.hunk_diff    IS 'Texto raw del bloque @@ incluyendo lineas + y -. Contenido exacto del hunk para visualizacion.';

-- PROJECT_SKILLS
COMMENT ON TABLE  project_skills               IS 'Tabla intermedia. Bateria oficial de skills asignada a un proyecto por un administrador. Define que skills estan disponibles para los usuarios de ese proyecto.';
COMMENT ON COLUMN project_skills.id            IS 'PK generado por Oracle (SYS_GUID).';
COMMENT ON COLUMN project_skills.project_id    IS 'FK a PROJECTS.id. Proyecto al que pertenece la bateria.';
COMMENT ON COLUMN project_skills.skill_id      IS 'FK a SKILLS.id. Skill incluido en la bateria del proyecto.';
COMMENT ON COLUMN project_skills.enabled_by    IS 'FK a USERS.id. Administrador que habilito el skill para este proyecto.';
COMMENT ON COLUMN project_skills.enabled_at    IS 'Timestamp en que el skill fue agregado a la bateria del proyecto.';
COMMENT ON COLUMN project_skills.active        IS '1 = habilitado, 0 = deshabilitado. Permite remover un skill de la bateria sin borrar el registro.';

-- USER_PRIVATE_SKILLS
COMMENT ON TABLE  user_private_skills          IS 'Tabla intermedia. Skills registrados por un usuario para su uso personal en el contexto de un proyecto. NO forman parte de la bateria oficial. Son invisibles para otros usuarios del mismo proyecto.';
COMMENT ON COLUMN user_private_skills.id          IS 'PK generado por Oracle (SYS_GUID).';
COMMENT ON COLUMN user_private_skills.user_id     IS 'FK a USERS.id. Usuario propietario del skill privado.';
COMMENT ON COLUMN user_private_skills.project_id  IS 'FK a PROJECTS.id. Proyecto en cuyo contexto el usuario usa este skill.';
COMMENT ON COLUMN user_private_skills.skill_id    IS 'FK a SKILLS.id. El skill privado existe en el catalogo global pero solo este usuario puede verlo en este proyecto.';
COMMENT ON COLUMN user_private_skills.added_at    IS 'Timestamp en que el usuario registro el skill para uso personal.';

-- USER_PROJECT_ROLES
COMMENT ON TABLE  user_project_roles           IS 'Tabla intermedia. Define el rol de cada usuario en cada proyecto. Controla que operaciones puede realizar: global_admin gestiona el catalogo, project_admin gestiona la bateria, user configura sus preferencias.';
COMMENT ON COLUMN user_project_roles.id            IS 'PK generado por Oracle (SYS_GUID).';
COMMENT ON COLUMN user_project_roles.user_id       IS 'FK a USERS.id.';
COMMENT ON COLUMN user_project_roles.project_id    IS 'FK a PROJECTS.id.';
COMMENT ON COLUMN user_project_roles.role          IS 'Rol del usuario: global_admin (todos los permisos), project_admin (gestiona bateria del proyecto), user (configura preferencias personales).';
COMMENT ON COLUMN user_project_roles.granted_at    IS 'Timestamp en que se asigno el rol.';

-- USER_PREFERENCES
COMMENT ON TABLE  user_preferences             IS 'Tabla intermedia. Configuracion de seleccion de skills por usuario y proyecto. El modo define como se resuelve que skills ve el usuario: additive suma los suyos a la bateria completa, restrictive muestra solo los que el eligio.';
COMMENT ON COLUMN user_preferences.id              IS 'PK generado por Oracle (SYS_GUID).';
COMMENT ON COLUMN user_preferences.user_id         IS 'FK a USERS.id.';
COMMENT ON COLUMN user_preferences.project_id      IS 'FK a PROJECTS.id.';
COMMENT ON COLUMN user_preferences.selection_mode  IS 'additive: el usuario ve toda la bateria del proyecto mas sus skills privados. restrictive: el usuario ve solo los skills que selecciono de la bateria via USER_PREFERENCE_SKILLS.';
COMMENT ON COLUMN user_preferences.active          IS '1 = configuracion vigente, 0 = desactivada.';
COMMENT ON COLUMN user_preferences.configured_at   IS 'Ultima vez que el usuario modifico su configuracion.';

-- USER_PREFERENCE_SKILLS
COMMENT ON TABLE  user_preference_skills       IS 'Tabla intermedia. Subset de skills de la bateria del proyecto que el usuario selecciono explicitamente. Solo tiene efecto cuando el modo en USER_PREFERENCES es restrictive.';
COMMENT ON COLUMN user_preference_skills.id            IS 'PK generado por Oracle (SYS_GUID).';
COMMENT ON COLUMN user_preference_skills.preference_id IS 'FK a USER_PREFERENCES.id. Configuracion a la que pertenece esta seleccion.';
COMMENT ON COLUMN user_preference_skills.skill_id      IS 'FK a SKILLS.id. Skill de la bateria que el usuario selecciono.';

-- SESSIONS
COMMENT ON TABLE  sessions                     IS 'Tabla intermedia. Representa una conversacion del usuario con el agente MCP dentro de un proyecto. closed_at NULL indica sesion activa. Cada sesion tiene su propio registro de uso de skills.';
COMMENT ON COLUMN sessions.id                  IS 'PK generado por Oracle (SYS_GUID). Equivale al session_id que maneja el agente.';
COMMENT ON COLUMN sessions.user_id             IS 'FK a USERS.id. Usuario que inicio la sesion.';
COMMENT ON COLUMN sessions.project_id          IS 'FK a PROJECTS.id. Proyecto en cuyo contexto ocurre la sesion.';
COMMENT ON COLUMN sessions.started_at          IS 'Timestamp de inicio de la sesion.';
COMMENT ON COLUMN sessions.last_activity       IS 'Timestamp de la ultima interaccion. Usado para TTL y deteccion de sesiones huerfanas.';
COMMENT ON COLUMN sessions.closed_at           IS 'Timestamp de cierre de la sesion. NULL = sesion activa. Se setea al llamar al tool close_session.';

-- SESSION_SKILL_USAGE
COMMENT ON TABLE  session_skill_usage          IS 'Tabla intermedia. Auditoria de cada skill consultado dentro de una sesion. Registra tanto skills de la bateria del proyecto como skills privados del usuario. Permite analizar patrones de uso por sesion.';
COMMENT ON COLUMN session_skill_usage.id           IS 'PK generado por Oracle (SYS_GUID).';
COMMENT ON COLUMN session_skill_usage.session_id   IS 'FK a SESSIONS.id. Sesion en la que se consulto el skill.';
COMMENT ON COLUMN session_skill_usage.skill_id     IS 'FK a SKILLS.id. Skill que fue consultado.';
COMMENT ON COLUMN session_skill_usage.queried_at   IS 'Timestamp exacto de la consulta.';
COMMENT ON COLUMN session_skill_usage.query_text   IS 'Texto de la query semantica que devolvio este skill. Util para analizar como varia el uso entre sesiones.';


-- ============================================================
-- VECTOR INDEXES — HNSW para busqueda de similitud por coseno
-- Oracle 23ai: ORGANIZATION NEIGHBOR PARTITIONS = indice HNSW
-- ============================================================

CREATE VECTOR INDEX vidx_skill_chunks_embedding
    ON skill_chunks (embedding)
    ORGANIZATION NEIGHBOR PARTITIONS
    WITH DISTANCE COSINE
    WITH TARGET ACCURACY 95;

CREATE VECTOR INDEX vidx_skills_embedding
    ON skills (embedding)
    ORGANIZATION NEIGHBOR PARTITIONS
    WITH DISTANCE COSINE
    WITH TARGET ACCURACY 95;

CREATE VECTOR INDEX vidx_memory_changes_embedding
    ON memory_changes (embedding)
    ORGANIZATION NEIGHBOR PARTITIONS
    WITH DISTANCE COSINE
    WITH TARGET ACCURACY 95;


-- ============================================================
-- INDEXES — FKs frecuentes en JOINs y filtros
-- ============================================================

-- skill_chunks: JOIN por skill y busqueda por nombre de chunk
CREATE INDEX idx_skill_chunks_skill    ON skill_chunks (skill_id, position);
-- idx_skill_chunks_name omitted: covered by uk_skill_chunks_skill_chunk unique constraint

-- project_skills: JOIN por project_id y skill_id
CREATE INDEX idx_project_skills_project ON project_skills (project_id, active);
CREATE INDEX idx_project_skills_skill   ON project_skills (skill_id);

-- user_private_skills: busqueda por usuario+proyecto
CREATE INDEX idx_user_private_skills_user    ON user_private_skills (user_id, project_id);
CREATE INDEX idx_user_private_skills_skill   ON user_private_skills (skill_id);

-- user_project_roles: lookup de rol por usuario (covered by unique constraint)
-- CREATE INDEX idx_user_project_roles_user     ON user_project_roles (user_id, project_id);

-- user_preferences: lookup por usuario+proyecto
CREATE INDEX idx_user_preferences_user       ON user_preferences (user_id, project_id);

-- user_preference_skills: filtro por preferencia
CREATE INDEX idx_user_preference_skills_pref ON user_preference_skills (preference_id);
CREATE INDEX idx_user_preference_skills_skill ON user_preference_skills (skill_id);

-- sessions: sesiones activas por usuario y proyecto
CREATE INDEX idx_sessions_user              ON sessions (user_id, project_id);
CREATE INDEX idx_sessions_active            ON sessions (project_id, closed_at);

-- session_skill_usage: historial por sesion
CREATE INDEX idx_session_skill_usage_sess   ON session_skill_usage (session_id, queried_at DESC);

-- memory_changes: busqueda por proyecto y rama
CREATE INDEX idx_memory_changes_project     ON memory_changes (project_id, created_at DESC);
CREATE INDEX idx_memory_changes_commit      ON memory_changes (commit_hash);
CREATE INDEX idx_memory_changes_file        ON memory_changes (project_id, file_path);

-- memory_change_hunks: lookup por change parent y por rango de lineas
CREATE INDEX idx_memory_change_hunks_mc     ON memory_change_hunks (memory_change_id);
CREATE INDEX idx_memory_change_hunks_lines  ON memory_change_hunks (memory_change_id, lines_start, lines_end);


-- ============================================================
-- DOCUMENTS  —  Knowledge base: documentación del proyecto indexada
-- ============================================================
CREATE TABLE documents (
    id                   RAW(16)                             DEFAULT SYS_GUID() NOT NULL,
    project_id           RAW(16)                             NOT NULL,
    source_path          VARCHAR2(1000)                      NOT NULL,
    title                VARCHAR2(500)                       NOT NULL,
    doc_type             VARCHAR2(50)                        NOT NULL,
    content              CLOB                                NOT NULL,
    embedding            VECTOR(384, FLOAT32)               ,
    indexed_at           TIMESTAMP WITH TIME ZONE            DEFAULT SYSTIMESTAMP NOT NULL,
    source_modified_at   TIMESTAMP WITH TIME ZONE            NOT NULL,
    stale                NUMBER(1)                           DEFAULT 0 NOT NULL,
    CONSTRAINT pk_documents PRIMARY KEY (id),
    CONSTRAINT fk_documents_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT uk_documents_path UNIQUE (project_id, source_path),
    CONSTRAINT ck_documents_stale CHECK (stale IN (0, 1)),
    CONSTRAINT ck_documents_type CHECK (doc_type IN (
        'ADR', 'API_SPEC', 'RUNBOOK', 'GUIDE', 'README', 'CHANGELOG', 'ONBOARDING', 'DESIGN', 'OTHER'
    ))
);


-- ============================================================
-- DOCUMENT_SECTIONS  —  Secciones con embedding individual para RAG granular
-- ============================================================
CREATE TABLE document_sections (
    id                   RAW(16)                             DEFAULT SYS_GUID() NOT NULL,
    document_id          RAW(16)                             NOT NULL,
    heading              VARCHAR2(500)                       NOT NULL,
    content              CLOB                                NOT NULL,
    embedding            VECTOR(384, FLOAT32)               ,
    position             NUMBER(5)                           NOT NULL,
    indexed_at           TIMESTAMP WITH TIME ZONE            DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_document_sections PRIMARY KEY (id),
    CONSTRAINT fk_doc_sections_doc FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);


-- ── Indexes for Knowledge ───────────────────────────────────

-- documents: búsqueda por proyecto y tipo
CREATE INDEX idx_documents_project          ON documents (project_id, doc_type);
CREATE INDEX idx_documents_stale            ON documents (project_id, stale);

-- document_sections: lookup por documento
CREATE INDEX idx_doc_sections_doc           ON document_sections (document_id, position);


-- ── Comments for Knowledge ──────────────────────────────────

COMMENT ON TABLE  documents                    IS 'Knowledge base del proyecto. Cada fila es un documento indexado con embedding para busqueda semantica.';
COMMENT ON COLUMN documents.id                 IS 'PK generado por Oracle (SYS_GUID). RAW(16) equivale a UUID.';
COMMENT ON COLUMN documents.project_id         IS 'FK al proyecto al que pertenece este documento.';
COMMENT ON COLUMN documents.source_path        IS 'Ruta relativa del archivo fuente dentro del repositorio (ej: docs/architecture.md).';
COMMENT ON COLUMN documents.title              IS 'Titulo del documento extraido del contenido o del nombre del archivo.';
COMMENT ON COLUMN documents.doc_type           IS 'Tipo de documento: ADR, API_SPEC, RUNBOOK, GUIDE, README, CHANGELOG, ONBOARDING, DESIGN, OTHER.';
COMMENT ON COLUMN documents.content            IS 'Contenido completo del documento en texto plano o markdown.';
COMMENT ON COLUMN documents.embedding          IS 'Vector de 384 dimensiones (multilingual-e5-small) del contenido completo.';
COMMENT ON COLUMN documents.indexed_at         IS 'Timestamp de la ultima indexacion del documento.';
COMMENT ON COLUMN documents.source_modified_at IS 'Timestamp de la ultima modificacion del archivo fuente.';
COMMENT ON COLUMN documents.stale              IS '1 = documento potencialmente desactualizado respecto a cambios recientes, 0 = vigente.';

COMMENT ON TABLE  document_sections            IS 'Secciones individuales de un documento con embedding propio para RAG granular por heading.';
COMMENT ON COLUMN document_sections.id         IS 'PK generado por Oracle (SYS_GUID). RAW(16) equivale a UUID.';
COMMENT ON COLUMN document_sections.document_id IS 'FK al documento padre. CASCADE en delete.';
COMMENT ON COLUMN document_sections.heading    IS 'Heading o titulo de la seccion dentro del documento.';
COMMENT ON COLUMN document_sections.content    IS 'Contenido de la seccion en texto plano o markdown.';
COMMENT ON COLUMN document_sections.embedding  IS 'Vector de 384 dimensiones (multilingual-e5-small) del contenido de la seccion.';
COMMENT ON COLUMN document_sections.position   IS 'Orden de la seccion dentro del documento (0-based).';
COMMENT ON COLUMN document_sections.indexed_at IS 'Timestamp de la ultima indexacion de esta seccion.';
