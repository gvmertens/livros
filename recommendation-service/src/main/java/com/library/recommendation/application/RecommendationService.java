package com.library.recommendation.application;

import com.library.recommendation.infrastructure.OpenAiClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Generates book recommendations for a user by:
 * 1. Loading the user's accumulated reading history (ratings + reviews)
 * 2. Building a prompt summarising their taste
 * 3. Calling the OpenAI Chat Completions API
 * 4. Parsing the response into a list of book suggestions
 *
 * <p>If the user has no history, or if the OpenAI call fails, an empty list
 * is returned so the endpoint degrades gracefully.
 *
 * Requirements: 10.2, 10.4
 */
@ApplicationScoped
public class RecommendationService {

    private static final Logger log = Logger.getLogger(RecommendationService.class);

    @Inject
    UserReadingHistory history;

    @Inject
    @RestClient
    OpenAiClient openAiClient;

    @ConfigProperty(name = "openai.api-key")
    String apiKey;

    @ConfigProperty(name = "openai.model", defaultValue = "gpt-4o-mini")
    String model;

    @ConfigProperty(name = "openai.max-tokens", defaultValue = "300")
    int maxTokens;

    /**
     * Returns up to 5 book recommendations for the given user.
     * Returns an empty list if the user has no history or the LLM call fails.
     */
    public List<String> recommend(UUID userId) {
        var entries = history.getHistory(userId);
        if (entries.isEmpty()) {
            log.debugf("No reading history for user %s — returning empty recommendations", userId);
            return List.of();
        }

        String prompt = buildPrompt(entries);

        try {
            var request = new OpenAiClient.ChatCompletionRequest(
                    model,
                    List.of(
                            new OpenAiClient.Message("system",
                                    "You are a helpful book recommendation assistant. " +
                                    "Respond with a numbered list of exactly 5 book recommendations " +
                                    "based on the user's reading history. " +
                                    "Format each line as: 1. Title by Author"),
                            new OpenAiClient.Message("user", prompt)
                    ),
                    maxTokens
            );

            var response = openAiClient.complete("Bearer " + apiKey, request);

            if (response.choices() == null || response.choices().isEmpty()) {
                return List.of();
            }

            String content = response.choices().get(0).message().content();
            return parseRecommendations(content);

        } catch (Exception e) {
            log.errorf(e, "OpenAI call failed for user %s — returning empty list", userId);
            return List.of();
        }
    }

    private String buildPrompt(List<UserReadingHistory.ReadingEntry> entries) {
        var sb = new StringBuilder("Here is my reading history:\n\n");
        for (var entry : entries) {
            sb.append("- ").append(entry.bookTitle());
            if (entry.rating() != null) {
                sb.append(" (rated ").append(entry.rating()).append("/10)");
            }
            if (entry.review() != null && !entry.review().isBlank()) {
                sb.append(": \"").append(entry.review()).append("\"");
            }
            sb.append("\n");
        }
        sb.append("\nBased on this, please recommend 5 books I would enjoy.");
        return sb.toString();
    }

    private List<String> parseRecommendations(String content) {
        List<String> result = new ArrayList<>();
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            // Match lines starting with a number and period: "1. Title by Author"
            if (trimmed.matches("^\\d+\\.\\s+.+")) {
                String recommendation = trimmed.replaceFirst("^\\d+\\.\\s+", "").trim();
                if (!recommendation.isBlank()) {
                    result.add(recommendation);
                }
            }
        }
        return result;
    }
}
