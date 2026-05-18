package com.cloudcentinel.memory_management_mcp.application.memory;

import java.util.Set;

class KindClassifier {

    private static final Set<String> CODE_EXT = Set.of(
        ".java", ".py", ".js", ".ts", ".jsx", ".tsx", ".go", ".rs",
        ".kt", ".kts", ".cs", ".cpp", ".c", ".h", ".swift", ".rb",
        ".php", ".scala", ".clj", ".cljs", ".sql", ".sh", ".bash",
        ".zsh", ".groovy", ".dart", ".ex", ".exs", ".hs", ".lua",
        ".pl", ".r", ".jl", ".zig"
    );

    private static final Set<String> DOC_EXT = Set.of(
        ".md", ".mdx", ".rst", ".adoc", ".txt"
    );

    private static final Set<String> DOC_PATH_SIGNALS = Set.of(
        "openapi", "swagger", "api-spec", "docs/", "doc/", "spec/"
    );

    static String classify(String filePath) {
        if (filePath == null || filePath.isBlank()) return "config";
        String lower = filePath.toLowerCase();
        int dot = lower.lastIndexOf('.');
        String ext = dot >= 0 ? lower.substring(dot) : "";

        if (CODE_EXT.contains(ext)) return "code";
        if (DOC_EXT.contains(ext))  return "doc";

        if (Set.of(".yaml", ".yml", ".json", ".toml", ".xml").contains(ext)) {
            for (String signal : DOC_PATH_SIGNALS) {
                if (lower.contains(signal)) return "doc";
            }
        }
        return "config";
    }
}
