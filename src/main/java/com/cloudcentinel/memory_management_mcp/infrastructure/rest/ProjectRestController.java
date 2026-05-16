package com.cloudcentinel.memory_management_mcp.infrastructure.rest;

import com.cloudcentinel.memory_management_mcp.application.project.GetOrCreateProjectHandler;
import com.cloudcentinel.memory_management_mcp.domain.project.entity.Project;
import com.cloudcentinel.memory_management_mcp.domain.user.valueobject.UserId;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/projects")
public class ProjectRestController {

    private final GetOrCreateProjectHandler handler;

    public ProjectRestController(GetOrCreateProjectHandler handler) {
        this.handler = handler;
    }

    public record ProjectRequest(String name, String description, String createdBy) {}
    public record ProjectResponse(String projectId, String name, String description, boolean active) {}

    @PostMapping
    public Mono<ProjectResponse> getOrCreate(@RequestBody ProjectRequest req) {
        return Mono.fromCallable(() -> {
            Project project = handler.handle(new GetOrCreateProjectHandler.Command(
                    req.name(), req.description(), UserId.of(req.createdBy())));
            return new ProjectResponse(
                    project.id().value().toString(),
                    project.name().value(),
                    project.description(),
                    project.isActive());
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
