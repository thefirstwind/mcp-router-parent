package com.nacos.mcp.router.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class NacosConfiguration {

    @Value("${nacos.discovery.server-addr}")
    private String serverAddr;

    @Value("${nacos.discovery.namespace:}")
    private String namespace;

    @Bean
    public NamingService namingService() throws NacosException {
        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        properties.put("namespace", namespace);
        return NacosFactory.createNamingService(properties);
    }

    @Bean
    public ConfigService configService() throws NacosException {
        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        properties.put("namespace", namespace);
        return NacosFactory.createConfigService(properties);
    }
} 