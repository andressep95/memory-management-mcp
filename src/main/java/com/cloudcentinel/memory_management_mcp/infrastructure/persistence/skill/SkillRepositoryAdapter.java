package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.skill;

import com.cloudcentinel.memory_management_mcp.domain.skill.entity.Skill;
import com.cloudcentinel.memory_management_mcp.domain.skill.entity.SkillChunk;
import com.cloudcentinel.memory_management_mcp.domain.skill.repository.ScoredChunk;
import com.cloudcentinel.memory_management_mcp.domain.skill.repository.ScoredSkill;
import com.cloudcentinel.memory_management_mcp.domain.skill.repository.SkillRepository;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.ChunkName;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillContent;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillId;
import com.cloudcentinel.memory_management_mcp.infrastructure.persistence.shared.UuidRawConverter;
import oracle.sql.VECTOR;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SkillRepositoryAdapter implements SkillRepository {

    private static final String FIND_SIMILAR_SKILLS_SQL = """
            SELECT s.id, s.name, s.content, s.active, s.synced_at,
                   VECTOR_DISTANCE(s.embedding, ?, COSINE) AS score
            FROM skills s
            JOIN project_skills ps ON ps.skill_id = s.id
            WHERE ps.project_id = ?
              AND ps.active = 1
              AND s.active = 1
            ORDER BY score ASC
            FETCH FIRST ? ROWS ONLY
            """;

    private static final String FIND_SIMILAR_CHUNKS_SQL = """
            SELECT sc.id, sc.chunk_name, sc.content, sc.position, sc.synced_at,
                   s.id AS skill_id, s.name AS skill_name,
                   VECTOR_DISTANCE(sc.embedding, ?, COSINE) AS score
            FROM skill_chunks sc
            JOIN skills s ON s.id = sc.skill_id
            JOIN project_skills ps ON ps.skill_id = s.id
            WHERE ps.project_id = ?
              AND ps.active = 1
              AND s.active = 1
            ORDER BY score ASC
            FETCH FIRST ? ROWS ONLY
            """;

    private final SkillSpringDataRepository skillRepo;
    private final SkillChunkSpringDataRepository chunkRepo;
    private final JdbcTemplate jdbcTemplate;
    private final UuidRawConverter uuidConverter = new UuidRawConverter();

    public SkillRepositoryAdapter(SkillSpringDataRepository skillRepo,
                                   SkillChunkSpringDataRepository chunkRepo,
                                   JdbcTemplate jdbcTemplate) {
        this.skillRepo  = skillRepo;
        this.chunkRepo  = chunkRepo;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(Skill skill) {
        skillRepo.save(SkillJpaEntity.from(skill));
        if (skill.embedding() != null) {
            updateSkillEmbedding(skill.id().value(), skill.embedding().values());
        }
        for (SkillChunk chunk : skill.chunks()) {
            if (chunk.embedding() != null) {
                updateChunkEmbeddingBySkillAndName(
                        skill.id().value(), chunk.name().value(), chunk.embedding().values());
            }
        }
    }

    @Override
    public Optional<Skill> findById(SkillId id) {
        return skillRepo.findById(id.value()).map(SkillJpaEntity::toDomain);
    }

    @Override
    public Optional<Skill> findByName(String name) {
        return skillRepo.findByName(name).map(SkillJpaEntity::toDomain);
    }

    @Override
    public List<ScoredSkill> findSimilar(EmbeddingVector query, String projectId, int limit) {
        Object vectorParam    = toOracleVector(query.values());
        byte[] projectIdBytes = uuidConverter.convertToDatabaseColumn(UUID.fromString(projectId));

        return jdbcTemplate.query(
                FIND_SIMILAR_SKILLS_SQL,
                ps -> {
                    ps.setObject(1, vectorParam);
                    ps.setBytes(2, projectIdBytes);
                    ps.setInt(3, limit);
                },
                (rs, rowNum) -> {
                    byte[] idBytes     = rs.getBytes("id");
                    String name        = rs.getString("name");
                    String content     = rs.getString("content");
                    boolean active     = rs.getInt("active") == 1;
                    Timestamp syncedTs = rs.getTimestamp("synced_at");
                    double score       = rs.getDouble("score");

                    Skill s = Skill.reconstitute(
                            SkillId.of(toUuid(idBytes)), name,
                            new SkillContent(content), null,
                            active, syncedTs != null ? syncedTs.toInstant() : Instant.now(),
                            List.of()
                    );
                    return new ScoredSkill(s, 1.0 - score);
                }
        );
    }

    @Override
    public List<ScoredChunk> findSimilarChunks(EmbeddingVector query, String projectId, int limit) {
        Object vectorParam    = toOracleVector(query.values());
        byte[] projectIdBytes = uuidConverter.convertToDatabaseColumn(UUID.fromString(projectId));

        return jdbcTemplate.query(
                FIND_SIMILAR_CHUNKS_SQL,
                ps -> {
                    ps.setObject(1, vectorParam);
                    ps.setBytes(2, projectIdBytes);
                    ps.setInt(3, limit);
                },
                (rs, rowNum) -> {
                    String chunkName   = rs.getString("chunk_name");
                    String content     = rs.getString("content");
                    int position       = rs.getInt("position");
                    Timestamp syncedTs = rs.getTimestamp("synced_at");
                    byte[] skillIdB    = rs.getBytes("skill_id");
                    String skillName   = rs.getString("skill_name");
                    double score       = rs.getDouble("score");

                    SkillChunk chunk = new SkillChunk(
                            new ChunkName(chunkName),
                            new SkillContent(content),
                            null,
                            position,
                            syncedTs != null ? syncedTs.toInstant() : Instant.now()
                    );
                    return new ScoredChunk(chunk, SkillId.of(toUuid(skillIdB)), skillName, 1.0 - score);
                }
        );
    }

    private void updateSkillEmbedding(UUID skillId, float[] values) {
        byte[] idBytes = uuidConverter.convertToDatabaseColumn(skillId);
        jdbcTemplate.update("UPDATE skills SET embedding = ? WHERE id = ?", ps -> {
            ps.setObject(1, toOracleVector(values));
            ps.setBytes(2, idBytes);
        });
    }

    private void updateChunkEmbeddingBySkillAndName(UUID skillId, String chunkName, float[] values) {
        byte[] idBytes = uuidConverter.convertToDatabaseColumn(skillId);
        jdbcTemplate.update(
                "UPDATE skill_chunks SET embedding = ? WHERE skill_id = ? AND chunk_name = ?",
                ps -> {
                    ps.setObject(1, toOracleVector(values));
                    ps.setBytes(2, idBytes);
                    ps.setString(3, chunkName);
                }
        );
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
