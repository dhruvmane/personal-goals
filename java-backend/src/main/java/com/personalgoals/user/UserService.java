package com.personalgoals.user;

import com.personalgoals.auth.AuthService;
import com.personalgoals.common.ConflictException;
import com.personalgoals.common.Emails;
import com.personalgoals.common.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthService authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    public List<UserResponse> list() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    public UserResponse getById(UUID id) {
        return UserResponse.from(requireById(id));
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        User user = requireById(id);

        if (request.hasEmail()) {
            String email = Emails.normalize(request.email());
            userRepository.findByEmailIgnoreCase(email)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ConflictException("Email is already registered");
                    });
            user.changeEmail(email);
        }

        if (request.hasDisplayName()) {
            user.changeDisplayName(request.displayName().isBlank() ? null : request.displayName().trim());
        }

        if (request.hasPassword()) {
            user.changePasswordHash(passwordEncoder.encode(request.password()));
            // Any refresh token issued before this change must stop working immediately.
            authService.revokeAllForUser(id);
        }

        if (request.hasRole()) {
            user.changeRole(request.role());
        }

        return UserResponse.from(user);
    }

    @Transactional
    public void delete(UUID id) {
        User user = requireById(id);
        userRepository.delete(user);
    }

    public User requireById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
    }
}
