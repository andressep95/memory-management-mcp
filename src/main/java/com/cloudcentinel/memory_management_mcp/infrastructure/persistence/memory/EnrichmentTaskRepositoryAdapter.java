package com.cloudcentinel.memory_management_mcp.infrastructure.persistence.memory;

import com.cloudcentinel.memory_management_mcp.domain.memory.entity.EnrichmentTask;
import com.cloudcentinel.memory_management_mcp.domain.memory.repository.EnrichmentTaskRepository;
import com.cloudcentinel.memory_management_mcp.domain.memory.valueobject.MemoryChangeId;
import com.cloudcentinel.memory_management_mcp.infrastructure.persistence.shared.UuidRawConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class EnrichmentTaskRepositoryAdapter implements EnrichmentTaskRepository {

    private static final String INSERT_SQL = """
            INSERT INTO enrichment_queue (id, memory_change_id, status, attempts, created_at)
            VALUES (?, ?, 'PENDING', 0, SYSTIMESTAMP)
            """;

    private static final String FIND_PENDING_SQL = """
            SELECT id, memory_change_id, status, attempts, last_error, created_at, processed_at
            FROM enrichment_queue
            WHERE id IN (
                SELECT id FROM (
                    SELECT id FROM enrichment_queue
                    WHERE status = 'PENDING'
                    ORDER BY created_at ASC
                ) WHERE ROWNUM <= ?
            )
            FOR UPDATE SKIP LOCKED
            """;

    private static final String UPDATE_SQL = """
            UPDATE enrichment_queue
            SET status = ?, attempts = ?, last_error = ?, processed_at = ?
            WHERE id = ?
            """;

    private static final String EXISTS_SQL = """
            SELECT COUNT(1) FROM enrichment_queue WHERE memory_change_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final UuidRawConverter uuidConverter = new UuidRawConverter();

    public EnrichmentTaskRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(EnrichmentTask task) {
        if (task.status() == EnrichmentTask.Status.PENDING && task.attempts() == 0) {
            jdbcTemplate.update(INSERT_SQL, ps -> {
                ps.setBytes(1, uuidConverter.convertToDatabaseColumn(task.id()));
                ps.setBytes(2, uuidConverter.convertToDatabaseColumn(task.memoryChangeId().value()));
            });
        } else {
            jdbcTemplate.update(UPDATE_SQL, ps -> {
                ps.setString(1, task.status().name());
                ps.setInt(2, task.attempts());
                ps.setString(3, task.lastError());
                ps.setTimestamp(4, task.processedAt() != null ? Timestamp.from(task.processedAt()) : null);
                ps.setBytes(5, uuidConverter.convertToDatabaseColumn(task.id()));
            });
        }
    }

    @Override
    public void saveAll(List<EnrichmentTask> tasks) {
        if (tasks == null || tasks.isEmpty()) return;
        jdbcTemplate.batchUpdate(INSERT_SQL, tasks, tasks.size(), (ps, task) -> {
            ps.setBytes(1, uuidConverter.convertToDatabaseColumn(task.id()));
            ps.setBytes(2, uuidConverter.convertToDatabaseColumn(task.memoryChangeId().value()));
        });
    }

    @Override
    public List<EnrichmentTask> findPendingBatch(int limit) {
        return jdbcTemplate.query(FIND_PENDING_SQL,
                ps -> ps.setInt(1, limit),
                (rs, rowNum) -> {
                    UUID id = toUuid(rs.getBytes("id"));
                    UUID mcId = toUuid(rs.getBytes("memory_change_id"));
                    String status = rs.getString("status");
                    int attempts = rs.getInt("attempts");
                    String lastError = rs.getString("last_error");
                    Timestamp createdTs = rs.getTimestamp("created_at");
                    Timestamp processedTs = rs.getTimestamp("processed_at");

                    return EnrichmentTask.reconstitute(id, new MemoryChangeId(mcId),
                            EnrichmentTask.Status.valueOf(status), attempts, lastError,
                            createdTs.toInstant(),
                            processedTs != null ? processedTs.toInstant() : null);
                });
    }

    @Override
    public boolean existsByMemoryChangeId(MemoryChangeId memoryChangeId) {
        byte[] mcIdBytes = uuidConverter.convertToDatabaseColumn(memoryChangeId.value());
        Integer count = jdbcTemplate.queryForObject(EXISTS_SQL, Integer.class, (Object) mcIdBytes);
        return count != null && count > 0;
    }

    private UUID toUuid(byte[] bytes) {
        if (bytes == null) return null;
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }
}
