package com.nacos.mcp.server.v2.config;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Map;

@Configuration
public class NacosRegistrationConfig implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(NacosRegistrationConfig.class);

    private final NacosRegistration registration;
    private final Environment environment;
    private final NacosServiceManager nacosServiceManager;
    private final NacosDiscoveryProperties nacosDiscoveryProperties;


    public NacosRegistrationConfig(NacosRegistration registration,
                                   Environment environment,
                                   NacosServiceManager nacosServiceManager,
                                   NacosDiscoveryProperties nacosDiscoveryProperties) {
        this.registration = registration;
        this.environment = environment;
        this.nacosServiceManager = nacosServiceManager;
        this.nacosDiscoveryProperties = nacosDiscoveryProperties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Application ready. Updating Nacos metadata and re-registering.");
        Map<String, String> metadata = registration.getMetadata();
        metadata.put("name", environment.getProperty("spring.ai.mcp.server.name", ""));
        metadata.put("description", environment.getProperty("spring.ai.mcp.server.description", ""));
        metadata.put("version", environment.getProperty("spring.ai.mcp.server.version", ""));
        metadata.put("transportType", environment.getProperty("spring.ai.mcp.server.transport-type", ""));
        String toolsJson = environment.getProperty("spring.ai.mcp.server.tools", "[]");
        metadata.put("tools", toolsJson);
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
}