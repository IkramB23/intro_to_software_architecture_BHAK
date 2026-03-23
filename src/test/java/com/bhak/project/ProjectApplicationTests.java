package com.bhak.project;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test verifying that the Spring Boot context starts correctly.
 *
 * <h2>Purpose</h2>
 * Ensures that the entire Spring configuration (beans, auto-configuration,
 * database connection, RabbitMQ, etc.) loads without errors. If this test fails,
 * it usually means a configuration is missing or invalid.
 *
 * <h2>Technologies</h2>
 * {@code @SpringBootTest} (loads the full context), JUnit 5.
 */
@SpringBootTest
class ProjectApplicationTests {

	@Test
	void contextLoads() {
	}

}
