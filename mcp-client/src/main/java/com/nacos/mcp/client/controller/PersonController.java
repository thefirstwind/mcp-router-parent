package com.nacos.mcp.client.controller;

import com.nacos.mcp.client.service.McpRouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Person Controller
 * 通过MCP Router访问person相关的工具和资源
 * 
 * 这个控制器现在：
 * 1. 通过MCP Router获取整合的工具列表
 * 2. 调用person-mcp-server的工具（通过MCP Router路由）
 * 3. 支持AI增强的查询处理
 * 4. 提供统一的REST API接口
 */
@RestController
@RequestMapping("/persons")
@RequiredArgsConstructor
@Slf4j
public class PersonController {

    private final McpRouterService mcpRouterService;

    @GetMapping("/nationality/{nationality}")
    public Mono<String> findByNationality(@PathVariable String nationality) {
        log.info("Finding persons with nationality: {} (via MCP Router)", nationality);
        return mcpRouterService.getPersonsByNationality(nationality);
    }

    @GetMapping("/count-by-nationality/{nationality}")
    public Mono<String> countByNationality(@PathVariable String nationality) {
        log.info("Counting persons with nationality: {} (via MCP Router)", nationality);
        return mcpRouterService.countPersonsByNationality(nationality);
    }

    @GetMapping("/all")
    public Mono<String> getAllPersons() {
        log.info("Getting all persons (via MCP Router)");
        return mcpRouterService.getAllPersons();
    }

    @GetMapping("/{id}")
    public Mono<String> getPersonById(@PathVariable Long id) {
        log.info("Getting person with ID: {} (via MCP Router)", id);
        return mcpRouterService.getPersonById(id);
    }

    @PostMapping
    public Mono<String> addPerson(@RequestBody Map<String, Object> personData) {
        log.info("Adding new person (via MCP Router): {}", personData);
        
        String firstName = (String) personData.get("firstName");
        String lastName = (String) personData.get("lastName");
        Integer age = (Integer) personData.get("age");
        String nationality = (String) personData.get("nationality");
        String gender = (String) personData.get("gender");
        
        return mcpRouterService.addPerson(firstName, lastName, age, nationality, gender);
    }

    @DeleteMapping("/{id}")
    public Mono<String> deletePerson(@PathVariable Long id) {
        log.info("Deleting person with ID: {} (via MCP Router)", id);
        return mcpRouterService.deletePerson(id);
    }

    @PostMapping("/query")
    public Mono<String> queryPersons(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        log.info("Processing custom query (via MCP Router): {}", query);
        return mcpRouterService.handleCustomQuery(query);
    }

    // ==================== MCP Router 管理端点 ====================

    @GetMapping("/mcp/tools")
    public Mono<String> getAvailableTools() {
        log.info("Getting available MCP tools through router");
        return mcpRouterService.getAvailableTools();
    }

    @GetMapping("/mcp/servers")
    public Mono<String> listMcpServers() {
        log.info("Listing registered MCP servers");
        return mcpRouterService.listMcpServers();
    }

    @GetMapping("/mcp/system-info")
    public Mono<String> getSystemInfo() {
        log.info("Getting MCP Router system information");
        return mcpRouterService.getSystemInfo();
    }

    @GetMapping("/mcp/status")
    public Mono<String> checkMcpStatus() {
        log.info("Checking MCP Router connection status");
        return mcpRouterService.checkMcpConnectionStatus();
    }

    @GetMapping("/mcp/discovered-servers")
    public Mono<String> getDiscoveredServers() {
        log.info("Getting discovered MCP servers list");
        return mcpRouterService.getDiscoveredServers();
    }

    @GetMapping("/mcp/server-status/{serverName}")
    public Mono<String> checkServerStatus(@PathVariable String serverName) {
        log.info("Checking status for server: {}", serverName);
        return mcpRouterService.checkServerAvailability(serverName);
    }

    /**
     * 检查指定工具的健康度
     */
    @GetMapping("/tools/{toolName}/health")
    public ResponseEntity<Map<String, Object>> checkToolHealth(@PathVariable String toolName) {
        log.info("检查工具健康度: {}", toolName);
        Map<String, Object> healthStatus = mcpRouterService.checkToolHealth(toolName);
        return ResponseEntity.ok(healthStatus);
    }
    
    /**
     * 批量检查所有工具的健康度
     */
    @GetMapping("/tools/health/batch")
    public ResponseEntity<Map<String, Object>> checkAllToolsHealth() {
        log.info("批量检查所有工具健康度");
        Map<String, Object> batchHealthStatus = mcpRouterService.checkAllToolsHealth();
        return ResponseEntity.ok(batchHealthStatus);
    }
    
    /**
     * 验证指定工具是否注册成功
     */
    @GetMapping("/tools/{toolName}/verify")
    public ResponseEntity<Map<String, Object>> verifyToolRegistration(@PathVariable String toolName) {
        log.info("验证工具注册状态: {}", toolName);
        Map<String, Object> verificationResult = mcpRouterService.verifyToolRegistration(toolName);
        return ResponseEntity.ok(verificationResult);
    }
    
    /**
     * 获取所有已注册的工具列表
     */
    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> getAllRegisteredTools() {
        log.info("获取所有已注册工具列表");
        Map<String, Object> toolsList = mcpRouterService.getAllRegisteredTools();
        return ResponseEntity.ok(toolsList);
    }
    
    /**
     * 使用健康检查调用工具 - 根据ID获取人员信息
     */
    @GetMapping("/persons/{id}/with-health-check")
    public ResponseEntity<Map<String, Object>> getPersonByIdWithHealthCheck(@PathVariable Long id) {
        log.info("带健康检查的获取人员信息: ID = {}", id);
        
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        
        Map<String, Object> result = mcpRouterService.callToolWithHealthCheck(
            "person-mcp-server", "getPersonById", params);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 使用健康检查调用工具 - 获取所有人员信息
     */
    @GetMapping("/persons/all/with-health-check")
    public ResponseEntity<Map<String, Object>> getAllPersonsWithHealthCheck() {
        log.info("带健康检查的获取所有人员信息");
        
        Map<String, Object> result = mcpRouterService.callToolWithHealthCheck(
            "person-mcp-server", "getAllPersons", new HashMap<>());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 使用健康检查调用工具 - 根据国籍获取人员信息
     */
    @GetMapping("/persons/nationality/{nationality}/with-health-check")
    public ResponseEntity<Map<String, Object>> getPersonsByNationalityWithHealthCheck(@PathVariable String nationality) {
        log.info("带健康检查的根据国籍获取人员信息: nationality = {}", nationality);
        
        Map<String, Object> params = new HashMap<>();
        params.put("nationality", nationality);
        
        Map<String, Object> result = mcpRouterService.callToolWithHealthCheck(
            "person-mcp-server", "getPersonsByNationality", params);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 使用健康检查调用工具 - 添加新人员
     */
    @PostMapping("/persons/with-health-check")
    public ResponseEntity<Map<String, Object>> addPersonWithHealthCheck(@RequestBody Map<String, Object> personData) {
        log.info("带健康检查的添加人员: {}", personData);
        
        Map<String, Object> result = mcpRouterService.callToolWithHealthCheck(
            "person-mcp-server", "addPerson", personData);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 使用健康检查调用工具 - 删除人员
     */
    @DeleteMapping("/persons/{id}/with-health-check")
    public ResponseEntity<Map<String, Object>> deletePersonWithHealthCheck(@PathVariable Long id) {
        log.info("带健康检查的删除人员: ID = {}", id);
        
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        
        Map<String, Object> result = mcpRouterService.callToolWithHealthCheck(
            "person-mcp-server", "deletePerson", params);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 测试工具链：验证注册 -> 检查健康度 -> 调用工具
     */
    @GetMapping("/test/tool-chain/{toolName}")
    public ResponseEntity<Map<String, Object>> testToolChain(@PathVariable String toolName) {
        log.info("测试工具链: {}", toolName);
        
        Map<String, Object> chainResult = new HashMap<>();
        chainResult.put("toolName", toolName);
        chainResult.put("timestamp", System.currentTimeMillis());
        
        try {
            // 1. 验证工具注册
            Map<String, Object> verification = mcpRouterService.verifyToolRegistration(toolName);
            chainResult.put("verification", verification);
            
            Boolean isRegistered = (Boolean) verification.get("registered");
            if (isRegistered == null || !isRegistered) {
                chainResult.put("success", false);
                chainResult.put("step", "verification");
                chainResult.put("message", "工具未注册或注册验证失败");
                return ResponseEntity.ok(chainResult);
            }
            
            // 2. 检查健康度
            Map<String, Object> healthCheck = mcpRouterService.checkToolHealth(toolName);
            chainResult.put("healthCheck", healthCheck);
            
            Boolean isHealthy = (Boolean) healthCheck.get("healthy");
            if (isHealthy == null || !isHealthy) {
                chainResult.put("success", false);
                chainResult.put("step", "healthCheck");
                chainResult.put("message", "工具健康检查失败");
                return ResponseEntity.ok(chainResult);
            }
            
            // 3. 执行示例调用（根据工具名称决定参数）
            Map<String, Object> params = new HashMap<>();
            if ("getPersonById".equals(toolName)) {
                params.put("id", 1L);
            } else if ("getPersonsByNationality".equals(toolName)) {
                params.put("nationality", "Chinese");
            }
            
            Map<String, Object> toolResult = mcpRouterService.callTool("person-mcp-server", toolName, params);
            chainResult.put("toolResult", toolResult);
            
            chainResult.put("success", true);
            chainResult.put("step", "completed");
            chainResult.put("message", "工具链测试完成");
            
        } catch (Exception e) {
            log.error("工具链测试异常: {}", toolName, e);
            chainResult.put("success", false);
            chainResult.put("error", e.getMessage());
            chainResult.put("step", "exception");
        }
        
        return ResponseEntity.ok(chainResult);
    }
} 