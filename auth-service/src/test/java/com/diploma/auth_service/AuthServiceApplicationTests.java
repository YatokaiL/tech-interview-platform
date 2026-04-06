package com.diploma.auth_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Requires running PostgreSQL/Kafka/Redis — enable in CI with TestContainers")
@SpringBootTest
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
