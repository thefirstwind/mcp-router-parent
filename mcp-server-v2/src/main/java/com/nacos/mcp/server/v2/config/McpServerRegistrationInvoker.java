package com.nacos.mcp.server.v2.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

//@Component
@RequiredArgsConstructor
@Slf4j
public class McpServerRegistrationInvoker implements ApplicationListener<ContextRefreshedEvent> {

    private final McpRouterRegistrationConfig registrationConfig;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Application context refreshed, triggering MCP registration.");
        registrationConfig.registerWithRouter();
    }
} 