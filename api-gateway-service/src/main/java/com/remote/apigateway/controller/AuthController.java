package com.remote.apigateway.controller;

import com.remote.apigateway.exception.InvalidCredentialsException;
import com.remote.apigateway.model.User;
import com.remote.apigateway.repository.UserRepository;
import com.remote.apigateway.security.JwtUtils;
import com.remote.apigateway.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(UserService userService, JwtUtils jwtUtils, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody User user) {
        userService.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        User existingUser = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Username or password is incorrect."));

        if (!passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
            throw new InvalidCredentialsException("Username or password is incorrect.");
        }

        String token = jwtUtils.generateToken(user.getUsername());
        return ResponseEntity.ok(token);
    }

    @GetMapping("/check-auth")
    public ResponseEntity<String> checkingAuthorization () {
        return ResponseEntity.badRequest().body("It doesn't works");
    }

}
