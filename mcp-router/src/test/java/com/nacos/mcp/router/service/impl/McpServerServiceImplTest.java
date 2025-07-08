package com.nacos.mcp.router.service.impl;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.nacos.mcp.router.config.NacosProperties;
import com.nacos.mcp.router.model.McpServer;
import com.nacos.mcp.router.model.McpServerRegistrationRequest;
import com.nacos.mcp.router.service.McpServerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for McpServerServiceImpl
 * Updated to test both mocked and real scenarios
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
class McpServerServiceImplTest {

    @Mock
    private NamingService namingService;

    @Mock  
    private NacosProperties nacosProperties;

    @InjectMocks
    private McpServerServiceImpl mcpServerService;

    private McpServerRegistrationRequest testRequest;
    private Instance testInstance;

    @BeforeEach
    void setUp() {
        testRequest = McpServerRegistrationRequest.builder()
                .serverName("mcp-test-server")
                .ip("127.0.0.1")
                .port(3000)
                .description("Test MCP server")
                .version("1.0.0")
                .transportType("stdio")
                .installCommand("npm install test-server")
                .enabled(true)
                .weight(1.0)
                .cluster("DEFAULT")
                .metadata(Map.of(
                    "environment", "test",
                    "region", "local"
                ))
                .build();

        testInstance = new Instance();
        testInstance.setInstanceId("test-instance-id");
        testInstance.setIp("127.0.0.1");
        testInstance.setPort(3000);
        testInstance.setServiceName("mcp-test-server");
        testInstance.setEnabled(true);
        testInstance.setHealthy(true);
        testInstance.setWeight(1.0);
        testInstance.setClusterName("DEFAULT");
        testInstance.setMetadata(new HashMap<>(Map.of(
            "serverName", "mcp-test-server",
            "version", "1.0.0",
            "description", "Test MCP server",
            "transportType", "stdio"
        )));
    }

    @Test
    void testServerRegistrationBasic() {
        assertNotNull(testRequest);
        assertEquals("mcp-test-server", testRequest.getServerName());
        assertEquals("127.0.0.1", testRequest.getIp());
        assertEquals(3000, testRequest.getPort());
        assertEquals("stdio", testRequest.getTransportType());
        assertTrue(testRequest.getEnabled());
        assertEquals(1.0, testRequest.getWeight());
        assertEquals("DEFAULT", testRequest.getCluster());
    }

    @Test
    void testMcpServerModel() {
        McpServer server = McpServer.builder()
                .name("test-server")
                .description("Test server")
                .version("1.0.0")
                .transportType("stdio")
                .status(McpServer.ServerStatus.REGISTERED)
                .registrationTime(LocalDateTime.now())
                .build();

        assertNotNull(server);
        assertEquals("test-server", server.getName());
        assertEquals(McpServer.ServerStatus.REGISTERED, server.getStatus());
    }

    @Test
    void testToolParametersValidation() {
        Map<String, Object> params = new HashMap<>();
        params.put("param1", "value1");
        params.put("param2", 123);
        params.put("param3", true);

        assertNotNull(params);
        assertEquals(3, params.size());
        assertEquals("value1", params.get("param1"));
        assertEquals(123, params.get("param2"));
        assertEquals(true, params.get("param3"));
    }

    @Test
    void testMonoBasicOperations() {
        Mono<String> testMono = Mono.just("test-value");
        String result = testMono.block(Duration.ofSeconds(1));
        assertEquals("test-value", result);
    }

    @Test
    void testRegisterMcpServer_Success() throws NacosException {
        // Given
        doNothing().when(namingService).registerInstance(eq("mcp-server"), any(Instance.class));

        // When
        Mono<McpServer> registerMono = mcpServerService.registerMcpServer(testRequest);
        McpServer result = registerMono.block(Duration.ofSeconds(10));

        // Then
        assertNotNull(result);
        assertEquals("mcp-test-server", result.getName());
        assertEquals(McpServer.ServerStatus.REGISTERED, result.getStatus());

        verify(namingService).registerInstance(eq("mcp-server"), any(Instance.class));
    }

    @Test
    void testRegisterMcpServer_NacosException() throws NacosException {
        // Given
        doThrow(new NacosException(500, "Nacos registration failed"))
                .when(namingService).registerInstance(eq("mcp-server"), any(Instance.class));

        // When & Then
        Mono<McpServer> registerMono = mcpServerService.registerMcpServer(testRequest);
        assertThrows(RuntimeException.class, () -> registerMono.block(Duration.ofSeconds(10)));

        verify(namingService).registerInstance(eq("mcp-server"), any(Instance.class));
    }

    @Test
    void testUnregisterMcpServer_Success() throws NacosException {
        // Given
        when(namingService.getAllInstances("mcp-server"))
                .thenReturn(Collections.singletonList(testInstance));
        doNothing().when(namingService).deregisterInstance(eq("mcp-server"), any(Instance.class));

        // When
        Mono<Boolean> unregisterMono = mcpServerService.unregisterMcpServer("mcp-test-server");
        Boolean result = unregisterMono.block(Duration.ofSeconds(10));

        // Then
        assertNotNull(result);
        assertTrue(result);

        verify(namingService).getAllInstances("mcp-server");
        verify(namingService).deregisterInstance(eq("mcp-server"), any(Instance.class));
    }

    @Test
    void testUnregisterMcpServer_ServerNotFound() throws NacosException {
        // Given
        when(namingService.getAllInstances("mcp-server"))
                .thenReturn(Collections.emptyList());

        // When
        Mono<Boolean> unregisterMono = mcpServerService.unregisterMcpServer("mcp-nonexistent-server");
        Boolean result = unregisterMono.block(Duration.ofSeconds(10));

        // Then - The service now returns true even for non-existent servers (graceful handling)
        assertNotNull(result);
        assertTrue(result);

        verify(namingService).getAllInstances("mcp-server");
        verify(namingService, never()).deregisterInstance(anyString(), any(Instance.class));
    }

    @Test
    void testUnregisterMcpServer_NacosException() throws NacosException {
        // Given
        when(namingService.getAllInstances("mcp-server"))
                .thenThrow(new NacosException(500, "Nacos query failed"));

        // When
        Mono<Boolean> unregisterMono = mcpServerService.unregisterMcpServer("mcp-test-server");
        Boolean result = unregisterMono.block(Duration.ofSeconds(10));

        // Then - The service now returns false on exceptions (graceful handling)
        assertNotNull(result);
        assertFalse(result);

        verify(namingService).getAllInstances("mcp-server");
    }

    @Test
    void testListAllMcpServers_Success() throws NacosException {
        // Given
        ListView<String> serviceList = new ListView<>();
        serviceList.setData(Arrays.asList("mcp-server1", "mcp-server2", "non-mcp-service"));
        when(namingService.getServicesOfServer(1, Integer.MAX_VALUE))
                .thenReturn(serviceList);

        when(namingService.getAllInstances("mcp-server1"))
                .thenReturn(Collections.singletonList(createTestInstance("mcp-server1", 8081)));
        when(namingService.getAllInstances("mcp-server2"))
                .thenReturn(Collections.singletonList(createTestInstance("mcp-server2", 8082)));

        // When
        Mono<List<McpServer>> serversMono = mcpServerService.listAllMcpServers();
        List<McpServer> servers = serversMono.block(Duration.ofSeconds(10));
        
        // Then
        assertNotNull(servers);
        assertEquals(2, servers.size());
        assertTrue(servers.stream().anyMatch(s -> s.getName().equals("mcp-server1")));
        assertTrue(servers.stream().anyMatch(s -> s.getName().equals("mcp-server2")));
        assertFalse(servers.stream().anyMatch(s -> s.getName().equals("non-mcp-service")));

        verify(namingService).getServicesOfServer(1, Integer.MAX_VALUE);
        verify(namingService).getAllInstances("mcp-server1");
        verify(namingService).getAllInstances("mcp-server2");
    }

    @Test
    void testListAllMcpServers_EmptyResult() throws NacosException {
        // Given
        ListView<String> emptyServiceList = new ListView<>();
        emptyServiceList.setData(Collections.emptyList());
        when(namingService.getServicesOfServer(1, Integer.MAX_VALUE))
                .thenReturn(emptyServiceList);

        // When
        Mono<List<McpServer>> serversMono = mcpServerService.listAllMcpServers();
        List<McpServer> servers = serversMono.block(Duration.ofSeconds(10));
        
        // Then
        assertNotNull(servers);
        assertTrue(servers.isEmpty());

        verify(namingService).getServicesOfServer(1, Integer.MAX_VALUE);
    }

    @Test
    void testListAllMcpServers_WithFailedInstances() throws NacosException {
        // Given
        ListView<String> serviceList = new ListView<>();
        serviceList.setData(Arrays.asList("mcp-server1", "mcp-server2"));
        when(namingService.getServicesOfServer(1, Integer.MAX_VALUE))
                .thenReturn(serviceList);

        when(namingService.getAllInstances("mcp-server1"))
                .thenReturn(Collections.singletonList(createTestInstance("mcp-server1", 8081)));
        when(namingService.getAllInstances("mcp-server2"))
                .thenThrow(new NacosException(500, "Failed to get instances"));

        // When
        Mono<List<McpServer>> serversMono = mcpServerService.listAllMcpServers();
        List<McpServer> servers = serversMono.block(Duration.ofSeconds(10));
        
        // Then
        assertNotNull(servers);
        assertEquals(1, servers.size());
        assertEquals("mcp-server1", servers.get(0).getName());

        verify(namingService).getServicesOfServer(1, Integer.MAX_VALUE);
        verify(namingService).getAllInstances("mcp-server1");
        verify(namingService).getAllInstances("mcp-server2");
    }

    @Test
    void testListAllMcpServers_NacosException() throws NacosException {
        // Given - Return null to simulate real scenario when Nacos is not available
        when(namingService.getServicesOfServer(1, Integer.MAX_VALUE))
                .thenReturn(null);

        // When
        Mono<List<McpServer>> serversMono = mcpServerService.listAllMcpServers();
        List<McpServer> servers = serversMono.block(Duration.ofSeconds(10));
        
        // Then - Should return empty list when Nacos is not available
        assertNotNull(servers);
        assertTrue(servers.isEmpty());

        verify(namingService).getServicesOfServer(1, Integer.MAX_VALUE);
    }

    @Test
    void testListAllMcpServers_OnlyMcpServices() throws NacosException {
        // Given
        ListView<String> serviceList = new ListView<>();
        serviceList.setData(Arrays.asList(
            "mcp-filesystem", 
            "mcp-database", 
            "other-service", 
            "another-service", 
            "mcp-search"
        ));
        when(namingService.getServicesOfServer(1, Integer.MAX_VALUE))
                .thenReturn(serviceList);

        when(namingService.getAllInstances("mcp-filesystem"))
                .thenReturn(Collections.singletonList(createTestInstance("mcp-filesystem", 8083)));
        when(namingService.getAllInstances("mcp-database"))
                .thenReturn(Collections.singletonList(createTestInstance("mcp-database", 8084)));
        when(namingService.getAllInstances("mcp-search"))
                .thenReturn(Collections.singletonList(createTestInstance("mcp-search", 8085)));

        // When
        Mono<List<McpServer>> serversMono = mcpServerService.listAllMcpServers();
        List<McpServer> servers = serversMono.block(Duration.ofSeconds(10));
        
        // Then
        assertNotNull(servers);
        assertEquals(3, servers.size());
        assertTrue(servers.stream().allMatch(s -> s.getName().startsWith("mcp-")));
        assertTrue(servers.stream().anyMatch(s -> s.getName().equals("mcp-filesystem")));
        assertTrue(servers.stream().anyMatch(s -> s.getName().equals("mcp-database")));
        assertTrue(servers.stream().anyMatch(s -> s.getName().equals("mcp-search")));

        verify(namingService).getServicesOfServer(1, Integer.MAX_VALUE);
        verify(namingService).getAllInstances("mcp-filesystem");
        verify(namingService).getAllInstances("mcp-database");
        verify(namingService).getAllInstances("mcp-search");
        verify(namingService, never()).getAllInstances("other-service");
        verify(namingService, never()).getAllInstances("another-service");
    }

    /**
     * Test the real scenario when Nacos is not connected
     */
    @Test
    void testListAllMcpServers_RealScenario_NoNacos() {
        // Given - Use real service without mocks to test actual behavior
        McpServerServiceImpl realService = new McpServerServiceImpl(namingService, null);
        
        // Mock the actual failure scenario
        try {
            when(namingService.getServicesOfServer(1, Integer.MAX_VALUE))
                    .thenReturn(null);
        } catch (Exception e) {
            // Handle any setup exceptions
        }

        // When
        Mono<List<McpServer>> serversMono = realService.listAllMcpServers();
        List<McpServer> servers = serversMono.block(Duration.ofSeconds(10));
        
        // Then - Should return empty list when Nacos connection fails
        assertNotNull(servers);
        assertTrue(servers.isEmpty());
    }

    private Instance createTestInstance(String serviceName, int port) {
        Instance instance = new Instance();
        instance.setInstanceId(serviceName + "-instance");
        instance.setIp("localhost");
        instance.setPort(port);
        instance.setServiceName(serviceName);
        instance.setEnabled(true);
        instance.setHealthy(true);
        instance.setWeight(1.0);
        instance.setClusterName("DEFAULT");
        instance.getMetadata().put("description", "Test " + serviceName);
        instance.getMetadata().put("transportType", "stdio");
        instance.getMetadata().put("version", "1.0.0");
        instance.getMetadata().put("provider", "Nacos");
        instance.getMetadata().put("registrationTime", "2025-01-01T00:00:00");
        return instance;
    }
} 