#!/bin/bash

# Test script for Nacos MCP Router Spring Application

echo "Testing Nacos MCP Router Application..."

# Configuration
BASE_URL="http://localhost:8000"
API_BASE="$BASE_URL/api"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    if [ $2 -eq 0 ]; then
        echo -e "${GREEN}✓ $1${NC}"
    else
        echo -e "${RED}✗ $1${NC}"
    fi
}

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local expected_status=$4
    
    echo -e "${YELLOW}Testing: $method $endpoint${NC}"
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "%{http_code}" -X "$method" "$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    else
        response=$(curl -s -w "%{http_code}" -X "$method" "$endpoint")
    fi
    
    status_code="${response: -3}"
    body="${response%???}"
    
    if [ "$status_code" = "$expected_status" ]; then
        print_status "$method $endpoint - Status: $status_code" 0
        echo "Response: $body"
    else
        print_status "$method $endpoint - Expected: $expected_status, Got: $status_code" 1
        echo "Response: $body"
    fi
    
    echo ""
}

# Wait for application to start
echo "Waiting for application to start..."
sleep 5

# Test health endpoint
test_endpoint "GET" "$BASE_URL/actuator/health" "" "200"

# Test listing servers (should be empty initially)
test_endpoint "GET" "$API_BASE/mcp/servers" "" "200"

# Test registering a server
registration_data='{
  "serverName": "mcp-test-server",
  "ip": "localhost",
  "port": 9001,
  "transportType": "stdio",
  "description": "Test MCP server for testing purposes",
  "version": "1.0.0",
  "enabled": true,
  "weight": 1.0
}'

test_endpoint "POST" "$API_BASE/mcp/register" "$registration_data" "200"

# Test listing servers again (should have one server)
test_endpoint "GET" "$API_BASE/mcp/servers" "" "200"

# Test search functionality
search_data='{
  "taskDescription": "find servers",
  "limit": 10,
  "minSimilarity": 0.5
}'

test_endpoint "POST" "$API_BASE/search" "$search_data" "200"

# Test unregistering the server
test_endpoint "DELETE" "$API_BASE/mcp/unregister/mcp-test-server" "" "200"

# Test listing servers again (should be empty)
test_endpoint "GET" "$API_BASE/mcp/servers" "" "200"

echo -e "${GREEN}Testing completed!${NC}" 

# Test script for Nacos MCP Router Spring Application

echo "Testing Nacos MCP Router Application..."

# Configuration
BASE_URL="http://localhost:8000"
API_BASE="$BASE_URL/api"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    if [ $2 -eq 0 ]; then
        echo -e "${GREEN}✓ $1${NC}"
    else
        echo -e "${RED}✗ $1${NC}"
    fi
}

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local expected_status=$4
    
    echo -e "${YELLOW}Testing: $method $endpoint${NC}"
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "%{http_code}" -X "$method" "$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    else
        response=$(curl -s -w "%{http_code}" -X "$method" "$endpoint")
    fi
    
    status_code="${response: -3}"
    body="${response%???}"
    
    if [ "$status_code" = "$expected_status" ]; then
        print_status "$method $endpoint - Status: $status_code" 0
        echo "Response: $body"
    else
        print_status "$method $endpoint - Expected: $expected_status, Got: $status_code" 1
        echo "Response: $body"
    fi
    
    echo ""
}

# Wait for application to start
echo "Waiting for application to start..."
sleep 5

# Test health endpoint
test_endpoint "GET" "$BASE_URL/actuator/health" "" "200"

# Test listing servers (should be empty initially)
test_endpoint "GET" "$API_BASE/mcp/servers" "" "200"

# Test registering a server
registration_data='{
  "serverName": "mcp-test-server",
  "ip": "localhost",
  "port": 9001,
  "transportType": "stdio",
  "description": "Test MCP server for testing purposes",
  "version": "1.0.0",
  "enabled": true,
  "weight": 1.0
}'

test_endpoint "POST" "$API_BASE/mcp/register" "$registration_data" "200"

# Test listing servers again (should have one server)
test_endpoint "GET" "$API_BASE/mcp/servers" "" "200"

# Test search functionality
search_data='{
  "taskDescription": "find servers",
  "limit": 10,
  "minSimilarity": 0.5
}'

test_endpoint "POST" "$API_BASE/search" "$search_data" "200"

# Test unregistering the server
test_endpoint "DELETE" "$API_BASE/mcp/unregister/mcp-test-server" "" "200"

# Test listing servers again (should be empty)
test_endpoint "GET" "$API_BASE/mcp/servers" "" "200"

echo -e "${GREEN}Testing completed!${NC}" 