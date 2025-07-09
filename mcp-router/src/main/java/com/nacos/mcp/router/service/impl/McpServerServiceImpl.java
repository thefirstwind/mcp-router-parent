package com.nacos.mcp.router.service.impl;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nacos.mcp.router.model.McpServer;
import com.nacos.mcp.router.model.McpServerRegistrationRequest;
import com.nacos.mcp.router.model.McpTool;
import com.nacos.mcp.router.service.McpServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * MCP Server Service Implementation
 */
@Slf4j
@Service
public class McpServerServiceImpl implements McpServerService {

    private static final String GROUP_NAME = "MCP_SERVER_GROUP";
    private static final String TOOLS_CONFIG_GROUP = "MCP_TOOLS";

    private final NamingService namingService;
    private final ConfigService configService;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;
    private final Map<String, McpServer> connectedServers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, McpServer> registeredServers = new ConcurrentHashMap<>();

    @Autowired
    public McpServerServiceImpl(NamingService namingService, ConfigService configService, ObjectMapper objectMapper, WebClient.Builder webClientBuilder) {
        this.namingService = namingService;
        this.configService = configService;
        this.objectMapper = objectMapper;
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public Mono<McpServer> addMcpServer(String serverName) {
        log.info("Adding MCP server: {}", serverName);
        
        return Mono.fromCallable(() -> {
            try {
                // Try to get server information from Nacos
                List<Instance> instances = namingService.getAllInstances(serverName);
                if (!instances.isEmpty()) {
                    Instance instance = instances.get(0); // Use first available instance
                    McpServer server = convertInstanceToMcpServer(instance, serverName);
                    connectedServers.put(serverName, server);
                    log.info("Successfully added MCP server: {}", serverName);
                    return server;
                } else {
                    log.warn("No instances found for MCP server: {}", serverName);
                    throw new RuntimeException("MCP server not found in Nacos registry: " + serverName);
                }
            } catch (Exception e) {
                log.error("Failed to add MCP server: {}", serverName, e);
                throw new RuntimeException("Failed to add MCP server: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Object> useTool(String serverName, String toolName, Map<String, Object> params) {
        log.info("Using tool '{}' from server '{}' with params: {}", toolName, serverName, params);

        McpServer server = registeredServers.get(serverName);
        if (server == null) {
            return Mono.error(new RuntimeException("MCP server not connected: " + serverName));
        }

        WebClient webClient = webClientBuilder.baseUrl("http://" + server.getIp() + ":" + server.getPort()).build();

        return webClient.post()
                .uri("/mcp-server-v2/tools/call")
                .bodyValue(Map.of("toolName", toolName, "arguments", params))
                .retrieve()
                .bodyToMono(Object.class);
    }

    @Override
    public Mono<McpServer> getMcpServer(String serverName) {
        return Mono.fromCallable(() -> {
            try {
                Instance instance = namingService.selectOneHealthyInstance(serverName, GROUP_NAME);
                return convertToMcpServer(instance);
            } catch (Exception e) {
                log.error("Failed to get MCP server {} from Nacos", serverName, e);
                return null;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> removeMcpServer(String serverName) {
        return unregisterMcpServer(serverName);
    }

    @Override
    public Mono<McpServer> registerMcpServer(McpServerRegistrationRequest request) {
        return registerMcpServerWithTools(request);
    }

    @Override
    public Mono<McpServer> registerMcpServerWithTools(McpServerRegistrationRequest request) {
        return Mono.fromCallable(() -> {
            McpServer server = McpServer.builder()
                    .name(request.getServerName())
                    .ip(request.getIp())
                    .port(request.getPort())
                    .status(McpServer.ServerStatus.HEALTHY)
                    .lastHeartbeat(LocalDateTime.now())
                    .tools(request.getTools())
                    .build();
            registeredServers.put(server.getName(), server);
            log.info("Server {} registered with tools.", server.getName());
            return server;
        });
    }

    @Override
    public Mono<Boolean> unregisterMcpServer(String serverName) {
        return Mono.fromCallable(() -> {
            try {
                Instance instance = namingService.selectOneHealthyInstance(serverName, GROUP_NAME);
                if (instance != null) {
                    namingService.deregisterInstance(serverName, GROUP_NAME, instance);
                    log.info("Unregistered MCP server: {}", serverName);
                } else {
                    log.warn("Attempted to unregister a server that was not found or not healthy: {}", serverName);
                }
                return true;
            } catch (Exception e) {
                log.error("Failed to unregister server {}", serverName, e);
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<List<McpServer>> listAllMcpServers() {
        return Mono.just(new ArrayList<>(registeredServers.values()));
    }

    @Override
    public Mono<Boolean> updateServerHeartbeat(String serverName, Long timestamp, String status) {
        log.warn("updateServerHeartbeat is not implemented yet");
        return Mono.just(false);
    }

    public Mono<List<McpServer>> searchMcpServers(String query) {
        log.info("Searching MCP servers in Nacos with query: {}", query);
        
        return listAllMcpServers()
            .map(servers -> servers.stream()
                .filter(server -> matchesQuery(server, query))
                .collect(Collectors.toList()));
    }
    
    private boolean matchesQuery(McpServer server, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        
        String lowerQuery = query.toLowerCase();
        return server.getName().toLowerCase().contains(lowerQuery) ||
               server.getDescription().toLowerCase().contains(lowerQuery) ||
               server.getProvider().toLowerCase().contains(lowerQuery);
    }

    @Override
    public Mono<Void> recordHeartbeat(String serverName) {
        return Mono.fromRunnable(() -> {
            McpServer server = registeredServers.get(serverName);
            if (server != null) {
                server.setLastHeartbeat(LocalDateTime.now());
                server.setStatus(McpServer.ServerStatus.HEALTHY);
                log.debug("Heartbeat recorded for server: {}", serverName);
            } else {
                log.warn("Heartbeat received for unknown server: {}", serverName);
            }
        });
    }

    private McpServer convertToMcpServer(Instance instance) {
        if (instance == null) {
            return null;
        }

        Map<String, String> metadata = instance.getMetadata();
        String serverName = metadata.get("serverName");
        if (serverName == null || serverName.isEmpty()) {
            serverName = instance.getServiceName();
        }

        McpServer.McpServerBuilder serverBuilder = McpServer.builder()
                .name(serverName)
                .description(metadata.get("description"))
                .version(metadata.get("version"))
                .transportType(metadata.get("transportType"))
                .endpoint(instance.getIp() + ":" + instance.getPort())
                .status(McpServer.ServerStatus.valueOf(metadata.getOrDefault("status", "UNKNOWN")));

        try {
            String dataId = "tools." + serverName;
            String toolsJson = configService.getConfig(dataId, TOOLS_CONFIG_GROUP, 5000);
            if (toolsJson != null && !toolsJson.isEmpty()) {
                List<McpTool> tools = objectMapper.readValue(toolsJson, new TypeReference<List<McpTool>>() {});
                serverBuilder.tools(tools);
                log.info("Successfully loaded {} tools for server {}", tools.size(), serverName);
            }
        } catch (Exception e) {
            log.error("Failed to load or parse tools for server {}: {}", serverName, e.getMessage());
        }

        return serverBuilder.build();
    }

    private McpServer convertInstanceToMcpServer(Instance instance, String serviceName) {
        Map<String, String> metadata = instance.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        
        return McpServer.builder()
                .name(metadata.getOrDefault("serverName", serviceName))
                .version(metadata.getOrDefault("version", "1.0.0"))
                .description(metadata.getOrDefault("description", "MCP Server from Nacos"))
                .transportType(metadata.getOrDefault("transportType", "stdio"))
                .endpoint(String.format("http://%s:%d", instance.getIp(), instance.getPort()))
                .provider("Nacos")
                .status(instance.isEnabled() ? McpServer.ServerStatus.CONNECTED : McpServer.ServerStatus.DISCONNECTED)
                .registrationTime(LocalDateTime.now())
                .lastUpdateTime(LocalDateTime.now())
                .relevanceScore(1.0)
                .build();
    }

    @Override
    public Mono<Boolean> pingServer(String serverName) {
        log.info("Pinging MCP server: {}", serverName);
        
        return Mono.fromCallable(() -> {
            try {
                // Check if server is in connected servers first
                if (connectedServers.containsKey(serverName)) {
                    return true;
                }
                
                // Check if server exists in Nacos registry
                List<Instance> instances = namingService.getAllInstances("mcp-server");
                boolean serverExists = instances.stream()
                        .anyMatch(instance -> {
                            String name = instance.getMetadata().get("serverName");
                            return serverName.equals(name) && instance.isEnabled();
                        });
                
                if (serverExists) {
                    log.info("Server '{}' found and is enabled in Nacos", serverName);
                    return true;
                } else {
                    log.warn("Server '{}' not found or disabled in Nacos", serverName);
                    return false;
                }
                
            } catch (Exception e) {
                log.error("Failed to ping server '{}': {}", serverName, e.getMessage(), e);
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public void registerServer(McpServerRegistrationRequest registrationRequest) {
        McpServer server = McpServer.builder()
                .name(registrationRequest.getServerName())
                .ip(registrationRequest.getIp())
                .port(registrationRequest.getPort())
                .status(McpServer.ServerStatus.REGISTERED)
                .lastHeartbeat(LocalDateTime.now())
                .tools(registrationRequest.getTools())
                .description(registrationRequest.getDescription())
                .version(registrationRequest.getVersion())
                .relevanceScore(1.0)
                .build();
        registeredServers.put(server.getName(), server);
        log.info("Server {} registered in-memory with {} tools.", server.getName(), server.getTools() != null ? server.getTools().size() : 0);
    }

    @Override
    public Flux<McpServer> getRegisteredServers() {
        return Flux.fromIterable(registeredServers.values());
    }

    @Override
    public Mono<Void> deregisterMcpServer(String serverName) {
        return Mono.fromRunnable(() -> {
            registeredServers.remove(serverName);
            log.info("Server {} deregistered.", serverName);
        });
    }

    @Override
    public Mono<McpServer> getServerByName(String serverName) {
        return Mono.justOrEmpty(registeredServers.get(serverName));
    }

    @Override
    public Mono<McpServer> getNextAvailableServer() {
        return Flux.fromIterable(registeredServers.values())
                .filter(s -> s.getStatus() == McpServer.ServerStatus.HEALTHY)
                .next()
                .doOnSuccess(server -> log.info("Next available server is: {}", server != null ? server.getName() : "None"));
    }
}
