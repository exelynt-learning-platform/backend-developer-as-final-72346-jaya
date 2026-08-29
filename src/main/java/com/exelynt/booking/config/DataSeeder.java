package com.exelynt.booking.config;

import com.exelynt.booking.entity.Resource;
import com.exelynt.booking.entity.Role;
import com.exelynt.booking.entity.User;
import com.exelynt.booking.repository.ResourceRepository;
import com.exelynt.booking.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seedData(UserRepository userRepository,
                               ResourceRepository resourceRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByEmail("admin@example.com")) {
                userRepository.save(new User(
                        "Admin User",
                        "admin@example.com",
                        passwordEncoder.encode("admin123"),
                        Role.ADMIN
                ));
            }
            if (!userRepository.existsByEmail("user@example.com")) {
                userRepository.save(new User(
                        "Regular User",
                        "user@example.com",
                        passwordEncoder.encode("user123"),
                        Role.USER
                ));
            }
            if (!userRepository.existsByEmail("user2@example.com")) {
                userRepository.save(new User(
                        "Second User",
                        "user2@example.com",
                        passwordEncoder.encode("user2123"),
                        Role.USER
                ));
            }
            if (resourceRepository.count() == 0) {
                resourceRepository.save(new Resource(
                        "Conference Room A",
                        "ROOM",
                        "Large meeting room with projector and video conferencing.",
                        new BigDecimal("50.00"),
                        true
                ));
                resourceRepository.save(new Resource(
                        "Company Vehicle",
                        "VEHICLE",
                        "Five-seat vehicle available for local business travel.",
                        new BigDecimal("25.00"),
                        true
                ));
                resourceRepository.save(new Resource(
                        "DSLR Camera Kit",
                        "EQUIPMENT",
                        "Camera body, lens, tripod, and carry case.",
                        new BigDecimal("15.50"),
                        true
                ));
            }
        };
    }
}
