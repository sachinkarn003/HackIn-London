package com.karn01.authservice.config;

import com.karn01.authservice.entity.Role;
import com.karn01.authservice.entity.User;
import com.karn01.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(BootstrapAdminProperties.class)
public class AdminBootstrapConfig {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties adminProperties;

    @Bean
    public CommandLineRunner bootstrapAdminUser() {
        return args -> userRepository.findByEmail(adminProperties.email().trim().toLowerCase())
                .orElseGet(() -> {
                    User admin = new User();
                    admin.setName(adminProperties.name());
                    admin.setEmail(adminProperties.email().trim().toLowerCase());
                    admin.setPassword(passwordEncoder.encode(adminProperties.password()));
                    admin.setRole(Role.ADMIN);
                    return userRepository.save(admin);
                });
    }
}
