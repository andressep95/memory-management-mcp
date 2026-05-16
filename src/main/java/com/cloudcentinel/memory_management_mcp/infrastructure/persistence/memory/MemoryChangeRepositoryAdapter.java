package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.memory;

import com.cloudcentinel.memory_management_mcp.domain.memory.entity.MemoryChange;
import com.cloudcentinel.memory_management_mcp.domain.memory.repository.MemoryChangeRepository;
import com.cloudcentinel.memory_management_mcp.domain.memory.repository.ScoredMemoryChange;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.ChangeIntent;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.CommitHash;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.MemoryChangeId;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
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
                   intent, what, why, language, tags, created_at,
                   VECTOR_DISTANCE(embedding, ?, COSINE) AS score
            FROM memory_changes
            WHERE project_id = ?
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
               intent, what, why, language, tags, raw_diff,
               content_before, content_after, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)
            """;

    private static final String UPDATE_EMBEDDING_SQL =
            "UPDATE memory_changes SET embedding = ? WHERE id = ?";

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
        MemoryChangeJpaEntity entity = MemoryChangeJpaEntity.from(change);
        jpaRepo.save(entity);
        if (change.embedding() != null) {
            updateEmbedding(change.id().value(), change.embedding().values());
        }
    }

    @Override
    public void saveAll(List<MemoryChange> changes) {
        if (changes == null || changes.isEmpty()) return;

        jdbcTemplate.batchUpdate(INSERT_SQL, changes, changes.size(), (ps, change) -> {
            ps.setBytes(1,  uuidConverter.convertToDatabaseColumn(change.id().value()));
            ps.setBytes(2,  uuidConverter.convertToDatabaseColumn(change.projectId().value()));
            ps.setString(3, change.commitHash().value());
            ps.setString(4, change.branch());
            ps.setString(5, change.author());
            ps.setString(6, change.filePath());
            ps.setString(7, change.intent() != null ? change.intent().name() : null);
            ps.setString(8, change.what());
            ps.setString(9, change.why());
            ps.setString(10, change.language());
            ps.setString(11, change.tags().isEmpty() ? null : String.join(",", change.tags()));
            ps.setString(12, change.rawDiff());
            ps.setString(13, change.contentBefore());
            ps.setString(14, change.contentAfter());
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
    public boolean existsByProjectCommitAndFile(ProjectId projectId, CommitHash commitHash, String filePath) {
        return jpaRepo.existsByProjectIdAndCommitHashAndFilePath(
                projectId.value(), commitHash.value(), filePath);
    }

    @Override
    public Set<String> findIndexedCommitHashes(ProjectId projectId) {
        byte[] projectIdBytes = uuidConverter.convertToDatabaseColumn(projectId.value());
        List<String> hashes = jdbcTemplate.query(
                FIND_COMMIT_HASHES_SQL,
                ps -> ps.setBytes(1, projectIdBytes),
                (rs, rowNum) -> rs.getString("commit_hash")
        );
        return new HashSet<>(hashes);
    }

    @Override
    public Set<String> findIndexedCommitFilePairs(ProjectId projectId) {
        byte[] projectIdBytes = uuidConverter.convertToDatabaseColumn(projectId.value());
        List<String> pairs = jdbcTemplate.query(
                FIND_COMMIT_FILE_PAIRS_SQL,
                ps -> ps.setBytes(1, projectIdBytes),
                (rs, rowNum) -> rs.getString("commit_hash") + ":" + rs.getString("file_path")
        );
        return new HashSet<>(pairs);
    }

    @Override
    public List<ScoredMemoryChange> findSimilar(EmbeddingVector query, ProjectId projectId, int limit) {
        Object vectorParam    = toOracleVector(query.values());
        byte[] projectIdBytes = uuidConverter.convertToDatabaseColumn(projectId.value());

        return jdbcTemplate.query(
                FIND_SIMILAR_SQL,
                ps -> {
                    ps.setObject(1, vectorParam);
                    ps.setBytes(2, projectIdBytes);
                    ps.setInt(3, limit);
                },
                (rs, rowNum) -> {
                    byte[] idBytes    = rs.getBytes("id");
                    byte[] projIdB    = rs.getBytes("project_id");
                    String hash       = rs.getString("commit_hash");
                    String branch     = rs.getString("branch");
                    String author     = rs.getString("author");
                    String filePath   = rs.getString("file_path");
                    String intent     = rs.getString("intent");
                    String what       = rs.getString("what");
                    String why        = rs.getString("why");
                    String language   = rs.getString("language");
                    String tags       = rs.getString("tags");
                    Timestamp createdTs = rs.getTimestamp("created_at");
                    double score      = rs.getDouble("score");

                    List<String> tagList = (tags != null && !tags.isBlank())
                            ? Arrays.asList(tags.split(","))
                            : List.of();

                    MemoryChange change = MemoryChange.reconstitute(
                            new MemoryChangeId(toUuid(idBytes)),
                            ProjectId.of(toUuid(projIdB)),
                            new CommitHash(hash),
                            branch, author, filePath,
                            ChangeIntent.fromString(intent),
                            what, why, language, tagList,
                            null, null, null,
                            null,
                            createdTs != null ? createdTs.toInstant() : Instant.now(),
                            List.of()
                    );
                    return new ScoredMemoryChange(change, 1.0 - score);
                }
        );
    }

    private void updateEmbedding(UUID id, float[] values) {
        byte[] idBytes = uuidConverter.convertToDatabaseColumn(id);
        Object vector  = toOracleVector(values);
        jdbcTemplate.update(UPDATE_EMBEDDING_SQL, ps -> {
            ps.setObject(1, vector);
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
