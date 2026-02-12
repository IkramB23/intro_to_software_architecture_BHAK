package com.bhak.project.configuration;

import com.bhak.project.entity.Role;
import com.bhak.project.entity.RoleType;
import com.bhak.project.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// insere les roles dans la base au demarrage si ils n'existent pas
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        for (RoleType type : RoleType.values()) {
            if (!roleRepository.existsByName(type)) {
                Role role = new Role();
                role.setName(type);
                role.setDescription(type.getLabel());
                roleRepository.save(role);
                log.info("Niveau d'autorisation {} initialisé", type.name());
            }
        }
    }
}
