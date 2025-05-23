package com.example.autofinder.service;

import com.example.autofinder.model.Role;
import com.example.autofinder.model.User;
import com.example.autofinder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 회원가입 메서드
     * @param username 사용자 아이디
     * @param password 비밀번호
     * @return 저장된 User 객체
     */
    public User registerUser(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 사용자 이름입니다.");
        }

        String encodedPassword = passwordEncoder.encode(password);
        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setRole(Role.USER); // 기본 역할 설정

        return userRepository.save(user);
    }

    /**
     * 사용자 인증 메서드
     * @param username 사용자 아이디
     * @param password 입력된 비밀번호
     * @return 인증된 User 객체
     */
    public User authenticateUser(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("해당 사용자 이름을 찾을 수 없습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");  // 예외 처리
        }

        return user;
    }
}