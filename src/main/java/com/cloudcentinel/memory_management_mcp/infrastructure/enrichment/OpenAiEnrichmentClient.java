package com.cloudcentinel.memory_management_mcp.infrastructure.enrichment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "enrichment.llm.provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiEnrichmentClient implements EnrichmentLlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEnrichmentClient.class);

    private static final String SYSTEM_PROMPT = """
            You are a git commit analyzer. Given the commit message, file path, and diff,
            infer structured semantic fields. Respond ONLY with valid JSON, no markdown fences.
            {
              "intent": "feat|fix|refactor|perf|docs|test|chore|ci|style|sec",
              "what": "<one sentence: what the code does now that it didn't before>",
              "why": "<one sentence: why this change was necessary>"
            }""";

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public OpenAiEnrichmentClient(
            @Value("${enrichment.llm.openai.api-key:}") String apiKey,
            @Value("${enrichment.llm.openai.model:gpt-4o-mini}") String model,
            @Value("${enrichment.llm.openai.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public EnrichmentResult enrich(String commitMessage, String filePath, String diff) {
        String truncatedDiff = diff != null && diff.length() > 2000 ? diff.substring(0, 2000) : diff;
        String userPrompt = "Commit message: %s\nFile: %s\nDiff:\n%s".formatted(
                commitMessage, filePath, truncatedDiff != null ? truncatedDiff : "(no diff)");

        try {
            String requestBody = mapper.writeValueAsString(new ChatRequest(model,
                    new Message[]{
                            new Message("system", SYSTEM_PROMPT),
                            new Message("user", userPrompt)
                    }, 0.1));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("OpenAI API returned " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            String content = root.at("/choices/0/message/content").asText();
            JsonNode parsed = mapper.readTree(content);

            return new EnrichmentResult(
                    parsed.path("intent").asText(null),
                    parsed.path("what").asText(null),
                    parsed.path("why").asText(null)
            );
        } catch (Exception e) {
            log.error("OpenAI enrichment failed for {}: {}", filePath, e.getMessage());
            throw new RuntimeException("Enrichment failed: " + e.getMessage(), e);
        }
    }

    private record ChatRequest(String model, Message[] messages, double temperature) {}
    private record Message(String role, String content) {}
}
