package com.nacos.mcp.server.v1.tools;

import com.nacos.mcp.server.v1.model.Person;
import com.nacos.mcp.server.v1.repository.PersonRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class PersonModifyTools {

    private final PersonRepository personRepository;

    public PersonModifyTools(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Tool(name = "addPerson_v1", description = "Add a new person to the repository (v1)")
    public Mono<Person> addPerson(
        @ToolParam(description = "The first name of the person")
        String firstName,
        @ToolParam(description = "The last name of the person")
        String lastName,
        @ToolParam(description = "The age of the person")
        int age,
        @ToolParam(description = "The nationality of the person")
        String nationality,
        @ToolParam(description = "The gender of the person")
        String gender
    ) {
        Person newPerson = new Person();
        newPerson.setFirstName(firstName);
        newPerson.setLastName(lastName);
        newPerson.setAge(age);
        newPerson.setNationality(nationality);
        newPerson.setGender(Person.Gender.valueOf(gender));
        return personRepository.save(newPerson);
    }

    @Tool(name = "deletePerson_v1", description = "Delete a person by their ID (v1)")
    public Mono<Void> deletePerson(
        @ToolParam(description = "The ID of the person to delete")
        Long id
    ) {
        return personRepository.deleteById(id);
    }
}
