package com.nacos.mcp.server.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nacos.mcp.server.model.Person;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Object> callTool(@RequestBody Map<String, Object> request) {
        try {
            String toolName = (String) request.get("toolName");
            Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");
            
            log.info("Calling tool: {} with arguments: {}", toolName, arguments);
            
            Object result = executeToolByName(toolName, arguments);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "result", result
            ));
            
        } catch (Exception e) {
            log.error("Error calling tool", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    private Object executeToolByName(String toolName, Map<String, Object> arguments) throws Exception {
        switch (toolName) {
            // PersonQueryTools
            case "getAllPersons":
                return personQueryTools.getAllPersons();
            case "getPersonById":
                Long id = getLong(arguments, "id");
                return personQueryTools.getPersonById(id);
            case "getPersonsByNationality":
                String nationality = getString(arguments, "nationality");
                return personQueryTools.getPersonsByNationality(nationality);
            case "countPersonsByNationality":
                String countNationality = getString(arguments, "nationality");
                return personQueryTools.countPersonsByNationality(countNationality);
                
            // PersonModifyTools
            case "addPerson":
                String firstName = getString(arguments, "firstName");
                String lastName = getString(arguments, "lastName");
                Integer age = getInteger(arguments, "age");
                String addNationality = getString(arguments, "nationality");
                String genderStr = getString(arguments, "gender");
                Person.Gender gender = genderStr != null ? Person.Gender.valueOf(genderStr.toUpperCase()) : Person.Gender.MALE;
                return personModifyTools.addPerson(firstName, lastName, age, addNationality, gender);
                
            case "deletePerson":
                Long deleteId = getLong(arguments, "id");
                return personModifyTools.deletePerson(deleteId);
                
            default:
                throw new RuntimeException("Tool not found: " + toolName);
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