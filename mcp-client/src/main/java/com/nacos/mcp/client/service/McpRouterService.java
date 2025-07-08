package com.nacos.mcp.client.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.HashMap;

/**
 * MCP Router Service
 * 通过MCP Router访问所有已注册的MCP服务器工具
 * 
 * 这个服务负责：
 * 1. 与MCP Router建立连接
 * 2. 使用ChatClient调用MCP工具
 * 3. 提供统一的接口供Controller使用
 * 4. 在MCP工具不可用时提供AI回退机制
 */
@Service
@Slf4j
public class McpRouterService {

    private final ChatClient chatClient;

    public McpRouterService(ChatClient.Builder chatClientBuilder) {
        // ChatClient会自动通过配置的MCP连接使用可用的工具
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 获取指定国籍的人员信息
     * 使用ChatClient自动调用可用的MCP工具
     */
    public Mono<String> getPersonsByNationality(String nationality) {
        log.info("Getting persons with nationality: {}", nationality);
        
        String prompt = String.format(
            "Find all persons with %s nationality. " +
            "Use the getPersonsByNationality tool if available. " +
            "List their names, ages, and any other relevant information.",
            nationality
        );
        
        return executeWithChatClient(prompt);
    }

    /**
     * 统计指定国籍的人员数量
     */
    public Mono<String> countPersonsByNationality(String nationality) {
        log.info("Counting persons with nationality: {}", nationality);
        
        String prompt = String.format(
            "How many persons are from %s? " +
            "Use the countPersonsByNationality tool if available. " +
            "Please provide the exact count and list their names.",
            nationality
        );
        
        return executeWithChatClient(prompt);
    }

    /**
     * 获取所有人员信息
     */
    public Mono<String> getAllPersons() {
        log.info("Getting all persons");
        
        String prompt = "List all persons in the database. " +
                       "Use the getAllPersons tool if available. " +
                       "Include their details: name, age, nationality, and gender.";
        
        return executeWithChatClient(prompt);
    }

    /**
     * 根据ID获取人员信息
     */
    public Mono<String> getPersonById(Long id) {
        log.info("Getting person with ID: {}", id);
        
        String prompt = String.format(
            "Find the person with ID %d. " +
            "Use the getPersonById tool if available. " +
            "Provide their complete details.",
            id
        );
        
        return executeWithChatClient(prompt);
    }

    /**
     * 添加新人员
     */
    public Mono<String> addPerson(String firstName, String lastName, Integer age, String nationality, String gender) {
        log.info("Adding new person: {} {} from {}", firstName, lastName, nationality);
        
        String prompt = String.format(
            "Add a new person to the database with the following details: " +
            "First Name: %s, Last Name: %s, Age: %d, Nationality: %s, Gender: %s. " +
            "Use the addPerson tool if available. " +
            "Confirm the addition was successful.",
            firstName, lastName, age, nationality, gender
        );
        
        return executeWithChatClient(prompt);
    }

    /**
     * 删除人员
     */
    public Mono<String> deletePerson(Long id) {
        log.info("Deleting person with ID: {}", id);
        
        String prompt = String.format(
            "Delete the person with ID %d from the database. " +
            "Use the deletePerson tool if available. " +
            "Confirm the deletion was successful.",
            id
        );
        
        return executeWithChatClient(prompt);
    }

    /**
     * 获取系统信息（MCP Router的内置工具）
     */
    public Mono<String> getSystemInfo() {
        log.info("Getting system information from MCP Router");
        
        String prompt = "Get system information. " +
                       "Use the get_system_info tool if available. " +
                       "Show details about the MCP Router system.";
        
        return executeWithChatClient(prompt);
    }

    /**
     * 列出所有已注册的MCP服务器
     */
    public Mono<String> listMcpServers() {
        log.info("Listing all registered MCP servers");
        
        String prompt = "List all registered MCP servers. " +
                       "Use the list_servers tool if available. " +
                       "Show their status and available tools.";
        
        return executeWithChatClient(prompt);
    }

    /**
     * 处理自定义查询
     */
    public Mono<String> handleCustomQuery(String query) {
        log.info("Processing custom query: {}", query);
        
        // 增强查询，提示AI使用可用的MCP工具
        String enhancedQuery = query + 
            " (Use any available MCP tools if they can help answer this query)";
        
        return executeWithChatClient(enhancedQuery);
    }

    /**
     * 使用ChatClient执行提示
     * ChatClient会自动使用配置的MCP连接中的工具
     */
    private Mono<String> executeWithChatClient(String prompt) {
        return Mono.fromSupplier(() -> {
            try {
                String response = chatClient.prompt(prompt).call().content();
                log.info("ChatClient request completed successfully");
                return response;
            } catch (Exception e) {
                log.error("ChatClient request failed: {}", e.getMessage(), e);
                return "Sorry, I encountered an error while processing your request: " + e.getMessage();
            }
        });
    }

    /**
     * 检查MCP连接状态
     */
    public Mono<String> checkMcpConnectionStatus() {
        log.info("Checking MCP Router connection status");
        
        String prompt = "Check the status of MCP Router connection. " +
                       "Use the ping_server tool if available. " +
                       "Report on available tools and services.";
        
        return executeWithChatClient(prompt);
    }

    /**
     * 获取可用工具列表
     */
    public Mono<String> getAvailableTools() {
        log.info("Getting list of available MCP tools");
        
        String prompt = "What tools are available through the MCP Router? " +
                       "List all the tools I can use and describe what each one does.";
        
        return executeWithChatClient(prompt);
    }
} 