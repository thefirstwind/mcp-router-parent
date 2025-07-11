# TODO11.md - MCP Router 项目深度分析报告

## 📋 项目概述

**项目名称**: MCP Router Parent  
**技术栈**: Spring Boot 3.2.5, Spring Cloud Alibaba, Nacos, Spring AI, WebFlux  
**架构模式**: 微服务 + 响应式编程 + MCP 协议  
**分析时间**: 2025年7月11日  

## 🏗️ 项目结构分析

### 模块组成
```
mcp-router-parent/
├── mcp-client (端口 8070)          # MCP 客户端
├── mcp-router (端口 8050)          # MCP 路由器
├── mcp-server-v1 (端口 8060)       # MCP 服务器 V1
├── mcp-server-v2 (端口 8061)       # MCP 服务器 V2  
├── mcp-server-v3 (端口 8062)       # MCP 服务器 V3
└── mcp-server (已废弃)             # 废弃的服务器
```

### 依赖关系
- **服务发现**: 基于 Nacos 的服务注册与发现
- **通信协议**: 混合模式 (HTTP + SSE + MCP协议)
- **数据库**: H2 内存数据库 (R2DBC响应式驱动)
- **工具框架**: Spring AI Tool Annotations

## 🔍 技术实现分析

### 1. MCP 服务器实现 ✅

**优点**:
- 使用标准的 Spring AI MCP 依赖
- 正确实现了 `@Tool` 和 `@ToolParam` 注解
- SSE 端点正确暴露 (`/sse`)
- 工具自动注册到 Nacos 元数据
- 响应式数据库操作 (R2DBC)

**配置示例** (mcp-server-v2):
```yaml
spring:
  ai:
    mcp:
      server:
        name: "mcp-server-v2"
        transport-type: sse
        endpoint: /sse
        type: ASYNC
```

**工具实现**:
```java
@Tool(name = "getAllPersons", description = "Get a list of all persons in the repository")
public Flux<Person> getAllPersons() {
    return personRepository.findAll();
}
```

### 2. MCP 路由器实现 ⚠️

**当前架构**:
- 同时提供 JSON-RPC 和 SSE 端点
- 支持服务发现和智能路由
- 实现了工具调用的代理功能

**关键问题**:
```java
// 问题：仍在使用 HTTP POST 调用工具
return webClient.post()
    .uri("/tools/call")  // ❌ 违反 MCP 协议
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(Map.of("toolName", toolName, "arguments", params))
```

**正确的实现应该是**:
- 使用 MCP 协议的 SSE 通信
- 遵循 JSON-RPC over SSE 规范
- 维护 MCP 会话状态

### 3. MCP 客户端实现 ❌

**核心问题**:
```java
// 当前实现：直接 HTTP 调用
return webClient.post()
    .uri("/mcp/jsonrpc")
    .bodyValue(requestBody)
    .retrieve()
    .bodyToMono(String.class);
```

**应该使用的方式**:
```java
// 正确实现：使用 McpAsyncClient
@Bean
public List<McpFunctionCallback> functionCallbacks(McpSyncClient mcpClient) {
    return mcpClient.listTools(null)
        .tools()
        .stream()
        .map(tool -> new McpFunctionCallback(mcpClient, tool))
        .toList();
}
```

## 🧪 功能验证结果

### ✅ 成功验证的功能

1. **服务注册与发现**
   - 所有服务成功注册到 Nacos
   - 服务健康检查正常
   - 工具元数据正确同步

2. **工具发现**
   - 成功获取 21 个工具
   - 包含所有版本的 Person 操作工具
   - 工具描述和参数定义完整

3. **SSE 连接**
   - SSE 端点可正常建立连接
   - 心跳机制工作正常
   - 连接管理功能完善

### ❌ 失败的功能

1. **工具调用执行**
   ```bash
   # 测试结果
   curl -X POST http://localhost:8070/mcp-client/api/v1/tools/call \
   -d '{"toolName": "getAllPersons", "arguments": {}}'
   
   # 错误响应
   {"error": "404 Not Found from POST http://192.168.0.102:8061/mcp-server-v2/tools/call"}
   ```

2. **数据库操作**
   - 无法通过 MCP 协议操作数据库
   - 用户数据的增删改查功能不可用

## 🔧 核心问题分析

### 1. 协议违规问题

**TODO10.md 明确要求**:
> mcp-router调用mcp-server-v2使用sse协议，遵从mcp的规约，不要使用http或者https协议调用

**当前违规行为**:
- `mcp-router` 使用 HTTP POST 调用工具
- `mcp-client` 使用 HTTP 而非 MCP 客户端
- 缺少真正的 MCP 协议实现

### 2. 架构不一致

**期望的架构**:
```
mcp-client --[MCP/SSE]--> mcp-router --[MCP/SSE]--> mcp-server
```

**实际的架构**:
```
mcp-client --[HTTP]--> mcp-router --[HTTP]--> mcp-server
```

### 3. 依赖配置问题

**mcp-client 当前依赖**:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client-webflux</artifactId>
</dependency>
```

**但实际未使用 McpAsyncClient 进行通信**

## 🎯 修复建议

### 1. 高优先级修复

#### 修复 mcp-client
```java
// 替换当前的 WebClient 实现
@Bean
public McpSyncClient mcpClient() {
    var sseParams = SseServerTransport.builder("http://localhost:8050/sse")
        .build();
    return McpClient.sync(new SseServerTransport(sseParams), 
                         Duration.ofSeconds(10), 
                         new ObjectMapper());
}
```

#### 修复 mcp-router
```java
// 实现真正的 MCP 协议路由
@Service
public class McpProxyService {
    
    private final Map<String, McpSyncClient> serverClients = new HashMap<>();
    
    public Mono<Object> callTool(String toolName, Map<String, Object> arguments) {
        // 使用 MCP 客户端调用服务器
        return findServerForTool(toolName)
            .flatMap(client -> client.callTool(toolName, arguments));
    }
}
```

### 2. 配置修复

#### mcp-client 配置
```yaml
spring:
  ai:
    mcp:
      client:
        sse:
          connections:
            mcp-router:
              url: http://localhost:8050
```

#### mcp-router 配置
```yaml
spring:
  ai:
    mcp:
      client:
        sse:
          connections:
            server-v1:
              url: http://localhost:8060
            server-v2:  
              url: http://localhost:8061
            server-v3:
              url: http://localhost:8062
```

## 📊 当前状态统计

### 运行状态
- **运行中的服务**: 5 个 (router, client, server-v1, server-v2, server-v3)
- **健康检查**: 全部通过
- **可用工具数**: 21 个
- **注册的服务器**: 3 个

### 功能完成度
- **服务发现**: 100% ✅
- **工具注册**: 100% ✅  
- **SSE 连接**: 100% ✅
- **工具调用**: 0% ❌
- **数据库操作**: 0% ❌
- **MCP 协议合规**: 20% ❌

## 🚀 下一步行动计划

### Phase 1: 协议修复 (高优先级)
1. 重构 `mcp-client` 使用 `McpAsyncClient`
2. 修复 `mcp-router` 的工具调用实现
3. 确保所有通信遵循 MCP 协议

### Phase 2: 功能验证
1. 验证 `getAllPersons` 工具调用
2. 验证 `addPerson` 数据库操作
3. 验证跨版本工具调用

### Phase 3: 性能优化
1. 优化 SSE 连接管理
2. 实现连接池和重连机制
3. 添加监控和指标收集

## 📝 结论

项目具备了良好的基础架构和服务发现能力，但**核心的 MCP 协议实现严重偏离标准**。当前实现更像是一个基于 HTTP 的工具调用框架，而非真正的 MCP 系统。

**主要差距**:
1. 通信协议不符合 MCP 规范
2. 客户端实现未使用标准的 MCP 客户端库
3. 工具调用链路完全基于 HTTP 而非 SSE

**修复成功的关键**:
- 严格遵循 spring-ai-alibaba 的 MCP 实现模式
- 使用标准的 MCP 客户端库进行通信
- 确保所有模块间通信使用 SSE/MCP 协议

完成这些修复后，项目将成为一个真正符合 MCP 标准的分布式工具调用系统。 