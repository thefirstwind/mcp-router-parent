package com.nacos.mcp.server.v3.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nacos.mcp.server.v3.model.Person;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
public class ToolCallController {

    private final PersonQueryTools personQueryTools;
    private final PersonModifyTools personModifyTools;
    private final ObjectMapper objectMapper;

    @PostMapping("/call")
    public Mono<ResponseEntity<Map<String, Object>>> callTool(@RequestBody Map<String, Object> request) {
        try {
            String toolName = (String) request.get("toolName");
            Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");
            
            log.info("Calling tool: {} with arguments: {}", toolName, arguments);
            
            return executeToolByName(toolName, arguments)
                .map(result -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "result", result
                )))
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Tool execution failed"
                )));
            
        } catch (Exception e) {
            log.error("Error calling tool", e);
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            )));
        }
    }
    
    private Mono<Object> executeToolByName(String toolName, Map<String, Object> arguments) {
        switch (toolName) {
            // PersonQueryTools (using tool names with v3 suffix)
            case "getAllPersons_v3":
                return personQueryTools.getAllPersons()
                    .collectList()
                    .cast(Object.class);
            case "getPersonById_v3":
                Long id = getLong(arguments, "id");
                return personQueryTools.getPersonById(id)
                    .cast(Object.class);
            case "getPersonsByNationality_v3":
                String nationality = getString(arguments, "nationality");
                return personQueryTools.getPersonsByNationality(nationality)
                    .collectList()
                    .cast(Object.class);
            case "countByNationality_v3":
                String countNationality = getString(arguments, "nationality");
                return personQueryTools.countByNationality(countNationality)
                    .cast(Object.class);
                
            // PersonModifyTools
            case "addPerson_v3":
                String firstName = getString(arguments, "firstName");
                String lastName = getString(arguments, "lastName");
                Integer age = getInteger(arguments, "age");
                String addNationality = getString(arguments, "nationality");
                String gender = getString(arguments, "gender");
                return personModifyTools.addPerson(firstName, lastName, age, addNationality, gender)
                    .cast(Object.class);
                
            case "deletePerson_v3":
                Long deleteId = getLong(arguments, "id");
                return personModifyTools.deletePerson(deleteId)
                    .then(Mono.just("Person deleted successfully"))
                    .cast(Object.class);
                
            default:
                return Mono.error(new RuntimeException("Tool not found: " + toolName));
        }
    }
    
    private String getString(Map<String, Object> args, String key) {
        Object value = args.get(key);
        return value != null ? value.toString() : null;
    }
    
    private Long getLong(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(value.toString());
    }
    
    private Integer getInteger(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(value.toString());
    }
} 