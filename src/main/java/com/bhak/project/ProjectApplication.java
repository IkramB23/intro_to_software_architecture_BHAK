package com.bhak.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principale de l'application Spring Boot
 * 
 * @SpringBootApplication combine :
 * - @Configuration : Indique que c'est une classe de configuration
 * - @EnableAutoConfiguration : Active l'auto-configuration de Spring Boot
 * - @ComponentScan : Scanne les composants dans ce package et ses sous-packages
 */
@SpringBootApplication
public class ProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectApplication.class, args);
	}

}
