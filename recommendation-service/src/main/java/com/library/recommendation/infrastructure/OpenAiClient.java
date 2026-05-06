package com.library.recommendation.infrastructure;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * Typed REST client for the OpenAI Chat Completions API.
 * Base URL is configured via {@code quarkus.rest-client.openai.url} in application.properties.
 *
 * Requirements: 10.2
 */
@RegisterRestClient(configKey = "openai")
@Path("/v1/chat/completions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface OpenAiClient {

    @POST
    ChatCompletionResponse complete(
            @HeaderParam("Authorization") String bearerToken,
            ChatCompletionRequest request);

    // ── Request / Response records ────────────────────────────────────────────

    record ChatCompletionRequest(String model, List<Message> messages, int max_tokens) {}

    record Message(String role, String content) {}

    record ChatCompletionResponse(List<Choice> choices) {}

    record Choice(Message message) {}
}
