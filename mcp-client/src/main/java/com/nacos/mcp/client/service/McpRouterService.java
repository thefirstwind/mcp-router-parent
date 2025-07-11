package com.nacos.mcp.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class McpRouterService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AtomicLong requestIdCounter = new AtomicLong(1);

    public McpRouterService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8050").build();
        this.objectMapper = objectMapper;
    }

    /**
     * 使用正确的MCP SSE协议获取工具列表
     */
    public Mono<String> listTools(String task) {
        log.info("McpRouterService#listTools using MCP SSE protocol: {}", task);
        
        // 构建符合MCP协议的JSON-RPC请求
        Map<String, Object> requestBody = Map.of(
            "jsonrpc", "2.0",
            "method", "tools/list",
            "id", requestIdCounter.getAndIncrement()
        );
        
        return sendMcpRequest(requestBody);
    }

    /**
     * 使用正确的MCP SSE协议调用工具
     */
    public Mono<String> callTool(String jsonPayload) {
        log.info("McpRouterService#callTool using MCP SSE protocol: {}", jsonPayload);
        try {
            JsonNode rootNode = objectMapper.readTree(jsonPayload);
            String toolName = rootNode.get("toolName").asText();
            Map<String, Object> arguments = objectMapper.convertValue(rootNode.get("arguments"), Map.class);

            // 构建符合MCP协议的JSON-RPC请求
            Map<String, Object> requestBody = Map.of(
                "jsonrpc", "2.0",
                "method", "tools/call",
                "params", Map.of(
                    "name", toolName,
                    "arguments", arguments
                ),
                "id", requestIdCounter.getAndIncrement()
            );

            return sendMcpRequest(requestBody);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON payload for tool call", e);
            return Mono.just("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 发送MCP请求，根据响应类型处理
     * 支持普通HTTP响应和SSE流响应
     */
    private Mono<String> sendMcpRequest(Map<String, Object> requestBody) {
        return webClient.post()
                .uri("/mcp/jsonrpc")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM) // 支持两种响应类型
                .bodyValue(requestBody)
                .exchangeToMono(response -> {
                    String contentType = response.headers().asHttpHeaders().getFirst("Content-Type");
                    
                    if (contentType != null && contentType.contains("text/event-stream")) {
                        // 处理SSE响应
                        log.debug("Handling SSE response for request: {}", requestBody.get("method"));
                        return handleSseResponse(response.bodyToFlux(String.class).map(data -> ServerSentEvent.builder().data(data).build()));
                    } else {
                        // 处理普通JSON响应
                        log.debug("Handling JSON response for request: {}", requestBody.get("method"));
                        return response.bodyToMono(String.class);
                    }
                })
                .doOnNext(result -> log.debug("MCP response received: {}", result))
                .onErrorReturn("{\"error\": \"MCP request failed\"}");
    }

    /**
     * 处理SSE响应流，收集所有事件并返回最终结果
     */
    private Mono<String> handleSseResponse(Flux<ServerSentEvent<Object>> sseFlux) {
        return sseFlux
                .filter(event -> event.data() != null)
                .map(event -> {
                    try {
                        return objectMapper.writeValueAsString(event.data());
                    } catch (Exception e) {
                        log.warn("Failed to serialize SSE event data: {}", e.getMessage());
                        return event.data().toString();
                    }
                })
                .reduce("", (acc, current) -> {
                    // 合并多个SSE事件，保留最后一个有效的JSON响应
                    if (current.contains("\"result\"") || current.contains("\"error\"")) {
                        return current;
                    }
                    return acc.isEmpty() ? current : acc;
                })
                .defaultIfEmpty("{\"jsonrpc\": \"2.0\", \"error\": {\"code\": -1, \"message\": \"No response received\"}}");
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