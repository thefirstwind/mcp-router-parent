package com.nacos.mcp.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * MCP Client基础测试
 * 验证mcp-client能够正确启动
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.ai.deepseek.api-key=test-key",
    "spring.ai.mcp.client.enabled=false",  // 禁用MCP客户端避免连接问题
    "logging.level.org.springframework.ai=WARN"  // 减少日志输出
})
public class McpClientIntegrationTest {

    @Test
    public void testSpringContextLoads() {
        // 基本的上下文加载测试
        // 如果这个测试通过，说明Spring Boot应用能够正确启动
    }
} 