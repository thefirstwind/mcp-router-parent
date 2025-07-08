package com.nacos.mcp.router.service.provider;

import com.nacos.mcp.router.config.McpRouterProperties;
import com.nacos.mcp.router.model.SearchRequest;
import com.nacos.mcp.router.model.McpServer;
import com.nacos.mcp.router.model.McpTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Compass Search Provider Implementation
 * Searches for MCP servers using the Compass API (external service discovery)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompassSearchProvider implements SearchProvider {

    private final McpRouterProperties mcpRouterProperties;
    private final WebClient.Builder webClientBuilder;

    @Override
    public Mono<List<McpServer>> search(SearchRequest request) {
        log.info("Searching MCP servers using Compass with request: {}", request);
        
        // For now, return mock results as Compass API integration is not implemented
        return Mono.just(createMockResults(request));
        
        /* TODO: Implement actual Compass API integration
        String apiUrl = mcpRouterProperties.getCompass().getApiUrl() + "/search";
        WebClient webClient = webClientBuilder.build();
        
        return webClient.post()
                .uri(apiUrl)
                .bodyValue(createCompassRequest(request))
                .retrieve()
                .bodyToMono(CompassResponse.class)
                .map(this::convertCompassResponse)
                .onErrorResume(throwable -> {
                    log.warn("Compass search failed: {}", throwable.getMessage());
                    return Mono.just(Collections.emptyList());
                }); */
    }

    @Override
    public String getProviderName() {
        return "Compass";
    }

    private List<McpServer> createMockResults(SearchRequest request) {
        // Return mock results for testing
        return List.of(
            McpServer.builder()
                    .name("mcp-filesystem-server")
                    .description("File system operations MCP server")
                    .version("1.0.0")
                    .provider("Compass")
                    .transportType("stdio")
                    .installCommand("npm install -g @mcp/filesystem-server")
                    .status(McpServer.ServerStatus.REGISTERED)
                    .relevanceScore(0.9)
                    .registrationTime(LocalDateTime.now())
                    .lastUpdateTime(LocalDateTime.now())
                    .build(),
            McpServer.builder()
                    .name("mcp-database-server")
                    .description("Database operations MCP server")
                    .version("1.0.0")
                    .provider("Compass")
                    .transportType("sse")
                    .endpoint("http://localhost:3001/mcp")
                    .status(McpServer.ServerStatus.REGISTERED)
                    .relevanceScore(0.8)
                    .registrationTime(LocalDateTime.now())
                    .lastUpdateTime(LocalDateTime.now())
                    .build()
        );
    }

    private Map<String, Object> createCompassRequest(SearchRequest request) {
        return Map.of(
                "query", request.getTaskDescription(),
                "keywords", request.getKeywords() != null ? request.getKeywords() : Collections.emptyList(),
                "limit", request.getLimit() != null ? request.getLimit() : 10
        );
    }

    private List<McpServer> convertCompassResponse(CompassResponse response) {
        if (response == null || response.getResults() == null) {
            return Collections.emptyList();
        }

        return response.getResults().stream()
                .map(this::convertCompassResult)
                .collect(Collectors.toList());
    }

    private McpServer convertCompassResult(CompassResult result) {
        return McpServer.builder()
                .name(result.getName())
                .description(result.getDescription())
                .version(result.getVersion())
                .provider("Compass")
                .transportType(result.getTransportType())
                .endpoint(result.getEndpoint())
                .installCommand(result.getInstallCommand())
                .status(McpServer.ServerStatus.REGISTERED)
                .tools(convertTools(result.getTools()))
                .metadata(result.getMetadata())
                .registrationTime(LocalDateTime.now())
                .lastUpdateTime(LocalDateTime.now())
                .relevanceScore(result.getScore())
                .build();
    }

    private List<McpTool> convertTools(List<Map<String, Object>> tools) {
        if (tools == null) {
            return Collections.emptyList();
        }

        return tools.stream()
                .map(toolMap -> {
                    McpTool.InputSchema inputSchema = null;
                    Object schemaObj = toolMap.get("inputSchema");
                    if (schemaObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> schemaMap = (Map<String, Object>) schemaObj;
                        inputSchema = McpTool.InputSchema.builder()
                                .type((String) schemaMap.getOrDefault("type", "object"))
                                .build();
                    }
                    
                    return McpTool.builder()
                            .name((String) toolMap.get("name"))
                            .description((String) toolMap.get("description"))
                            .inputSchema(inputSchema)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // Inner classes for Compass API response
    private static class CompassResponse {
        private List<CompassResult> results;
        private int total;

        public List<CompassResult> getResults() {
            return results;
        }

        public void setResults(List<CompassResult> results) {
            this.results = results;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }

    private static class CompassResult {
        private String name;
        private String description;
        private String version;
        private String transportType;
        private String endpoint;
        private String installCommand;
        private List<Map<String, Object>> tools;
        private Map<String, Object> metadata;
        private double score;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getTransportType() {
            return transportType;
        }

        public void setTransportType(String transportType) {
            this.transportType = transportType;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getInstallCommand() {
            return installCommand;
        }

        public void setInstallCommand(String installCommand) {
            this.installCommand = installCommand;
        }

        public List<Map<String, Object>> getTools() {
            return tools;
        }

        public void setTools(List<Map<String, Object>> tools) {
            this.tools = tools;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }
    }
}