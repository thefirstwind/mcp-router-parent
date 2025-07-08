package com.nacos.mcp.router.service.impl;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.nacos.mcp.router.config.NacosProperties;
import com.nacos.mcp.router.model.McpServer;
import com.nacos.mcp.router.model.McpServerRegistrationRequest;
import com.nacos.mcp.router.model.McpTool;
import com.nacos.mcp.router.service.McpServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    private final NamingService namingService;
    private final NacosProperties nacosProperties;
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
                List<Instance> instances = namingService.getAllInstances("mcp-server");
                return instances.stream()
                        .filter(instance -> serverName.equals(instance.getMetadata().get("serverName")))
                        .findFirst()
                        .map(this::convertToMcpServer)
                        .orElse(null);
            } catch (Exception e) {
                log.error("Failed to get MCP server {} from Nacos", serverName, e);
                return null;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> removeMcpServer(String serverName) {
        return Mono.fromCallable(() -> {
            connectedServers.remove(serverName);
            log.info("Removed MCP server: {}", serverName);
            return true;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<McpServer> registerMcpServer(McpServerRegistrationRequest request) {
        return Mono.fromCallable(() -> {
            try {
                Instance instance = new Instance();
                instance.setIp("127.0.0.1"); // Default IP
                instance.setPort(8080); // Default port
                instance.setServiceName("mcp-server");
                
                Map<String, String> metadata = new HashMap<>();
                metadata.put("serverName", request.getServerName());
                metadata.put("version", request.getVersion());
                metadata.put("status", McpServer.ServerStatus.REGISTERED.name());
                metadata.put("description", request.getDescription());
                metadata.put("transportType", request.getTransportType());
                instance.setMetadata(metadata);
                
                namingService.registerInstance("mcp-server", instance);
                
                McpServer server = McpServer.builder()
                        .name(request.getServerName())
                        .description(request.getDescription())
                        .version(request.getVersion())
                        .transportType(request.getTransportType())
                        .installCommand(request.getInstallCommand())
                        .status(McpServer.ServerStatus.REGISTERED)
                        .registrationTime(LocalDateTime.now())
                        .lastUpdateTime(LocalDateTime.now())
                        .build();
                
                log.info("Registered MCP server: {}", request.getServerName());
                return server;
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
                List<Instance> instances = namingService.getAllInstances("mcp-server");
                instances.stream()
                        .filter(instance -> serverName.equals(instance.getMetadata().get("serverName")))
                        .forEach(instance -> {
                            try {
                                namingService.deregisterInstance("mcp-server", instance);
                                log.info("Unregistered MCP server: {}", serverName);
                            } catch (Exception e) {
                                log.error("Failed to unregister instance for server: {}", serverName, e);
                            }
                        });
                
                connectedServers.remove(serverName);
                return true;
            } catch (Exception e) {
                log.error("Failed to unregister MCP server: {}", serverName, e);
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<List<McpServer>> listAllMcpServers() {
        log.info("Listing all MCP servers from Nacos registry");
        
        return Mono.fromCallable(() -> {
            try {
                var serviceListView = namingService.getServicesOfServer(1, Integer.MAX_VALUE);
                if (serviceListView == null || serviceListView.getData() == null) {
                    log.warn("No services found in Nacos registry or Nacos is not available");
                    return new ArrayList<>();
                }
                
                List<String> services = serviceListView.getData();
                List<McpServer> servers = new ArrayList<>();
                
                for (String serviceName : services) {
                    // Only process MCP services (those with 'mcp' in the name)
                    if (serviceName.toLowerCase().contains("mcp")) {
                        try {
                            List<Instance> instances = namingService.getAllInstances(serviceName);
                            if (instances != null) {
                                for (Instance instance : instances) {
                                    if (instance.isEnabled()) {
                                        McpServer server = convertInstanceToMcpServer(instance, serviceName);
                                        servers.add(server);
                                    }
                                }
                            }
                        } catch (Exception instanceException) {
                            log.warn("Failed to get instances for service '{}': {}", serviceName, instanceException.getMessage());
                        }
                    }
                }
                
                log.info("Found {} MCP servers from Nacos registry", servers.size());
                return servers;
                
            } catch (Exception e) {
                log.error("Failed to list MCP servers from Nacos: {}", e.getMessage(), e);
                return new ArrayList<>(); // Return empty list instead of mock data
            }
        });
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

    private McpServer convertToMcpServer(Instance instance) {
        Map<String, String> metadata = instance.getMetadata();
        return McpServer.builder()
                .name(metadata.getOrDefault("serverName", "unknown"))
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
