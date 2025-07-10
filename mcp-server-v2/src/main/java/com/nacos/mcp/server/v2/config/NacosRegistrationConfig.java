package com.nacos.mcp.server.v2.config;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.Objects;

@Configuration
public class NacosRegistrationConfig implements ApplicationListener<ApplicationReadyEvent> {

    private final NacosDiscoveryProperties nacosDiscoveryProperties;
    private final Environment environment;

    public NacosRegistrationConfig(NacosDiscoveryProperties nacosDiscoveryProperties, Environment environment) {
        this.nacosDiscoveryProperties = nacosDiscoveryProperties;
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Map<String, String> metadata = nacosDiscoveryProperties.getMetadata();
        metadata.put("name", environment.getProperty("spring.ai.mcp.server.name", ""));
        metadata.put("description", environment.getProperty("spring.ai.mcp.server.description", ""));
        metadata.put("version", environment.getProperty("spring.ai.mcp.server.version", ""));
        metadata.put("transportType", environment.getProperty("spring.ai.mcp.server.transport-type", ""));
        metadata.put("tools", environment.getProperty("spring.ai.mcp.server.tools", ""));
        metadata.put("context-path", environment.getProperty("server.servlet.context-path", ""));
    }
} 