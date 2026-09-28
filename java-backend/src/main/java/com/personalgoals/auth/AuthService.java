package com.personalgoals.auth;

import com.personalgoals.common.ConflictException;
import com.personalgoals.common.Emails;
import com.personalgoals.user.User;
import com.personalgoals.user.UserRepository;
import com.personalgoals.user.UserResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = Emails.normalize(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }

        String displayName = request.displayName() == null || request.displayName().isBlank()
                ? null
                : request.displayName().trim();

        User user = userRepository.save(
                new User(email, passwordEncoder.encode(request.password()), displayName));

        return issueToken(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = Emails.normalize(request.email());

        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        return issueToken(user);
    }

    private AuthResponse issueToken(User user) {
        return AuthResponse.bearer(
                jwtService.generateToken(user),
                jwtService.expiresInSeconds(),
                UserResponse.from(user)
        );
    }
}
