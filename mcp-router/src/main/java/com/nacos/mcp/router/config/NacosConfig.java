package com.nacos.mcp.router.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.naming.NamingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * Nacos Configuration
 */
@Configuration
public class NacosConfig {

    @Value("${nacos.discovery.server-addr}")
    private String addr;

    @Value("${nacos.discovery.username}")
    private String username;

    @Value("${nacos.discovery.password}")
    private String password;

    @Value("${nacos.discovery.namespace}")
    private String namespace;

    @Bean
    public NamingService namingService() throws Exception {
        Properties properties = new Properties();
        properties.put("serverAddr", addr);
        properties.put("username", username);
        properties.put("password", password);
        properties.put("namespace", namespace);

        System.out.println("Initializing Nacos naming service with server: " + addr);
        return NacosFactory.createNamingService(properties);
    }
}