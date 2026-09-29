package com.personalgoals.auth;

import com.personalgoals.common.ConflictException;
import com.personalgoals.common.Emails;
import com.personalgoals.user.User;
import com.personalgoals.user.UserRepository;
import com.personalgoals.user.UserResponse;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenHasher tokenHasher;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenHasher tokenHasher) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenHasher = tokenHasher;
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

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = Emails.normalize(request.email());

        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        return issueTokens(user);
    }

    // A rejected refresh token must not roll back the session revocation that happens above:
    // that revocation is the whole point of detecting reuse, so it has to survive the 401.
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResponse refresh(RefreshRequest request) {
        UUID userId = parseRefreshSubject(request.refreshToken());

        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(tokenHasher.hash(request.refreshToken()))
                .orElseThrow(() -> new BadCredentialsException("Refresh token is not recognised"));

        if (stored.isRotated()) {
            // Replaying a rotated token means the original secret leaked. Drop every session
            // for this user so both the attacker and the victim have to sign in again.
            log.warn("Refresh token reuse detected for user {}; revoking all sessions", userId);
            refreshTokenRepository.revokeAllForUser(userId, Instant.now());
            throw new BadCredentialsException("Refresh token has already been used");
        }

        if (!stored.isActive()) {
            refreshTokenRepository.delete(stored);
            throw new BadCredentialsException("Refresh token has expired");
        }

        User user = userRepository
                .findById(userId)
                .filter(candidate -> candidate.getId().equals(stored.getUser().getId()))
                .orElseThrow(() -> new BadCredentialsException("Refresh token is no longer valid"));

        stored.rotate();
        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        try {
            parseRefreshSubject(request.refreshToken());
        } catch (BadCredentialsException ex) {
            // Signing out is best effort: an expired or forged token means there is nothing
            // left to revoke, so report success rather than surfacing a confusing error.
            return;
        }

        refreshTokenRepository
                .findByTokenHash(tokenHasher.hash(request.refreshToken()))
                .filter(RefreshToken::isActive)
                .ifPresent(RefreshToken::rotate);
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    private UUID parseRefreshSubject(String token) {
        try {
            return jwtService.parseUserId(token, JwtService.TYPE_REFRESH);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Refresh token is invalid or has expired");
        }
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        refreshTokenRepository.save(
                new RefreshToken(user, tokenHasher.hash(refreshToken), jwtService.refreshExpiry()));

        return AuthResponse.bearer(
                accessToken,
                jwtService.expiresInSeconds(),
                refreshToken,
                jwtService.refreshExpiresInSeconds(),
                UserResponse.from(user)
        );
    }
}
