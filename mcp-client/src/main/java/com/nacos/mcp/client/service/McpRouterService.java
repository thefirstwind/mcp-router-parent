package com.nacos.mcp.client.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class McpRouterService {

    private final WebClient webClient;

    public McpRouterService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8050/api/mcp").build();
    }

    public Mono<String> getCompletions(String model, String prompt) {
        // This is just an example, the actual implementation will depend on the router's API
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue("{\"model\": \"" + model + "\", \"prompt\": \"" + prompt + "\"}")
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> register(String name) {
        return webClient.post()
                .uri("/servers/{serverName}", name)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> unregister(String name) {
        return webClient.delete()
                .uri("/unregister/{serverName}", name)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> heartbeat(String name) {
        return webClient.post()
                .uri("/servers/{serverName}/heartbeat", name)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> subscribe(String name, String toolName) {
        // This is a placeholder, the actual implementation will depend on the router's API
        return Mono.just("Subscribed");
    }

    public Mono<String> find(String name, String toolName) {
        // This is a placeholder, the actual implementation will depend on the router's API
        return Mono.just("Found");
    }

    public Mono<String> list(String name, String type) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/servers/{serverName}/" + type).build(name))
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> unsubscribe(String name, String toolName) {
        // This is a placeholder, the actual implementation will depend on the router's API
        return Mono.just("Unsubscribed");
    }

    public Mono<String> call(String name, String toolName, String prompt) {
        return webClient.post()
                .uri("/servers/{serverName}/tools/{toolName}", name, toolName)
                .bodyValue(prompt)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> callWithStream(String name, String toolName, String prompt) {
        // This is a placeholder, the actual implementation will depend on the router's API
        return Mono.just("Called with stream");
    }

    public Mono<String> listChanged(String name, String timestamp) {
        // This is a placeholder, the actual implementation will depend on the router's API
        return Mono.just("List of changed items");
    }

    public Mono<String> progress(String name, String toolName, String status) {
        // This is a placeholder, the actual implementation will depend on the router's API
        return Mono.just("Progress updated");
    }

    public Mono<String> cancel(String name, String toolName) {
        // This is a placeholder, the actual implementation will depend on the router's API
        return Mono.just("Cancelled");
    }

    public Mono<String> listPlugins(String name) {
        // This is a placeholder, the actual implementation will depend on the router's API
        return Mono.just("List of plugins");
    }

    public Mono<String> callPlugin(String name, String pluginName, String parameters) {
        // This is a placeholder, the actual implementation will depend on the router's API
        return Mono.just("Plugin called");
    }

    public Mono<String> listAllServers() {
        return webClient.get()
                .uri("/servers")
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> listToolsByServer(String serverName) {
        // This is not a standard MCP endpoint, it's a client-side convenience method
        // that calls the router. The router will then call the appropriate MCP server.
        // The demo script calls /mcp-client/api/v1/tools/list, which should be implemented here.
        // It seems the intention is to call the router's /search endpoint.
        // I will implement a call to the router's /servers/{serverName} endpoint to get the tools.
        return webClient.get()
                .uri("/servers/{serverName}", serverName)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> getServerByName(String serverName) {
        return webClient.get()
                .uri("/servers/{serverName}", serverName)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> listAllTools() {
         return webClient.get()
                .uri("/search?taskDescription=all")
                .retrieve()
                .bodyToMono(String.class);
    }
} 