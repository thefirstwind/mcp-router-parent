package com.nacos.mcp.server.v1.config;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nacos.mcp.server.v1.tools.PersonModifyTools;
import com.nacos.mcp.server.v1.tools.PersonQueryTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
public class NacosRegistrationConfig implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(NacosRegistrationConfig.class);

    private final NacosRegistration registration;
    private final Environment environment;
    private final NacosServiceManager nacosServiceManager;
    private final NacosDiscoveryProperties nacosDiscoveryProperties;
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    public NacosRegistrationConfig(NacosRegistration registration,
                                   Environment environment,
                                   NacosServiceManager nacosServiceManager,
                                   NacosDiscoveryProperties nacosDiscoveryProperties,
                                   ApplicationContext applicationContext,
                                   ObjectMapper objectMapper) {
        this.registration = registration;
        this.environment = environment;
        this.nacosServiceManager = nacosServiceManager;
        this.nacosDiscoveryProperties = nacosDiscoveryProperties;
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Application ready. Updating Nacos metadata and re-registering.");
        Map<String, String> metadata = registration.getMetadata();
        metadata.put("name", environment.getProperty("spring.ai.mcp.server.name", ""));
        metadata.put("description", environment.getProperty("spring.ai.mcp.server.description", ""));
        metadata.put("version", environment.getProperty("spring.ai.mcp.server.version", ""));
        metadata.put("transportType", environment.getProperty("spring.ai.mcp.server.transport-type", ""));

        List<Map<String, Object>> toolDefinitions = new ArrayList<>();
        addToolDefinitions(toolDefinitions);

        try {
            String toolsJson = objectMapper.writeValueAsString(toolDefinitions);
            metadata.put("tools", toolsJson);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize tools to JSON", e);
            metadata.put("tools", "[]");
        }

        metadata.put("context-path", environment.getProperty("server.servlet.context-path", ""));
        log.info("Updated metadata: {}", metadata);

        try {
            // Create a new Nacos Instance object from the Spring Cloud Registration
            Instance instance = new Instance();
            instance.setIp(registration.getHost());
            instance.setPort(registration.getPort());
            instance.setWeight(registration.getMetadata().get("weight") == null ? 1.0D : Double.parseDouble(registration.getMetadata().get("weight")));
            instance.setClusterName(nacosDiscoveryProperties.getClusterName());
            instance.setMetadata(registration.getMetadata());
            instance.setEphemeral(nacosDiscoveryProperties.isEphemeral());

            // Re-register the service with updated metadata
            nacosServiceManager.getNamingService(nacosDiscoveryProperties.getNacosProperties())
                    .registerInstance(registration.getServiceId(), nacosDiscoveryProperties.getGroup(), instance);
            log.info("Successfully re-registered service '{}' with Nacos.", registration.getServiceId());
        } catch (NacosException e) {
            log.error("Failed to re-register service with Nacos", e);
            throw new RuntimeException("Failed to re-register service with Nacos", e);
        }
    }

    private void addToolDefinitions(List<Map<String, Object>> toolDefinitions) {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object toolBean = applicationContext.getBean(beanName);
            Class<?> targetClass = AopUtils.getTargetClass(toolBean);
            for (Method method : targetClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    Tool toolAnnotation = method.getAnnotation(Tool.class);
                    toolDefinitions.add(Map.of(
                            "name", toolAnnotation.name(),
                            "description", toolAnnotation.description()
                    ));
                }
            }
        }
    }
}