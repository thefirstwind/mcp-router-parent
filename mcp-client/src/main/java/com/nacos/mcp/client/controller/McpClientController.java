package com.nacos.mcp.client.controller;

import com.nacos.mcp.client.service.McpRouterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/mcp-client/api/v1")
public class McpClientController {

    private final McpRouterService mcpRouterService;

    public McpClientController(McpRouterService mcpRouterService) {
        this.mcpRouterService = mcpRouterService;
    }

    @GetMapping("/tools/list")
    public Mono<ResponseEntity<String>> listTools(@RequestParam(defaultValue = "all") String task) {
        log.info("McpClientController#listTools: {}", task);
        return mcpRouterService.listTools(task)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(e.getMessage())));
    }

    @PostMapping("/tools/call")
    public Mono<ResponseEntity<String>> callTool(@RequestBody String jsonPayload) {
        log.info("McpClientController#callTool: {}", jsonPayload);
        return mcpRouterService.callTool(jsonPayload)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(e.getMessage())));
    }

    @GetMapping("/completions")
    public Mono<String> getCompletions(@RequestParam String prompt) {
        log.info("McpClientController#getCompletions: {}", prompt);
        return mcpRouterService.getCompletions("deepseek-chat", prompt);
    }

    @PostMapping("/register")
    public Mono<String> register(@RequestParam String name) {
        log.info("McpClientController#register: {}", name);
        return mcpRouterService.register(name);
    }

    @PostMapping("/unregister")
    public Mono<String> unregister(@RequestParam String name) {
        log.info("McpClientController#unregister: {}", name);
        return mcpRouterService.unregister(name);
    }

    @PostMapping("/heartbeat")
    public Mono<String> heartbeat(@RequestParam String name) {
        log.info("McpClientController#heartbeat: {}", name);
        return mcpRouterService.heartbeat(name);
    }

    @PostMapping("/subscribe")
    public Mono<String> subscribe(@RequestParam String name, @RequestParam String toolName) {
        log.info("McpClientController#subscribe: {}", name);
        return mcpRouterService.subscribe(name, toolName);
    }

    @GetMapping("/find")
    public Mono<String> find(@RequestParam String name, @RequestParam String toolName) {
        log.info("McpClientController#find: {}", name);
        return mcpRouterService.find(name, toolName);
    }

    @GetMapping("/list")
    public Mono<String> list(@RequestParam String name, @RequestParam String type) {
        log.info("list: {}", name);
        return mcpRouterService.list(name, type);
    }

    @PostMapping("/unsubscribe")
    public Mono<String> unsubscribe(@RequestParam String name, @RequestParam String toolName) {
        log.info("McpClientController#unsubscribe: {}", name);
        return mcpRouterService.unsubscribe(name, toolName);
    }

    @PostMapping("/call")
    public Mono<String> call(@RequestParam String name, @RequestParam String toolName, @RequestBody String prompt) {
        log.info("McpClientController#call: {}", name);
        return mcpRouterService.call(name, toolName, prompt);
    }

    @PostMapping("/call-with-stream")
    public Mono<String> callWithStream(@RequestParam String name, @RequestParam String toolName, @RequestBody String prompt) {
        log.info("McpClientController#callWithStream: {}", name);
        return mcpRouterService.callWithStream(name, toolName, prompt);
    }

    @GetMapping("/list-changed")
    public Mono<String> listChanged(@RequestParam String name, @RequestParam String timestamp) {
        log.info("listChanged: {}", name);
        return mcpRouterService.listChanged(name, timestamp);
    }

    @PostMapping("/progress")
    public Mono<String> progress(@RequestParam String name, @RequestParam String toolName, @RequestBody String status) {
        log.info("McpClientController#progress: {}", name);
        return mcpRouterService.progress(name, toolName, status);
    }

    @PostMapping("/cancel")
    public Mono<String> cancel(@RequestParam String name, @RequestParam String toolName) {
        log.info("McpClientController#cancel: {}", name);
        return mcpRouterService.cancel(name, toolName);
    }

    @GetMapping("/list-plugins")
    public Mono<String> listPlugins(@RequestParam String name) {
        log.info("McpClientController#listPlugins: {}", name);
        return mcpRouterService.listPlugins(name);
    }

    @PostMapping("/call-plugin")
    public Mono<String> callPlugin(@RequestParam String name, @RequestParam String pluginName, @RequestBody String parameters) {
        log.info("McpClientController#callPlugin: {}", name);
        return mcpRouterService.callPlugin(name, pluginName, parameters);
    }

    @GetMapping("/list-all-servers")
    public Mono<String> listAllServers() {
        log.info("McpClientController#listAllServers");
        return mcpRouterService.listAllServers();
    }

    @GetMapping("/list-tools-by-server")
    public Mono<String> listToolsByServer(@RequestParam String serverName) {
        log.info("McpClientController#listToolsByServer: {}", serverName);
        return mcpRouterService.listToolsByServer(serverName);
    }

    @GetMapping("/get-server-by-name")
    public Mono<String> getServerByName(@RequestParam String serverName) {
        log.info("McpClientController#getServerByName: {}", serverName);
        return mcpRouterService.getServerByName(serverName);
    }
} 