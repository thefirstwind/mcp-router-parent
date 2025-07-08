package com.nacos.mcp.router.service.provider;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.nacos.mcp.router.model.SearchRequest;
import com.nacos.mcp.router.model.McpServer;
import com.nacos.mcp.router.model.McpTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Nacos Search Provider - Simplified version using keyword-based search
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NacosSearchProvider implements SearchProvider {

    private final NamingService namingService;

    @Override
    public Mono<List<McpServer>> search(SearchRequest request) {
        return Mono.fromCallable(() -> {
            try {
                // Get all MCP services from Nacos
                List<String> services = namingService.getServicesOfServer(1, Integer.MAX_VALUE).getData();
                List<McpServer> mcpServers = new ArrayList<>();

                for (String serviceName : services) {
                    // Only process MCP services (starting with "mcp-")
                    if (serviceName.startsWith("mcp-")) {
                        List<Instance> instances = namingService.getAllInstances(serviceName);
                        for (Instance instance : instances) {
                            // Only include enabled instances
                            if (instance.isEnabled()) {
                                McpServer server = convertInstanceToMcpServer(instance, serviceName);
                                if (server != null) {
                                    mcpServers.add(server);
                                }
                            }
                        }
                    }
                }

                // Calculate relevance scores using keyword matching
                return calculateKeywordBasedScores(mcpServers, request);
            } catch (Exception e) {
                log.error("Failed to search MCP servers from Nacos: {}", e.getMessage());
                return Collections.emptyList(); // Return empty list instead of throwing exception
            }
        });
    }

    @Override
    public String getProviderName() {
        return "Nacos";
    }

    private McpServer convertInstanceToMcpServer(Instance instance, String serviceName) {
        try {
            Map<String, String> metadata = instance.getMetadata();
            
            // Handle case where metadata is null
            if (metadata == null) {
                metadata = Collections.emptyMap();
            }
            
            return McpServer.builder()
                    .name(serviceName)
                    .description(metadata.getOrDefault("description", "Unknown"))
                    .version(metadata.getOrDefault("version", "Unknown"))
                    .provider("Nacos")
                    .transportType(metadata.getOrDefault("transportType", "stdio"))
                    .endpoint(String.format("http://%s:%d", instance.getIp(), instance.getPort()))
                    .installCommand(metadata.getOrDefault("installCommand", ""))
                    .status(instance.isEnabled() ? McpServer.ServerStatus.CONNECTED : McpServer.ServerStatus.DISCONNECTED)
                    .tools(parseTools(metadata.getOrDefault("tools", "")))
                    .metadata(metadata.isEmpty() ? Collections.emptyMap() : 
                            metadata.entrySet().stream()
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            entry -> (Object) entry.getValue())))
                    .registrationTime(LocalDateTime.now())
                    .lastUpdateTime(LocalDateTime.now())
                    .relevanceScore(0.0)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to convert instance to MCP server: {}", e.getMessage());
            return null;
        }
    }

    private List<McpTool> parseTools(String toolsJson) {
        // Simple parsing - in real implementation, you might want to use JSON parsing
        if (toolsJson == null || toolsJson.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // This is a simplified implementation
            // In real scenario, you would parse JSON to extract tool information
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to parse tools JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<McpServer> calculateKeywordBasedScores(List<McpServer> servers, SearchRequest request) {
        if (servers.isEmpty()) {
            return servers;
        }

        String searchQuery = request.getTaskDescription().toLowerCase();
        List<String> keywords = request.getKeywords();
        
        for (McpServer server : servers) {
            double score = calculateKeywordScore(server, searchQuery, keywords);
            server.setRelevanceScore(score);
        }

        // Filter servers by minimum similarity and limit results
        return servers.stream()
                .filter(server -> server.getRelevanceScore() >= (request.getMinSimilarity() != null ? request.getMinSimilarity() : 0.0))
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                .limit(request.getLimit() != null ? request.getLimit() : 10)
                .collect(Collectors.toList());
    }

    private double calculateKeywordScore(McpServer server, String searchQuery, List<String> keywords) {
        double score = 0.0;
        
        String serverText = (server.getName() + " " + server.getDescription()).toLowerCase();
        
        // Check if search query appears in server name or description
        if (serverText.contains(searchQuery)) {
            score += 1.0;
        }
        
        // Check for keyword matches
        if (keywords != null) {
            for (String keyword : keywords) {
                if (serverText.contains(keyword.toLowerCase())) {
                    score += 0.5;
                }
            }
        }
        
        // Boost score for exact name matches
        if (server.getName().toLowerCase().contains(searchQuery)) {
            score += 0.5;
        }
        
        return score;
    }
} 