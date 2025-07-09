#!/bin/bash
set -e

# 🚀 MCP Router 改造演示脚本
# 此脚本演示改造后的 MCP Router 系统功能

# Function to check if a port is in use
is_port_in_use() {
    lsof -i:$1 > /dev/null
    return $?
}

# Kill processes using specified ports
kill_process_on_port() {
    if is_port_in_use $1; then
        echo "Port $1 is in use. Killing the process..."
        lsof -t -i:$1 | xargs kill -9
    fi
}

# mcp-router port, mcp-server port
PORTS=(8080 8081)

# Kill existing processes on the ports
for port in "${PORTS[@]}"; do
    kill_process_on_port $port
done

echo "🚀 MCP Router 改造演示"
echo "========================"

# 设置颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查 Java 版本
echo -e "${BLUE}📋 检查环境...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java 未安装，请安装 Java 17+${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt "17" ]; then
    echo -e "${RED}❌ 需要 Java 17 或更高版本，当前版本: $JAVA_VERSION${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Java 版本检查通过: $(java -version 2>&1 | head -1)${NC}"

# 编译和打包项目
echo -e "${BLUE}🔨 编译和打包项目...${NC}"
mvn clean package -DskipTests
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 项目打包成功${NC}"
else
    echo -e "${RED}❌ 项目打包失败${NC}"
    exit 1
fi

# 启动 MCP Server
echo -e "${BLUE}🖥️  启动 MCP Server (端口 8081)...${NC}"
java -jar -Dserver.port=8081 mcp-server/target/mcp-server-1.0.0.jar > logs/mcp-server-demo.log 2>&1 &
MCP_SERVER_PID=$!

# 等待服务启动
echo -e "${YELLOW}⏳ 等待 MCP Server 启动...${NC}"
sleep 10

# 检查 MCP Server 是否启动成功
if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ MCP Server 启动成功 (PID: $MCP_SERVER_PID)${NC}"
else
    echo -e "${YELLOW}⚠️  MCP Server 健康检查失败，继续启动 Router...${NC}"
fi

# 启动 MCP Router
echo -e "${BLUE}🔀 启动 MCP Router (端口 8080)...${NC}"
java -jar -Dserver.port=8080 mcp-router/target/nacos-mcp-router-1.0.0.jar > logs/mcp-router-demo.log 2>&1 &
MCP_ROUTER_PID=$!

# 等待服务启动
echo -e "${YELLOW}⏳ 等待 MCP Router 启动...${NC}"
sleep 15

# 检查 MCP Router 是否启动成功
if curl -s http://localhost:8080/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ MCP Router 启动成功 (PID: $MCP_ROUTER_PID)${NC}"
else
    echo -e "${RED}❌ MCP Router 启动失败${NC}"
    kill $MCP_SERVER_PID $MCP_ROUTER_PID 2>/dev/null
    exit 1
fi

echo ""
echo -e "${GREEN}🎉 系统启动完成！${NC}"
echo "========================"
echo ""

# 演示 MCP 协议功能
echo -e "${BLUE}📡 演示 MCP JSON-RPC 协议...${NC}"
echo ""

# 1. 测试 initialize 方法
echo -e "${YELLOW}1️⃣  测试 MCP initialize 握手...${NC}"
INIT_RESPONSE=$(curl -s -X POST http://localhost:8080/mcp/jsonrpc \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "capabilities": {},
      "clientInfo": {
        "name": "demo-client",
        "version": "1.0.0"
      }
    }
  }')

if echo "$INIT_RESPONSE" | grep -q "protocolVersion"; then
    echo -e "${GREEN}✅ MCP 握手成功${NC}"
    echo "   服务器信息: $(echo "$INIT_RESPONSE" | jq -r '.result.serverInfo.name')"
else
    echo -e "${RED}❌ MCP 握手失败${NC}"
fi

echo ""

# 2. 测试 tools/list 方法
echo -e "${YELLOW}2️⃣  测试工具列表...${NC}"
TOOLS_RESPONSE=$(curl -s -X POST http://localhost:8080/mcp/jsonrpc \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list",
    "params": {}
  }')

if echo "$TOOLS_RESPONSE" | grep -q "tools"; then
    TOOL_COUNT=$(echo "$TOOLS_RESPONSE" | jq '.result.tools | length')
    echo -e "${GREEN}✅ 发现 $TOOL_COUNT 个可用工具${NC}"
    echo "$TOOLS_RESPONSE" | jq -r '.result.tools[] | "   - \(.name): \(.description)"'
else
    echo -e "${RED}❌ 获取工具列表失败${NC}"
fi

echo ""

# 3. 测试内置工具调用
echo -e "${YELLOW}3️⃣  测试内置工具: get_system_info...${NC}"
TOOL_RESPONSE=$(curl -s -X POST http://localhost:8080/mcp/jsonrpc \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "get_system_info",
      "arguments": {}
    }
  }')

if echo "$TOOL_RESPONSE" | grep -q "Nacos MCP Router"; then
    echo -e "${GREEN}✅ 系统信息工具调用成功${NC}"
    echo "$TOOL_RESPONSE" | jq -r '.result.content[0].text' | head -5
else
    echo -e "${RED}❌ 系统信息工具调用失败${NC}"
fi

echo ""

# 4. 测试服务器列表工具
echo -e "${YELLOW}4️⃣  测试服务器列表工具...${NC}"
LIST_SERVERS_RESPONSE=$(curl -s -X POST http://localhost:8080/mcp/jsonrpc \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "tools/call",
    "params": {
      "name": "list_servers",
      "arguments": {}
    }
  }')

if echo "$LIST_SERVERS_RESPONSE" | grep -q "Available MCP Servers"; then
    echo -e "${GREEN}✅ 服务器列表工具调用成功${NC}"
    echo "$LIST_SERVERS_RESPONSE" | jq -r '.result.content[0].text'
else
    echo -e "${RED}❌ 服务器列表工具调用失败${NC}"
fi

echo ""

# 演示传统 REST API 兼容性
echo -e "${BLUE}🔄 演示向后兼容的 REST API...${NC}"

# 测试健康检查
echo -e "${YELLOW}5️⃣  测试健康检查 API...${NC}"
HEALTH_RESPONSE=$(curl -s http://localhost:8080/health)
if echo "$HEALTH_RESPONSE" | grep -q "UP"; then
    echo -e "${GREEN}✅ 系统健康状态正常${NC}"
else
    echo -e "${YELLOW}⚠️  健康检查响应: $HEALTH_RESPONSE${NC}"
fi

echo ""

# 显示日志信息
echo -e "${BLUE}📋 系统状态信息${NC}"
echo "========================"
echo -e "MCP Server PID: ${GREEN}$MCP_SERVER_PID${NC}"
echo -e "MCP Router PID: ${GREEN}$MCP_ROUTER_PID${NC}"
echo -e "MCP Router URL: ${GREEN}http://localhost:8080${NC}"
echo -e "MCP Server URL: ${GREEN}http://localhost:8081${NC}"
echo -e "JSON-RPC 端点: ${GREEN}http://localhost:8080/mcp/jsonrpc${NC}"
echo ""

# 提供交互选项
echo -e "${BLUE}🎮 交互选项${NC}"
echo "========================"
echo "1. 查看 MCP Router 日志: tail -f logs/mcp-router-demo.log"
echo "2. 查看 MCP Server 日志: tail -f logs/mcp-server-demo.log"
echo "3. 测试自定义 MCP 工具调用:"
echo "   curl -X POST http://localhost:8080/mcp/jsonrpc \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -d '{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\",\"params\":{\"name\":\"ping_server\",\"arguments\":{\"serverName\":\"test\"}}}'"
echo ""
echo "4. 停止演示系统:"
echo "   kill $MCP_SERVER_PID $MCP_ROUTER_PID"
echo ""

echo -e "${GREEN}🎉 MCP Router 改造演示完成！${NC}"
echo ""
echo -e "${YELLOW}💡 提示:${NC}"
echo "- 这个系统现在完全符合 MCP JSON-RPC 2.0 协议标准"
echo "- 可以与标准 MCP 客户端 (如 Claude Desktop) 进行通信"
echo "- 支持动态工具注册和服务发现"
echo "- 提供企业级的监控和错误处理机制"
echo ""
echo -e "${BLUE}📚 详细信息请查看: TRANSFORMATION_SUMMARY.md${NC}" 