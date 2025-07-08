package com.nacos.mcp.router.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class McpRouterBasicTest {

    @Test
    void contextLoads() {
        // Basic test to verify Spring context loads correctly
        assertTrue(true, "Spring context should load without errors");
    }

    @Test
    void testBasicConfiguration() {
        // Test that basic configuration properties are working
        String profile = System.getProperty("spring.profiles.active", "test");
        assertNotNull(profile);
    }
} 
 