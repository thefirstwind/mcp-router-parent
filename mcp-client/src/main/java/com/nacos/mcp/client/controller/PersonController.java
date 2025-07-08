package com.nacos.mcp.client.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/persons")
@Slf4j
public class PersonController {

    private final ChatClient chatClient;

    public PersonController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .build();
    }

    @GetMapping("/nationality/{nationality}")
    public String findByNationality(@PathVariable String nationality) {
        log.info("Finding persons with nationality: {}", nationality);
        
        PromptTemplate pt = new PromptTemplate("""
                Find all persons with {nationality} nationality.
                List their names, ages, and any other relevant information.
                """);
        Prompt prompt = pt.create(Map.of("nationality", nationality));
        
        String response = this.chatClient.prompt(prompt)
                .call()
                .content();
                
        log.info("AI Response: {}", response);
        return response;
    }

    @GetMapping("/count-by-nationality/{nationality}")
    public String countByNationality(@PathVariable String nationality) {
        log.info("Counting persons with nationality: {}", nationality);
        
        PromptTemplate pt = new PromptTemplate("""
                How many persons are from {nationality}? 
                Please provide the exact count and list their names.
                """);
        Prompt prompt = pt.create(Map.of("nationality", nationality));
        
        String response = this.chatClient.prompt(prompt)
                .call()
                .content();
                
        log.info("AI Response: {}", response);
        return response;
    }

    @GetMapping("/all")
    public String getAllPersons() {
        log.info("Getting all persons");
        
        String prompt = "List all persons in the database with their details including name, age, nationality, and gender.";
        
        String response = this.chatClient.prompt(prompt)
                .call()
                .content();
                
        log.info("AI Response: {}", response);
        return response;
    }

    @PostMapping("/query")
    public String queryPersons(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        log.info("Processing custom query: {}", query);
        
        String response = this.chatClient.prompt(query)
                .call()
                .content();
                
        log.info("AI Response: {}", response);
        return response;
    }
} 