package com.nacos.mcp.client.controller;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
@RequestMapping("/mcp-client/api/v1")
public class PersonController {

    private final com.nacos.mcp.client.service.McpRouterService mcpRouterService;

    public PersonController(com.nacos.mcp.client.service.McpRouterService mcpRouterService) {
        this.mcpRouterService = mcpRouterService;
    }

    @GetMapping("/tools/list")
    public Mono<String> listAllTools() {
        return mcpRouterService.listAllTools();
    }

    @PostMapping("/tools/call")
    public Mono<String> callTool(@RequestBody Map<String, Object> payload) {
        String toolName = (String) payload.get("toolName");
        // This is a simplification. In a real scenario, you would need to figure out which server has the tool.
        // For the demo, we'll assume the tool is on the v2 server.
        return mcpRouterService.call("mcp-server-v2", toolName, payload.get("arguments").toString());
    }

    @GetMapping("/completions")
    public Mono<String> getCompletions(@RequestParam String prompt) {
        return mcpRouterService.getCompletions("deepseek-chat", prompt);
    }

    @PostMapping("/register")
    public Mono<String> register(@RequestParam String name) {
        return mcpRouterService.register(name);
    }

    @PostMapping("/unregister")
    public Mono<String> unregister(@RequestParam String name) {
        return mcpRouterService.unregister(name);
    }

    @PostMapping("/heartbeat")
    public Mono<String> heartbeat(@RequestParam String name) {
        return mcpRouterService.heartbeat(name);
    }

    @PostMapping("/subscribe")
    public Mono<String> subscribe(@RequestParam String name, @RequestParam String toolName) {
        return mcpRouterService.subscribe(name, toolName);
    }

    @GetMapping("/find")
    public Mono<String> find(@RequestParam String name, @RequestParam String toolName) {
        return mcpRouterService.find(name, toolName);
    }

    @GetMapping("/list")
    public Mono<String> list(@RequestParam String name, @RequestParam String type) {
        return mcpRouterService.list(name, type);
    }

    @PostMapping("/unsubscribe")
    public Mono<String> unsubscribe(@RequestParam String name, @RequestParam String toolName) {
        return mcpRouterService.unsubscribe(name, toolName);
    }

    @PostMapping("/call")
    public Mono<String> call(@RequestParam String name, @RequestParam String toolName, @RequestBody String prompt) {
        return mcpRouterService.call(name, toolName, prompt);
    }

    @PostMapping("/call-with-stream")
    public Mono<String> callWithStream(@RequestParam String name, @RequestParam String toolName, @RequestBody String prompt) {
        return mcpRouterService.callWithStream(name, toolName, prompt);
    }

    @GetMapping("/list-changed")
    public Mono<String> listChanged(@RequestParam String name, @RequestParam String timestamp) {
        return mcpRouterService.listChanged(name, timestamp);
    }

    @PostMapping("/progress")
    public Mono<String> progress(@RequestParam String name, @RequestParam String toolName, @RequestBody String status) {
        return mcpRouterService.progress(name, toolName, status);
    }

    @PostMapping("/cancel")
    public Mono<String> cancel(@RequestParam String name, @RequestParam String toolName) {
        return mcpRouterService.cancel(name, toolName);
    }

    @GetMapping("/list-plugins")
    public Mono<String> listPlugins(@RequestParam String name) {
        return mcpRouterService.listPlugins(name);
    }

    @PostMapping("/call-plugin")
    public Mono<String> callPlugin(@RequestParam String name, @RequestParam String pluginName, @RequestBody String parameters) {
        return mcpRouterService.callPlugin(name, pluginName, parameters);
    }

    @GetMapping("/list-all-servers")
    public Mono<String> listAllServers() {
        return mcpRouterService.listAllServers();
    }

    @GetMapping("/list-tools-by-server")
    public Mono<String> listToolsByServer(@RequestParam String serverName) {
        return mcpRouterService.listToolsByServer(serverName);
    }

    @GetMapping("/get-server-by-name")
    public Mono<String> getServerByName(@RequestParam String serverName) {
        return mcpRouterService.getServerByName(serverName);
    }
} 