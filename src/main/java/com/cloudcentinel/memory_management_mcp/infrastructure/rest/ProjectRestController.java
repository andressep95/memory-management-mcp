package com.cloudcentinel.memory_management_mcp.infrastructure.rest;

import com.cloudcentinel.memory_management_mcp.application.project.CreateProjectHandler;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/projects")
public class ProjectRestController {

    private final CreateProjectHandler createHandler;

    public ProjectRestController(CreateProjectHandler createHandler) {
        this.createHandler = createHandler;
    }

    public record CreateRequest(String name) {}
    public record CreateResponse(String projectId, String apiKey, String name) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CreateResponse> create(@RequestBody CreateRequest req) {
        return Mono.fromCallable(() -> {
            CreateProjectHandler.Result r = createHandler.handle(
                    new CreateProjectHandler.Command(req.name()));
            return new CreateResponse(r.projectId(), r.apiKey(), r.name());
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
