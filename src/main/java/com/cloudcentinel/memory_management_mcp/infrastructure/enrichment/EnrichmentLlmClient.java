package com.cloudcentinel.memory_management_mcp.infrastructure.enrichment;

/**
 * Port for LLM-based commit enrichment. Implementations are swappable
 * (OpenAI, Claude, Ollama) via configuration.
 */
public interface EnrichmentLlmClient {

    record EnrichmentResult(String intent, String what, String why) {}

    EnrichmentResult enrich(String commitMessage, String filePath, String diff);
}
