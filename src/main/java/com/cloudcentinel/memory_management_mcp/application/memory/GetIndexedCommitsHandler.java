package com.cloudcentinel.memory_management_mcp.application.memory;

import com.cloudcentinel.memory_management_mcp.domain.memory.repository.MemoryChangeRepository;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class GetIndexedCommitsHandler {

    private final MemoryChangeRepository repository;

    public GetIndexedCommitsHandler(MemoryChangeRepository repository) {
        this.repository = repository;
    }

    public Set<String> handle(String projectId) {
        return repository.findIndexedCommitHashes(projectId);
    }
}
