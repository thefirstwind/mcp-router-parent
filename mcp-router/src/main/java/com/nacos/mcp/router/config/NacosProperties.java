package com.nacos.mcp.router.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Nacos Configuration Properties
 */
@Data
@Validated
@ConfigurationProperties(prefix = "nacos.discovery")
public class NacosProperties {

    /**
     * Nacos server address
     */
    @NotBlank(message = "Nacos address cannot be blank")
    private String addr = "127.0.0.1:8848";

    /**
     * Nacos username
     */
    private String username = "nacos";

    /**
     * Nacos password
     */
    @NotBlank(message = "Nacos password cannot be blank")
    private String password;

    /**
     * Nacos namespace
     */
    private String namespace = "public";
} 