package com.cloudcentinel.memory_management_mcp.application.skill;

import com.cloudcentinel.memory_management_mcp.domain.skill.entity.Skill;
import com.cloudcentinel.memory_management_mcp.domain.skill.repository.SkillRepository;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.ChunkName;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.EmbeddingVector;
import com.cloudcentinel.memory_management_mcp.domain.skill.valueobject.SkillContent;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;
import com.cloudcentinel.memory_management_mcp.infrastructure.embedding.EmbeddingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SyncSkillHandler {

    private final SkillRepository skillRepository;
    private final EmbeddingService embeddingService;

    public SyncSkillHandler(SkillRepository skillRepository, EmbeddingService embeddingService) {
        this.skillRepository  = skillRepository;
        this.embeddingService = embeddingService;
    }

    public record ChunkEntry(String name, String content, int position) {}

    public record Command(String name, String content, UserId createdBy, List<ChunkEntry> chunks) {}

    @Transactional
    public Skill handle(Command command) {
        SkillContent newContent = new SkillContent(command.content());

        Skill skill = skillRepository.findByName(command.name())
                .map(existing -> {
                    if (existing.hasContentChangedFrom(newContent)) {
                        EmbeddingVector newEmbedding = embeddingService.embed(command.content());
                        existing.updateContent(newContent, newEmbedding);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Skill created = Skill.create(command.name(), newContent, command.createdBy());
                    EmbeddingVector embedding = embeddingService.embed(command.content());
                    created.assignEmbedding(embedding);
                    return created;
                });

        for (ChunkEntry entry : command.chunks()) {
            skill.syncChunk(new ChunkName(entry.name()), new SkillContent(entry.content()), entry.position());
        }

        for (var chunk : skill.chunksPendingEmbedding()) {
            EmbeddingVector chunkEmbedding = embeddingService.embed(chunk.content().value());
            chunk.assignEmbedding(chunkEmbedding);
        }

        skillRepository.save(skill);
        return skill;
    }
}
