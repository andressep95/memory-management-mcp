package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.memory;

import com.cloudcentinel.memory_management_mcp.domain.memory.entity.MemoryChange;
import com.cloudcentinel.memory_management_mcp.domain.memory.repository.MemoryChangeRepository;
import com.cloudcentinel.memory_management_mcp.domain.memory.repository.ScoredMemoryChange;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.ChangeIntent;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.CommitHash;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.MemoryChangeId;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;
import com.cloudcentinel.memory_management_mcp.infrastructure.persistence.shared.UuidRawConverter;
import oracle.sql.VECTOR;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class MemoryChangeRepositoryAdapter implements MemoryChangeRepository {

    private static final String FIND_SIMILAR_SQL = """
            SELECT id, project_id, commit_hash, branch, author, file_path,
                   kind, intent, what, why, language, tags, created_at,
                   VECTOR_DISTANCE(embedding, ?, COSINE) AS score
            FROM memory_changes
            WHERE project_id = ?
            ORDER BY score ASC
            FETCH FIRST ? ROWS ONLY
            """;

    private static final String FIND_SIMILAR_BY_KIND_SQL = """
            SELECT id, project_id, commit_hash, branch, author, file_path,
                   kind, intent, what, why, language, tags, created_at,
                   VECTOR_DISTANCE(embedding, ?, COSINE) AS score
            FROM memory_changes
            WHERE project_id = ? AND kind = ?
            ORDER BY score ASC
            FETCH FIRST ? ROWS ONLY
            """;

    private static final String FIND_COMMIT_HASHES_SQL = """
            SELECT DISTINCT commit_hash FROM memory_changes WHERE project_id = ?
            """;

    private static final String FIND_COMMIT_FILE_PAIRS_SQL = """
            SELECT commit_hash, file_path FROM memory_changes WHERE project_id = ?
            """;

    private static final String INSERT_SQL = """
            INSERT INTO memory_changes
              (id, project_id, commit_hash, branch, author, file_path,
               kind, intent, what, why, language, tags, raw_diff,
               content_before, content_after, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)
            """;

    private static final String UPDATE_EMBEDDING_SQL =
            "UPDATE memory_changes SET embedding = ? WHERE id = ?";

    private static final String UPDATE_ENRICHMENT_SQL = """
            UPDATE memory_changes SET intent = ?, what = ?, why = ?, embedding = ? WHERE id = ?
            """;

    private final MemoryChangeSpringDataRepository jpaRepo;
    private final JdbcTemplate jdbcTemplate;
    private final UuidRawConverter uuidConverter = new UuidRawConverter();

    public MemoryChangeRepositoryAdapter(MemoryChangeSpringDataRepository jpaRepo,
                                         JdbcTemplate jdbcTemplate) {
        this.jpaRepo      = jpaRepo;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(MemoryChange change) {
        jpaRepo.save(MemoryChangeJpaEntity.from(change));
        if (change.embedding() != null) {
            updateEmbedding(change.id().value(), change.embedding().values());
        }
    }

    @Override
    public void saveAll(List<MemoryChange> changes) {
        if (changes == null || changes.isEmpty()) return;

        jdbcTemplate.batchUpdate(INSERT_SQL, changes, changes.size(), (ps, change) -> {
            ps.setBytes(1,  uuidConverter.convertToDatabaseColumn(change.id().value()));
            ps.setBytes(2,  uuidConverter.convertToDatabaseColumn(UUID.fromString(change.projectId())));
            ps.setString(3, change.commitHash().value());
            ps.setString(4, change.branch());
            ps.setString(5, change.author());
            ps.setString(6, change.filePath());
            ps.setString(7, change.kind());
            ps.setString(8, change.intent() != null ? change.intent().name() : null);
            ps.setString(9, change.what());
            ps.setString(10, change.why());
            ps.setString(11, change.language());
            ps.setString(12, change.tags().isEmpty() ? null : String.join(",", change.tags()));
            ps.setString(13, change.rawDiff());
            ps.setString(14, change.contentBefore());
            ps.setString(15, change.contentAfter());
        });

        jdbcTemplate.batchUpdate(UPDATE_EMBEDDING_SQL, changes, changes.size(), (ps, change) -> {
            if (change.embedding() != null) {
                ps.setObject(1, toOracleVector(change.embedding().values()));
            } else {
                ps.setNull(1, java.sql.Types.OTHER);
            }
            ps.setBytes(2, uuidConverter.convertToDatabaseColumn(change.id().value()));
        });
    }

    @Override
    public Optional<MemoryChange> findById(MemoryChangeId id) {
        return jpaRepo.findById(id.value()).map(MemoryChangeJpaEntity::toDomain);
    }

    @Override
    public boolean existsByProjectCommitAndFile(String projectId, CommitHash commitHash, String filePath) {
        return jpaRepo.existsByProjectIdAndCommitHashAndFilePath(
                UUID.fromString(projectId), commitHash.value(), filePath);
    }

    @Override
    public Set<String> findIndexedCommitHashes(String projectId) {
        byte[] projectIdBytes = uuidConverter.convertToDatabaseColumn(UUID.fromString(projectId));
        List<String> hashes = jdbcTemplate.query(
                FIND_COMMIT_HASHES_SQL,
                ps -> ps.setBytes(1, projectIdBytes),
                (rs, rowNum) -> rs.getString("commit_hash")
        );
        return new HashSet<>(hashes);
    }

    @Override
    public Set<String> findIndexedCommitFilePairs(String projectId) {
        byte[] projectIdBytes = uuidConverter.convertToDatabaseColumn(UUID.fromString(projectId));
        List<String> pairs = jdbcTemplate.query(
                FIND_COMMIT_FILE_PAIRS_SQL,
                ps -> ps.setBytes(1, projectIdBytes),
                (rs, rowNum) -> rs.getString("commit_hash") + ":" + rs.getString("file_path")
        );
        return new HashSet<>(pairs);
    }

    @Override
    public List<ScoredMemoryChange> findSimilar(EmbeddingVector query, String projectId, int limit) {
        Object vectorParam    = toOracleVector(query.values());
        byte[] projectIdBytes = uuidConverter.convertToDatabaseColumn(UUID.fromString(projectId));

        return jdbcTemplate.query(
                FIND_SIMILAR_SQL,
                ps -> {
                    ps.setObject(1, vectorParam);
                    ps.setBytes(2, projectIdBytes);
                    ps.setInt(3, limit);
                },
                (rs, rowNum) -> mapRow(rs)
        );
    }

    @Override
    public List<ScoredMemoryChange> findSimilar(EmbeddingVector query, String projectId, int limit, String kind) {
        Object vectorParam    = toOracleVector(query.values());
        byte[] projectIdBytes = uuidConverter.convertToDatabaseColumn(UUID.fromString(projectId));

        return jdbcTemplate.query(
                FIND_SIMILAR_BY_KIND_SQL,
                ps -> {
                    ps.setObject(1, vectorParam);
                    ps.setBytes(2, projectIdBytes);
                    ps.setString(3, kind);
                    ps.setInt(4, limit);
                },
                (rs, rowNum) -> mapRow(rs)
        );
    }

    @Override
    public void updateEnrichment(MemoryChangeId id, ChangeIntent intent, String what, String why, EmbeddingVector embedding) {
        byte[] idBytes = uuidConverter.convertToDatabaseColumn(id.value());
        jdbcTemplate.update(UPDATE_ENRICHMENT_SQL, ps -> {
            ps.setString(1, intent != null ? intent.name() : null);
            ps.setString(2, what);
            ps.setString(3, why);
            ps.setObject(4, embedding != null ? toOracleVector(embedding.values()) : null);
            ps.setBytes(5, idBytes);
        });
    }

    @Override
    public Optional<EnrichmentData> findForEnrichment(MemoryChangeId id) {
        byte[] idBytes = uuidConverter.convertToDatabaseColumn(id.value());
        List<EnrichmentData> results = jdbcTemplate.query(
                "SELECT id, commit_hash, what, file_path, raw_diff FROM memory_changes WHERE id = ?",
                ps -> ps.setBytes(1, idBytes),
                (rs, rowNum) -> new EnrichmentData(
                        new MemoryChangeId(toUuid(rs.getBytes("id"))),
                        rs.getString("commit_hash"),
                        rs.getString("what"),
                        rs.getString("file_path"),
                        rs.getString("raw_diff")
                )
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    private ScoredMemoryChange mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        byte[] idBytes    = rs.getBytes("id");
        byte[] projIdB    = rs.getBytes("project_id");
        String hash       = rs.getString("commit_hash");
        String branch     = rs.getString("branch");
        String author     = rs.getString("author");
        String filePath   = rs.getString("file_path");
        String kind       = rs.getString("kind");
        String intent     = rs.getString("intent");
        String what       = rs.getString("what");
        String why        = rs.getString("why");
        String language   = rs.getString("language");
        String tags       = rs.getString("tags");
        java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
        double score      = rs.getDouble("score");

        List<String> tagList = (tags != null && !tags.isBlank())
                ? Arrays.asList(tags.split(","))
                : List.of();

        MemoryChange change = MemoryChange.reconstitute(
                new MemoryChangeId(toUuid(idBytes)),
                toUuid(projIdB).toString(),
                new CommitHash(hash),
                branch, author, filePath,
                ChangeIntent.fromString(intent),
                what, why, kind, language, tagList,
                null, null, null,
                null,
                createdTs != null ? createdTs.toInstant() : Instant.now(),
                List.of()
        );
        return new ScoredMemoryChange(change, 1.0 - score);
    }

    private void updateEmbedding(UUID id, float[] values) {
        byte[] idBytes = uuidConverter.convertToDatabaseColumn(id);
        jdbcTemplate.update(UPDATE_EMBEDDING_SQL, ps -> {
            ps.setObject(1, toOracleVector(values));
            ps.setBytes(2, idBytes);
        });
    }

    private Object toOracleVector(float[] values) {
        try {
            return VECTOR.ofFloat32Values(values);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Oracle VECTOR", e);
        }
    }

    private UUID toUuid(byte[] bytes) {
        if (bytes == null) return null;
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }
}
