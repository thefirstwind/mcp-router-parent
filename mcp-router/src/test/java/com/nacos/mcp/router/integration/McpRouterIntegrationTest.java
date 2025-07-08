package com.nacos.mcp.router.integration;

import com.nacos.mcp.router.model.McpServerRegistrationRequest;
import com.nacos.mcp.router.model.SearchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "nacos.discovery.server-addr=127.0.0.1:8848",
    "nacos.discovery.enabled=false"
})
class McpRouterIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testCompleteWorkflow() {
        // Test registration
        McpServerRegistrationRequest request = McpServerRegistrationRequest.builder()
                .serverName("mcp-integration-test")
                .ip("localhost")
                .port(9999)
                .transportType("stdio")
                .description("Integration test server")
                .version("1.0.0")
                .healthPath("/health")
                .enabled(true)
                .weight(1.0)
                .cluster("DEFAULT")
                .build();

        webTestClient.post()
                .uri("/api/mcp/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        // Test listing servers
        webTestClient.get()
                .uri("/api/mcp/servers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray();

        // Test search
        SearchRequest searchRequest = SearchRequest.builder()
                .taskDescription("integration test")
                .limit(5)
                .build();

        webTestClient.post()
                .uri("/api/mcp/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(searchRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalResults").exists();

        // Test unregistration
        webTestClient.delete()
                .uri("/api/mcp/unregister/mcp-integration-test")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testHealthEndpoint() {
        webTestClient.get()
                .uri("/api/mcp/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
} 