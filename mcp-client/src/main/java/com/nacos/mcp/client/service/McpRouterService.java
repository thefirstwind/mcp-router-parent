package com.nacos.mcp.client.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

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
    private final RestTemplate restTemplate = new RestTemplate();
    private final String mcpRouterUrl = "http://localhost:8050/api/mcp"; // MCP Router URL
    
    // @Autowired(required = false)
    // private McpDiscoveryService discoveryService;

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
        
        String prompt = "List all available MCP tools. " +
                       "Use the list_tools tool if available. " +
                       "Show tool names, descriptions, and which servers provide them.";
        
        return executeWithChatClient(prompt);
    }

    /**
     * 获取通过服务发现找到的MCP服务器列表
     */
    public Mono<String> getDiscoveredServers() {
        return Mono.fromSupplier(() -> {
            return "服务发现功能暂时禁用，请通过mcp-router手动查询 http://localhost:8050/api/mcp/servers";
        });
    }

    /**
     * 检查特定服务器是否可用
     */
    public Mono<String> checkServerAvailability(String serverName) {
        return Mono.fromSupplier(() -> {
            return "服务器可用性检查功能暂时禁用，请通过mcp-router手动查询";
        });
    }

    /**
     * 调用指定服务器上的工具（基础方法）
     */
    public Map<String, Object> callTool(String serverName, String toolName, Map<String, Object> params) {
        String url = mcpRouterUrl + "/servers/" + serverName + "/tools/" + toolName;
        log.info("调用工具: {} -> {}, 参数: {}", toolName, url, params);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, params, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.getBody();
                log.info("工具调用成功: {}", result);
                return result;
            } else {
                log.warn("工具调用失败，状态码: {}", response.getStatusCode());
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", "工具调用失败，状态码: " + response.getStatusCode());
                errorResult.put("toolName", toolName);
                errorResult.put("timestamp", System.currentTimeMillis());
                return errorResult;
            }
        } catch (Exception e) {
            log.error("工具调用异常: {}", toolName, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "工具调用异常: " + e.getMessage());
            errorResult.put("toolName", toolName);
            errorResult.put("timestamp", System.currentTimeMillis());
            return errorResult;
        }
    }

    /**
     * 检查指定工具的健康度
     */
    public Map<String, Object> checkToolHealth(String toolName) {
        String url = mcpRouterUrl + "/tools/" + toolName + "/health";
        log.info("检查工具健康度: {} -> {}", toolName, url);
        
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> healthData = (Map<String, Object>) response.getBody();
                log.info("工具 {} 健康检查结果: {}", toolName, healthData);
                return healthData;
            } else {
                log.warn("工具健康检查失败，状态码: {}", response.getStatusCode());
                return createErrorHealthStatus(toolName, "健康检查请求失败");
            }
        } catch (Exception e) {
            log.error("检查工具健康度异常: {}", toolName, e);
            return createErrorHealthStatus(toolName, "健康检查异常: " + e.getMessage());
        }
    }
    
    /**
     * 批量检查所有工具的健康度
     */
    public Map<String, Object> checkAllToolsHealth() {
        String url = mcpRouterUrl + "/tools/health/batch";
        log.info("批量检查所有工具健康度: {}", url);
        
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> batchHealthData = (Map<String, Object>) response.getBody();
                log.info("批量健康检查结果: {} 个工具, 健康度: {}%", 
                    batchHealthData.get("totalTools"), batchHealthData.get("healthPercentage"));
                return batchHealthData;
            } else {
                log.warn("批量健康检查失败，状态码: {}", response.getStatusCode());
                return createErrorBatchHealthStatus("批量健康检查请求失败");
            }
        } catch (Exception e) {
            log.error("批量检查工具健康度异常", e);
            return createErrorBatchHealthStatus("批量健康检查异常: " + e.getMessage());
        }
    }
    
    /**
     * 调用工具前检查健康度，然后执行工具调用
     */
    public Map<String, Object> callToolWithHealthCheck(String serverName, String toolName, Map<String, Object> params) {
        log.info("准备调用工具 {} (服务器: {})，先进行健康度检查", toolName, serverName);
        
        // 1. 检查工具健康度
        Map<String, Object> healthStatus = checkToolHealth(toolName);
        Boolean isHealthy = (Boolean) healthStatus.get("healthy");
        
        if (isHealthy == null || !isHealthy) {
            log.warn("工具 {} 健康检查未通过: {}", toolName, healthStatus);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "工具健康检查未通过");
            errorResult.put("toolName", toolName);
            errorResult.put("healthStatus", healthStatus);
            errorResult.put("timestamp", System.currentTimeMillis());
            return errorResult;
        }
        
        log.info("工具 {} 健康检查通过，开始执行调用", toolName);
        
        // 2. 执行工具调用
        try {
            Map<String, Object> result = callTool(serverName, toolName, params);
            result.put("healthChecked", true);
            result.put("healthStatus", healthStatus);
            return result;
        } catch (Exception e) {
            log.error("工具调用执行失败: {}", toolName, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "工具调用执行失败: " + e.getMessage());
            errorResult.put("toolName", toolName);
            errorResult.put("healthChecked", true);
            errorResult.put("healthStatus", healthStatus);
            errorResult.put("timestamp", System.currentTimeMillis());
            return errorResult;
        }
    }
    
    /**
     * 验证工具是否注册成功
     */
    public Map<String, Object> verifyToolRegistration(String toolName) {
        String url = mcpRouterUrl + "/tools/" + toolName + "/verify";
        log.info("验证工具注册状态: {} -> {}", toolName, url);
        
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> verificationData = (Map<String, Object>) response.getBody();
                log.info("工具 {} 注册验证结果: {}", toolName, verificationData);
                return verificationData;
            } else {
                log.warn("工具注册验证失败，状态码: {}", response.getStatusCode());
                return createErrorVerificationStatus(toolName, "注册验证请求失败");
            }
        } catch (Exception e) {
            log.error("验证工具注册状态异常: {}", toolName, e);
            return createErrorVerificationStatus(toolName, "注册验证异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有已注册的工具列表
     */
    public Map<String, Object> getAllRegisteredTools() {
        String url = mcpRouterUrl + "/tools";
        log.info("获取所有已注册工具列表: {}", url);
        
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> toolsData = (Map<String, Object>) response.getBody();
                log.info("获取到 {} 个已注册工具", toolsData.get("totalCount"));
                return toolsData;
            } else {
                log.warn("获取工具列表失败，状态码: {}", response.getStatusCode());
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", "获取工具列表请求失败");
                errorResult.put("timestamp", System.currentTimeMillis());
                return errorResult;
            }
        } catch (Exception e) {
            log.error("获取工具列表异常", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "获取工具列表异常: " + e.getMessage());
            errorResult.put("timestamp", System.currentTimeMillis());
            return errorResult;
        }
    }
    
    /**
     * 创建错误的健康状态响应
     */
    private Map<String, Object> createErrorHealthStatus(String toolName, String errorMessage) {
        Map<String, Object> errorStatus = new HashMap<>();
        errorStatus.put("toolName", toolName);
        errorStatus.put("status", "ERROR");
        errorStatus.put("healthy", false);
        errorStatus.put("error", errorMessage);
        errorStatus.put("timestamp", System.currentTimeMillis());
        return errorStatus;
    }
    
    /**
     * 创建错误的批量健康状态响应
     */
    private Map<String, Object> createErrorBatchHealthStatus(String errorMessage) {
        Map<String, Object> errorStatus = new HashMap<>();
        errorStatus.put("status", "ERROR");
        errorStatus.put("totalTools", 0);
        errorStatus.put("healthyTools", 0);
        errorStatus.put("unhealthyTools", 0);
        errorStatus.put("overallHealthy", false);
        errorStatus.put("healthPercentage", 0.0);
        errorStatus.put("tools", new ArrayList<>());
        errorStatus.put("error", errorMessage);
        errorStatus.put("timestamp", System.currentTimeMillis());
        return errorStatus;
    }
    
    /**
     * 创建错误的验证状态响应
     */
    private Map<String, Object> createErrorVerificationStatus(String toolName, String errorMessage) {
        Map<String, Object> errorStatus = new HashMap<>();
        errorStatus.put("toolName", toolName);
        errorStatus.put("registered", false);
        errorStatus.put("status", "ERROR");
        errorStatus.put("error", errorMessage);
        errorStatus.put("timestamp", System.currentTimeMillis());
        return errorStatus;
    }
} 