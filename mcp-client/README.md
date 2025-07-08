# MCP Client

MCP Client模块通过**MCP Router**访问所有已注册的MCP服务器工具，实现了标准的MCP客户端功能。

## 🏗️ 架构设计

### 新架构 (修改后)
```
[mcp-client] → [mcp-router:8050] → [person-mcp-server:8060]
     ↓               ↓                      ↓
[ChatClient]   [JSON-RPC 2.0]          [Spring AI Tools]
     ↓               ↓                      ↓
[DeepSeek AI]   [工具整合路由]           [PersonTools]
```

### 关键改进
- ✅ **统一路由**: 通过MCP Router访问所有工具，而不是直接连接各个MCP服务器
- ✅ **工具整合**: MCP Router整合所有已注册服务器的工具列表
- ✅ **AI增强**: 使用ChatClient自动选择和调用合适的MCP工具
- ✅ **回退机制**: 当MCP工具不可用时，自动回退到AI聊天

## 📋 配置说明

### application.yml
```yaml
spring:
  ai:
    deepseek:
      api-key: "your-api-key"
      base-url: "https://api.deepseek.com"
    mcp:
      client:
        enabled: true
        sse:
          connections:
            nacos-mcp-router:
              url: http://localhost:8050/mcp/jsonrpc  # 连接到MCP Router
              request-timeout: 30s
              transport: sse
```

## 🔧 核心组件

### McpRouterService
负责通过MCP Router访问工具的核心服务：

```java
@Service
public class McpRouterService {
    private final ChatClient chatClient;
    
    // 通过ChatClient自动调用MCP工具
    public Mono<String> getPersonsByNationality(String nationality) {
        String prompt = "Find all persons with " + nationality + 
                       " nationality. Use the getPersonsByNationality tool if available.";
        return executeWithChatClient(prompt);
    }
}
```

### PersonController
提供RESTful API接口：

```java
@RestController
@RequestMapping("/persons")
public class PersonController {
    private final McpRouterService mcpRouterService;
    
    @GetMapping("/nationality/{nationality}")
    public Mono<String> findByNationality(@PathVariable String nationality) {
        return mcpRouterService.getPersonsByNationality(nationality);
    }
}
```

## 🚀 启动说明

### 1. 启动顺序
```bash
# 1. 启动Nacos (端口8848)
# 2. 启动mcp-server (端口8060) - 注册到Nacos
# 3. 启动mcp-router (端口8050) - 发现mcp-server
# 4. 启动mcp-client (端口8070) - 连接到mcp-router
```

### 2. 启动mcp-client
```bash
cd mcp-client
mvn spring-boot:run -Dserver.port=8070
```

## 📡 API端点

### Person管理
- `GET /persons/nationality/{nationality}` - 根据国籍查找人员
- `GET /persons/count-by-nationality/{nationality}` - 统计国籍人数
- `GET /persons/all` - 获取所有人员
- `GET /persons/{id}` - 根据ID查找人员
- `POST /persons` - 添加新人员
- `DELETE /persons/{id}` - 删除人员
- `POST /persons/query` - 自定义查询

### MCP管理
- `GET /persons/mcp/tools` - 获取可用工具列表
- `GET /persons/mcp/servers` - 列出已注册的MCP服务器
- `GET /persons/mcp/system-info` - 获取系统信息
- `GET /persons/mcp/status` - 检查MCP连接状态

## 🧪 测试示例

### 1. 查找美国人员
```bash
curl http://localhost:8070/persons/nationality/American
```

### 2. 统计中国人员数量
```bash
curl http://localhost:8070/persons/count-by-nationality/Chinese
```

### 3. 添加新人员
```bash
curl -X POST http://localhost:8070/persons \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe", 
    "age": 30,
    "nationality": "American",
    "gender": "MALE"
  }'
```

### 4. 自定义查询
```bash
curl -X POST http://localhost:8070/persons/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Find all persons older than 25 and group them by nationality"
  }'
```

### 5. 检查MCP状态
```bash
curl http://localhost:8070/persons/mcp/status
```

## 🔍 工作原理

### 1. **工具发现**
- mcp-client连接到mcp-router的JSON-RPC端点
- mcp-router返回所有已注册服务器的工具列表
- ChatClient自动了解可用的工具

### 2. **工具调用**
- 用户发送请求到mcp-client
- mcp-client构造包含工具提示的prompt
- ChatClient自动选择合适的MCP工具
- 通过mcp-router路由到相应的mcp-server
- 返回结果给用户

### 3. **智能回退**
- 如果MCP工具不可用，ChatClient自动回退到AI聊天
- 确保用户始终能得到有用的响应

## 🔧 开发说明

### 添加新的工具接口
1. 在`McpRouterService`中添加新方法
2. 在`PersonController`中添加对应的端点
3. 确保prompt中包含正确的工具名称

### 自定义AI模型
可以在配置中更换AI模型：
```yaml
spring:
  ai:
    openai:  # 替换为OpenAI
      api-key: "your-openai-key"
```

## 📈 性能优化

- ✅ **响应式编程**: 使用Reactor Mono支持异步处理
- ✅ **连接池**: MCP连接自动管理和重用
- ✅ **超时控制**: 配置合理的请求超时时间
- ✅ **错误处理**: 完整的异常处理和回退机制

## 🚧 故障排除

### 1. MCP连接失败
检查mcp-router是否运行在8050端口：
```bash
curl http://localhost:8050/health
```

### 2. 工具调用失败
检查mcp-server是否正确注册到Nacos：
```bash
curl http://localhost:8070/persons/mcp/servers
```

### 3. AI回退过多
可能是MCP工具名称不匹配，检查可用工具：
```bash
curl http://localhost:8070/persons/mcp/tools
```

## 📚 相关文档

- [MCP Router README](../mcp-router/README.md)
- [MCP Server README](../mcp-server/README.md)
- [项目总体说明](../README.md) 