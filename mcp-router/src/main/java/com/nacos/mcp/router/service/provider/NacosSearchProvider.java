package com.nacos.mcp.router.service.provider;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.nacos.mcp.router.model.SearchRequest;
import com.nacos.mcp.router.model.McpServer;
import com.nacos.mcp.router.model.McpTool;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.alibaba.nacos.api.exception.NacosException;
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
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Nacos Search Provider - Simplified version using keyword-based search
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NacosSearchProvider implements SearchProvider {

    private final NamingService namingService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<List<McpServer>> search(SearchRequest request) {
        return Mono.<List<McpServer>>fromCallable(() -> {
            try {
                // 动态发现所有mcp-server服务
                List<String> mcpServiceNames = List.of("mcp-server-v1", "mcp-server-v2", "mcp-server-v3");
                List<McpServer> allServers = new ArrayList<>();
                
                for (String serviceName : mcpServiceNames) {
                    try {
                        List<Instance> instances = namingService.selectInstances(serviceName, true);
                        log.info("Querying Nacos for service: '{}', found {} instances.", serviceName, instances.size());
                        
                        List<McpServer> serviceServers = instances.stream()
                                .map(this::toMcpServer)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        
                        allServers.addAll(serviceServers);
                    } catch (NacosException e) {
                        log.warn("Failed to query Nacos for service '{}': {}", serviceName, e.getMessage());
                        // 继续处理其他服务，不因为一个服务失败而全部失败
                    }
                }
                
                log.info("Total discovered MCP servers: {}", allServers.size());
                return allServers;

            } catch (Exception e) {
                log.error("Unexpected error in NacosSearchProvider search execution: {}", e.getMessage(), e);
                return Collections.emptyList();
            }
        }).doOnError(e -> log.error("Error in NacosSearchProvider search execution", e));
    }

    @Override
    public String getProviderName() {
        return "Nacos";
    }

    private McpServer toMcpServer(Instance instance) {
        try {
            Map<String, String> metadata = instance.getMetadata();
            
            // Handle case where metadata is null
            if (metadata == null) {
                metadata = Collections.emptyMap();
            }
            
            String contextPath = metadata.getOrDefault("context-path", "");
            String serviceName = instance.getServiceName();
            if (serviceName.contains("@@")) {
                serviceName = serviceName.substring(serviceName.indexOf("@@") + 2);
            }
            
            return McpServer.builder()
                    .name(serviceName)
                    .description(metadata.getOrDefault("description", "Unknown"))
                    .version(metadata.getOrDefault("version", "Unknown"))
                    .provider("Nacos")
                    .transportType(metadata.getOrDefault("transportType", "stdio"))
                    .endpoint(String.format("http://%s:%d%s", instance.getIp(), instance.getPort(), contextPath))
                    .installCommand(metadata.getOrDefault("installCommand", ""))
                    .status(instance.isEnabled() ? McpServer.ServerStatus.CONNECTED : McpServer.ServerStatus.DISCONNECTED)
                    .tools(parseTools(metadata.get("tools")))
                    .metadata(metadata.isEmpty() ? Collections.emptyMap() : 
                            metadata.entrySet().stream()
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            entry -> (Object) entry.getValue())))
                    .registrationTime(LocalDateTime.now())
                    .lastUpdateTime(LocalDateTime.now())
                    .relevanceScore(1.0)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to convert instance to MCP server for instance: {}", instance, e);
            return null;
        }
    }

    private List<McpTool> parseTools(String toolsJson) {
        // This method is now effectively unused for Nacos provider due to initialization order issues.
        if (toolsJson == null || toolsJson.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // Use ObjectMapper to parse the JSON string into a list of McpTool objects
            return objectMapper.readValue(toolsJson, new TypeReference<List<McpTool>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse tools JSON: {}. JSON content: {}", e.getMessage(), toolsJson);
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