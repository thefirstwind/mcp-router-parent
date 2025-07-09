package com.nacos.mcp.server.v2.repository;

import com.nacos.mcp.server.v2.model.Person;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonRepository extends CrudRepository<Person, Long> {
    List<Person> findByNationality(String nationality);
    int countByNationality(String nationality);
}
