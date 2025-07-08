package com.nacos.mcp.router.service.impl;

import com.nacos.mcp.router.model.McpResource;
import com.nacos.mcp.router.model.McpServer;
import com.nacos.mcp.router.service.McpResourceService;
import com.nacos.mcp.router.service.McpServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP Resource Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpResourceServiceImpl implements McpResourceService {

    private final McpServerService mcpServerService;

    @Override
    public Mono<List<McpResource>> listResources(String serverName) {
        log.info("Listing resources for MCP server: {}", serverName);
        
        return mcpServerService.getMcpServer(serverName)
                .flatMap(server -> {
                    // In a real implementation, this would query the actual MCP server
                    // For now, we return mock resources based on server type
                    return Mono.just(createMockResources(server));
                })
                .onErrorResume(throwable -> {
                    log.error("Failed to list resources for server {}: {}", serverName, throwable.getMessage());
                    return Mono.just(Collections.emptyList());
                });
    }

    @Override
    public Mono<List<McpResource>> listAllResources() {
        log.info("Listing all available resources from all MCP servers");
        
        return mcpServerService.listAllMcpServers()
                .flatMap(servers -> {
                    List<McpResource> allResources = new ArrayList<>();
                    
                    for (McpServer server : servers) {
                        if (server.getStatus() == McpServer.ServerStatus.CONNECTED) {
                            allResources.addAll(createMockResources(server));
                        }
                    }
                    
                    return Mono.just(allResources);
                })
                .onErrorResume(throwable -> {
                    log.error("Failed to list all resources: {}", throwable.getMessage());
                    return Mono.just(Collections.emptyList());
                });
    }

    @Override
    public Mono<McpResource> readResource(String serverName, String resourceUri) {
        log.info("Reading resource {} from server {}", resourceUri, serverName);
        
        return mcpServerService.getMcpServer(serverName)
                .flatMap(server -> {
                    // In a real implementation, this would read from the actual MCP server
                    return Mono.just(readMockResource(server, resourceUri));
                })
                .onErrorResume(throwable -> {
                    log.error("Failed to read resource {} from server {}: {}", resourceUri, serverName, throwable.getMessage());
                    return Mono.error(new RuntimeException("Resource not found: " + resourceUri));
                });
    }

    @Override
    public Mono<McpResource> subscribeToResource(String serverName, String resourceUri) {
        log.info("Subscribing to resource {} from server {}", resourceUri, serverName);
        
        // In a real implementation, this would establish a subscription to the MCP server
        // For now, we just return the current resource content
        return readResource(serverName, resourceUri);
    }

    @Override
    public Mono<List<McpResource>> searchResources(String pattern, String serverName) {
        log.info("Searching resources with pattern '{}' in server '{}'", pattern, serverName);
        
        if (serverName != null && !serverName.isEmpty()) {
            return listResources(serverName)
                    .map(resources -> filterResourcesByPattern(resources, pattern));
        } else {
            return listAllResources()
                    .map(resources -> filterResourcesByPattern(resources, pattern));
        }
    }

    private List<McpResource> createMockResources(McpServer server) {
        List<McpResource> resources = new ArrayList<>();
        String serverName = server.getName();
        
        // Create different mock resources based on server type
        if (serverName.contains("filesystem") || serverName.contains("file")) {
            resources.add(McpResource.builder()
                    .uri("file:///config/application.yml")
                    .name("Application Configuration")
                    .description("Main application configuration file")
                    .mimeType("application/yaml")
                    .contents("spring:\n  application:\n    name: " + serverName)
                    .build());
                    
            resources.add(McpResource.builder()
                    .uri("file:///logs/application.log")
                    .name("Application Log")
                    .description("Current application log file")
                    .mimeType("text/plain")
                    .contents("2024-01-01 00:00:00 [INFO] Application started")
                    .build());
        }
        
        if (serverName.contains("database") || serverName.contains("db")) {
            resources.add(McpResource.builder()
                    .uri("db://schema/users")
                    .name("Users Table Schema")
                    .description("Database schema for users table")
                    .mimeType("application/sql")
                    .contents("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT, email TEXT);")
                    .build());
        }
        
        if (serverName.contains("git")) {
            resources.add(McpResource.builder()
                    .uri("git://repository/readme")
                    .name("Repository README")
                    .description("Project README file")
                    .mimeType("text/markdown")
                    .contents("# " + serverName + "\n\nThis is a Git repository resource.")
                    .build());
        }
        
        return resources;
    }

    private McpResource readMockResource(McpServer server, String resourceUri) {
        List<McpResource> resources = createMockResources(server);
        
        return resources.stream()
                .filter(resource -> resource.getUri().equals(resourceUri))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Resource not found: " + resourceUri));
    }

    private List<McpResource> filterResourcesByPattern(List<McpResource> resources, String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return resources;
        }
        
        String lowerPattern = pattern.toLowerCase();
        return resources.stream()
                .filter(resource -> 
                    resource.getName().toLowerCase().contains(lowerPattern) ||
                    resource.getDescription().toLowerCase().contains(lowerPattern) ||
                    resource.getUri().toLowerCase().contains(lowerPattern))
                .collect(Collectors.toList());
    }
} 
 