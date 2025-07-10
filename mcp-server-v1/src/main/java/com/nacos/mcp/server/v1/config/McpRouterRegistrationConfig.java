//package com.nacos.mcp.server.v1.config;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.Data;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.event.ContextRefreshedEvent;
//import org.springframework.context.event.EventListener;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.retry.annotation.Backoff;
//import org.springframework.retry.annotation.EnableRetry;
//import org.springframework.retry.annotation.Retryable;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//import org.springframework.web.client.RestTemplate;
//
//import java.lang.reflect.Method;
//import java.lang.reflect.Parameter;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
////@Configuration
//@EnableRetry
//@RequiredArgsConstructor
//@Slf4j
//public class McpRouterRegistrationConfig {
//
//    private final McpServerConfig mcpServerConfig;
//    private final ApplicationContext applicationContext;
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    @EventListener(ContextRefreshedEvent.class)
//    public void onApplicationEvent() {
//        registerWithRouter();
//    }
//
//    @Retryable(
//            value = {Exception.class},
//            maxAttempts = 5,
//            backoff = @Backoff(delay = 5000, multiplier = 2)
//    )
//    public void registerWithRouter() {
//        String routerUrl = mcpServerConfig.getRouterUrl() + "/register";
//        log.info("Attempting to register with MCP Router at: {}", routerUrl);
//
//        try {
//            List<ToolDefinition> tools = discoverTools();
//            RegistrationRequest request = new RegistrationRequest(
//                    mcpServerConfig.getName(),
//                    mcpServerConfig.getIp(),
//                    mcpServerConfig.getPort(),
//                    tools
//            );
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            HttpEntity<String> entity = new HttpEntity<>(new ObjectMapper().writeValueAsString(request), headers);
//
//            ResponseEntity<String> response = restTemplate.postForEntity(routerUrl, entity, String.class);
//
//            if (response.getStatusCode().is2xxSuccessful()) {
//                log.info("Successfully registered with MCP Router. Response: {}", response.getBody());
//            } else {
//                log.error("Failed to register with MCP Router. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
//                throw new IllegalStateException("Failed to register with MCP Router");
//            }
//        } catch (Exception e) {
//            log.error("Error registering with MCP Router, will retry...", e);
//            throw new RuntimeException(e);
//        }
//    }
//
//    private List<ToolDefinition> discoverTools() {
//        List<ToolDefinition> toolDefinitions = new ArrayList<>();
//        String[] toolBeanNames = applicationContext.getBeanNamesForType(com.nacos.mcp.server.v1.tools.PersonQueryTools.class);
//        String[] toolBeanNames2 = applicationContext.getBeanNamesForType(com.nacos.mcp.server.v1.tools.PersonModifyTools.class);
//
//        List<String> allToolBeanNames = new ArrayList<>();
//        Collections.addAll(allToolBeanNames, toolBeanNames);
//        Collections.addAll(allToolBeanNames, toolBeanNames2);
//
//
//        for (String beanName : allToolBeanNames) {
//            Object toolBean = applicationContext.getBean(beanName);
//            for (Method method : toolBean.getClass().getDeclaredMethods()) {
//                toolDefinitions.add(createToolDefinition(beanName, method));
//            }
//        }
//        return toolDefinitions;
//    }
//
//    private ToolDefinition createToolDefinition(String beanName, Method method) {
//        String methodName = method.getName();
//        String description = "No description"; // Placeholder
//        List<ToolParameter> parameters = Arrays.stream(method.getParameters())
//                .map(this::createToolParameter)
//                .collect(Collectors.toList());
//        return new ToolDefinition(beanName + "." + methodName, description, parameters);
//    }
//
//    private ToolParameter createToolParameter(Parameter parameter) {
//        String name = parameter.getName();
//        String type = parameter.getType().getSimpleName();
//        String description = "No description"; // Placeholder
//        boolean required = true; // Placeholder
//        return new ToolParameter(name, type, description, required);
//    }
//
//    @Data
//    private static class RegistrationRequest {
//        private final String serverName;
//        private final String ip;
//        private final int port;
//        private final List<ToolDefinition> tools;
//    }
//
//    @Data
//    private static class ToolDefinition {
//        private final String name;
//        private final String description;
//        private final List<ToolParameter> parameters;
//    }
//
//    @Data
//    private static class ToolParameter {
//        private final String name;
//        private final String type;
//        private final String description;
//        private final boolean required;
//    }
//}