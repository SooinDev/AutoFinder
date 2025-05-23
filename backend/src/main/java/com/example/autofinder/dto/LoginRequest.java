package com.example.autofinder.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    private String username;
    private String password;
    private boolean rememberMe; // 로그인 유지 여부
}