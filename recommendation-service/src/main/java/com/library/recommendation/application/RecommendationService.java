package com.library.recommendation.application;

import com.library.recommendation.infrastructure.OpenAiClient;
import com.library.recommendation.domain.RecommendationResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

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
    public RecommendationResponse recommend(UUID userId) {
        var entries = history.getHistory(userId);
        if (entries.isEmpty()) {
            log.debugf("No reading history for user %s — returning empty recommendations", userId);
            return new RecommendationResponse("", List.of());
        }

        String prompt = buildPrompt(entries);

        try {
            var request = new OpenAiClient.ChatCompletionRequest(
                    model,
                    List.of(
                            new OpenAiClient.Message("system",
                                    "Você recomenda livros para leitores brasileiros. Responda em português. " +
                                    "Sugira somente livros com edição publicada em português, usando exatamente " +
                                    "o título e a editora dessa edição. Nunca sugira um livro do histórico. " +
                                    "Use exatamente este formato, sem texto adicional:\n" +
                                    "CRITÉRIOS: breve resumo das características usadas na classificação\n" +
                                    "1. Título em português | Editora da edição em português"),
                            new OpenAiClient.Message("user", prompt)
                    ),
                    maxTokens
            );

            var response = openAiClient.complete("Bearer " + apiKey, request);

            if (response.choices() == null || response.choices().isEmpty()) {
                return new RecommendationResponse("", List.of());
            }

            String content = response.choices().get(0).message().content();
            return parseRecommendations(content, entries);

        } catch (Exception e) {
            log.errorf(e, "OpenAI call failed for user %s — returning empty list", userId);
            return new RecommendationResponse("", List.of());
        }
    }

    private String buildPrompt(List<UserReadingHistory.ReadingEntry> entries) {
        var sb = new StringBuilder("Here is my reading history:\n\n");
        for (var entry : entries) {
            sb.append("- ").append(entry.bookTitle() != null ? entry.bookTitle() : entry.bookId());
            if (entry.publisherName() != null) {
                sb.append(" | Editora: ").append(entry.publisherName());
            }
            if (entry.rating() != null) {
                sb.append(" (rated ").append(entry.rating()).append("/10)");
            }
            if (entry.review() != null && !entry.review().isBlank()) {
                sb.append(": \"").append(entry.review()).append("\"");
            }
            sb.append("\n");
        }
        sb.append("\nRecomende 5 livros que não estejam nessa lista.");
        return sb.toString();
    }

    private RecommendationResponse parseRecommendations(String content,
            List<UserReadingHistory.ReadingEntry> entries) {
        String criteriaSummary = "";
        List<RecommendationResponse.BookRecommendation> result = new ArrayList<>();
        Set<String> readTitles = entries.stream()
                .map(UserReadingHistory.ReadingEntry::bookTitle)
                .filter(title -> title != null && !title.isBlank())
                .map(this::normalizeTitle)
                .collect(Collectors.toSet());
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.toUpperCase(Locale.ROOT).startsWith("CRITÉRIOS:")) {
                criteriaSummary = trimmed.substring(trimmed.indexOf(':') + 1).trim();
                continue;
            }
            if (trimmed.matches("^\\d+\\.\\s+.+")) {
                String recommendation = trimmed.replaceFirst("^\\d+\\.\\s+", "").trim();
                String[] parts = recommendation.split("\\|", 2);
                if (parts.length == 2) {
                    String title = parts[0].trim();
                    String publisher = parts[1].trim();
                    if (!title.isBlank() && !publisher.isBlank()
                            && !readTitles.contains(normalizeTitle(title))) {
                        result.add(new RecommendationResponse.BookRecommendation(title, publisher));
                    }
                }
            }
        }
        return new RecommendationResponse(criteriaSummary, result.stream().limit(5).toList());
    }

    private String normalizeTitle(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
