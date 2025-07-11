package com.nacos.mcp.server.v2.tools;

import com.nacos.mcp.server.v2.model.Person;
import com.nacos.mcp.server.v2.repository.PersonRepository;
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

    @Tool(name = "getPersonById", description = "Get a person by their ID")
    public Mono<Person> getPersonById(
        @ToolParam(description = "The ID of the person to retrieve")
        Long id
    ) {
        return personRepository.findById(id);
    }

    @Tool(name = "getPersonsByNationality", description = "Get all persons with a specific nationality")
    public Flux<Person> getPersonsByNationality(
        @ToolParam(description = "The nationality to filter by")
        String nationality
    ) {
        return personRepository.findByNationality(nationality);
    }

    @Tool(name = "getAllPersons", description = "Get a list of all persons in the repository")
    public Flux<Person> getAllPersons() {
        return personRepository.findAll();
    }

    @Tool(name = "countByNationality", description = "Count the number of persons with a specific nationality")
    public Mono<Integer> countByNationality(
        @ToolParam(description = "The nationality to count")
        String nationality
    ) {
        return personRepository.countByNationality(nationality);
    }
}
