项目实现必须参考： https://github.com/alibaba/spring-ai-alibaba/tree/main/spring-ai-alibaba-mcp/ 


其他参考文档：
https://nacos.io/en/blog/nacos-gvr7dx_awbbpb_gg16sv97bgirkixe/?spm=5238cd80.7f2fc5d1.0.0.642e5f9aoZLhEW&source=blog
https://nacos.io/en/blog/nacos-gvr7dx_awbbpb_qdi918msnqbvonx2/?spm=5238cd80.7f2fc5d1.0.0.642e5f9aoZLhEW&source=blog
https://github.com/alibaba/spring-ai-alibaba/tree/main/spring-ai-alibaba-mcp/ 
https://modelcontextprotocol.io/sdk/java/mcp-overview
https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html
https://docs.spring.io/spring-ai/reference/api/mcp/mcp-helpers.html
https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html


mcp-server-v2 中放开tool和toolParam注释，需要声明mcp server，这是核心逻辑
mcp-server-v2启动后，通过mcp-router注册到nacos中
mcp-client调用服务时，通过mcp-router查询nacos上的注册信息，寻找对应的mcp server服务
mcp-router端口 8050， mcp-server-v2端口 8061，mcp-client端口8070，不要修改。 
项目都使用响应式编程，依赖的jar包不要使用阻塞式
编译时，使用以下命令
    mvn -f mcp-server-v2/pom.xml clean install -nsu
    mvn -f mcp-router/pom.xml clean install -nsu
    mvn -f mcp-client/pom.xml clean install -nsu

mcp-router调用mcp-server-v2使用sse协议，遵从mcp的规约，不要使用http或者https协议调用
mcp-router除了调试用的接口，不提供对外的http服务
mcp-client调用mcp-router使用sse协议，遵从mcp的规约，不要使用http或者https协议调用
mcp-server-v2除了调试用的接口，不提供对外的http服务

项目执行时，通过 sh ./run-demo.sh 运行所有项目，分析输出日志分析问题。
spring-ai-alibaba 的官方文档或一个可运行的官方示例项目，来找出本项目配置上的差异。

mcp-client验证方法如下：
    # 获取工具列表
    curl "http://localhost:8070/mcp-client/api/v1/tools/list"

    # 调用系统信息工具
    curl -X POST http://localhost:8070/mcp-client/api/v1/tools/call \
    -H "Content-Type: application/json" \
    -d '{"toolName": "get_system_info", "arguments": {}}'

    # 列出已注册的服务
    curl -X POST http://localhost:8070/mcp-client/api/v1/tools/call \
    -H "Content-Type: application/json" \
    -d '{"toolName": "list_servers", "arguments": {}}'


参考mcp-server-v2 创建 mcp-server-v1 和 mcp-servcer-v3,为了与mcp-server-v2区分开，tools的名称 要改一下，这两个项目启动之后，都要注册到 nacos上，可以通过mcp-client调用


好的，我已经详细分析了 `TODO10.md` 文档并审视了当前项目的状况。以下是我的分析和修复问题的总结：

1.  **核心技术栈**: 项目的实现**必须**参考 `spring-ai-alibaba` 的 MCP 相关实现。这是解决问题的关键。
2.  **通信协议**: 所有模块间（client -> router, router -> server）的通信**必须**使用基于 SSE (Server-Sent Events) 的 MCP 协议，**不能**使用普通的 HTTP/HTTPS 请求。
3.  **模块职责**:
    *   `mcp-server-v2`: 作为工具提供方，通过注解（`@Tool`）暴露工具，并注册到 Nacos。
    *   `mcp-router`: 作为路由中心，从 Nacos 发现服务，并为客户端提供统一的 MCP/SSE 入口。
    *   `mcp-client`: 作为服务调用方，通过 `mcp-router` 与工具服务通信。
4.  **编程模型**: 整个项目应采用响应式编程（Reactive WebFlux），避免阻塞式操作。


#### `mcp-client` (存在核心阻塞问题 ❌)
*   **问题**: 这是当前项目的**核心问题所在**。
    1.  `McpRouterService.java` 最初的实现是通过 `WebClient` 发起普通 HTTP 请求，这严重违反了“必须使用 MCP/SSE 协议”的核心规约。
    2.  修复此问题的正确思路是使用由框架自动注入的 `McpAsyncClient` Bean。但我的多次尝试均因**编译失败**而告终。
*   **失败的根源**: 编译错误的核心是“无法解析 `com.alibaba.cloud.ai` 包”。尽管项目的父 `pom.xml` 文件中引入了 `spring-ai-alibaba-bom`，但在编译 `mcp-client` 模块时，Maven 始终无法找到正确的依赖包。
*   **当前状态**: 这是一个**根本性的依赖管理或环境问题**。我已经尝试修正 `pom.xml`，将依赖切换为更符合响应式编程要求的 `spring-ai-starter-mcp-client-webflux`，但问题依旧。在编译问题解决之前，无法完成对 `McpRouterService.java` 的逻辑修复。

