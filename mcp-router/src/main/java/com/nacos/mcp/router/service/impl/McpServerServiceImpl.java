package com.nacos.mcp.router.service.impl;

import com.nacos.mcp.router.model.McpServer;
import com.nacos.mcp.router.model.McpServerRegistrationRequest;
import com.nacos.mcp.router.service.McpServerService;
import com.nacos.mcp.router.service.provider.SearchProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class McpServerServiceImpl implements McpServerService {

    private final List<SearchProvider> searchProviders;
    private final ConcurrentHashMap<String, McpServer> registeredServers = new ConcurrentHashMap<>();
    // TODO: Add MCP client manager for SSE connections
    // private final McpClientManager mcpClientManager;

    @Autowired
    public McpServerServiceImpl(List<SearchProvider> searchProviders) {
        this.searchProviders = searchProviders;
    }

    @Override
    public Mono<Object> useTool(String serverName, String toolName, Map<String, Object> params) {
        log.info("Attempting to use tool '{}' on server '{}'", toolName, serverName);

        return getServerByName(serverName)
                .switchIfEmpty(Mono.error(new RuntimeException("Server not found in any provider: " + serverName)))
                .flatMap(server -> {
                    if (server.getEndpoint() == null || server.getEndpoint().isEmpty()) {
                        return Mono.error(new RuntimeException("Server endpoint is not defined for: " + serverName));
                    }

                    log.info("Found server '{}' at endpoint: {}", server.getName(), server.getEndpoint());

                    // TODO: Replace HTTP call with MCP SSE client connection per TODO10.md requirements
                    // This violates MCP protocol - should use SSE not HTTP
                    log.error("❌ PROTOCOL VIOLATION: Using HTTP instead of MCP SSE protocol for tool call to server '{}' tool '{}'", 
                            serverName, toolName);
                    log.error("❌ TODO10.md requirement: mcp-router连接 mcp-server拒绝使用http，改成sse");
                    
                    return Mono.error(new RuntimeException(
                            "Protocol violation: HTTP calls to MCP servers are forbidden. " +
                            "Must use MCP SSE protocol per TODO10.md requirements. " +
                            "Tool: " + toolName + ", Server: " + serverName));
                });
    }

    @Override
    public Mono<Object> useTool(String toolName, Map<String, Object> params) {
        log.info("Attempting to use tool '{}' without a specific server", toolName);
        return findServerByToolName(toolName)
                .switchIfEmpty(Mono.error(new RuntimeException("No server found providing tool: " + toolName)))
                .flatMap(server -> useTool(server.getName(), toolName, params));
    }

    private Mono<McpServer> findServerByToolName(String toolName) {
        return Flux.fromIterable(searchProviders)
                .flatMap(provider -> provider.search(null)
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorResume(e -> {
                            log.error("Error searching with provider {}: {}", provider.getProviderName(), e.getMessage());
                            return Mono.empty();
                        }))
                .flatMap(Flux::fromIterable)
                .filter(server -> server.getTools().stream().anyMatch(tool -> tool.getName().equalsIgnoreCase(toolName)))
                .next();
    }

    @Override
    public Mono<McpServer> getServerByName(String serverName) {
        return Flux.fromIterable(searchProviders)
                .flatMap(provider -> provider.search(null)
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorResume(e -> {
                            log.error("Error searching with provider {}: {}", provider.getProviderName(), e.getMessage());
                            return Mono.empty();
                        }))
                .flatMap(Flux::fromIterable)
                .filter(server -> serverName.equalsIgnoreCase(server.getName()))
                .next();
    }
    
    // Implement other methods from the interface with placeholder logic
    
    @Override
    public Mono<McpServer> addMcpServer(String serverName) {
        log.warn("addMcpServer is not fully implemented.");
        return Mono.empty();
    }

    @Override
    public Mono<McpServer> getMcpServer(String serverName) {
        return getServerByName(serverName);
    }

    @Override
    public Mono<Boolean> removeMcpServer(String serverName) {
        log.warn("removeMcpServer is not fully implemented.");
        return Mono.just(false);
    }
    
    @Override
    public Mono<McpServer> registerMcpServer(McpServerRegistrationRequest request) {
        log.warn("registerMcpServer is not fully implemented.");
        return Mono.empty();
    }

    @Override
    public Mono<McpServer> registerMcpServerWithTools(McpServerRegistrationRequest request) {
        return registerMcpServer(request);
    }
    
    @Override
    public Mono<Boolean> unregisterMcpServer(String serverName) {
        log.warn("unregisterMcpServer is not fully implemented.");
        return Mono.just(false);
    }

    @Override
    public Mono<List<McpServer>> listAllMcpServers() {
        return Flux.fromIterable(searchProviders)
                .flatMap(provider -> provider.search(null).subscribeOn(Schedulers.boundedElastic()))
                .flatMap(Flux::fromIterable)
                .collectList();
    }

    @Override
    public Mono<Boolean> pingServer(String serverName) {
        log.warn("pingServer is not fully implemented.");
        return Mono.just(false);
    }
    
    @Override
    public Mono<Boolean> updateServerHeartbeat(String serverName, Long timestamp, String status) {
        log.warn("updateServerHeartbeat is not implemented.");
        return Mono.just(false);
    }

    @Override
    public Mono<List<McpServer>> searchMcpServers(String query) {
        return listAllMcpServers()
                .map(servers -> servers.stream()
                        .filter(server -> server.getName().toLowerCase().contains(query.toLowerCase()))
                        .collect(Collectors.toList()));
    }
    
    @Override
    public Mono<Void> recordHeartbeat(String serverName) {
        log.warn("recordHeartbeat is not implemented.");
        return Mono.empty();
    }
    
    @Override
    public void registerServer(McpServerRegistrationRequest registrationRequest) {
        log.warn("registerServer is not fully implemented.");
    }
    
    @Override
    public Flux<McpServer> getRegisteredServers() {
        log.warn("getRegisteredServers is not fully implemented.");
        return Flux.empty();
    }
    
    @Override
    public Mono<Void> deregisterMcpServer(String serverName) {
        log.warn("deregisterMcpServer is not fully implemented.");
        return Mono.empty();
    }
    
    @Override
    public Mono<McpServer> getNextAvailableServer() {
        log.warn("getNextAvailableServer is not fully implemented.");
        return Mono.empty();
    }


} 