package com.nacos.mcp.router.service.impl;

import com.nacos.mcp.router.model.McpPrompt;
import com.nacos.mcp.router.model.McpServer;
import com.nacos.mcp.router.service.McpPromptService;
import com.nacos.mcp.router.service.McpServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP Prompt Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpPromptServiceImpl implements McpPromptService {

    private final McpServerService mcpServerService;

    @Override
    public Mono<List<McpPrompt>> listPrompts(String serverName) {
        log.info("Listing prompts for MCP server: {}", serverName);
        
        return mcpServerService.getMcpServer(serverName)
                .flatMap(server -> {
                    // In a real implementation, this would query the actual MCP server
                    // For now, we return mock prompts based on server type
                    return Mono.just(createMockPrompts(server));
                })
                .onErrorResume(throwable -> {
                    log.error("Failed to list prompts for server {}: {}", serverName, throwable.getMessage());
                    return Mono.just(Collections.emptyList());
                });
    }

    @Override
    public Mono<List<McpPrompt>> listAllPrompts() {
        log.info("Listing all available prompts from all MCP servers");
        
        return mcpServerService.listAllMcpServers()
                .flatMap(servers -> {
                    List<McpPrompt> allPrompts = new ArrayList<>();
                    
                    for (McpServer server : servers) {
                        if (server.getStatus() == McpServer.ServerStatus.CONNECTED) {
                            allPrompts.addAll(createMockPrompts(server));
                        }
                    }
                    
                    return Mono.just(allPrompts);
                })
                .onErrorResume(throwable -> {
                    log.error("Failed to list all prompts: {}", throwable.getMessage());
                    return Mono.just(Collections.emptyList());
                });
    }

    @Override
    public Mono<McpPrompt> getPrompt(String serverName, String promptName, Map<String, Object> arguments) {
        log.info("Getting prompt {} from server {} with arguments: {}", promptName, serverName, arguments);
        
        return mcpServerService.getMcpServer(serverName)
                .flatMap(server -> {
                    // In a real implementation, this would get the prompt from the actual MCP server
                    McpPrompt prompt = findMockPrompt(server, promptName);
                    if (prompt != null) {
                        // Fill template with arguments if provided
                        if (arguments != null && !arguments.isEmpty()) {
                            prompt = fillPromptTemplate(prompt, arguments);
                        }
                        return Mono.just(prompt);
                    } else {
                        return Mono.error(new RuntimeException("Prompt not found: " + promptName));
                    }
                })
                .onErrorResume(throwable -> {
                    log.error("Failed to get prompt {} from server {}: {}", promptName, serverName, throwable.getMessage());
                    return Mono.error(throwable);
                });
    }

    @Override
    public Mono<List<McpPrompt.PromptMessage>> executePrompt(String serverName, String promptName, Map<String, Object> arguments) {
        log.info("Executing prompt {} from server {} with arguments: {}", promptName, serverName, arguments);
        
        return getPrompt(serverName, promptName, arguments)
                .map(McpPrompt::getMessages)
                .onErrorResume(throwable -> {
                    log.error("Failed to execute prompt {} from server {}: {}", promptName, serverName, throwable.getMessage());
                    return Mono.error(throwable);
                });
    }

    @Override
    public Mono<List<McpPrompt>> searchPrompts(String pattern, String serverName) {
        log.info("Searching prompts with pattern '{}' in server '{}'", pattern, serverName);
        
        if (serverName != null && !serverName.isEmpty()) {
            return listPrompts(serverName)
                    .map(prompts -> filterPromptsByPattern(prompts, pattern));
        } else {
            return listAllPrompts()
                    .map(prompts -> filterPromptsByPattern(prompts, pattern));
        }
    }

    @Override
    public Mono<Boolean> validatePromptArguments(String promptName, Map<String, Object> arguments) {
        log.info("Validating arguments for prompt {}: {}", promptName, arguments);
        
        // In a real implementation, this would validate against the prompt's argument schema
        // For now, we just check if required arguments are present
        return Mono.just(true); // Simplified validation
    }

    private List<McpPrompt> createMockPrompts(McpServer server) {
        List<McpPrompt> prompts = new ArrayList<>();
        String serverName = server.getName();
        
        // Create different mock prompts based on server type
        if (serverName.contains("filesystem") || serverName.contains("file")) {
            prompts.add(McpPrompt.builder()
                    .name("read_file_template")
                    .description("Template for reading file contents")
                    .arguments(List.of(
                        McpPrompt.PromptArgument.builder()
                                .name("filepath")
                                .description("Path to the file to read")
                                .required(true)
                                .build()
                    ))
                    .messages(List.of(
                        McpPrompt.PromptMessage.builder()
                                .role("user")
                                .content(McpPrompt.PromptContent.builder()
                                        .type("text")
                                        .text("Please read the file at path: {filepath}")
                                        .build())
                                .build()
                    ))
                    .build());
        }
        
        // Add basic prompts for all servers
        prompts.add(McpPrompt.builder()
                .name("help")
                .description("Get help information")
                .arguments(List.of())
                .messages(List.of(
                    McpPrompt.PromptMessage.builder()
                            .role("user")
                            .content(McpPrompt.PromptContent.builder()
                                    .type("text")
                                    .text("Please provide help information for this MCP server")
                                    .build())
                            .build()
                ))
                .build());
        
        return prompts;
    }

    private McpPrompt findMockPrompt(McpServer server, String promptName) {
        List<McpPrompt> prompts = createMockPrompts(server);
        return prompts.stream()
                .filter(prompt -> prompt.getName().equals(promptName))
                .findFirst()
                .orElse(null);
    }

    private McpPrompt fillPromptTemplate(McpPrompt prompt, Map<String, Object> arguments) {
        // Create a copy of the prompt with filled template
        List<McpPrompt.PromptMessage> filledMessages = prompt.getMessages().stream()
                .map(message -> {
                    String text = message.getContent().getText();
                    
                    // Replace template variables with actual values
                    for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                        String placeholder = "{" + entry.getKey() + "}";
                        text = text.replace(placeholder, entry.getValue().toString());
                    }
                    
                    return McpPrompt.PromptMessage.builder()
                            .role(message.getRole())
                            .content(McpPrompt.PromptContent.builder()
                                    .type(message.getContent().getType())
                                    .text(text)
                                    .annotations(message.getContent().getAnnotations())
                                    .build())
                            .build();
                })
                .collect(Collectors.toList());
        
        return McpPrompt.builder()
                .name(prompt.getName())
                .description(prompt.getDescription())
                .arguments(prompt.getArguments())
                .messages(filledMessages)
                .build();
    }

    private List<McpPrompt> filterPromptsByPattern(List<McpPrompt> prompts, String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return prompts;
        }
        
        String lowerPattern = pattern.toLowerCase();
        return prompts.stream()
                .filter(prompt -> 
                    prompt.getName().toLowerCase().contains(lowerPattern) ||
                    prompt.getDescription().toLowerCase().contains(lowerPattern))
                .collect(Collectors.toList());
    }
}