package com.nacos.mcp.server.v2.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mcp.server")
public class McpServerConfig {
    private String name;
    private String ip;
    private int port;
    private String routerUrl;
} 