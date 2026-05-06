package com.library.identity.application;

import com.library.identity.application.dto.LoginRequest;
import com.library.identity.application.dto.LoginResponse;
import com.library.identity.application.dto.RegisterRequest;
import com.library.identity.application.dto.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
