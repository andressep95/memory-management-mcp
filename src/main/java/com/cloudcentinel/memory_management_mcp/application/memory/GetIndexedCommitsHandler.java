package com.cloudcentinel.memory_management_mcp.application.memory;

import com.cloudcentinel.memory_management_mcp.domain.memory.repository.MemoryChangeRepository;
import com.cloudcentinel.memory_management_mcp.domain.project.valueobject.ProjectId;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class GetIndexedCommitsHandler {

    private final MemoryChangeRepository repository;

    public GetIndexedCommitsHandler(MemoryChangeRepository repository) {
        this.repository = repository;
    }

    public Set<String> handle(ProjectId projectId) {
        return repository.findIndexedCommitHashes(projectId);
    }
}
