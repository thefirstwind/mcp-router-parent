package com.nacos.mcp.router;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Nacos MCP Router Application
 * Acts as a bridge between MCP clients and multiple MCP servers
 * Uses Nacos for service discovery and Spring AI for MCP protocol support
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class NacosMcpRouterApplication {

    public static void main(String[] args) {
        SpringApplication.run(NacosMcpRouterApplication.class, args);
    }
} 