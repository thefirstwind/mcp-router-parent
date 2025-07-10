package com.nacos.mcp.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
public class WebFluxConfig {

    @Bean
    public RouterFunction<ServerResponse> indexRouter() {
        return route(GET("/"), request ->
                ServerResponse.ok().bodyValue("MCP Client is running."));
    }
} 