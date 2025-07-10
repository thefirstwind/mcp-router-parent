package com.nacos.mcp.server.v1.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nacos.mcp.server.v1.tools.PersonModifyTools;
import com.nacos.mcp.server.v1.tools.PersonQueryTools;
import lombok.Data;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class McpServerConfig {
    /**
     * 创建 ObjectMapper Bean
     * 用于 JSON 序列化和反序列化
     *
     * @return ObjectMapper 实例
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }


    /**
     * 明确声明 PersonTools 作为 toolProvider
     * 这确保 Spring AI MCP Server 能够正确发现和注册所有的 @Tool 方法
     *
     * @param personQueryTools 由 Spring 自动注入的 PersonTools 实例
     * @return PersonTools 实例作为工具提供者
     */
    @Bean
    public ToolCallbackProvider toolCallbackProvider(PersonQueryTools personQueryTools, PersonModifyTools personModifyTools) {
        // 返回已经由 Spring 管理的 PersonTools 实例
        // Spring AI MCP Server 会自动扫描这个 Bean 中的 @Tool 方法
        return MethodToolCallbackProvider.builder()
                .toolObjects(personQueryTools, personModifyTools)
                .build();
    }
} 