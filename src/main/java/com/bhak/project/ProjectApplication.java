package com.bhak.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Spring Boot application (main authentication service).
 *
 * <h2>How it works</h2>
 * This class starts the Spring container via {@code SpringApplication.run()}.
 * The composite annotation {@code @SpringBootApplication} enables:
 * <ul>
 *   <li>{@code @Configuration} – declares the class as a bean source.</li>
 *   <li>{@code @EnableAutoConfiguration} – automatically configures dependencies
 *       detected on the classpath (Spring Security, Spring Data JPA, RabbitMQ, etc.).</li>
 *   <li>{@code @ComponentScan} – recursively scans the {@code com.bhak.project} package
 *       to register components ({@code @Service}, {@code @Repository},
 *       {@code @Controller}, {@code @Component}).</li>
 * </ul>
 *
 * <h2>Technologies used</h2>
 * Spring Boot 3, Spring Security (stateless JWT), Spring Data JPA (PostgreSQL),
 * Spring AMQP (RabbitMQ), SpringDoc/OpenAPI (Swagger UI), Lombok.
 *
 * <h2>Overall architecture</h2>
 * This microservice handles registration, login (JWT), email verification,
 * and admin CRUD operations on accounts. It publishes business events to RabbitMQ,
 * consumed by the {@code notification-service} (email sending via MailHog).
 */
@SpringBootApplication
public class ProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectApplication.class, args);
	}

}
