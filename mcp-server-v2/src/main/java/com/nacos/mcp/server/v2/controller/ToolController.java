package com.nacos.mcp.server.v2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nacos.mcp.server.v2.model.Person;
import com.nacos.mcp.server.v2.tools.PersonModifyTools;
import com.nacos.mcp.server.v2.tools.PersonQueryTools;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/mcp-server-v2/tools")
@RequiredArgsConstructor
public class ToolController {

    private final PersonQueryTools personQueryTools;
    private final PersonModifyTools personModifyTools;
    private final ObjectMapper objectMapper;

    @PostMapping("/call")
    public Object callTool(@RequestBody Map<String, Object> payload) {
        String toolName = (String) payload.get("toolName");
        Map<String, Object> arguments = (Map<String, Object>) payload.get("arguments");

        switch (toolName) {
            case "personQueryTools.getAllPersons":
                return personQueryTools.getAllPersons();
            case "personQueryTools.getPersonById":
                return personQueryTools.getPersonById(((Number) arguments.get("id")).longValue());
            case "personQueryTools.getPersonsByNationality":
                return personQueryTools.getPersonsByNationality((String) arguments.get("nationality"));
            case "personQueryTools.countByNationality":
                return personQueryTools.countByNationality((String) arguments.get("nationality"));
            case "personModifyTools.addPerson":
                Person person = objectMapper.convertValue(arguments, Person.class);
                return personModifyTools.addPerson(person.getFirstName(), person.getLastName(), person.getAge(), person.getNationality(), person.getGender().name());
            case "personModifyTools.deletePerson":
                personModifyTools.deletePerson(((Number) arguments.get("id")).longValue());
                return Map.of("success", true);
            default:
                throw new IllegalArgumentException("Unknown tool: " + toolName);
        }
    }
} 