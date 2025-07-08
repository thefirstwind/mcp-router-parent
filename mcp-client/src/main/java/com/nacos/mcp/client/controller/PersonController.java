package com.nacos.mcp.client.controller;

import com.nacos.mcp.client.service.McpRouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
} 