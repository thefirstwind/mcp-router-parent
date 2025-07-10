package com.nacos.mcp.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
public class McpRouterService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public McpRouterService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8050").build();
        this.objectMapper = objectMapper;
    }

    public Mono<String> listTools(String task) {
        log.info("McpRouterService#listTools: {}", task);
        
        Map<String, Object> requestBody = Map.of(
            "jsonrpc", "2.0",
            "method", "tools/list",
            "id", 1
        );
        
        return webClient.post()
                .uri("/mcp/jsonrpc")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorReturn("[]");
    }

    public Mono<String> callTool(String jsonPayload) {
        log.info("McpRouterService#callTool: {}", jsonPayload);
        try {
            JsonNode rootNode = objectMapper.readTree(jsonPayload);
            String toolName = rootNode.get("toolName").asText();
            Map<String, Object> arguments = objectMapper.convertValue(rootNode.get("arguments"), Map.class);

            Map<String, Object> requestBody = Map.of(
                "jsonrpc", "2.0",
                "method", "tools/call",
                "params", Map.of(
                    "name", toolName,
                    "arguments", arguments
                ),
                "id", 2
            );

            return webClient.post()
                    .uri("/mcp/jsonrpc")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorReturn("{\"error\": \"Tool call failed\"}");

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON payload for tool call", e);
            return Mono.just("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // The methods below are from the old implementation and are not using McpClient.
    // They are kept for now to avoid breaking the McpClientController.
    // A proper implementation would either use McpClient if applicable or a different service.

    public Mono<String> getCompletions(String model, String prompt) {
        log.warn("getCompletions is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> register(String name) {
        log.warn("register is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> unregister(String name) {
        log.warn("unregister is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> heartbeat(String name) {
        log.warn("heartbeat is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> subscribe(String name, String toolName) {
        log.warn("subscribe is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> find(String name, String toolName) {
        log.warn("find is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> list(String name, String type) {
        log.warn("list is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> unsubscribe(String name, String toolName) {
        log.warn("unsubscribe is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> call(String name, String toolName, String prompt) {
        log.warn("call is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> callWithStream(String name, String toolName, String prompt) {
        log.warn("callWithStream is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> listChanged(String name, String timestamp) {
        log.warn("listChanged is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> progress(String name, String toolName, String status) {
        log.warn("progress is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> cancel(String name, String toolName) {
        log.warn("cancel is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> listPlugins(String name) {
        log.warn("listPlugins is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> callPlugin(String name, String pluginName, String parameters) {
        log.warn("callPlugin is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> listAllServers() {
        log.warn("listAllServers is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> listToolsByServer(String serverName) {
        log.warn("listToolsByServer is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }

    public Mono<String> getServerByName(String serverName) {
        log.warn("getServerByName is not implemented with McpClient");
        return Mono.just("Not Implemented");
    }
}