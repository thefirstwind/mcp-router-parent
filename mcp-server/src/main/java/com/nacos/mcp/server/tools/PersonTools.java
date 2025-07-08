package com.nacos.mcp.server.tools;

import com.nacos.mcp.server.model.Person;
import com.nacos.mcp.server.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Person management tools for MCP Server
 * These methods provide person management functionality via MCP protocol
 * Methods are automatically discovered by MethodToolCallbackProvider
 */
@Service
@RequiredArgsConstructor
public class PersonTools {

    private final PersonRepository personRepository;

    /**
     * Find a person by their ID
     */
    @Tool(description = "Find a person by their ID number")
    public Person getPersonById(Long id) {
        return personRepository.findById(id).orElse(null);
    }

    /**
     * Find all persons by their nationality
     */
    @Tool(description = "Find all persons with a specific nationality")
    public List<Person> getPersonsByNationality(String nationality) {
        return personRepository.findByNationality(nationality);
    }

    /**
     * Get all persons in the database
     */
    @Tool(description = "Get all persons in the database")
    public List<Person> getAllPersons() {
        return (List<Person>) personRepository.findAll();
    }

    /**
     * Count persons by nationality
     */
    @Tool(description = "Count how many persons have a specific nationality")
    public long countPersonsByNationality(String nationality) {
        return personRepository.findByNationality(nationality).size();
    }

    /**
     * Add a new person to the database
     */
    @Tool(description = "Add a new person to the database with their personal information")
    public Person addPerson(String firstName, String lastName, int age, String nationality, Person.Gender gender) {
        Person newPerson = new Person();
        newPerson.setFirstName(firstName);
        newPerson.setLastName(lastName);
        newPerson.setAge(age);
        newPerson.setNationality(nationality);
        newPerson.setGender(gender);
        return personRepository.save(newPerson);
    }

    /**
     * Delete a person by ID
     */
    @Tool(description = "Delete a person from the database using their ID number")
    public boolean deletePerson(Long id) {
        if (personRepository.existsById(id)) {
            personRepository.deleteById(id);
            return true;
        }
        return false;
    }
} 