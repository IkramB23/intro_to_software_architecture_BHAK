package com.bhak.project.configuration;

import com.bhak.project.entity.Credentials;
import com.bhak.project.entity.Role;
import com.bhak.project.entity.RoleType;
import com.bhak.project.entity.User;
import com.bhak.project.repository.RoleRepository;
import com.bhak.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// insere les roles et les comptes de demonstration au demarrage
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // creation des roles
        for (RoleType type : RoleType.values()) {
            if (!roleRepository.existsByName(type)) {
                Role role = new Role();
                role.setName(type);
                role.setDescription(type.getLabel());
                roleRepository.save(role);
                log.info("Role {} initialise", type.name());
            }
        }

        // compte admin de demonstration (id=1)
        if (userRepository.findByUsername("admin") == null) {
            Role adminRole = roleRepository.findByName(RoleType.ADMIN);
            User admin = new User();
            admin.setUsername("admin");
            admin.setRole(adminRole);

            Credentials adminCred = new Credentials();
            adminCred.setEmail("admin@bhak.com");
            adminCred.setPhoneNumber("+33600000001");
            adminCred.setPassword(passwordEncoder.encode("admin123"));
            adminCred.setUser(admin);
            admin.setCredentials(adminCred);

            userRepository.save(admin);
            log.info("Compte admin de demonstration cree (admin / admin123)");
        }

        // compte user de demonstration (id=2)
        if (userRepository.findByUsername("user1") == null) {
            Role userRole = roleRepository.findByName(RoleType.USER);
            User user = new User();
            user.setUsername("user1");
            user.setRole(userRole);

            Credentials userCred = new Credentials();
            userCred.setEmail("user1@bhak.com");
            userCred.setPhoneNumber("+33600000002");
            userCred.setPassword(passwordEncoder.encode("user123"));
            userCred.setUser(user);
            user.setCredentials(userCred);

            userRepository.save(user);
            log.info("Compte user de demonstration cree (user1 / user123)");
        }
    }
}
