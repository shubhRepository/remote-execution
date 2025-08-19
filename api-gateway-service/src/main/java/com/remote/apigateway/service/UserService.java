package com.remote.apigateway.service;

import com.remote.apigateway.model.User;
import com.remote.apigateway.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User save(User newUser) {
        return userRepository.save(newUser);
    }
}
