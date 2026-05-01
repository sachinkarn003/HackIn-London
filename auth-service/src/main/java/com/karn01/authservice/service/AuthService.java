package com.karn01.authservice.service;

import com.karn01.authservice.dto.LoginDto;
import com.karn01.authservice.dto.SignupDto;
import com.karn01.authservice.entity.Role;
import com.karn01.authservice.entity.User;
import com.karn01.authservice.exception.BadRequestException;
import com.karn01.authservice.exception.UnauthorizedException;
import com.karn01.authservice.repository.UserRepository;
import com.karn01.authservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public String signup(SignupDto dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()){
            throw new BadRequestException("Email already exists");
        }

        User user = new User();
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setRole(Role.CUSTOMER);

        userRepository.save(user);

        return "User registered successfully";
    }

    public String login(LoginDto dto){
        User user = userRepository.findByEmail(dto.getEmail().trim().toLowerCase()).orElseThrow(() -> new UnauthorizedException("Invalid email/password"));
        if (!passwordEncoder.matches(dto.getPassword(),user.getPassword())){
            throw new UnauthorizedException("Invalid email/password");
        }
        return jwtUtil.generateToken(user);
    }
}
