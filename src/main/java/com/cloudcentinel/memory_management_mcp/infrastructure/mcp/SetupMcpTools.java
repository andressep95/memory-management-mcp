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
import java.util.ArrayList;
import java.util.List;

@Component
public class SetupMcpTools {

    private final ProjectRepository projectRepository;

    public SetupMcpTools(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public record FileEntry(String path, String content, boolean executable) {}
    public record SymlinkEntry(String link, String target) {}
    public record GitHookEntry(String fromScaffold, String toHook) {}

    public record SetupBlueprint(
            String projectName,
            boolean alreadySetup,
            List<FileEntry>   files,
            List<String>      mkdirs,
            List<SymlinkEntry> symlinks,
            List<GitHookEntry> gitHooks,
            String gitignoreAppend,
            String nextSteps
    ) {}

    @Tool(description = """
            Initialize the agent environment for this repository.
            Creates .agents/config.json, scripts, hooks, and rules.md.
            Returns a blueprint with all files and instructions for the agent to apply.

            Ask the user for their API key before calling this tool.
            Ask the user for the MCP server URL (default: http://localhost:8080) if not local.

            Call this once per project. Calling again is safe — returns alreadySetup=true if already configured.
            """)
    public SetupBlueprint setupProject(
            @ToolParam(description = "Project API key — ask the user to provide it") String apiKey,
            @ToolParam(description = "MCP server base URL, e.g. http://localhost:8080") String serverUrl) {

        Project project = projectRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid API key. Ask the user to verify it or create a project first via POST /api/projects."));

        if (project.isSetupCompleted()) {
            return new SetupBlueprint(
                    project.name(), true,
                    List.of(), List.of(), List.of(), List.of(),
                    null,
                    "Project '" + project.name() + "' is already set up. " +
                    "To force re-setup, delete .agents/config.json and call this tool again.");
        }

        String resolvedUrl = (serverUrl == null || serverUrl.isBlank()) ? "http://localhost:8080" : serverUrl.strip();

        List<FileEntry>    files    = new ArrayList<>();
        List<String>       mkdirs   = List.of(".agents", ".agents/scripts", ".claude", ".kiro/hooks", ".kiro/steering");
        List<SymlinkEntry> symlinks = List.of(
                new SymlinkEntry("CLAUDE.md",                       ".agents/rules.md"),
                new SymlinkEntry("AGENTS.md",                       ".agents/rules.md"),
                new SymlinkEntry(".kiro/steering/project-rules.md", "../../.agents/rules.md")
        );
        List<GitHookEntry> gitHooks = List.of(
                new GitHookEntry(".agents/scripts/post-commit", ".git/hooks/post-commit")
        );

        files.add(new FileEntry(".agents/config.json",
                buildConfig(apiKey, resolvedUrl), false));

        files.add(new FileEntry(".agents/memory.state.json",
                "{\n  \"initialized\": false,\n  \"last_indexed_commit\": null,\n  \"last_indexed_at\": null\n}\n", false));

        loadScaffoldFiles(files, resolvedUrl, apiKey);

        project.markSetupCompleted();
        projectRepository.update(project);

        return new SetupBlueprint(
                project.name(), false,
                files, mkdirs, symlinks, gitHooks,
                ".agents/config.json\n",
                """
                Apply the blueprint in this order:
                1. Create all directories in mkdirs[].
                2. Create all files[] with their content. Mark executable=true files with chmod +x.
                3. Copy gitHooks[].fromScaffold → gitHooks[].toHook and chmod +x.
                4. Create symlinks[]. Use relative paths as shown.
                5. Append gitignoreAppend to .gitignore (create if missing).
                6. Tell the user: "Setup complete. Please restart your session to activate the hooks."
                7. After restart, session-start.sh will automatically detect that memory is uninitialized and bootstrap it in the background. No manual action required.
                """);
    }

    private String buildConfig(String apiKey, String serverUrl) {
        return "{\n" +
               "  \"apiKey\": \"" + apiKey + "\",\n" +
               "  \"serverUrl\": \"" + serverUrl + "\"\n" +
               "}\n";
    }

    private void loadScaffoldFiles(List<FileEntry> files, String serverUrl, String apiKey) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        String[] patterns = {
                "classpath:scaffold/rules.md",
                "classpath:scaffold/.claude/settings.json",
                "classpath:scaffold/.kiro/hooks/*.yaml",
                "classpath:scaffold/scripts/*"
        };
        String[] targetPaths = {
                ".agents/rules.md",
                ".claude/settings.json",
                null,  // kiro hooks — resolved below
                null   // scripts — resolved below
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
