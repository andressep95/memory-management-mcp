package com.cloudcentinel.memory_management_mcp.infrastructure.mcp;

import com.cloudcentinel.memory_management_mcp.domain.project.entity.Project;
import com.cloudcentinel.memory_management_mcp.domain.project.repository.ProjectRepository;
import com.cloudcentinel.memory_management_mcp.infrastructure.mcp.SetupMcpTools.FileEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class SetupValidationMcpTools {

    private static final Logger log = LoggerFactory.getLogger(SetupValidationMcpTools.class);

    private final ProjectRepository projectRepository;

    public SetupValidationMcpTools(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public record Failure(String file, String reason) {}

    public record SetupValidation(
            boolean valid,
            String setupCompletedAt,
            List<Failure> failures
    ) {}

    @Transactional
    @Tool(description = """
            Confirms that the agent successfully applied the setup blueprint.
            Call this AFTER applying all files, hooks, and symlinks from setupProject.

            The agent only needs to pass the configHash (found in .agents/config.json after writing the blueprint).
            The server recalculates the expected hash from scaffold sources.
            If the hash matches, setup is marked complete.
            If it doesn't match, the response includes per-file diagnostics showing which files differ.
            """)
    public SetupValidation confirmSetup(
            @ToolParam(description = "Project API key") String apiKey,
            @ToolParam(description = "SHA-256 hash of .agents/config.json content, as returned by setupProject in configHash field") String configHash) {

        log.info("[confirmSetup] Called with configHash={}", configHash);

        Project project = projectRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("Invalid API key."));

        log.info("[confirmSetup] Project found: {}, setupCompleted={}", project.name(), project.isSetupCompleted());

        if (project.isSetupCompleted()) {
            return new SetupValidation(true, project.setupCompletedAt().toString(), List.of());
        }

        // Rebuild the same file list that setupProject would produce (excluding config.json)
        List<FileEntry> expectedFiles = new ArrayList<>();
        expectedFiles.add(new FileEntry(".agents/memory.state.json",
                "{\n  \"initialized\": false,\n  \"last_indexed_commit\": null,\n  \"last_indexed_at\": null\n}\n", false));
        loadScaffoldFiles(expectedFiles);

        log.info("[confirmSetup] Loaded {} scaffold files for hash computation", expectedFiles.size());

        String expectedHash = SetupMcpTools.computeBlueprintHash(expectedFiles);

        log.info("[confirmSetup] expectedHash={}, receivedHash={}, match={}", expectedHash, configHash, expectedHash.equals(configHash));

        if (expectedHash.equals(configHash)) {
            project.markSetupCompleted();
            projectRepository.update(project);
            log.info("[confirmSetup] Setup marked complete for project {}", project.name());
            return new SetupValidation(true, project.setupCompletedAt().toString(), List.of());
        }

        // Hash mismatch — provide per-file diagnostics
        List<Failure> failures = new ArrayList<>();
        failures.add(new Failure("_overall",
                "Blueprint hash mismatch. Expected: " + expectedHash + ", received: " + configHash));

        // Compute individual file hashes for diagnostics
        HexFormat hex = HexFormat.of();
        for (FileEntry f : expectedFiles) {
            try {
                byte[] h = MessageDigest.getInstance("SHA-256")
                        .digest(f.content().getBytes(StandardCharsets.UTF_8));
                failures.add(new Failure(f.path(), "expected sha256:" + hex.formatHex(h)));
            } catch (Exception e) {
                failures.add(new Failure(f.path(), "could not compute hash"));
            }
        }

        return new SetupValidation(false, null, failures);
    }

    private void loadScaffoldFiles(List<FileEntry> files) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        String[] patterns = {
                "classpath:scaffold/rules.md",
                "classpath:scaffold/skills/commit/SKILL.md",
                "classpath:scaffold/.claude/settings.json",
                "classpath:scaffold/.kiro/hooks/*.yaml",
                "classpath:scaffold/scripts/*"
        };
        String[] targetPaths = {
                ".agents/rules.md",
                ".agents/skills/commit/SKILL.md",
                ".claude/settings.json",
                null,
                null
        };

        for (int i = 0; i < patterns.length; i++) {
            try {
                Resource[] resources = resolver.getResources(patterns[i]);
                for (Resource res : resources) {
                    String filename = res.getFilename();
                    if (filename == null) continue;
                    String content = readResource(res);
                    String targetPath = targetPaths[i] != null ? targetPaths[i]
                            : resolveTargetPath(patterns[i], filename);
                    boolean executable = filename.endsWith(".sh") || filename.equals("post-commit");
                    files.add(new FileEntry(targetPath, content, executable));
                }
            } catch (Exception e) {
                // skip missing optional resources
            }
        }
    }

    private String resolveTargetPath(String pattern, String filename) {
        if (pattern.contains(".kiro/hooks")) return ".kiro/hooks/" + filename;
        if (pattern.contains("scripts"))    return ".agents/scripts/" + filename;
        return filename;
    }

    private String readResource(Resource resource) {
        try (var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (Exception e) {
            return "";
        }
    }
}
