package com.nacos.mcp.server.v3.tools;

import com.nacos.mcp.server.v3.model.Person;
import com.nacos.mcp.server.v3.repository.PersonRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class PersonQueryTools {

    private final PersonRepository personRepository;

    public PersonQueryTools(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Tool(name = "getPersonById_v3", description = "Get a person by their ID (v3)")
    public Mono<Person> getPersonById(
        @ToolParam(description = "The ID of the person to retrieve")
        Long id
    ) {
        return personRepository.findById(id);
    }

    @Tool(name = "getPersonsByNationality_v3", description = "Get all persons with a specific nationality (v3)")
    public Flux<Person> getPersonsByNationality(
        @ToolParam(description = "The nationality to filter by")
        String nationality
    ) {
        return personRepository.findByNationality(nationality);
    }

    @Tool(name = "getAllPersons_v3", description = "Get a list of all persons in the repository (v3)")
    public Flux<Person> getAllPersons() {
        return personRepository.findAll();
    }

    @Tool(name = "countByNationality_v3", description = "Count the number of persons with a specific nationality (v3)")
    public Mono<Integer> countByNationality(
        @ToolParam(description = "The nationality to count")
        String nationality
    ) {
        return personRepository.countByNationality(nationality);
    }
}
