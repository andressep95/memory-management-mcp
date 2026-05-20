package com.cloudcentinel.memory_management_mcp.infrastructure.mcp;

import com.cloudcentinel.memory_management_mcp.domain.project.entity.Project;
import com.cloudcentinel.memory_management_mcp.domain.project.repository.ProjectRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class SetupMcpTools {

    private static final String SCAFFOLD_VERSION = "2026.05.1";

    private final ProjectRepository projectRepository;

    public SetupMcpTools(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public record FileEntry(String path, String content, boolean executable) {}
    public record SymlinkEntry(String link, String target) {}
    public record GitHookEntry(String fromScaffold, String toHook) {}
    public record ApplyStep(String action, String description) {}

    public record SetupBlueprint(
            String projectName,
            boolean alreadySetup,
            String scaffoldVersion,
            String configHash,
            List<FileEntry>    files,
            List<String>       mkdirs,
            List<SymlinkEntry> symlinks,
            List<GitHookEntry> gitHooks,
            String gitignoreAppend,
            List<ApplyStep> applySteps
    ) {}

    @Tool(description = """
            Initialize the agent environment for this repository.
            Returns a blueprint with all files, hooks, and symlinks to apply.

            IMPORTANT: This tool does NOT mark setup as completed.
            After applying the blueprint, you MUST call confirmSetup to validate and finalize.

            Flow: setupProject → apply blueprint → confirmSetup

            Ask the user for their API key before calling this tool.
            Call this once per project. Safe to re-call — returns alreadySetup=true if already confirmed.

            TRANSPORT: This server uses SSE (Server-Sent Events) at /sse.
            Clients MUST connect directly to the SSE endpoint — do NOT use mcp-remote as proxy,
            it causes hanging responses. Use native SSE support or streamable-http instead.
            """)
    public SetupBlueprint setupProject(
            @ToolParam(description = "Project API key — ask the user to provide it") String apiKey,
            @ToolParam(description = "MCP server base URL, e.g. http://localhost:8080") String serverUrl) {

        Project project = projectRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid API key. Ask the user to verify it or create a project first via POST /api/projects."));

        if (project.isSetupCompleted()) {
            return new SetupBlueprint(
                    project.name(), true, SCAFFOLD_VERSION, null,
                    List.of(), List.of(), List.of(), List.of(),
                    null, List.of());
        }

        String resolvedUrl = (serverUrl == null || serverUrl.isBlank()) ? "http://localhost:8080" : serverUrl.strip();

        List<FileEntry>    files    = new ArrayList<>();
        List<String>       mkdirs   = List.of(".agents", ".agents/scripts", ".agents/skills", ".agents/skills/commit", ".claude", ".kiro/hooks", ".kiro/steering", ".kiro/skills/commit");
        List<SymlinkEntry> symlinks = List.of(
                new SymlinkEntry("CLAUDE.md",                       ".agents/rules.md"),
                new SymlinkEntry("AGENTS.md",                       ".agents/rules.md"),
                new SymlinkEntry(".kiro/steering/project-rules.md", "../../.agents/rules.md"),
                new SymlinkEntry(".kiro/skills/commit/SKILL.md",    "../../../.agents/skills/commit/SKILL.md")
        );
        List<GitHookEntry> gitHooks = List.of(
                new GitHookEntry(".agents/scripts/post-commit", ".git/hooks/post-commit")
        );
        List<ApplyStep> applySteps = List.of(
                new ApplyStep("mkdirs", "Create all directories in mkdirs[]"),
                new ApplyStep("writeFiles", "Write all files[] with content. chmod +x where executable=true"),
                new ApplyStep("copyHooks", "Copy gitHooks[].fromScaffold → gitHooks[].toHook, chmod +x"),
                new ApplyStep("createSymlinks", "Create symlinks[] using relative paths as shown"),
                new ApplyStep("appendGitignore", "Append gitignoreAppend to .gitignore"),
                new ApplyStep("confirm", "Call confirmSetup(apiKey, configHash) — configHash is inside .agents/config.json")
        );

        // Load scaffold files first (before config, so we can hash everything)
        files.add(new FileEntry(".agents/memory.state.json",
                "{\n  \"initialized\": false,\n  \"last_indexed_commit\": null,\n  \"last_indexed_at\": null\n}\n", false));
        loadScaffoldFiles(files);

        // Compute blueprint hash from all scaffold files
        String configHash = computeBlueprintHash(files);

        // Now build config.json including the hash so the agent can read it later
        String configContent = buildConfig(apiKey, resolvedUrl, project.id().toString(), configHash);
        files.add(0, new FileEntry(".agents/config.json", configContent, false));

        return new SetupBlueprint(
                project.name(), false, SCAFFOLD_VERSION, configHash,
                files, mkdirs, symlinks, gitHooks,
                ".agents/config.json\n",
                applySteps);
    }

    private String buildConfig(String apiKey, String serverUrl, String projectId, String configHash) {
        return "{\n" +
               "  \"projectId\": \"" + projectId + "\",\n" +
               "  \"apiKey\": \"" + apiKey + "\",\n" +
               "  \"serverUrl\": \"" + serverUrl + "\",\n" +
               "  \"scaffoldVersion\": \"" + SCAFFOLD_VERSION + "\",\n" +
               "  \"configHash\": \"" + configHash + "\"\n" +
               "}\n";
    }

    /**
     * Computes a composite hash over all blueprint files (sorted by path).
     * Each file contributes: path + ":" + sha256(content).
     * The final hash is sha256 of all those lines joined by newline.
     */
    static String computeBlueprintHash(List<FileEntry> files) {
        try {
            MessageDigest fileMd = MessageDigest.getInstance("SHA-256");
            HexFormat hex = HexFormat.of();

            List<String> entries = files.stream()
                    .sorted((a, b) -> a.path().compareTo(b.path()))
                    .map(f -> {
                        try {
                            byte[] h = MessageDigest.getInstance("SHA-256")
                                    .digest(f.content().getBytes(StandardCharsets.UTF_8));
                            return f.path() + ":" + hex.formatHex(h);
                        } catch (Exception e) {
                            return f.path() + ":error";
                        }
                    })
                    .toList();

            String joined = String.join("\n", entries);
            byte[] finalHash = fileMd.digest(joined.getBytes(StandardCharsets.UTF_8));
            return hex.formatHex(finalHash);
        } catch (Exception e) {
            return "";
        }
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
