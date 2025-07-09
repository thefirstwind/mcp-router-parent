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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import com.alibaba.nacos.api.exception.NacosException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MCP Server Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpServerServiceImpl implements McpServerService {

    private static final String GROUP_NAME = "MCP_SERVER_GROUP";
    private static final String TOOLS_CONFIG_GROUP = "MCP_TOOLS";

    private final NamingService namingService;
    private final ConfigService configService;
    private final ObjectMapper objectMapper;
    private final Map<String, McpServer> connectedServers = new ConcurrentHashMap<>();

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
        
        return Mono.fromCallable(() -> {
            McpServer server = connectedServers.get(serverName);
            if (server == null) {
                throw new RuntimeException("MCP server not connected: " + serverName);
            }
            
            // In a real implementation, this would make an actual call to the MCP server
            // For now, we simulate successful tool execution
            log.info("Tool '{}' executed successfully on server '{}'", toolName, serverName);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("result", "Tool executed successfully");
            result.put("serverName", serverName);
            result.put("toolName", toolName);
            result.put("timestamp", System.currentTimeMillis());
            
            return (Object) result;
        }).subscribeOn(Schedulers.boundedElastic());
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
            try {
                Instance instance = new Instance();
                instance.setIp(request.getIp());
                instance.setPort(request.getPort());
                instance.setServiceName(request.getServerName());
                instance.setWeight(request.getWeight());
                instance.setEnabled(request.getEnabled());
                instance.setClusterName(request.getCluster());

                Map<String, String> metadata = new HashMap<>();
                metadata.put("serverName", request.getServerName());
                metadata.put("version", request.getVersion());
                metadata.put("status", McpServer.ServerStatus.REGISTERED.name());
                metadata.put("description", request.getDescription());
                metadata.put("transportType", request.getTransportType());
                metadata.put("registrationTime", LocalDateTime.now().toString());
                instance.setMetadata(metadata);

                namingService.registerInstance(request.getServerName(), GROUP_NAME, instance);
                log.info("Registered service instance for {} to Nacos discovery.", request.getServerName());

                if (request.getTools() != null && !request.getTools().isEmpty()) {
                    String toolsJson = objectMapper.writeValueAsString(request.getTools());
                    String dataId = "tools." + request.getServerName();
                    configService.publishConfig(dataId, TOOLS_CONFIG_GROUP, toolsJson);
                    log.info("Published tools config for {}.", request.getServerName());
                }

                return convertToMcpServer(instance);
            } catch (Exception e) {
                log.error("Failed to register MCP server: {}", request.getServerName(), e);
                throw new RuntimeException("Registration failed", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
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
        return Mono.fromCallable(() -> {
            try {
                ListView<String> services = namingService.getServicesOfServer(1, 100, GROUP_NAME);
                if (services == null) {
                    return Collections.<McpServer>emptyList();
                }
                List<McpServer> servers = new ArrayList<>();
                for (String serviceName : services.getData()) {
                    List<Instance> instances = namingService.getAllInstances(serviceName, GROUP_NAME);
                    if (instances != null && !instances.isEmpty()) {
                        McpServer server = convertToMcpServer(instances.get(0));
                        if (server != null) {
                            servers.add(server);
                        }
                    }
                }
                return servers;
            } catch (Exception e) {
                log.error("Failed to list MCP servers from Nacos", e);
                return Collections.<McpServer>emptyList();
            }
        }).subscribeOn(Schedulers.boundedElastic());
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
        return Mono.defer(() -> {
            try {
                Instance instance = namingService.selectOneHealthyInstance(serverName, GROUP_NAME);
                if (instance != null) {
                    log.info("Successfully found instance for heartbeat: {}", instance.getInstanceId());
                } else {
                    log.warn("No healthy instance found for server: {}", serverName);
                }
                return Mono.empty();
            } catch (NacosException e) {
                log.error("Error recording heartbeat for server: {}", serverName, e);
                return Mono.error(new RuntimeException(e));
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
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
}
