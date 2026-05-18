-- ============================================================
-- MCP Agent Server — Oracle 23ai Schema
-- 11 tables — simplified from original 15
-- Removed: user_project_roles, user_private_skills,
--          user_preferences, user_preference_skills,
--          session_skill_usage
-- ============================================================


-- ============================================================
-- PROJECTS  —  Registered via REST (POST /api/projects)
-- Identified by api_key in all subsequent requests.
-- ============================================================
CREATE TABLE projects (
    id                   RAW(16)                      DEFAULT SYS_GUID() NOT NULL,
    api_key              VARCHAR2(64)                 NOT NULL,
    name                 VARCHAR2(255)                NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE     DEFAULT SYSTIMESTAMP NOT NULL,
    setup_completed_at   TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_projects         PRIMARY KEY (id),
    CONSTRAINT uk_projects_api_key UNIQUE (api_key),
    CONSTRAINT uk_projects_name    UNIQUE (name)
);


-- ============================================================
-- USERS  —  Auto-created on first git activity
-- Identity derived from git config (user.name / user.email).
-- No registration flow required.
-- ============================================================
CREATE TABLE users (
    git_username  VARCHAR2(255)              NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE   DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (git_username)
);


-- ============================================================
-- SESSIONS  —  One row per Claude/Kiro conversation
-- session_id sourced from Claude Code / Kiro hook payloads.
-- ============================================================
CREATE TABLE sessions (
    session_id     VARCHAR2(64)               NOT NULL,
    project_id     RAW(16)                    NOT NULL,
    git_username   VARCHAR2(255)              NOT NULL,
    agent          VARCHAR2(20)               NOT NULL,
    started_at     TIMESTAMP WITH TIME ZONE   DEFAULT SYSTIMESTAMP NOT NULL,
    last_activity  TIMESTAMP WITH TIME ZONE   DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_sessions             PRIMARY KEY (session_id),
    CONSTRAINT fk_sessions_project     FOREIGN KEY (project_id)   REFERENCES projects (id),
    CONSTRAINT fk_sessions_user        FOREIGN KEY (git_username) REFERENCES users (git_username),
    CONSTRAINT ck_sessions_agent       CHECK (agent IN ('claude', 'kiro'))
);


-- ============================================================
-- SKILLS  —  Global skill catalogue (not scoped to a project)
-- ============================================================
CREATE TABLE skills (
    id         RAW(16)                      DEFAULT SYS_GUID() NOT NULL,
    name       VARCHAR2(255)                NOT NULL,
    content    CLOB                         NOT NULL,
    embedding  VECTOR(384, FLOAT32)        ,
    active     NUMBER(1)                    DEFAULT 1 NOT NULL,
    synced_at  TIMESTAMP WITH TIME ZONE     DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_skills        PRIMARY KEY (id),
    CONSTRAINT uk_skills_name   UNIQUE (name),
    CONSTRAINT ck_skills_active CHECK (active IN (0, 1))
);


-- ============================================================
-- SKILL_CHUNKS  —  Sub-documents of a skill with their own
-- embedding for fine-grained RAG retrieval.
-- ============================================================
CREATE TABLE skill_chunks (
    id          RAW(16)                      DEFAULT SYS_GUID() NOT NULL,
    skill_id    RAW(16)                      NOT NULL,
    chunk_name  VARCHAR2(255)                NOT NULL,
    content     CLOB                         NOT NULL,
    embedding   VECTOR(384, FLOAT32)        ,
    position    NUMBER(5)                    NOT NULL,
    synced_at   TIMESTAMP WITH TIME ZONE     DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_skill_chunks       PRIMARY KEY (id),
    CONSTRAINT fk_skill_chunks_skill FOREIGN KEY (skill_id) REFERENCES skills (id),
    CONSTRAINT uk_skill_chunks_name  UNIQUE (skill_id, chunk_name)
);


-- ============================================================
-- PROJECT_SKILLS  —  Skills enabled for a project
-- Controlled by the project owner via MCP tools.
-- ============================================================
CREATE TABLE project_skills (
    project_id  RAW(16)                      NOT NULL,
    skill_id    RAW(16)                      NOT NULL,
    active      NUMBER(1)                    DEFAULT 1 NOT NULL,
    enabled_at  TIMESTAMP WITH TIME ZONE     DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_project_skills         PRIMARY KEY (project_id, skill_id),
    CONSTRAINT fk_project_skills_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_skills_skill   FOREIGN KEY (skill_id)   REFERENCES skills (id),
    CONSTRAINT ck_project_skills_active  CHECK (active IN (0, 1))
);


-- ============================================================
-- USER_SKILL_PREFS  —  Per-user skill preferences within a project
-- Set via MCP tools during a session.
-- ============================================================
CREATE TABLE user_skill_prefs (
    git_username  VARCHAR2(255)              NOT NULL,
    project_id    RAW(16)                    NOT NULL,
    skill_id      RAW(16)                    NOT NULL,
    added_at      TIMESTAMP WITH TIME ZONE   DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_user_skill_prefs         PRIMARY KEY (git_username, project_id, skill_id),
    CONSTRAINT fk_user_skill_prefs_user    FOREIGN KEY (git_username) REFERENCES users (git_username),
    CONSTRAINT fk_user_skill_prefs_project FOREIGN KEY (project_id)   REFERENCES projects (id),
    CONSTRAINT fk_user_skill_prefs_skill   FOREIGN KEY (skill_id)     REFERENCES skills (id)
);


-- ============================================================
-- MEMORY_CHANGES  —  Vectorised git commit history
-- Granularity: 1 row per file per commit.
-- ============================================================
CREATE TABLE memory_changes (
    id              RAW(16)                  DEFAULT SYS_GUID() NOT NULL,
    project_id      RAW(16)                  NOT NULL,
    commit_hash     VARCHAR2(64)             NOT NULL,
    branch          VARCHAR2(255)            NOT NULL,
    author          VARCHAR2(255)            NOT NULL,
    file_path       VARCHAR2(1000)           NOT NULL,
    kind            VARCHAR2(10)            ,
    intent          VARCHAR2(50)            ,
    what            CLOB                     NOT NULL,
    why             CLOB                    ,
    language        VARCHAR2(50)            ,
    tags            VARCHAR2(500)           ,
    raw_diff        CLOB                    ,
    content_before  CLOB                    ,
    content_after   CLOB                    ,
    embedding       VECTOR(384, FLOAT32)    ,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_memory_changes             PRIMARY KEY (id),
    CONSTRAINT fk_memory_changes_project     FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT uk_memory_changes_commit_file UNIQUE (project_id, commit_hash, file_path),
    CONSTRAINT ck_memory_changes_kind        CHECK (kind IN ('code', 'doc', 'config'))
);


-- ============================================================
-- MEMORY_CHANGE_HUNKS  —  Individual @@ diff blocks
-- Children of MEMORY_CHANGES (CASCADE DELETE).
-- ============================================================
CREATE TABLE memory_change_hunks (
    id                RAW(16)                DEFAULT SYS_GUID() NOT NULL,
    memory_change_id  RAW(16)                NOT NULL,
    lines_start       NUMBER(10)             NOT NULL,
    lines_end         NUMBER(10)             NOT NULL,
    symbol            VARCHAR2(500)         ,
    change_type       VARCHAR2(20)           NOT NULL,
    hunk_diff         CLOB                   NOT NULL,
    CONSTRAINT pk_memory_change_hunks    PRIMARY KEY (id),
    CONSTRAINT fk_memory_change_hunks_mc
        FOREIGN KEY (memory_change_id) REFERENCES memory_changes (id) ON DELETE CASCADE,
    CONSTRAINT ck_memory_change_hunks_type
        CHECK (change_type IN ('addition', 'deletion', 'modification'))
);


-- ============================================================
-- DOCUMENTS  —  Project knowledge base (docs, ADRs, guides)
-- ============================================================
CREATE TABLE documents (
    id                  RAW(16)              DEFAULT SYS_GUID() NOT NULL,
    project_id          RAW(16)              NOT NULL,
    source_path         VARCHAR2(1000)       NOT NULL,
    title               VARCHAR2(500)        NOT NULL,
    doc_type            VARCHAR2(50)         NOT NULL,
    content             CLOB                 NOT NULL,
    embedding           VECTOR(384, FLOAT32),
    indexed_at          TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    source_modified_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    stale               NUMBER(1)            DEFAULT 0 NOT NULL,
    CONSTRAINT pk_documents          PRIMARY KEY (id),
    CONSTRAINT fk_documents_project  FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT uk_documents_path     UNIQUE (project_id, source_path),
    CONSTRAINT ck_documents_stale    CHECK (stale IN (0, 1)),
    CONSTRAINT ck_documents_type     CHECK (doc_type IN (
        'ADR', 'API_SPEC', 'RUNBOOK', 'GUIDE', 'README',
        'CHANGELOG', 'ONBOARDING', 'DESIGN', 'OTHER'
    ))
);


-- ============================================================
-- DOCUMENT_SECTIONS  —  Granular RAG sections with own embedding
-- ============================================================
CREATE TABLE document_sections (
    id           RAW(16)                     DEFAULT SYS_GUID() NOT NULL,
    document_id  RAW(16)                     NOT NULL,
    heading      VARCHAR2(500)               NOT NULL,
    content      CLOB                        NOT NULL,
    embedding    VECTOR(384, FLOAT32)       ,
    position     NUMBER(5)                   NOT NULL,
    indexed_at   TIMESTAMP WITH TIME ZONE    DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_document_sections     PRIMARY KEY (id),
    CONSTRAINT fk_doc_sections_document
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);


-- ============================================================
-- VECTOR INDEXES  —  HNSW cosine similarity (Oracle 23ai)
-- ============================================================
CREATE VECTOR INDEX vidx_skills_embedding
    ON skills (embedding)
    ORGANIZATION NEIGHBOR PARTITIONS
    WITH DISTANCE COSINE
    WITH TARGET ACCURACY 95;

CREATE VECTOR INDEX vidx_skill_chunks_embedding
    ON skill_chunks (embedding)
    ORGANIZATION NEIGHBOR PARTITIONS
    WITH DISTANCE COSINE
    WITH TARGET ACCURACY 95;

CREATE VECTOR INDEX vidx_memory_changes_embedding
    ON memory_changes (embedding)
    ORGANIZATION NEIGHBOR PARTITIONS
    WITH DISTANCE COSINE
    WITH TARGET ACCURACY 95;

CREATE VECTOR INDEX vidx_documents_embedding
    ON documents (embedding)
    ORGANIZATION NEIGHBOR PARTITIONS
    WITH DISTANCE COSINE
    WITH TARGET ACCURACY 95;

CREATE VECTOR INDEX vidx_document_sections_embedding
    ON document_sections (embedding)
    ORGANIZATION NEIGHBOR PARTITIONS
    WITH DISTANCE COSINE
    WITH TARGET ACCURACY 95;


-- ============================================================
-- INDEXES  —  Frequent lookups and range scans
-- ============================================================

-- sessions
CREATE INDEX idx_sessions_project  ON sessions (project_id, started_at DESC);
CREATE INDEX idx_sessions_user     ON sessions (git_username, project_id);

-- skill_chunks
CREATE INDEX idx_skill_chunks_skill ON skill_chunks (skill_id, position);

-- project_skills
CREATE INDEX idx_project_skills_skill ON project_skills (skill_id);

-- user_skill_prefs
CREATE INDEX idx_user_skill_prefs_project ON user_skill_prefs (project_id, git_username);

-- memory_changes
CREATE INDEX idx_memory_changes_project ON memory_changes (project_id, created_at DESC);
CREATE INDEX idx_memory_changes_commit  ON memory_changes (commit_hash);
CREATE INDEX idx_memory_changes_file    ON memory_changes (project_id, file_path);
CREATE INDEX idx_memory_changes_kind    ON memory_changes (project_id, kind);

-- memory_change_hunks
CREATE INDEX idx_memory_change_hunks_mc    ON memory_change_hunks (memory_change_id);
CREATE INDEX idx_memory_change_hunks_lines ON memory_change_hunks (memory_change_id, lines_start, lines_end);

-- documents
CREATE INDEX idx_documents_project ON documents (project_id, doc_type);
CREATE INDEX idx_documents_stale   ON documents (project_id, stale);

-- document_sections
CREATE INDEX idx_doc_sections_document ON document_sections (document_id, position);


-- ============================================================
-- COMMENTS
-- ============================================================

COMMENT ON TABLE  projects           IS 'Projects registered via REST (POST /api/projects). Identified by api_key in all requests.';
COMMENT ON COLUMN projects.api_key              IS 'Secret key used to route all batch indexing and MCP calls to this project.';
COMMENT ON COLUMN projects.name                 IS 'Human-readable project name, typically the git repository folder name.';
COMMENT ON COLUMN projects.setup_completed_at   IS 'Timestamp of the last successful setupProject MCP tool call. NULL means setup has not been run yet.';

COMMENT ON TABLE  users              IS 'Developers auto-created on first git activity. Identity from git config (user.name / user.email). No explicit registration.';
COMMENT ON COLUMN users.git_username IS 'Primary key. Git author name — used as identity across sessions and commits.';

COMMENT ON TABLE  sessions           IS 'One row per Claude or Kiro conversation. session_id sourced from hook payload.';
COMMENT ON COLUMN sessions.agent     IS 'Agent type: claude or kiro.';

COMMENT ON TABLE  skills             IS 'Global skill catalogue. Skills are not scoped to a project — any project can enable them via project_skills.';
COMMENT ON COLUMN skills.embedding   IS '384-dim FLOAT32 vector (multilingual-e5-small). Used for cosine similarity search.';
COMMENT ON COLUMN skills.synced_at   IS 'Last sync timestamp. Updated when content hash changes.';

COMMENT ON TABLE  skill_chunks            IS 'Sub-documents of a skill with their own embedding for fine-grained RAG. Search returns the most relevant chunk; agent loads parent skill for full context.';
COMMENT ON COLUMN skill_chunks.chunk_name IS 'Relative name/path of the sub-document within the skill. Unique within a skill.';
COMMENT ON COLUMN skill_chunks.position   IS 'Order within the skill. Allows reconstructing the full skill by concatenating chunks in ascending order.';

COMMENT ON TABLE  project_skills     IS 'Skills enabled for a project. Controlled by the project owner via MCP tools. active=0 disables without deleting.';

COMMENT ON TABLE  user_skill_prefs   IS 'Per-user skill preferences within a project. Set via MCP tools during a session. Allows each developer to configure their own subset of enabled skills.';

COMMENT ON TABLE  memory_changes                IS 'Vectorised git commit history. 1 row per file per commit. Enables semantic search over past decisions and implementations.';
COMMENT ON COLUMN memory_changes.project_id     IS 'FK to PROJECTS.id. Scopes memory to a specific repository.';
COMMENT ON COLUMN memory_changes.intent         IS 'Commit type: feat, fix, refactor, docs, test, chore, perf, style.';
COMMENT ON COLUMN memory_changes.what           IS 'What changed. Parsed from commit body (what: line). Key field for semantic search.';
COMMENT ON COLUMN memory_changes.why            IS 'Why this change was made. Parsed from commit body (why: line). Most important field for semantic search.';
COMMENT ON COLUMN memory_changes.kind           IS 'File discriminator: code (source/test/script), doc (markdown/specs), config (build/infra). Drives queryCode and queryDocs MCP tools.';
COMMENT ON COLUMN memory_changes.embedding      IS '384-dim vector of (intent + what + why + filePath). Enables intent-based similarity search.';

COMMENT ON TABLE  memory_change_hunks               IS 'Individual @@ diff blocks of a file change. CASCADE DELETE from memory_changes. Enables granular line-range navigation.';
COMMENT ON COLUMN memory_change_hunks.change_type   IS 'Hunk type: addition (+ only), deletion (- only), modification (both).';

COMMENT ON TABLE  documents                    IS 'Project knowledge base: ADRs, API specs, runbooks, guides, READMEs, and other documentation indexed for semantic search.';
COMMENT ON COLUMN documents.doc_type           IS 'Document type: ADR, API_SPEC, RUNBOOK, GUIDE, README, CHANGELOG, ONBOARDING, DESIGN, OTHER.';
COMMENT ON COLUMN documents.stale              IS '1 = potentially outdated relative to recent changes, 0 = current.';

COMMENT ON TABLE  document_sections            IS 'Individual sections of a document with their own embedding for granular RAG retrieval by heading.';
COMMENT ON COLUMN document_sections.position   IS 'Section order within the document (0-based).';
