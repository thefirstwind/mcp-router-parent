#!/bin/bash

# Test coverage script for Nacos MCP Router

echo "Running tests with coverage..."

# Run tests with JaCoCo coverage
mvn clean test jacoco:report

# Check if coverage report was generated
if [ -f "target/site/jacoco/index.html" ]; then
    echo "Coverage report generated at: target/site/jacoco/index.html"
    
    # Try to open the report (macOS)
    if command -v open &> /dev/null; then
        echo "Opening coverage report in browser..."
        open target/site/jacoco/index.html
    fi
    
    # Extract coverage summary from the report
    if command -v grep &> /dev/null && [ -f "target/site/jacoco/index.html" ]; then
        echo "Coverage Summary:"
        grep -o "Total.*%" target/site/jacoco/index.html | head -1 || echo "Could not extract coverage percentage"
    fi
else
    echo "Coverage report not found. Make sure JaCoCo plugin is configured in pom.xml"
fi

echo "Test coverage analysis completed!" 

# Test coverage script for Nacos MCP Router

echo "Running tests with coverage..."

# Run tests with JaCoCo coverage
mvn clean test jacoco:report

# Check if coverage report was generated
if [ -f "target/site/jacoco/index.html" ]; then
    echo "Coverage report generated at: target/site/jacoco/index.html"
    
    # Try to open the report (macOS)
    if command -v open &> /dev/null; then
        echo "Opening coverage report in browser..."
        open target/site/jacoco/index.html
    fi
    
    # Extract coverage summary from the report
    if command -v grep &> /dev/null && [ -f "target/site/jacoco/index.html" ]; then
        echo "Coverage Summary:"
        grep -o "Total.*%" target/site/jacoco/index.html | head -1 || echo "Could not extract coverage percentage"
    fi
else
    echo "Coverage report not found. Make sure JaCoCo plugin is configured in pom.xml"
fi

echo "Test coverage analysis completed!" 